package traffcik.controllers

import traffcik.core.*
import traffcik.simulation.TrafficController
import traffcik.simulation.InitialStates

import scala.math.*

/** Similar to LeftTurnController, but now handles probability of crosswalks and takes functions as
  * arguments.
  *
  * @param isCrossingGreenDynamic
  *   takes hour and reutrns if crossings fired, can use random side-effects
  * @param hourInUnits
  *   used to compute units
  * @param cutCrossingDelay
  *   added time for green on crossing
  *
  * @param maxTime
  *   (same as LeftTurnController) its set to 5 if less (necessary for cars to move)
  * @param leftTurnTime
  *   (same as LeftTurnController) used on left turns instead of timePerCar
  */
class DynamicController(
  isCrossingGreenDynamic: (Int) => Boolean,
  hourInUnits: Int,
  cutCrossingDelay: Int,
  timePerCar: Int,
  leftTurnTime: Int,
  maxTime: Int,
  startHour: Int = 0
) extends TrafficController[(Boolean, Int)]:
  override def initialTrafficState      = InitialStates.alsoTurnLeft
  override def defaultInitialStoreVaule = (true, 0)
  val minimumPhazeTime                  = 5 // no less that 5 (or cars wont move)
  val totalTime                         = Math.max(maxTime, minimumPhazeTime)

  val greenStartTime = 2 // we wait when yellow
  val greenEndTime   = 2 // we stop when red soon

  def timeFor(cutTime: Int, v: Vehicle) =
    if Turn.of(v.startRoad, v.endRoad) == Turn.Forward then timePerCar
    else // left turn (ignore Right/Back turns)
      leftTurnTime + cutTime

  def canFit(cutTime: Int, v: Vehicle, timeLeft: Int) =
    timeLeft - timeFor(cutTime, v) >= 0

  def drectionsForPhaze(phaze: Boolean) =
    if phaze then (Direction.North, Direction.South)
    else (Direction.West, Direction.East)

  type Flushes = List[(Int, Direction, Direction, Int)]
  private def generateFlushes(
    removedVeh: List[Vehicle],
    time: Int,
    timelineDirection: Direction,
    cutTime: Int
  ) =
    def loop(vehicles: List[Vehicle], currentTime: (Int, Int), acc: Flushes): Flushes =
      vehicles match
        case v :: rest =>
          val timeline        = v.startRoad == timelineDirection
          val ctime           = Concurrent.get(currentTime, timeline)
          val currentTimeNext = Concurrent.set(ctime + timeFor(cutTime, v), currentTime, timeline)
          loop(
            rest,
            currentTimeNext,
            (ctime, v.startRoad, v.endRoad, 1) :: acc
          )
        case Nil => acc.reverse
    val res = loop(removedVeh, (greenStartTime, greenStartTime), List())
    res
  end generateFlushes

  private def processVehicles(
    lanesque: LanesQue,
    directions: (Direction, Direction),
    cutTime: Int
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

        val notSwappedPriority = lq.hasNext(nextInLine)
        val timeline           = priority && notSwappedPriority
        val tleft              = Concurrent.get(timeLeft, timeline)
        val timeLeftNext       = Concurrent.set(tleft - timeFor(cutTime, v), timeLeft, timeline)

        if canFit(cutTime, v, tleft) then loop(newLQ, v :: passedVeh, timeLeftNext, !priority)
        else (lq, passedVeh.reverse)
      else (lq, passedVeh.reverse)
    end loop
    val timeLeftForBoth = totalTime - greenStartTime - greenEndTime
    loop(lanesque, List(), (timeLeftForBoth, timeLeftForBoth), true)
  end processVehicles

  override def process(
    lq: LanesQue,
    store: (Boolean, Int)
  ): (LanesQue, StepResult, (Boolean, Int)) =
    val (phaze, simTimeCounter) = store
    val simTime                 = simTimeCounter + (startHour * hourInUnits)
    val time                    = 0 // we start each step from 0 seconds/time-units
    val dire                    = drectionsForPhaze(phaze)

    val enableCorssings = isCrossingGreenDynamic((simTime / hourInUnits) % 24)
    val cutTime         = if enableCorssings then cutCrossingDelay else 0

    val (newLQ, removedVeh) =
      processVehicles(lq, dire, cutTime)

    val timePassed = totalTime // TODO: we could alter it, but fixed for now

    val other = drectionsForPhaze(!phaze)

    // we dont set 'other' directions here casue we init them at start then just swap phazes
    val lightsData = List(
      (time, dire._1, dire._2, Light.Yellow),
      (time, dire._2, dire._1, Light.Yellow),
      (time, dire._1, Turn.leftOf(dire._1), Light.Yellow),
      (time, dire._2, Turn.leftOf(dire._2), Light.Yellow),
      (time + greenStartTime, dire._1, dire._2, Light.Green),
      (time + greenStartTime, dire._2, dire._1, Light.Green),
      (time + greenStartTime, dire._1, Turn.leftOf(dire._1), Light.Green),
      (time + greenStartTime, dire._2, Turn.leftOf(dire._2), Light.Green),
      (timePassed - greenEndTime, dire._1, dire._2, Light.Yellow),
      (timePassed - greenEndTime, dire._2, dire._1, Light.Yellow),
      (timePassed - greenEndTime, dire._1, Turn.leftOf(dire._1), Light.Yellow),
      (timePassed - greenEndTime, dire._2, Turn.leftOf(dire._2), Light.Yellow),
      (timePassed, dire._1, dire._2, Light.Red),
      (timePassed, dire._2, dire._1, Light.Red),
      (timePassed, dire._1, Turn.leftOf(dire._1), Light.Red),
      (timePassed, dire._2, Turn.leftOf(dire._2), Light.Red)
    )

    val crossingsData = List(
      (time, other._1, enableCorssings),
      (time, other._2, enableCorssings),
      (time, dire._1, false),
      (time, dire._2, false)
    )

    val flushes = generateFlushes(removedVeh, timePassed, dire._1, cutTime)

    (
      newLQ,
      StepResult(removedVeh, timePassed, lightsData, crossingsData, flushes),
      (!phaze, simTime + timePassed)
    )
  end process
end DynamicController
