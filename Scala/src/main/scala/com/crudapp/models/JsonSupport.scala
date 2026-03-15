package com.crudapp.models

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  // BigDecimal support
  implicit object BigDecimalFormat extends JsonFormat[BigDecimal] {
    def write(bd: BigDecimal): JsValue = JsNumber(bd)
    def read(json: JsValue): BigDecimal = json match {
      case JsNumber(n) => n
      case JsString(s) => BigDecimal(s)
      case _           => deserializationError("BigDecimal expected")
    }
  }

  implicit val userFormat: RootJsonFormat[User]                         = jsonFormat3(User)
  implicit val productFormat: RootJsonFormat[Product]                   = jsonFormat3(Product)
  implicit val loginRequestFormat: RootJsonFormat[LoginRequest]         = jsonFormat2(LoginRequest)
  implicit val loginResponseFormat: RootJsonFormat[LoginResponse]       = jsonFormat2(LoginResponse)
  implicit val createUserFormat: RootJsonFormat[CreateUserRequest]      = jsonFormat2(CreateUserRequest)
  implicit val updateUserFormat: RootJsonFormat[UpdateUserRequest]      = jsonFormat2(UpdateUserRequest)
  implicit val createProductFormat: RootJsonFormat[CreateProductRequest] = jsonFormat2(CreateProductRequest)
  implicit val updateProductFormat: RootJsonFormat[UpdateProductRequest] = jsonFormat2(UpdateProductRequest)
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse]       = jsonFormat2(ErrorResponse)

  // Generic ApiResponse — we write it manually because Scala 2 macros can't
  // derive it for a type-parametric case class automatically.
  implicit def apiResponseFormat[T: JsonFormat]: RootJsonFormat[ApiResponse[T]] =
    new RootJsonFormat[ApiResponse[T]] {
      def write(r: ApiResponse[T]): JsValue = JsObject(
        "success" -> JsBoolean(r.success),
        "message" -> JsString(r.message),
        "data"    -> r.data.toJson
      )
      def read(json: JsValue): ApiResponse[T] = {
        val fields = json.asJsObject.fields
        ApiResponse(
          success = fields("success").convertTo[Boolean],
          message = fields("message").convertTo[String],
          data    = fields.get("data").flatMap(_.convertTo[Option[T]])
        )
      }
    }
}