package traffcik.simulation

import traffcik.core.*
import traffcik.simulation.TrafficController

/* OPTIMIZE: we could use more Queue class here but we have no guarantee the Controller likes that solution
    also mutable Queue uses Lists as part of immplementation so O(n) is complexity here, that's acceptable
    Still rethink design to use other data syructures
 */

class TrafficManager[T](
  tasks: List[TrafficTask],
  tcontroller: TrafficController[T],
  initialStore: T
):
  def run =
    def loop(
      tasks: List[TrafficTask],
      vehiclesBeforeStep: List[Vehicle],
      lq: LanesQue,
      store: T
    ): List[StepResult] =
      tasks match
        case t :: rest =>
          t match
            case AddVehicle(vehicleId, s, e) =>
              loop(rest, Vehicle(vehicleId, s, e) :: vehiclesBeforeStep, lq, store)
            case Step() =>
              val (newLQ, stepOut, newStore) =
                tcontroller.process(lq.withArrival(vehiclesBeforeStep.reverse), store)
              stepOut :: loop(rest, List(), newLQ, newStore)
        case Nil => Nil
    loop(tasks, List(), new LanesQue(), initialStore)

// OPTIMIZE: We could try foldRight, but it doesn't natural here:
/*
        def processTask(task: TrafficTask, previous: (List[Vehicle], List[StepResult], T)): (List[Vehicle], List[StepResult], T) =
            val (vehicles, results, store) = previous
            task match
            case AddVehicle(vehicleId, s, e) =>
                (vehicles :+ Vehicle(vehicleId, s, e), results, store)
            case Step() =>
                val (newVehicles, stepOut, newStore) = tcontroller.process(vehicles, store)
                (newVehicles, stepOut :: results, newStore)
        val res = tasks.reverse.foldRight((List[Vehicle](), List[StepResult](), initialStore))(processTask(_, _))
        res._2.reverse // we only return StepResults (in order)
 */

end TrafficManager

object TrafficManager:
  def apply[T](tasks: List[TrafficTask], tcontroller: TrafficController[T], initialStore: T) =
    new TrafficManager(tasks, tcontroller, initialStore)

  def apply[T](tasks: List[TrafficTask], tcontroller: TrafficController[T]) =
    new TrafficManager(tasks, tcontroller, tcontroller.defaultInitialStoreVaule)
