package com.crudapp.routes

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

object StaticRoutes {
  val routes: Route =
    pathEndOrSingleSlash {
      getFromResource("public/index.html")
    } ~
    getFromResourceDirectory("public")
}