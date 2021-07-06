package in.rcard.zio.playground.streams

import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.serde.Serde
import zio.{ExitCode, Has, RManaged, URIO, ZLayer}

object ZioKafka extends zio.App {

  case class Stock(name: String, price: Double)

  val consumerSettings: ConsumerSettings =
    ConsumerSettings(List("localhost:9092"))
      .withGroupId("stocks-consumer")

  val managedConsumer: RManaged[Clock with Blocking, Consumer.Service] =
    Consumer.make(consumerSettings)

  val consumer: ZLayer[Clock with Blocking, Throwable, Has[Consumer.Service]] =
    ZLayer.fromManaged(managedConsumer)

  Consumer.subscribeAnd(Subscription.topics("stocks"))
    .plainStream(Serde.string, Serde.double)
    .map(record => Stock(record.key, record.value))
//    .map()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = ???
}
