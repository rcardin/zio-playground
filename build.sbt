name := "zio-playground"

version := "0.1"

scalaVersion := "2.13.6"

val ZioVersion = "1.0.9"
val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.6"

libraryDependencies ++= Seq(
  "dev.zio"           %% "zio"              % ZioVersion,
  "dev.zio"           %% "zio-streams"      % ZioVersion,
  "dev.zio"           %% "zio-kafka"        % "0.15.0",
  "dev.zio"           %% "zio-json"         % "0.1.5",
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream"      % AkkaVersion,
  "com.typesafe.akka" %% "akka-http"        % AkkaHttpVersion
)
