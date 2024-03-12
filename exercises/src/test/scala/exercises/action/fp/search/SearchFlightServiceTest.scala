package exercises.action.fp.search

import exercises.action.DateGenerator._
import exercises.action.fp.IO
import exercises.action.fp.search.Airport._
import exercises.action.fp.search.SearchFlightGenerator._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.time.{Duration, Instant, LocalDate}
import scala.concurrent.ExecutionContext
import scala.util.Random

// Run the test using the green arrow next to class name (if using IntelliJ)
// or run `sbt` in the terminal to open it in shell mode, then type:
// testOnly exercises.action.fp.search.SearchFlightServiceTest
class SearchFlightServiceTest extends AnyFunSuite with ScalaCheckDrivenPropertyChecks {

  test("fromTwoClients example") {
    val now   = Instant.now()
    val today = LocalDate.now()

    val flight1 = Flight("1", "BA", parisOrly, londonGatwick, now, Duration.ofMinutes(100), 0, 89.5, "")
    val flight2 = Flight("2", "LH", parisOrly, londonGatwick, now, Duration.ofMinutes(105), 0, 96.5, "")
    val flight3 = Flight("3", "BA", parisOrly, londonGatwick, now, Duration.ofMinutes(140), 1, 234.0, "")
    val flight4 = Flight("4", "LH", parisOrly, londonGatwick, now, Duration.ofMinutes(210), 2, 55.5, "")

    val client1 = SearchFlightClient.constant(IO(List(flight3, flight1)))
    val client2 = SearchFlightClient.constant(IO(List(flight2, flight4)))

    val service = SearchFlightService.fromTwoClients(client1, client2)
    val result  = service.search(parisOrly, londonGatwick, today).unsafeRun()

    assert(result == SearchResult(List(flight1, flight2, flight3, flight4)))
  }


  test("fromTwoClients should handle errors gracefully") {
    val now = Instant.now()
    val today = LocalDate.now()

    val flight1 = Flight("1", "BA", parisOrly, londonGatwick, now, Duration.ofMinutes(100), 0, 89.5, "")
    val flight2 = Flight("2", "LH", parisOrly, londonGatwick, now, Duration.ofMinutes(105), 0, 96.5, "")
    val flight3 = Flight("3", "BA", parisOrly, londonGatwick, now, Duration.ofMinutes(140), 1, 234.0, "")
    val flight4 = Flight("4", "LH", parisOrly, londonGatwick, now, Duration.ofMinutes(210), 2, 55.5, "")

    val client1 = SearchFlightClient.constant(IO(List(flight3, flight1)))
    val client2 = SearchFlightClient.constant(IO.fail(new Exception("Boom")))

    val service = SearchFlightService.fromTwoClients(client1, client2)
    val result = service.search(parisOrly, londonGatwick, today).unsafeRun()

    assert(result == SearchResult(List(flight1, flight3)))
  }


    test("always return a successful result event if clients fail") {
  forAll{ (client1: SearchFlightClient, client2: SearchFlightClient) =>
      val today = LocalDate.now()
      val service = SearchFlightService.fromTwoClients(client1, client2)


      val result = service.search(parisOrly, londonGatwick, today).attempt

      assert(result.unsafeRun().isSuccess)
    }
  }

  test("fromClients should handle multiple clients gracefully") {
  forAll{
    (clientList: List[SearchFlightClient]) =>
      val today = LocalDate.now()
      val service = SearchFlightService.fromClients(clientList)

      val result = service.search(parisOrly, londonGatwick, today).attempt.unsafeRun()

      assert(result.isSuccess)
  }
  }

  //harcoded example
  //NOTE you can use arbitrary as implicit or explicit gen
  test("fromClients should get the data from multiple clients"){
    forAll(Gen.listOf(flightGen), Gen.listOf(flightGen), Gen.listOf(flightGen)){ (flights1: List[Flight] , flights2 : List[Flight], flights3: List[Flight]) =>

      val client1 = SearchFlightClient.constant(IO(flights1))
      val client2 = SearchFlightClient.constant(IO(flights2))
      val client3 = SearchFlightClient.constant(IO(flights3))

      val today = LocalDate.now()
      val service = SearchFlightService.fromClients(List(client1,client2,client3))

      val result = service.search(parisOrly, londonGatwick, today).unsafeRun()

      assert(result == SearchResult(flights1 ++ flights2 ++flights3))
    }
  }

  test("fromClients - clieints order doesn't matter"){
    forAll {
      (clientList: List[SearchFlightClient]) =>
        val today = LocalDate.now()
        val service1 = SearchFlightService.fromClients(clientList)
        val service2 = SearchFlightService.fromClients(Random.shuffle(clientList))

        val result1 = service1.search(parisOrly, londonGatwick, today).unsafeRun()
        val result2 = service2.search(parisOrly, londonGatwick, today).unsafeRun()
        assert(result1 == result2)
    }
  }


}
