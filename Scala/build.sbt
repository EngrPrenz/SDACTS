name := "scala-crud-app"
version := "1.0.0"
scalaVersion := "2.13.12"

val AkkaVersion = "2.8.5"
val AkkaHttpVersion = "10.5.3"

libraryDependencies ++= Seq(
  // Akka HTTP
  "com.typesafe.akka" %% "akka-actor-typed"         % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream"               % AkkaVersion,
  "com.typesafe.akka" %% "akka-http"                 % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json"      % AkkaHttpVersion,

  // MSSQL JDBC Driver
  "com.microsoft.sqlserver" % "mssql-jdbc" % "12.4.2.jre11",

  // Slick for DB
  "com.typesafe.slick" %% "slick"           % "3.4.1",
  "com.typesafe.slick" %% "slick-hikaricp"  % "3.4.1",

  // Password Hashing
  "org.mindrot" % "jbcrypt" % "0.4",

  // JWT  (jwt-core only — no spray-json integration needed)
  "com.github.jwt-scala" %% "jwt-core" % "9.4.4",

  // Logging
  "ch.qos.logback" % "logback-classic" % "1.4.11",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",

  // Config
  "com.typesafe" % "config" % "1.4.3"
)

Compile / mainClass := Some("com.crudapp.Main")

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf"              => MergeStrategy.concat
  case x                             => MergeStrategy.first
}
