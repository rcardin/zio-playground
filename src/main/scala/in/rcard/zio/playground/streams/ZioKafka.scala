package in.rcard.zio.playground.streams

import zio.blocking.Blocking
import zio.clock.Clock
import zio.{ExitCode, Has, RManaged, URIO, ZIO, ZLayer, console}
import zio.console.Console
import zio.kafka.consumer.{CommittableRecord, Consumer, ConsumerSettings, SubscribedConsumerFromEnvironment, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

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
  case class Players(p1: String, p2: String)

  case class Match(players: String, score: String)

  val playersSerde: Serde[Any, Players] = Serde.string.inmapM { playersAsString =>
    ZIO.effect {
      if (!playersAsString.matches("...-...")) {
        throw new IllegalArgumentException(s"$playersAsString doesn't represents two players")
      }
      val split = playersAsString.split("-")
      Players(split(0), split(1))
    }
  } { players =>
    ZIO.succeed {
      s"${players.p1}-${players.p2}"
    }
  }

  val consumerSettings: ConsumerSettings =
    ConsumerSettings(List("localhost:9092"))
      .withGroupId("stocks-consumer")

  val managedConsumer: RManaged[Clock with Blocking, Consumer.Service] =
    Consumer.make(consumerSettings)

  val consumer: ZLayer[Clock with Blocking, Throwable, Has[Consumer.Service]] =
    ZLayer.fromManaged(managedConsumer)

  val matchesStreams: ZStream[Consumer, Throwable, CommittableRecord[String, String]] =
    Consumer.subscribeAnd(Subscription.topics("updates"))
      .plainStream(Serde.string, Serde.string)

  val itaMatchesStreams: SubscribedConsumerFromEnvironment =
    Consumer.subscribeAnd(Subscription.pattern("updates|.*ITA.*".r))

  val partitionedMatchesStreams: SubscribedConsumerFromEnvironment =
    Consumer.subscribeAnd(Subscription.manual("updates", 1))

  val stream: ZStream[Console with Consumer with Clock, Throwable, Unit] =
    Consumer.subscribeAnd(Subscription.topics("updates"))
      .plainStream(Serde.string, Serde.string)
      .tap(cr => console.putStrLn(s"| ${cr.key} | ${cr.value} |"))
      .map(_.offset)
      .aggregateAsync(Consumer.offsetBatches)
      .mapM(_.commit)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    stream.provideSomeLayer(consumer ++ zio.console.Console.live).runDrain.exitCode
}
