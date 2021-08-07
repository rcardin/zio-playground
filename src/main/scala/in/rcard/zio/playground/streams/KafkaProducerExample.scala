package in.rcard.zio.playground.streams
import org.apache.kafka.clients.producer.RecordMetadata
import zio.blocking.Blocking
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.{ExitCode, Has, RIO, URIO, ZLayer}

object KafkaProducerExample extends zio.App {

  val producerSettings: ProducerSettings = ProducerSettings(List("localhost:9092"))

  val producer: ZLayer[Blocking, Throwable, Producer[Any, String, String]] =
    ZLayer.fromManaged(Producer.make[Any, String, String](producerSettings, Serde.string, Serde.string))

  val effect: RIO[Producer[Any, String, String], RecordMetadata] =
    Producer.produce[Any, String, String]("topic", "key", "value")

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    effect.provideSomeLayer(producer).exitCode
  }
}
