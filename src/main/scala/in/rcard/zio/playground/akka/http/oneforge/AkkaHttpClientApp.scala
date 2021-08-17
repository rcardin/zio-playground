package in.rcard.zio.playground.akka.http.oneforge

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import in.rcard.zio.playground.akka.http.oneforge.OneForge.OneForge
import in.rcard.zio.playground.akka.http.oneforge.Rate.Pair
import zio.{ExitCode, Has, Managed, UManaged, URIO, ZIO, ZLayer, ZManaged}

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

    val result: ZIO[OneForge, OneForgeError, Rate] = OneForge.get(Pair(Currency.EUR, Currency.USD))
    ???
  }
}
