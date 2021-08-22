package in.rcard.zio.playground.akka.http.oneforge.domain

import java.time.OffsetDateTime

final case class Rate(
  pair: Rate.Pair,
  price: Price,
  timestamp: OffsetDateTime
)

object Rate {
  final case class Pair(from: Currency, to: Currency)
}
