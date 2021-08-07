package in.rcard.zio.playground.streams

import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.duration.durationInt
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}
import zio.kafka.consumer._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream.ZSink
import zio.{ExitCode, Has, RIO, RManaged, URIO, ZIO, ZLayer, console}

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

  val itaMatchesStreams: SubscribedConsumerFromEnvironment =
    Consumer.subscribeAnd(Subscription.pattern("updates|.*ITA.*".r))

  val partitionedMatchesStreams: SubscribedConsumerFromEnvironment =
    Consumer.subscribeAnd(Subscription.manual("updates", 1))

  val matchesStreams: ZIO[Console with Any with Consumer with Clock, Throwable, Unit] =
    Consumer.subscribeAnd(Subscription.topics("updates"))
      .plainStream(Serde.uuid, matchSerde)
      .map(cr => (cr.value.score, cr.offset))
      .tap { case (score, _) => console.putStrLn(s"| $score |") }
      .map { case (_, offset) => offset }
      .aggregateAsync(Consumer.offsetBatches)
      .run(ZSink.foreach(_.commit))

  val producerSettings: ProducerSettings = ProducerSettings(List("localhost:9092"))

  val producer: ZLayer[Blocking, Throwable, Producer[Any, UUID, Match]] =
    ZLayer.fromManaged(Producer.make[Any, UUID, Match](producerSettings, Serde.uuid, matchSerde))

  val itaEngFinalMatchScore: Match = Match(Array(Player("ITA", 3), Player("ENG", 2)))
  val messagesToSend: ProducerRecord[UUID, Match] =
    new ProducerRecord(
      "updates",
      UUID.fromString("b91a7348-f9f0-4100-989a-cbdd2a198096"),
      itaEngFinalMatchScore
  )

  val producerEffect: RIO[Producer[Any, UUID, Match], RecordMetadata] =
    Producer.produce[Any, UUID, Match](messagesToSend)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val program = for {
      _ <- matchesStreams.provideSomeLayer(consumer ++ zio.console.Console.live).fork
      _ <- producerEffect.provideSomeLayer(producer) *> ZIO.sleep(5.seconds)
    } yield ()
    program.exitCode
  }
}
