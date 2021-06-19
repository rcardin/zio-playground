package in.rcard.zio.playground.fibers

import zio._
import zio.clock.Clock
import zio.duration.durationInt

object FibersTutorial extends zio.App {

  def printThread = s"[${Thread.currentThread().getName}]"

  val bathTime = ZIO.succeed("Going to the bathroom")
  val boilingWater = ZIO.succeed("Boiling some water")
  val preparingCoffee = ZIO.succeed("Preparing the coffee")

  def sequentialWakeUpRoutine(): ZIO[Any, Nothing, Unit] = for {
    _ <- bathTime.debug(printThread)
    _ <- boilingWater.debug(printThread)
    _ <- preparingCoffee.debug(printThread)
  } yield ()

  def concurrentBathroomTimeAndBoilingWater(): ZIO[Any, Nothing, Unit] = for {
    _ <- bathTime.debug(printThread).fork
    _ <- boilingWater.debug(printThread)
  } yield ()

  def concurrentWakeUpRoutine(): ZIO[Any, Nothing, Unit] = for {
    bathFiber <- bathTime.debug(printThread).fork
    boilingFiber <- boilingWater.debug(printThread).fork
    zippedFiber = bathFiber.zip(boilingFiber)
    result <- zippedFiber.join.debug(printThread)
    _ <- ZIO.succeed(s"$result...done").debug(printThread) *> preparingCoffee.debug(printThread)
  } yield ()

  val aliceCalling = ZIO.succeed("Alice's call")
  val boilingWaterWithSleep =
    boilingWater.debug(printThread) *>
      ZIO.sleep(5.seconds) *>
      ZIO.succeed("Boiled water ready")

  def concurrentWakeUpRoutineWithAliceCall(): ZIO[Clock, Nothing, Unit] = for {
    _ <- bathTime.debug(printThread)
    boilingFiber <- boilingWaterWithSleep.fork
    _ <- aliceCalling.debug(printThread).fork *> boilingFiber.interrupt.debug(printThread)
    _ <- ZIO.succeed("Going to the Cafe with Alice").debug(printThread)
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    concurrentWakeUpRoutineWithAliceCall().exitCode
}
