package com.crudapp.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.crudapp.middleware.AuthMiddleware
import com.crudapp.models._
import com.crudapp.service.{AuthService, ProductService}

import scala.concurrent.ExecutionContext

class ProductRoutes(val authService: AuthService, productService: ProductService)
                   (implicit ec: ExecutionContext) extends JsonSupport with AuthMiddleware {

  val routes: Route =
    pathPrefix("api" / "products") {
      authenticated { _ =>
        concat(
          pathEnd {
            get {
              onSuccess(productService.getAllProducts()) { products =>
                complete(StatusCodes.OK -> ApiResponse(success = true, message = "Products retrieved", data = Some(products)))
              }
            } ~
            post {
              entity(as[CreateProductRequest]) { req =>
                onSuccess(productService.createProduct(req)) {
                  case Right(id) =>
                    complete(StatusCodes.Created -> ApiResponse[Int](success = true, message = s"Product created with id $id", data = Some(id)))
                  case Left(err) =>
                    complete(StatusCodes.BadRequest -> ErrorResponse(message = err))
                }
              }
            }
          },
          path(IntNumber) { id =>
            get {
              onSuccess(productService.getProductById(id)) {
                case Some(p) =>
                  complete(StatusCodes.OK -> ApiResponse(success = true, message = "Product found", data = Some(p)))
                case None =>
                  complete(StatusCodes.NotFound -> ErrorResponse(message = "Product not found"))
              }
            } ~
            put {
              entity(as[UpdateProductRequest]) { req =>
                onSuccess(productService.updateProduct(id, req)) {
                  case Right(_) =>
                    complete(StatusCodes.OK -> ApiResponse[String](success = true, message = "Product updated"))
                  case Left(err) =>
                    complete(StatusCodes.NotFound -> ErrorResponse(message = err))
                }
              }
            } ~
            delete {
              onSuccess(productService.deleteProduct(id)) {
                case Right(_) =>
                  complete(StatusCodes.OK -> ApiResponse[String](success = true, message = "Product deleted"))
                case Left(err) =>
                  complete(StatusCodes.NotFound -> ErrorResponse(message = err))
              }
            }
          }
        )
      }
    }
}