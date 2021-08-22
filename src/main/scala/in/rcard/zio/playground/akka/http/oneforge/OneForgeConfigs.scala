package in.rcard.zio.playground.akka.http.oneforge

import zio.config.ConfigDescriptor._
import zio.config._

// Configuration of the application. I cannot find any example of automatic derivation
// using zio-config-yaml :(
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
