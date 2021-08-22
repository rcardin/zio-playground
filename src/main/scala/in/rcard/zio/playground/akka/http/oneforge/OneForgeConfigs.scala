package in.rcard.zio.playground.akka.http.oneforge

import zio.config.ConfigDescriptor._
import zio.config._

object OneForgeConfigs {
  case class Client(uri: String, apiKey: String)
  object Client {
    val descriptor = ConfigDescriptor.nested("oneforge") {
      ConfigDescriptor.nested("client") {
        (string("uri") |@| string("api-key")) (
          Client.apply,
          Client.unapply
        )
      }
    }
  }
}

//case class OneForgeConfig(client: Client)
//object OneForgeConfig {
//  val descriptor = ConfigDescriptor.nested("client")
//}
//case class Client(uri: String, apiKey: String)
//object Client {
//  val descriptor = DeriveConfigDescriptor.descriptor[Client]
//}
