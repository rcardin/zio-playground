package in.rcard.zio.playground.akka.http.oneforge

import zio.config.magnolia.DeriveConfigDescriptor

case class OneForgeConfig(client: Client)
object OneForgeConfig {
  val descriptor = DeriveConfigDescriptor.descriptor[OneForgeConfig]
}
case class Client(uri: String, apiKey: String)
