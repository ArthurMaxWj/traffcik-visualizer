package traffcik.simulation

import traffcik.core.*
import traffcik.simulation.InitialStates

abstract class TrafficController[T]:
  def process(lq: LanesQue, store: T): (LanesQue, StepResult, T)
  def defaultInitialStoreVaule: T
  def initialTrafficState: (List[(Direction, Direction, Light)], List[(Direction, Boolean)]) =
    InitialStates.onlyForward
