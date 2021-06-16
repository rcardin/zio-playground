package in.rcard.zio.playground.fibers

import zio._

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

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    sequentialWakeUpRoutine().exitCode
}
