package com.crudapp.service

import com.crudapp.models.{LoginRequest, LoginResponse}
import com.crudapp.repository.UserRepository
import com.typesafe.config.ConfigFactory
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant

class AuthService(userRepository: UserRepository)(implicit ec: ExecutionContext) {

  private val config    = ConfigFactory.load()
  private val jwtSecret = config.getString("app.jwt-secret")
  private val jwtExpiry = config.getInt("app.jwt-expiry-hours")

  /** Login — supports both plain-text legacy passwords (e.g. "1234") AND
   *  bcrypt hashes so the existing admin account works out of the box. */
  def login(request: LoginRequest): Future[Option[LoginResponse]] =
    userRepository.findByUsername(request.username).map {
      case Some(user) =>
        val valid = if (user.password.startsWith("$2"))
          BCrypt.checkpw(request.password, user.password)
        else
          user.password == request.password  // plain-text fallback for legacy rows

        if (valid) {
          val claim = JwtClaim(
            content    = s"""{"username":"${user.username}","userId":${user.id}}""",
            expiration = Some(Instant.now.plusSeconds(jwtExpiry * 3600L).getEpochSecond),
            issuedAt   = Some(Instant.now.getEpochSecond)
          )
          val token = Jwt.encode(claim, jwtSecret, JwtAlgorithm.HS256)
          Some(LoginResponse(token, user.username))
        } else None
      case None => None
    }

  /** Validate JWT and extract username using simple regex — no extra JSON lib needed. */
  def validateToken(token: String): Option[String] =
    Jwt.decode(token, jwtSecret, Seq(JwtAlgorithm.HS256)).toOption.flatMap { claim =>
      // Extract "username":"value" from the JSON content string
      """"username"\s*:\s*"([^"]+)"""".r
        .findFirstMatchIn(claim.content)
        .map(_.group(1))
    }
}