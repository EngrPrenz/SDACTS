package com.crudapp.models

// ── Domain models ────────────────────────────────────────────────────────────

case class User(id: Int, username: String, password: String)

case class Product(id: Int, name: String, price: BigDecimal)

// ── Request / Response DTOs ───────────────────────────────────────────────────

case class LoginRequest(username: String, password: String)
case class LoginResponse(token: String, username: String)

case class CreateUserRequest(username: String, password: String)
case class UpdateUserRequest(username: String, password: String)

case class CreateProductRequest(name: String, price: BigDecimal)
case class UpdateProductRequest(name: String, price: BigDecimal)

case class ApiResponse[T](success: Boolean, message: String, data: Option[T] = None)
case class ErrorResponse(success: Boolean = false, message: String)