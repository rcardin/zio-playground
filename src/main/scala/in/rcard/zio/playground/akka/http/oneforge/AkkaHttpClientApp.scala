package in.rcard.zio.playground.akka.http.oneforge

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import in.rcard.zio.playground.akka.http.oneforge.Rate.Pair
import zio.console._
import zio.{ExitCode, Has, Managed, URIO, ZIO, ZLayer, ZManaged}

object AkkaHttpClientApp extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val managedActorSystem: Managed[Throwable, ActorSystem[Nothing]] =
      ZManaged.make {
        ZIO.effect(ActorSystem(Behaviors.empty, "AkkaHttpZio"))
      } { sys =>
        ZIO.effect(sys.terminate()).orDie
      }
    val layeredActorSystem: ZLayer[Any, Throwable, Has[ActorSystem[Nothing]]] =
      managedActorSystem.toLayer

    val app = for {
      rate <- OneForge.get(Pair(Currency.EUR, Currency.USD))
      _ <- putStrLn(s"The rate between EUR and USD is ${rate.price}")
    } yield ()
    val dependencies = (layeredActorSystem >>> OneForge.live) ++ Console.live

    app.provideSomeLayer(dependencies).exitCode
  }
}
