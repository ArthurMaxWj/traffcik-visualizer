package traffcik.controllers

import traffcik.core.*
import traffcik.simulation.TrafficController

class DumbController extends TrafficController[Unit]:
  override def process(lq: LanesQue, store: Unit): (LanesQue, StepResult, Unit) =
    LanesQue.spitOut(lq) match
      case v :: rest => (LanesQue.slurpIn(rest), StepResult(List(v)), ())
      case Nil       => (new LanesQue(), StepResult(List()), ())
  override def defaultInitialStoreVaule = ()

class ClearAllController extends TrafficController[Unit]:
  override def process(lq: LanesQue, store: Unit) =
    def loop(veh: List[Vehicle], result: StepResult): (LanesQue, StepResult, Unit) =
      veh match
        case v :: rest =>
          loop(rest, StepResult(v :: result.vehiclesLeft))
        case Nil =>
          (new LanesQue(), StepResult(result.vehiclesLeft.reverse), ())
    loop(LanesQue.spitOut(lq), StepResult(List()))
  override def defaultInitialStoreVaule = ()

class AllAtOnceController(maxAllowed: Int) extends TrafficController[Unit]:
  override def process(lq: LanesQue, store: Unit) =
    def loop(veh: List[Vehicle], result: List[Vehicle], count: Int): (LanesQue, StepResult, Unit) =
      veh match
        case v :: rest =>
          if count < maxAllowed then loop(rest, v :: result, count + 1)
          else
            (
              LanesQue.slurpIn(veh),
              StepResult(result.reverse),
              ()
            ) // finish loop cause cant allow more cars to pass
        case Nil =>
          (new LanesQue(), StepResult(result.reverse), ())
    loop(LanesQue.spitOut(lq), List(), 0)
  override def defaultInitialStoreVaule = ()
