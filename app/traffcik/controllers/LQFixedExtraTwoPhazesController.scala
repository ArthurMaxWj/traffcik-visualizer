package traffcik.controllers

import traffcik.core.*
import traffcik.simulation.TrafficController

import scala.math.*

/** A fixed version of FaultyExtraTwoPhazesController, using LanesQue fixes the order of cars.
  *
  * @param maxTime
  *   its set to 5 if less (necessary for cars to move)
  */
class LQFixedExtraTwoPhazesController(timePerCar: Int, maxTime: Int)
    extends TrafficController[Boolean]:
  override def defaultInitialStoreVaule = true
  val minimumPhazeTime                  = 5 // no less that 5 (or cars wont move)
  val totalTime                         = Math.max(maxTime, minimumPhazeTime)

  val greenStartTime = 2 // we wait when yellow
  val greenEndTime   = 2 // we stop when red soon

  def timeFor(v: Vehicle)               = timePerCar
  def canFit(v: Vehicle, timeLeft: Int) =
    timeLeft - timeFor(v) >= 0

  def drectionsForPhaze(phaze: Boolean) =
    if phaze then (Direction.North, Direction.South)
    else (Direction.West, Direction.East)

  type Flushes = List[(Int, Direction, Direction, Int)]
  private def generateFlushes(removedVeh: List[Vehicle], time: Int) =
    def loop(
      vehicles: List[Vehicle],
      currentTime: (Int, Int),
      timeline: Boolean,
      acc: Flushes
    ): Flushes =
      vehicles match
        case v :: rest =>
          val ctime           = Concurrent.get(currentTime, timeline)
          val currentTimeNext = Concurrent.set(ctime + timeFor(v), currentTime, timeline)
          loop(
            rest,
            currentTimeNext,
            !timeline,
            (ctime, v.startRoad, v.endRoad, 1) :: acc
          )
        case Nil => acc.reverse
    val res = loop(removedVeh, (greenStartTime, greenStartTime), true, List())
    res
  end generateFlushes

  private def processVehicles(
    lanesque: LanesQue,
    directions: (Direction, Direction)
  ): (LanesQue, List[Vehicle]) =
    def loop(
      lq: LanesQue,
      passedVeh: List[Vehicle],
      timeLeft: (Int, Int),
      priority: Boolean
    ): (LanesQue, List[Vehicle]) =
      val dire       = List(directions._1, directions._2)
      val nextInLine = if priority then directions._1 else directions._2

      if lq.hasNextIn(dire) then
        val (v, newLQ) = lq.withDeparture(nextInLine, dire)

        val tleft        = Concurrent.get(timeLeft, priority)
        val timeLeftNext = Concurrent.set(tleft - timeFor(v), timeLeft, priority)

        if canFit(v, tleft) then loop(newLQ, v :: passedVeh, timeLeftNext, !priority)
        else (lq, passedVeh.reverse)
      else (lq, passedVeh.reverse)
    val timeLeftForBoth = totalTime - greenStartTime - greenEndTime
    loop(lanesque, List(), (timeLeftForBoth, timeLeftForBoth), true)
  end processVehicles

  override def process(lq: LanesQue, phaze: Boolean): (LanesQue, StepResult, Boolean) =
    val time = 0 // we start each step from 0 seconds/time-units
    val dire = drectionsForPhaze(phaze)

    val (newLQ, removedVeh) =
      processVehicles(lq, dire)

    val timePassed = totalTime // TODO: we could alter it, but fixed for now

    val other = drectionsForPhaze(!phaze)

    // we dont set 'other' directions here casue we init them at start then just swap phazes
    val lightsData = List(
      (time, dire._1, dire._2, Light.Yellow),
      (time, dire._2, dire._1, Light.Yellow),
      (time + greenStartTime, dire._1, dire._2, Light.Green),
      (time + greenStartTime, dire._2, dire._1, Light.Green),
      (timePassed - greenEndTime, dire._1, dire._2, Light.Yellow),
      (timePassed - greenEndTime, dire._2, dire._1, Light.Yellow),
      (timePassed, dire._1, dire._2, Light.Red),
      (timePassed, dire._2, dire._1, Light.Red)
    )

    val crossingsData = List(
      (time, other._1, true),
      (time, other._2, true),
      (time, dire._1, false),
      (time, dire._2, false)
    )

    val flushes = generateFlushes(removedVeh, timePassed)

    (newLQ, StepResult(removedVeh, timePassed, lightsData, crossingsData, flushes), !phaze)
  end process
end LQFixedExtraTwoPhazesController
