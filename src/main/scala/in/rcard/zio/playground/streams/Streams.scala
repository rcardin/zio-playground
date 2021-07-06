package in.rcard.zio.playground.streams

import zio._
import zio.console.Console
import zio.{Chunk, ZIO}
import zio.stream.ZStream

import java.io.IOException

object Streams extends zio.App {

  val streamFromList: ZStream[Any, Nothing, Int] = ZStream.fromIterable(List(1, 2, 3, 4, 5, 6))
  val streamUsingApply: ZStream[Any, Nothing, Int] = ZStream(1, 2, 3, 4, 5, 6)

  val steamFromEffect: ZStream[Any, Nothing, Int] =
    ZStream.fromEffect(ZIO.succeed(42)) ++
    ZStream.fromEffect(ZIO.succeed(43))

  val streamFromRepeat: ZStream[Any, Nothing, Int] = ZStream.repeatEffect(ZIO.succeed(42))

//  trait Tweet
//  lazy val getTweets: ZIO[Any, Nothing, Chunk[Tweet]] = ???
//
//  val streamOfTweets: ZStream[Any, Nothing, Tweet] = ZStream.repeatEffectChunk(getTweets)

//  val zioFromDrainingStream: ZIO[Any, Nothing, Unit] = streamOfTweets.runDrain

  val foreachWithStreams: ZIO[Console, IOException, Unit] = for {
    x <- streamFromList
    y <- streamFromRepeat
  } console.putStrLn((x, y).toString())

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    streamFromList
      .mapM(n => console.putStrLn(n.toString))
      .runDrain
      .exitCode
  }
}
