package com.crudapp.service

import com.crudapp.models._
import com.crudapp.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.{ExecutionContext, Future}

class UserService(userRepository: UserRepository)(implicit ec: ExecutionContext) {

  def getAllUsers(): Future[Seq[User]] = userRepository.findAll()

  def getUserById(id: Int): Future[Option[User]] = userRepository.findById(id)

  def createUser(req: CreateUserRequest): Future[Either[String, Int]] =
    userRepository.findByUsername(req.username).flatMap {
      case Some(_) => Future.successful(Left("Username already exists"))
      case None =>
        val hashed = BCrypt.hashpw(req.password, BCrypt.gensalt())
        userRepository.create(User(0, req.username, hashed)).map(Right(_))
    }

  def updateUser(id: Int, req: UpdateUserRequest): Future[Either[String, Int]] =
    userRepository.findById(id).flatMap {
      case None => Future.successful(Left("User not found"))
      case Some(_) =>
        val hashed = BCrypt.hashpw(req.password, BCrypt.gensalt())
        userRepository.update(id, req.username, hashed).map(rows => Right(rows))
    }

  def deleteUser(id: Int): Future[Either[String, Int]] =
    userRepository.findById(id).flatMap {
      case None    => Future.successful(Left("User not found"))
      case Some(_) => userRepository.delete(id).map(rows => Right(rows))
    }
}