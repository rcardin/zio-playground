package in.rcard.zio.playground.akka.http

import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import zio._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}
import scala.util.control.NoStackTrace

/**
 * Client to the online service 1Forge.
 */
package object oneforge {

  sealed trait OneForgeError extends Throwable with NoStackTrace

  object OneForgeError {
    final case object Generic extends OneForgeError

    final case class System(underlying: Throwable) extends OneForgeError
  }

  object OneForge {

    type OneForge = Has[OneForge.Service]

    trait Service {
      def get(pair: Rate.Pair): IO[OneForgeError, Rate]
    }

    val live: ZLayer[Has[ActorSystem[Behavior[Nothing]]], Nothing, Has[Service]] =
      ZLayer.fromService[ActorSystem[Behavior[Nothing]], Service] { actorSystem =>
        new Service {

          implicit val sys = actorSystem
          implicit val executionContext: ExecutionContextExecutor = sys.executionContext

          override def get(pair: Rate.Pair): IO[OneForgeError, Rate] = {
            val params = Map(
              "pairs" -> s"${pair.from}/${pair.to}",
              "api_key" -> ""
            )
            val response = Http().singleRequest(
              HttpRequest(
                method = HttpMethods.GET,
                uri = Uri("https://api.1forge.com/quotes?").withQuery(Uri.Query(params))
              )
            )
            response.onComplete {
              case Success(value) => ???
              case Failure(exception) => ???
            }
          }
        }
      }
  }
}
