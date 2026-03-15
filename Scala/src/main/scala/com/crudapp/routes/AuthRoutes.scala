package com.crudapp.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.crudapp.models._
import com.crudapp.service.AuthService

import scala.concurrent.ExecutionContext

class AuthRoutes(authService: AuthService)(implicit ec: ExecutionContext) extends JsonSupport {

  val routes: Route =
    pathPrefix("api" / "auth") {
      path("login") {
        post {
          entity(as[LoginRequest]) { req =>
            onSuccess(authService.login(req)) {
              case Some(response) =>
                complete(StatusCodes.OK -> ApiResponse(success = true, message = "Login successful", data = Some(response)))
              case None =>
                complete(StatusCodes.Unauthorized -> ErrorResponse(message = "Invalid username or password"))
            }
          }
        }
      }
    }
}