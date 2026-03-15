package com.crudapp.middleware

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import com.crudapp.models.{ErrorResponse, JsonSupport}
import com.crudapp.service.AuthService

trait AuthMiddleware extends JsonSupport {

  def authService: AuthService

  def authenticated: Directive1[String] =
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(header) if header.startsWith("Bearer ") =>
        val token = header.drop(7)
        authService.validateToken(token) match {
          case Some(username) => provide(username)
          case None =>
            complete(StatusCodes.Unauthorized -> ErrorResponse(message = "Invalid or expired token"))
        }
      case _ =>
        complete(StatusCodes.Unauthorized -> ErrorResponse(message = "Missing Authorization header"))
    }
}