package in.rcard.zio.playground.streams

import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.json._
import zio.kafka.consumer._
import zio.kafka.serde.Serde
import zio.stream.ZSink

import scala.util.{Failure, Success}

// Commands for Kafka broker
//
// docker exec -it broker bash
//
// kafka-topics \
//   --bootstrap-server localhost:9092 \
//   --topic updates \
//   --create
//
// kafka-console-producer \
//   --topic updates \
//   --broker-list localhost:9092 \
//   --property parse.key=true \
//   --property key.separator=,
//
object ZioKafkaFinal extends zio.App {

  case class Player(name: String, score: Int) {
    override def toString: String = s"$name: $score"
  }

  object Player {
    implicit val decoder: JsonDecoder[Player] = DeriveJsonDecoder.gen[Player]
    implicit val encoder: JsonEncoder[Player] = DeriveJsonEncoder.gen[Player]
  }

  case class Match(players: Array[Player]) {
    def score: String = s"${players(0)} - ${players(1)}"
  }

  object Match {
    implicit val decoder: JsonDecoder[Match] = DeriveJsonDecoder.gen[Match]
    implicit val encoder: JsonEncoder[Match] = DeriveJsonEncoder.gen[Match]
  }

  val matchSerde: Serde[Any, Match] = Serde.string.inmapM { matchAsString =>
    ZIO.fromEither(matchAsString.fromJson[Match].left.map(new RuntimeException(_)))
  } { matchAsObj =>
    ZIO.effect(matchAsObj.toJson)
  }

  val consumerSettings: ConsumerSettings =
    ConsumerSettings(List("localhost:9092"))
      .withGroupId("updates-consumer")

  val managedConsumer: RManaged[Clock with Blocking, Consumer.Service] =
    Consumer.make(consumerSettings)

  val consumer: ZLayer[Clock with Blocking, Throwable, Consumer] =
    ZLayer.fromManaged(managedConsumer)

  val matchesStreams: ZIO[Console with Any with Consumer with Clock, Throwable, Unit] =
    Consumer.subscribeAnd(Subscription.topics("updates"))
      .plainStream(Serde.uuid, matchSerde.asTry)
      .map(cr => (cr.value, cr.offset))
      .tap { case (tryMatch, _) =>
        tryMatch match {
          case Success(matchz) => console.putStrLn(s"| ${matchz.score} |")
          case Failure(ex) => console.putStrLn(s"Poison pill ${ex.getMessage}")
        }
      }
      .map { case (_, offset) => offset }
      .aggregateAsync(Consumer.offsetBatches)
      .run(ZSink.foreach(_.commit))

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    matchesStreams.provideSomeLayer(consumer ++ zio.console.Console.live).exitCode
}
