import org.scalatest.funspec.AnyFunSpec

import scala.util.Random
import traffcik.core.*


class LanesQueSpec extends AnyFunSpec:

    val allDirections = List(Direction.North, Direction.South, Direction.East, Direction.West)
    
    val allNames = List("Max", "Alex", "Leo", "Skoda", "Ferrari", "Bmw", "Cadilac")
    val allVehicles = allNames.map(name => Vehicle(name, Direction.North, Direction.South))

    def randomIn[T](list: List[T]): T = 
        require(list.size > 0, "List can't be empty")
        list(Random.nextInt(list.size))
    describe("LanesQue"):
        describe("hasNext"):
            describe("when empty"):
                it("should return true"):
                    val dir = randomIn(allDirections)
                    val lq = new LanesQue()

                    assert(!lq.hasNext(dir))
            describe("when non-empty"):
                it("should return true"):
                    val dir = randomIn(allDirections)
                    val mapp = Map(dir -> List(randomIn(allVehicles)))

                    val lq = new LanesQue(mapp)

                    assert(lq.hasNext(dir))
        describe("hasNextIn"):
            describe("when empty"):
                it("should return true"):
                    val dir = randomIn(allDirections)
                    val lq = new LanesQue()

                    assert(!lq.hasNextIn(List(dir)))
            describe("when non-empty"):
                it("should return true"):
                    val dir = randomIn(allDirections)
                    val mapp = Map(dir -> List(randomIn(allVehicles)))

                    val lq = new LanesQue(mapp)

                    assert(lq.hasNextIn(List(dir)))
    // TODO: add more tests here
        

end LanesQueSpec