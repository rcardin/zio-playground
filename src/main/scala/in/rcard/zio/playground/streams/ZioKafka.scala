package in.rcard.zio.playground.streams

import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{ExitCode, Has, RManaged, URIO, ZLayer}

object ZioKafka extends zio.App {

  case class Crypto(name: String, price: Double)

  val consumerSettings: ConsumerSettings =
    ConsumerSettings(List("localhost:9092"))
      .withGroupId("stocks-consumer")

  val managedConsumer: RManaged[Clock with Blocking, Consumer.Service] =
    Consumer.make(consumerSettings)

  val consumer: ZLayer[Clock with Blocking, Throwable, Has[Consumer.Service]] =
    ZLayer.fromManaged(managedConsumer)

  val stream: ZStream[Any with Consumer with Clock, Throwable, Unit] =
    Consumer.subscribeAnd(Subscription.topics("crypto"))
      .plainStream(Serde.string, Serde.double)
      .map(_.offset)
      .aggregateAsync(Consumer.offsetBatches) // ZTrasducer???
      .mapM(_.commit)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    stream.provideSomeLayer(consumer).runDrain.exitCode
}
