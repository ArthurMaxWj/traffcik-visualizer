package traffcik.core

enum Direction:
  case North, South, West, East

enum Light:
  case Red, Yellow, Green

abstract class TrafficTask
case class Step() extends TrafficTask
case class AddVehicle(vehicleId: String, startRoad: Direction, endRoad: Direction)
    extends TrafficTask

case class Vehicle(vehicleId: String, startRoad: Direction, endRoad: Direction)
case class StepResult(
  vehiclesLeft: List[Vehicle],
  duration: Int,
  // lightsData means: (Int is for time in simulation, Directions specify which light to fire, Light is value
  lightsData: List[(Int, Direction, Direction, Light)],
  crossingsData: List[(Int, Direction, Boolean)],
  // at timstamp (Int) it flushes certain amount of vehicles (Int) from lane (Direction, Direction):
  visualizerFlushes: List[(Int, Direction, Direction, Int)]
)

object StepResult:
  // def apply(vehiclesLeft: List[Vehicle],
  //     duration: Int,
  //     lightsData: List[(Int, Direction, Direction, Light)],
  //     crossingsData: List[(Int, Direction, Boolean)],
  //     visualizerFlushes: List[(Int, Direction, Direction, Int)]
  // ): StepResult =
  //     require(crossingsData.size == 4, "Only four crossroads allowed")
  //     new StepResult(vehiclesLeft, duration, lightsData, crossingsData, visualizerFlushes)

  def apply(vehiclesLeft: List[Vehicle]): StepResult =
    StepResult(vehiclesLeft, 0, List(), List(), List())

object CoreConversions:
  // FIX: important note: we are ignoring trailing AddVheicle-s if more Step() <-- we cant leave for now
  def tasksToArrivals(tasks: List[TrafficTask]): List[List[Vehicle]] =
    def loop(
      tasks: List[TrafficTask],
      awaiting: List[Vehicle],
      arrivals: List[List[Vehicle]]
    ): List[List[Vehicle]] =
      tasks match
        case t :: rest =>
          t match
            case Step() =>
              loop(rest, List(), awaiting :: arrivals)
            case AddVehicle(vid, vfrom, vto) =>
              loop(rest, Vehicle(vid, vfrom, vto) :: awaiting, arrivals)
        case Nil =>
          arrivals.reverse // this ingores last AddVehciles if no Step()

    loop(tasks, List(), List())

/** Represents turns from one direction to other.
  */
enum Turn:
  case Forward, Left, Right, Back;

object Turn:
  /** return Turn from one direction to other
    */
  def of(from: Direction, to: Direction) =
    import Direction.*

    // looking from South road towards North
    val fromSouthViewpoint = List(South, West, North, East)
    val ifrom              = fromSouthViewpoint.indexOf(from)
    val ito                = fromSouthViewpoint.indexOf(to)

    Map(
      0 -> Back,
      1 -> Left,
      2 -> Forward,
      3 -> Right
    )((ito - ifrom).abs) // abs makes it work all strating viewpoints

  /** returns left turn of given direction
    */
  def leftOf(d: Direction): Direction =
    import Direction.*

    // looking from South road towards North

    val fromSouthViewpoint = List(South, West, North, East)
    val idx                = fromSouthViewpoint.indexOf(d)
    fromSouthViewpoint((idx + 1) % 4)
end Turn

/** Helps handle concurrent processing of objects
  *
  * I suggest using true as starting value in any recursion to avoid mistakes.
  */
object Concurrent:
  def get[T](touple: (T, T), timeline: Boolean): T =
    if timeline then touple._1 else touple._2

  def set[T](value: T, touple: (T, T), timeline: Boolean): (T, T) =
    if timeline then (value, touple._2) else (touple._1, value)
