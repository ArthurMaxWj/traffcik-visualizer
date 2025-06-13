import org.scalatest.funspec.AnyFunSpec

import scala.util.Random

import traffcik.simulation.*
import traffcik.controllers.*
import traffcik.core.*


class TrafficControllerSpec extends AnyFunSpec:
    def runIt[T](taks: List[TrafficTask], tcon: TrafficController[T]) =
        TrafficManager(taks, dumbCon).run

    val allNames = List("Max", "Alex", "Leo", "Skoda", "Ferrari", "Bmw", "Cadilac")
    val allVehiclesTasks = allNames.map(name => AddVehicle(name, Direction.North, Direction.South))

    def randomIn[T](list: List[T]): T = 
        require(list.size > 0, "List can't be empty")
        list(Random.nextInt(list.size))

    def twoRandomIn[T](list: List[T]): List[T] = 
        val first = randomIn(list)
        val newList = list.filter(e => e != first)
        val second = randomIn(newList)
        List(first, second)

    val dumbCon = new DumbController
    val flushAllCon = new ClearAllController
    val protoTwoCon = new PrototypeTwoPhazesController

    describe("DumbController"):
        describe("given empty task list"):
            it("should return no StepResults"):
                val res = runIt(List(), dumbCon)

                assert(res.length == 0)
        describe("given one vehicle and step"):
            it("should return 1 result for 1 vahicle input"):
                val tasks = List(randomIn(allVehiclesTasks), Step())
                val res = runIt(tasks, dumbCon)

                assert(res.length == 1 && res(0).vehiclesLeft.length == 1)
        describe("given 2 vehicles and still 1 step"):
            it("should return 1 vehicle no matter the number of vehicles given"):
                val tasks = twoRandomIn(allVehiclesTasks) :+ Step()
                val res = runIt(tasks, dumbCon)

                assert(res.length == 1  && res(0).vehiclesLeft.length == 1)
        describe("given many vehicles and 2 steps"):
            it("should return one vehicle per each step "):
                val tasks = allVehiclesTasks :+ Step() :+ Step()
                val res = runIt(tasks, dumbCon)

                assert(res.length == 2 && res.map(_.vehiclesLeft.length) == List(1, 1))
        describe("given 7 vehicles but 8 steps"):
            it("should return vehicle for each step except the last one"):
                val veh = allVehiclesTasks
                val tasks = veh ++ List(Step(), Step(), Step(), Step(), Step(), Step(), Step(), Step())
                val res = runIt(tasks, dumbCon)

                assert(res.length == 8)
                assert(res.map(_.vehiclesLeft.length) == List(1, 1, 1, 1, 1, 1, 1, 0))

end TrafficControllerSpec