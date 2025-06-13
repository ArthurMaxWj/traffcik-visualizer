package traffcik.core

import traffcik.core.*

import Direction.*

/** Sorts four directions into queues. helps controllers flush all sides equally
  *
  * LanesQue is short of Queue of Lanes. names for short after Tarasque (french dragon)
  */
class LanesQue(q: Map[Direction, List[Vehicle]] = Map()):
  val emptyQueue =
    Map(
      North -> List(),
      South -> List(),
      East  -> List(),
      West  -> List()
    )
  val queues = emptyQueue ++ q

  def withArrival(vehicles: List[Vehicle]): LanesQue = // TODO: refactor this (maybe not?)
    val north                                    = vehicles.filter(v => v.startRoad == North)
    val south                                    = vehicles.filter(v => v.startRoad == South)
    val west                                     = vehicles.filter(v => v.startRoad == West)
    val east                                     = vehicles.filter(v => v.startRoad == East)
    val newqueues: Map[Direction, List[Vehicle]] = (
      (((queues + (North -> (queues(North) ++ north)))
        + (South         -> (queues(South) ++ south)))
        + (West          -> (queues(West) ++ west)))
        + (East          -> (queues(East) ++ east))
    )
    new LanesQue(newqueues)

  def hasNext(direction: Direction) =
    queues(direction).size > 0

  def hasNextIn(directions: List[Direction]): Boolean =
    directions.exists(direction => hasNext(direction))

  def withDeparture(priority: Direction, fallback: List[Direction]): (Vehicle, LanesQue) =
    require(hasNextIn(priority :: fallback), "required directions can't be all empty")

    val d         = (priority :: fallback).find(hasNext(_)).getOrElse(North) // ignore casue of require
    val v         = queues(d).head
    val newqueues = queues + (d -> queues(d).tail)

    (v, LanesQue(newqueues))

  def getAllUnsorted: List[Vehicle] =
    queues(North) ++ queues(South) ++ queues(East) ++ queues(West)

end LanesQue

object LanesQue:
  /** Inspired by Pearl's naming, we do something we shoudn't do becuse we ingore inner LanesQue
    * mehanics
    *
    * Use insated of 'new LanesQue().withArrival(vehicles)' to emphasize ur ignorance
    */
  def slurpIn(vehicles: List[Vehicle]): LanesQue =
    new LanesQue().withArrival(vehicles)

  /** Inspired by Pearl's naming, we do something we shoudn't do becuse we ingore inner LanesQue
    * mehanics
    *
    * Use insated of 'lq.getAllUnsorted' to emphasize ur ignorance
    */
  def spitOut(lq: LanesQue): List[Vehicle] = lq.getAllUnsorted
