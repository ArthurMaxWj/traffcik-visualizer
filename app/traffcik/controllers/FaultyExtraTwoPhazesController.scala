package traffcik.controllers

import traffcik.core.*
import traffcik.simulation.TrafficController

/** Similar to FullTwoPhazesController, but instead of changing time based on muber of vehicles, it
  * allows only a number of Vehicles based on time
  *
  * But it doesn't work however because it doesn't use LanesQue (because it can get easily bloced by
  * one lane)
  */
class FaultyExtraTwoPhazesController(timePerCar: Int, maxTime: Int)
    extends TrafficController[Boolean]:
  override def defaultInitialStoreVaule = true
  val minimumPhazeTime                  = 6 // no less that 6 (even when no cars)

  private def drectionsForPhaze(phaze: Boolean) =
    if phaze then (Direction.North, Direction.South)
    else (Direction.West, Direction.East)

  def processVehicles(
    vehicles: List[Vehicle],
    directions: (Direction, Direction)
  ): (List[Vehicle], List[Vehicle]) =
    def cond(dir: Direction) = dir == directions._1 || dir == directions._2
    def canDrive(v: Vehicle) = cond(v.startRoad) && cond(v.endRoad)

    def timeFor(v: Vehicle)               = timePerCar
    def canFit(v: Vehicle, timeLeft: Int) =
      timeLeft - timeFor(v) >= 0

    def loop(
      vehicles: List[Vehicle],
      passedVeh: List[Vehicle],
      omittedVeh: List[Vehicle],
      timeLeft: Int
    ): (List[Vehicle], List[Vehicle]) =
      vehicles match
        case v :: rest =>
          if canDrive(v) then
            if canFit(v, timeLeft) then
              loop(rest, v :: passedVeh, omittedVeh, timeLeft - timeFor(v))
            else (omittedVeh.reverse ++ vehicles, passedVeh.reverse)
          else loop(rest, passedVeh, v :: omittedVeh, timeLeft)
        case Nil => (omittedVeh.reverse ++ vehicles, passedVeh.reverse)
    loop(vehicles, List(), List(), maxTime)
  end processVehicles

  override def process(lq: LanesQue, phaze: Boolean): (LanesQue, StepResult, Boolean) =
    val vehicles = LanesQue.spitOut(lq)

    val time = 0 // we start each step from 0 seconds/time-units
    val dire = drectionsForPhaze(phaze)

    val (newVeh, removedVeh) =
      processVehicles(vehicles, dire)

    val len        = removedVeh.length * timePerCar
    val timePassed = if len > minimumPhazeTime then len else minimumPhazeTime

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
end FaultyExtraTwoPhazesController
