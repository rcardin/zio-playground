package in.rcard.zio.playground.akka.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import de.heikoseeberger.akkahttpziojson.ZioJsonSupport
import zio._
import zio.json.{DeriveJsonDecoder, JsonDecoder, jsonField}

import java.time.OffsetDateTime
import scala.concurrent.Future
import scala.util.control.NoStackTrace

/**
 * Client to the online service 1Forge.
 */
package object oneforge {

  object OneForge {

    type OneForge = Has[OneForge.Service]

    trait Service {
      def get(pair: Rate.Pair): IO[OneForgeError, Rate]
    }

    val live: ZLayer[Has[ActorSystem[Nothing]], Nothing, Has[Service]] =
      ZLayer.fromService[ActorSystem[Nothing], Service] { actorSystem =>
        new Service {

          implicit val sys = actorSystem
          implicit val executionContext = sys.executionContext

          import ZioJsonSupport._

          override def get(pair: Rate.Pair): IO[OneForgeError, Rate] = {
            val params = Map(
              "pairs" -> s"${pair.from}/${pair.to}",
              "api_key" -> "b7mNj3MOKQJ0HJPVOssc82QRN2xJgykS" // TODO Move into external configuration
            )
            val response = Http().singleRequest(
              HttpRequest(
                method = HttpMethods.GET,
                uri = Uri("https://api.1forge.com/quotes?").withQuery(Uri.Query(params))
              )
            ).flatMap {
              httpResponse =>
                val oneForgeRate: Future[List[OneForgeRate]] = Unmarshal(httpResponse).to[List[OneForgeRate]]
                oneForgeRate.map(rateList => {
                  Rate(
                    pair,
                    Price(BigDecimal.decimal(rateList.head.price)),
                    OffsetDateTime.now()
                  )
                })
            }
            ZIO.fromFuture(_ => response).mapError { ex =>
              // TODO Use a logger
              System.out.println(s"Error in calling OneForge API: ${ex}")
              OneForgeError.System(ex)
            }
          }
        }
      }

    // Accessor method
    def get(pair: Rate.Pair): ZIO[OneForge, OneForgeError, Rate] =
      ZIO.accessM[OneForge](_.get.get(pair))
  }

  sealed trait OneForgeError extends Throwable with NoStackTrace

  object OneForgeError {
    final case object Generic extends OneForgeError

    final case class System(underlying: Throwable) extends OneForgeError
  }

  private case class OneForgeRate(
    @jsonField("s") symbol: String,
    @jsonField("p") price: Double,
    @jsonField("b") bid: Double,
    @jsonField("a") ask: Double,
    @jsonField("t") timestamp: Long
  )

  private object OneForgeRate {
    implicit val oneForgeRateDecoder: JsonDecoder[OneForgeRate] =
      DeriveJsonDecoder.gen[OneForgeRate]
  }
}
