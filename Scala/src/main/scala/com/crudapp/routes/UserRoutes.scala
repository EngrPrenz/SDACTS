package com.crudapp.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.crudapp.middleware.AuthMiddleware
import com.crudapp.models._
import com.crudapp.service.{AuthService, UserService}

import scala.concurrent.ExecutionContext

class UserRoutes(val authService: AuthService, userService: UserService)
                (implicit ec: ExecutionContext) extends JsonSupport with AuthMiddleware {

  val routes: Route =
    pathPrefix("api" / "users") {
      authenticated { _ =>
        concat(
          // GET /api/users
          pathEnd {
            get {
              onSuccess(userService.getAllUsers()) { users =>
                complete(StatusCodes.OK -> ApiResponse(success = true, message = "Users retrieved", data = Some(users)))
              }
            } ~
            // POST /api/users
            post {
              entity(as[CreateUserRequest]) { req =>
                onSuccess(userService.createUser(req)) {
                  case Right(id) =>
                    complete(StatusCodes.Created -> ApiResponse[Int](success = true, message = s"User created with id $id", data = Some(id)))
                  case Left(err) =>
                    complete(StatusCodes.Conflict -> ErrorResponse(message = err))
                }
              }
            }
          },
          // GET /api/users/:id
          path(IntNumber) { id =>
            get {
              onSuccess(userService.getUserById(id)) {
                case Some(user) =>
                  complete(StatusCodes.OK -> ApiResponse(success = true, message = "User found", data = Some(user)))
                case None =>
                  complete(StatusCodes.NotFound -> ErrorResponse(message = "User not found"))
              }
            } ~
            // PUT /api/users/:id
            put {
              entity(as[UpdateUserRequest]) { req =>
                onSuccess(userService.updateUser(id, req)) {
                  case Right(_) =>
                    complete(StatusCodes.OK -> ApiResponse[String](success = true, message = "User updated"))
                  case Left(err) =>
                    complete(StatusCodes.NotFound -> ErrorResponse(message = err))
                }
              }
            } ~
            // DELETE /api/users/:id
            delete {
              onSuccess(userService.deleteUser(id)) {
                case Right(_) =>
                  complete(StatusCodes.OK -> ApiResponse[String](success = true, message = "User deleted"))
                case Left(err) =>
                  complete(StatusCodes.NotFound -> ErrorResponse(message = err))
              }
            }
          }
        )
      }
    }
}