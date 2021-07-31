package in.rcard.zio.playground.streams

import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}
import zio.kafka.consumer._
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{ExitCode, Has, RManaged, URIO, ZIO, ZLayer, console}

import java.util.UUID

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
object ZioKafka extends zio.App {

  // {ITA-ENG, 1-1}
  case class Teams(p1: String, p2: String)
  case class Score(p1: Int, p2: Int)

  // {
  //    players: [
  //      {
  //        "name": "ITA",
  //        "score": 3
  //      },
  //      {
  //        "name": "ENG",
  //        "score": 2
  //      }
  //    ]
  // }
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

  val teamsSerde: Serde[Any, Teams] = Serde.string.inmapM { teamsAsString =>
    ZIO.effect {
      if (!teamsAsString.matches("...-...")) {
        throw new IllegalArgumentException(s"$teamsAsString doesn't represents two teams")
      }
      val split = teamsAsString.split("-")
      Teams(split(0), split(1))
    }
  } { teams =>
    ZIO.succeed {
      s"${teams.p1}-${teams.p2}"
    }
  }

  val matchSerde: Serde[Any, Match] = Serde.string.inmapM { matchAsString =>
    ZIO.fromEither(matchAsString.fromJson[Match].left.map(new RuntimeException(_)))
  } { matchAsObj =>
    ZIO.effect(matchAsObj.toJson)
  }

  val consumerSettings: ConsumerSettings =
    ConsumerSettings(List("localhost:9092"))
      .withGroupId("stocks-consumer")

  val managedConsumer: RManaged[Clock with Blocking, Consumer.Service] =
    Consumer.make(consumerSettings)

  val consumer: ZLayer[Clock with Blocking, Throwable, Has[Consumer.Service]] =
    ZLayer.fromManaged(managedConsumer)

  val matchesStreams: ZStream[Consumer, Throwable, CommittableRecord[UUID, Match]] =
    Consumer.subscribeAnd(Subscription.topics("updates"))
      .plainStream(Serde.uuid, matchSerde)

  val itaMatchesStreams: SubscribedConsumerFromEnvironment =
    Consumer.subscribeAnd(Subscription.pattern("updates|.*ITA.*".r))

  val partitionedMatchesStreams: SubscribedConsumerFromEnvironment =
    Consumer.subscribeAnd(Subscription.manual("updates", 1))

  val stream: ZStream[Console with Consumer with Clock, Throwable, Unit] =
    Consumer.subscribeAnd(Subscription.topics("updates-json"))
      .plainStream(Serde.uuid, matchSerde)
      .tap(cr => console.putStrLn(s"| ${cr.key} | ${cr.value.score} |"))
      .map(_.offset)
      .aggregateAsync(Consumer.offsetBatches)
      .mapM(_.commit)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    stream.provideSomeLayer(consumer ++ zio.console.Console.live).runDrain.exitCode
}
