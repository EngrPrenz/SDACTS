package com.crudapp.repository

import com.crudapp.models.User
import slick.jdbc.SQLServerProfile.api._
import scala.concurrent.Future

class UserTable(tag: Tag) extends Table[User](tag, "Users") {
  def id       = column[Int]("Id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("Username")
  def password = column[String]("Password")
  def *        = (id, username, password) <> (User.tupled, User.unapply)
}

class UserRepository {
  import DatabaseConfig.db

  val users = TableQuery[UserTable]

  def findAll(): Future[Seq[User]] =
    db.run(users.result)

  def findById(id: Int): Future[Option[User]] =
    db.run(users.filter(_.id === id).result.headOption)

  def findByUsername(username: String): Future[Option[User]] =
    db.run(users.filter(_.username === username).result.headOption)

  def create(user: User): Future[Int] =
    db.run((users returning users.map(_.id)) += user)

  def update(id: Int, username: String, password: String): Future[Int] =
    db.run(users.filter(_.id === id).map(u => (u.username, u.password)).update((username, password)))

  def delete(id: Int): Future[Int] =
    db.run(users.filter(_.id === id).delete)
}