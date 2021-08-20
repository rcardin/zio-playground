name := "zio-playground"

version := "0.1"

scalaVersion := "2.13.6"

val ZioVersion = "1.0.9"
val ZioConfigVersion = "1.0.6"
val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.6"

libraryDependencies ++= Seq(
  "dev.zio"           %% "zio"                 % ZioVersion,
  "dev.zio"           %% "zio-streams"         % ZioVersion,
  "dev.zio"           %% "zio-kafka"           % "0.15.0",
  "dev.zio"           %% "zio-json"            % "0.1.5",
  "dev.zio"           %% "zio-logging-slf4j"   % "0.5.11",
  "dev.zio"           %% "zio-config"          % ZioConfigVersion,
  "dev.zio"           %% "zio-config-magnolia" % ZioConfigVersion,
  "dev.zio"           %% "zio-config-yaml"     % ZioConfigVersion,
  "com.typesafe.akka" %% "akka-actor-typed"    % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream"         % AkkaVersion,
  "com.typesafe.akka" %% "akka-http"           % AkkaHttpVersion,
  "de.heikoseeberger" %% "akka-http-zio-json"  % "1.36.0",
  "ch.qos.logback"     % "logback-classic"     % "1.2.3"
)
