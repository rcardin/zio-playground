package in.rcard.zio.playground.akka.http.oneforge

import java.time.OffsetDateTime

final case class Rate(
  pair: Rate.Pair,
  price: Price,
  timestamp: OffsetDateTime
)

object Rate {
  final case class Pair(from: Currency, to: Currency)
}
