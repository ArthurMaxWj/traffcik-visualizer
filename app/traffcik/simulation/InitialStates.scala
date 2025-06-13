package traffcik.simulation

import traffcik.core.*

object InitialStates:
  val onlyForward: (List[(Direction, Direction, Light)], List[(Direction, Boolean)]) =
    val lightsData = List(
      (Direction.North, Direction.South, Light.Red),
      (Direction.South, Direction.North, Light.Red),
      (Direction.East, Direction.West, Light.Red),
      (Direction.West, Direction.East, Light.Red)
    )
    val crossingsData = List(
      (Direction.North, false),
      (Direction.South, false),
      (Direction.East, false),
      (Direction.West, false)
    )
    (lightsData, crossingsData)

  val alsoTurnLeft: (List[(Direction, Direction, Light)], List[(Direction, Boolean)]) =
    val lightsData = List(
      (Direction.North, Direction.South, Light.Red),
      (Direction.South, Direction.North, Light.Red),
      (Direction.East, Direction.West, Light.Red),
      (Direction.West, Direction.East, Light.Red),
      // left turns:
      (Direction.North, Direction.East, Light.Red),
      (Direction.South, Direction.West, Light.Red),
      (Direction.East, Direction.South, Light.Red),
      (Direction.West, Direction.North, Light.Red)
    )
    val crossingsData = onlyForward._2 // same as before
    (lightsData, crossingsData)
end InitialStates
