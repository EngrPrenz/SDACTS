package com.crudapp.service

import com.crudapp.models._
import com.crudapp.repository.ProductRepository

import scala.concurrent.{ExecutionContext, Future}

class ProductService(productRepository: ProductRepository)(implicit ec: ExecutionContext) {

  def getAllProducts(): Future[Seq[Product]] = productRepository.findAll()

  def getProductById(id: Int): Future[Option[Product]] = productRepository.findById(id)

  def createProduct(req: CreateProductRequest): Future[Either[String, Int]] =
    productRepository.create(Product(0, req.name, req.price)).map(Right(_))

  def updateProduct(id: Int, req: UpdateProductRequest): Future[Either[String, Int]] =
    productRepository.findById(id).flatMap {
      case None    => Future.successful(Left("Product not found"))
      case Some(_) => productRepository.update(id, req.name, req.price).map(rows => Right(rows))
    }

  def deleteProduct(id: Int): Future[Either[String, Int]] =
    productRepository.findById(id).flatMap {
      case None    => Future.successful(Left("Product not found"))
      case Some(_) => productRepository.delete(id).map(rows => Right(rows))
    }
}