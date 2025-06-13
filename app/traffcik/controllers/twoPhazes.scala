package traffcik.controllers

import traffcik.core.*
import traffcik.simulation.TrafficController

/** FLushes all cars from North to South and South to North, then does the same in next phaze but
  * for Weast-East axis
  */
class PrototypeTwoPhazesController extends TrafficController[Boolean]:
  override def process(lq: LanesQue, phaze: Boolean) =
    val vehicles = LanesQue.spitOut(lq)
    if phaze then // North-South
      def cond(dir: Direction) = dir == Direction.North || dir == Direction.South
      val (removedVeh, newVeh) = vehicles.partition(v => cond(v.startRoad) && cond(v.endRoad))

      (LanesQue.slurpIn(newVeh), StepResult(removedVeh), !phaze)
    else // West-East
      def cond(dir: Direction) = dir == Direction.East || dir == Direction.West
      val (removedVeh, newVeh) = vehicles.partition(v => cond(v.startRoad) && cond(v.endRoad))

      (LanesQue.slurpIn(newVeh), StepResult(removedVeh), !phaze)
  override def defaultInitialStoreVaule = true

/** Same as PrototypeTwoPhazesController */
class FullTwoPhazesController(minimumGreenTime: Int, timePerCar: Int)
    extends TrafficController[Boolean]:
  override def defaultInitialStoreVaule = true

  private def drectionsForPhaze(phaze: Boolean) =
    if phaze then (Direction.North, Direction.South)
    else (Direction.West, Direction.East)

  override def process(lq: LanesQue, phaze: Boolean): (LanesQue, StepResult, Boolean) =
    val time                 = 0 // we start each step from 0 seconds/time-units
    val dire                 = drectionsForPhaze(phaze)
    def cond(dir: Direction) = dir == dire._1 || dir == dire._2

    val (removedVeh, newVeh): (List[Vehicle], List[Vehicle]) =
      LanesQue.spitOut(lq).partition(v => cond(v.startRoad) && cond(v.endRoad))

    val len        = removedVeh.length * timePerCar
    val timePassed = if len > minimumGreenTime then len else minimumGreenTime

    val other = drectionsForPhaze(!phaze)

    // we dont set 'other' directions here casue we init them at start then just swap phazes
    val lightsData = List(
      (time, dire._1, dire._2, Light.Yellow),
      (time, dire._2, dire._1, Light.Yellow),
      (time + 2, dire._1, dire._2, Light.Green),
      (time + 2, dire._2, dire._1, Light.Green),
      (timePassed - 2, dire._1, dire._2, Light.Yellow),
      (timePassed - 2, dire._2, dire._1, Light.Yellow),
      (timePassed, dire._1, dire._2, Light.Red),
      (timePassed, dire._2, dire._1, Light.Red)
    )

    val crossingsData = List(
      (time, other._1, true),
      (time, other._2, true),
      (time, dire._1, false),
      (time, dire._2, false)
    )

    val flushes = List(
      (time + 2, dire._1, dire._2, removedVeh.filter(_.startRoad == dire._1).length),
      (time + 2, dire._2, dire._1, removedVeh.filter(_.startRoad == dire._2).length)
    )

    (
      LanesQue.slurpIn(newVeh),
      StepResult(removedVeh, timePassed, lightsData, crossingsData, flushes),
      !phaze
    )
  end process
end FullTwoPhazesController
