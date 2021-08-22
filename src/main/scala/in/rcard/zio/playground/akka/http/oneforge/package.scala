package in.rcard.zio.playground.akka.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import de.heikoseeberger.akkahttpziojson.ZioJsonSupport
import zio._
import zio.json.{DeriveJsonDecoder, JsonDecoder, jsonField}
import zio.logging.{Logger, Logging}

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}
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

    val live: ZLayer[Has[ActorSystem[Nothing]] with Logging with Has[OneForgeConfigs.Client], Nothing, Has[Service]] =
      ZLayer.fromServices[ActorSystem[Nothing], Logger[String], OneForgeConfigs.Client, Service] {
        (actorSystem, log, config) =>
        new Service {

          implicit val sys = actorSystem
          implicit val executionContext = sys.executionContext

          override def get(pair: Rate.Pair): IO[OneForgeError, Rate] = {
            val params = Map(
              "pairs" -> s"${pair.from}/${pair.to}",
              "api_key" -> config.apiKey
            )
            val response = callOneForge(config, params).flatMap {
              transformHttpResponseToRate(pair)
            }
            ZIO.fromFuture(_ => response).mapError { ex =>
              log.error("Error calling the 1Forge API", Cause.Fail(ex))
              OneForgeError.System(ex)
            }
          }
        }
      }

    private def callOneForge(config: OneForgeConfigs.Client, params: Map[String, String])(implicit sys: ActorSystem[Nothing]) = {
      Http().singleRequest(
        HttpRequest(
          method = HttpMethods.GET,
          uri = Uri(config.uri).withQuery(Uri.Query(params))
        )
      )
    }

    private def transformHttpResponseToRate(pair: Rate.Pair)
                      (implicit sys: ActorSystem[Nothing], ec: ExecutionContext) = {
      (httpResponse: HttpResponse) =>

        import ZioJsonSupport._

        val oneForgeRate: Future[List[OneForgeRate]] = Unmarshal(httpResponse).to[List[OneForgeRate]]
        oneForgeRate.map(rateList => {
          Rate(
            pair,
            Price(BigDecimal.decimal(rateList.head.price)),
            OffsetDateTime.now()
          )
        })
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
