package in.rcard.zio.playground.akka.http.oneforge

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import in.rcard.zio.playground.akka.http.oneforge.Rate.Pair
import zio.clock.Clock
import zio.config.yaml.YamlConfig
import zio.console._
import zio.logging.Logging
import zio.{ExitCode, Has, Managed, URIO, ZIO, ZLayer, ZManaged}

import scala.io.Source

object AkkaHttpClientApp extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    // Very easy use case with only one call to the client
    val app = for {
      rate <- OneForge.get(Pair(Currency.EUR, Currency.USD))
      _    <- putStrLn(s"The rate between EUR and USD is ${rate.price}")
    } yield ()

    app.provideSomeLayer(environment()).exitCode
  }

  private def environment(): ZLayer[Any with Console with Clock, Throwable, Has[OneForge.Service] with Console] = {
    val actorSystem: ZLayer[Any, Throwable, Has[ActorSystem[Nothing]]] = {
      lazy val akkaStart = ZIO.effect(ActorSystem(Behaviors.empty, "AkkaHttpZio"))
      lazy val akkaStop = (sys: ActorSystem[Nothing]) => ZIO.effect(sys.terminate()).orDie
      ZManaged.make(akkaStart)(akkaStop)
    }.toLayer

    val configLayer = YamlConfig.fromString(
      Source.fromResource("application.yml").mkString,
      OneForgeConfigs.Client.descriptor
    )

    val logging = Logging.console() >>> Logging.withRootLoggerName("akka-http-client-app")

    ((actorSystem ++ logging ++ configLayer) >>> OneForge.live) ++ Console.live
  }
}
