package com.crudapp.repository

import slick.jdbc.SQLServerProfile.api._

object DatabaseConfig {
  // lazy — won't attempt to connect until the first query is made,
  // so the HTTP server starts successfully even if the DB is not yet reachable.
  lazy val db: Database = Database.forConfig("database.db")
}