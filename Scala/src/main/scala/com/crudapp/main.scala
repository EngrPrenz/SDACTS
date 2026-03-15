package com.crudapp

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import com.crudapp.models.{ErrorResponse, JsonSupport}
import com.crudapp.repository.{ProductRepository, UserRepository}
import com.crudapp.routes.{AuthRoutes, ProductRoutes, StaticRoutes, UserRoutes}
import com.crudapp.service.{AuthService, ProductService, UserService}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Success}

object Main extends App with LazyLogging with JsonSupport {

  println("==============================================")
  println("  ScalaDB CRUD Manager - Starting up...")
  println("==============================================")

  implicit val system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "crud-system")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  val userRepository    = new UserRepository
  val productRepository = new ProductRepository
  val authService       = new AuthService(userRepository)
  val userService       = new UserService(userRepository)
  val productService    = new ProductService(productRepository)

  implicit val exHandler: ExceptionHandler = ExceptionHandler {
    case ex: Exception =>
      logger.error("Unhandled exception", ex)
      complete(
        HttpResponse(StatusCodes.InternalServerError,
          entity = HttpEntity(ContentTypes.`application/json`,
            ErrorResponse(message = s"Internal server error: ${ex.getMessage}").toJson.compactPrint))
      )
  }

  def corsHeaders(inner: Route): Route =
    respondWithHeaders(
      akka.http.scaladsl.model.headers.RawHeader("Access-Control-Allow-Origin",  "*"),
      akka.http.scaladsl.model.headers.RawHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"),
      akka.http.scaladsl.model.headers.RawHeader("Access-Control-Allow-Headers", "Content-Type,Authorization")
    )(inner)

  val authRoutes    = new AuthRoutes(authService)
  val userRoutes    = new UserRoutes(authService, userService)
  val productRoutes = new ProductRoutes(authService, productService)

  val allRoutes: Route = corsHeaders {
    handleExceptions(exHandler) {
      options { complete(StatusCodes.OK) } ~
      authRoutes.routes    ~
      userRoutes.routes    ~
      productRoutes.routes ~
      StaticRoutes.routes
    }
  }

  val config = ConfigFactory.load()
  val host   = config.getString("app.host")
  val port   = config.getInt("app.port")

  Http().newServerAt(host, port).bind(allRoutes).onComplete {
    case Success(binding) =>
      println("==============================================")
      println("  SERVER IS RUNNING!")
      println("  Open this in your browser:")
      println(s"  >>> http://localhost:$port <<<")
      println("  Press ENTER to stop the server.")
      println("==============================================")
      logger.info(s"Server bound to ${binding.localAddress}")
    case Failure(ex) =>
      println(s"[FATAL] Failed to bind to port $port: ${ex.getMessage}")
      logger.error("Failed to start server", ex)
      system.terminate()
  }

  // Block the main thread forever — keeps the server alive under sbt run
  Await.result(system.whenTerminated, Duration.Inf)
}