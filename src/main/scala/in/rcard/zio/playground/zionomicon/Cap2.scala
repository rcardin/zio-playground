package in.rcard.zio.playground.zionomicon

import zio.{Task, ZIO}

import scala.io.{BufferedSource, Source}

object Cap2 {

  object Ex1 {
    // Implement a ZIO version of the function readFile by using the ZIO.effect constructor
    def readFileZio(file: String): Task[String] = ZIO.effect(readFile(file))

    def readFile(file: String): String = {
      val source: BufferedSource = Source.fromFile(file)

      try source.getLines.mkString finally source.close()
    }
  }

  object Ex2 {
    // Implement a ZIO version of the writeFile by using the ZIO.effect constructor
    def writeFileZio(file: String, text: String): Task[Unit] = ZIO.effect(writeFile(file, text))

    def writeFile(file: String, text: String): Unit = {
      import java.io._
      val pw = new PrintWriter(new File(file))
      try pw.write(text) finally pw.close()
    }
  }

  object Ex3 {
    // Using the flatMap method of the ZIO effect, together with the readFileZio and writeFileZio
    // functions that you wrote, implement a ZIO version of the function copyFile
    import Ex1._
    import Ex2._
    def copyFile(source: String, dest: String): Unit = {
      val contents = readFile(source)
      writeFile(dest, contents)
    }

    def copyFileZio(source: String, dest: String): ZIO[Any, Throwable, Unit] =
      for {
        contents <- readFileZio(source)
        _ <- writeFileZio(dest, contents)
      } yield ()
  }

  object Ex4 {
    // Rewrite the following ZIO code that uses flatMap into a for-comprehension
    def printLine(line: String): Task[Unit] = ZIO.effect(println(line))
    val readLine: Task[String] = ZIO.effect(scala.io.StdIn.readLine())

    printLine("What's you name?").flatMap(_ =>
      readLine.flatMap(name =>
        printLine(s"Hello, $name!")
      )
    )

    for {
      _ <- printLine("What's you name?")
      name <- readLine
      _ <- printLine(s"Hello, $name!")
    } yield ()
  }
}
