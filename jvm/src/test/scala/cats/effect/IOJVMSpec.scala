/*
 * Copyright 2017 Daniel Spiewak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats
package effect

import org.scalatest._

import scala.concurrent.ExecutionContext

import java.{util => ju}
import java.util.concurrent.{AbstractExecutorService, TimeUnit}

class IOJVMSpec extends FunSuite with Matchers {

  val ThreadName = "test-thread"

  val TestES = new AbstractExecutorService {
    def execute(r: Runnable): Unit = {
      new Thread {
        setName(ThreadName)
        start()

        override def run() = r.run()
      }
    }

    // Members declared in java.util.concurrent.ExecutorService
    def awaitTermination(time: Long, unit: TimeUnit): Boolean = true
    def isShutdown(): Boolean = true
    def isTerminated(): Boolean = true
    def shutdown(): Unit = ()
    def shutdownNow(): ju.List[Runnable] = new ju.ArrayList[Runnable]
  }

  val TestEC = ExecutionContext.fromExecutorService(TestES)

  test("shift contiguous prefix and suffix, but not interfix") {
    val name: IO[String] = IO { Thread.currentThread().getName() }

    val aname: IO[String] = IO async { cb =>
      new Thread {
        start()

        override def run() = cb(Right(Thread.currentThread().getName()))
      }
    }

    val test = for {
      n1 <- name
      n2 <- name
      n3 <- aname
      n4 <- name
      n5 <- name.shift(TestEC)
      n6 <- name
    } yield (n1, n2, n3, n4, n5, n6)

    val (n1, n2, n3, n4, n5, n6) = test.shift(TestEC).unsafeRunSync()

    n1 shouldEqual ThreadName
    n2 shouldEqual ThreadName
    n3 should not equal ThreadName
    n4 should not equal ThreadName
    n5 shouldEqual ThreadName
    n6 shouldEqual ThreadName
  }
}
