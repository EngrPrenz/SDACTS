package com.crudapp.repository

import com.crudapp.models.Product
import slick.jdbc.SQLServerProfile.api._
import scala.concurrent.Future

class ProductTable(tag: Tag) extends Table[Product](tag, "Products") {
  def id    = column[Int]("Id", O.PrimaryKey, O.AutoInc)
  def name  = column[String]("Name")
  def price = column[BigDecimal]("Price")
  def *     = (id, name, price) <> (Product.tupled, Product.unapply)
}

class ProductRepository {
  import DatabaseConfig.db

  val products = TableQuery[ProductTable]

  def findAll(): Future[Seq[Product]] =
    db.run(products.result)

  def findById(id: Int): Future[Option[Product]] =
    db.run(products.filter(_.id === id).result.headOption)

  def create(product: Product): Future[Int] =
    db.run((products returning products.map(_.id)) += product)

  def update(id: Int, name: String, price: BigDecimal): Future[Int] =
    db.run(products.filter(_.id === id).map(p => (p.name, p.price)).update((name, price)))

  def delete(id: Int): Future[Int] =
    db.run(products.filter(_.id === id).delete)
}