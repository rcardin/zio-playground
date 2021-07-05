name := "zio-playground"

version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "1.0.9",
  "dev.zio" %% "zio-streams" % "1.0.9",
  "dev.zio" %% "zio-kafka"   % "0.15.0"
)
