import org.scalatest.funspec.AnyFunSpec

import scala.util.Random

import traffcik.simulation.TrafficManager
import traffcik.controllers.*
import traffcik.core.*


class TrafficManagerSpec extends AnyFunSpec:
    val allNames = List("Max", "Alex", "Leo", "Skoda", "Ferrari", "Bmw", "Cadilac")

    def randomIn[T](list: List[T]): T = 
        require(list.size > 0, "List can't be empty")
        list(Random.nextInt(list.size))

    val dumbCon = new DumbController
    val flushAllCon = new ClearAllController
    val protoTwoCon = new PrototypeTwoPhazesController
    
    describe("TrafficManager"):

        describe("when given empty task list"):
            it("should return no StepResults"):
                val res = TrafficManager(List(), dumbCon).run

                assert(res.length == 0)

            it("should not need new keywoard to be instantiated"):
                val tm1 = new TrafficManager(List(), dumbCon, ()) // we added optional new keywoard
                val tm2 = TrafficManager(List(), dumbCon, ())

                assert(tm1.run == tm2.run)
            
            it("can find initialStoreValue by itself"):
                val tm1 = TrafficManager(List(), dumbCon, ()) // we added optional initialStore value
                val tm2 = TrafficManager(List(), dumbCon)

                assert(tm1.run == tm2.run)

        describe("when given one addvehicle and step tasks"):
            it("should return one StepResult with its vehicleId in it"):
                val name = randomIn(allNames)

                val tasks = List(AddVehicle(name, Direction.North, Direction.South), Step())
                val res = TrafficManager(tasks, dumbCon).run

                assert(res.length == 1 && res(0).vehiclesLeft.length == 1)
                assert(res(0).vehiclesLeft(0).vehicleId == name)
            
        describe("when given many addVehicles and step tasks"):
            it("can fluash all tasks in one step (with ClearAllController)"):
                val tasks = allNames.map(vid => AddVehicle(vid, Direction.North, Direction.South)) :+ Step()
                val res = TrafficManager(tasks, flushAllCon).run

                assert(res.length == 1 && res(0).vehiclesLeft.length == 7)
                assert(res(0).vehiclesLeft.map(_.vehicleId) == allNames)
        describe("when given complex case"):
            it("processes it step by step (with PrototypeTwoPhazesController)"):
                val tasks = List(
                    AddVehicle("skoda", Direction.North, Direction.South),
                    AddVehicle("cadilac", Direction.East, Direction.West),
                    AddVehicle("bmw", Direction.North, Direction.South),
                    AddVehicle("mustang", Direction.East, Direction.West),
                    AddVehicle("ferrari", Direction.East, Direction.West),
                    Step(), Step(), Step()
                )
                val res = TrafficManager(tasks, protoTwoCon).run

                assert(res.length == 3)
                assert( res.map(_.vehiclesLeft.length) == List(2, 3, 0))
end TrafficManagerSpec