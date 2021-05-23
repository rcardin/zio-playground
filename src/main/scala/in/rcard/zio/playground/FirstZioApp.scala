package in.rcard.zio.playground
import zio.console._
import zio.{ExitCode, IO, Task, UIO, URIO, ZIO}

import java.io.IOException

object FirstZioApp extends zio.App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    appLogic().exitCode

  def appLogic(): ZIO[Console, IOException, Unit] =
    for {
      _ <- putStrLn("Hello! What's your name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello, $name, welcome to the ZIO world!")
    } yield ()

  val meaningOfLife: UIO[Int] = ZIO.succeed(42)
  val now: UIO[Long] = ZIO.effectTotal(System.currentTimeMillis())

  val f1: IO[String, Nothing] = ZIO.fail("Uh oh!")
  val f2: Task[Nothing] = Task.fail(new Exception("Uh oh!"))

  val zioOption: IO[Option[Nothing], Int] = ZIO.fromOption(Some(42))
}

// ZIO[R, E, A] is effectful version of the function R => Either[E, A]
// UIO[A]     = ZIO[Any, Nothing, A]
// URIO[R, A] = ZIO[R, Nothing, A]
// TaskA[A]   = ZIO[Any, Throwable, A]
// RIO[R, A]  = ZIO[R, Throwable, A]
// IO[E, A]   = ZIO[Any, E, A]
