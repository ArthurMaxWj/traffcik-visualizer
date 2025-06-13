package traffcik.simulation

import traffcik.core.*
import play.api.libs.json.Json

object JsonConversions:

  def enclosed(str: String)                                              = "{\n\n  " + str + "\n\n}"
  def hasProps(field: Map[String, String], props: List[String]): Boolean =
    props.forall(field.contains(_))

  def isDirection(dire: String): Boolean   = List("North", "South", "West", "East").contains(dire)
  def toDirection(dire: String): Direction = Direction.valueOf(dire)
  def isOkId(vehicleId: String): Boolean   = vehicleId != ""

  def tasksFromMaps(fields: List[Map[String, String]]): List[(Boolean, TrafficTask, String)] =
    def success(task: TrafficTask): (Boolean, TrafficTask, String) = (true, task, "")
    def error(msg: String): (Boolean, TrafficTask, String)         = (false, Step(), msg)
    fields.map(field =>
      if hasProps(field, List("type")) then
        field("type") match
          case "step"       => success(Step())
          case "addVehicle" =>
            if !hasProps(field, List("vehicleId", "startRoad", "endRoad")) then
              error("missing required fields for 'addvehicle'")
            else
              val (vehicleId, startRoadStr, endRoadStr) =
                (field("vehicleId"), field("startRoad"), field("endRoad"))
              val allFieldsOk =
                isDirection(startRoadStr) && isDirection(endRoadStr) && isOkId(vehicleId)
              if allFieldsOk then
                val (startRoad, endRoad) = (toDirection(startRoadStr), toDirection(endRoadStr))
                success(AddVehicle(vehicleId, startRoad, endRoad))
              else error("AddVehicle got incorrect value for one of its fields")
          case _ => error("unknown task type")
      else error("no task type specified")
    )
  end tasksFromMaps

  def tasksFromJson(jsonString: String): (Boolean, List[TrafficTask], List[String]) =
    def tryFromTasks(
      tasks: List[(Boolean, TrafficTask, String)]
    ): (Boolean, List[TrafficTask], List[String]) =
      if tasks.forall(t => t._1) then (true, tasks.map(_._2), List())
      else
        (
          false,
          List(),
          tasks.zipWithIndex
            .filter(t => !t._1._1)
            .map(ti => s"In task ${ti._2}: ${ti._1._3}") // tasks are numbered from 0
        )

    def fromString(jsonString: String): (Boolean, List[TrafficTask], List[String]) =
      try
        val jsonObject = Json.parse(jsonString)
        val tasksData  = (jsonObject \ "commands").as[List[Map[String, String]]]
        tryFromTasks(tasksFromMaps(tasksData))
      catch
        case _: Throwable =>
          (false, List(), List("Wrong JSON format, required: {\"commands\": [{task...}, ...]}"))
    fromString(jsonString)
  end tasksFromJson

  def resultsToJson(
    results: List[StepResult],
    simple: Boolean = false,
    enclose: Boolean = true
  ): String =
    def vehListToJsonArray(veh: List[Vehicle]): String =
      "[ " + veh.map(_.vehicleId).map(id => s"\"${id}\"").mkString(", ") + " ]"

    def complexStepToString(
      vehicles: List[Vehicle],
      time: Int,
      lightsData: List[(Int, Direction, Direction, Light)],
      crossingsData: List[(Int, Direction, Boolean)],
      flushesData: List[(Int, Direction, Direction, Int)]
    ): String =
      val vehiclesLeft = vehListToJsonArray(vehicles)
      val lightsArray  = "[" + lightsData
        .map(change =>
          val (ts, dir1, dir2, lightColor) = change
          List(
            s"\n{  \"timestamp\": \"${ts}\"",
            s"\"fromDirection\": \"${dir1}\"",
            s"\"toDirection\": \"${dir2}\"",
            s"\"lightColor\": \"${lightColor}\"  }"
          ).mkString(", ")
        )
        .mkString(", ") + "\n ] "
      val crossingsArray = "[ " + crossingsData
        .map(change =>
          val (ts, dir1, canCross) = change
          List(
            s"\n {  \"timestamp\": \"${ts}\"",
            s"\"fromDirection\": \"${dir1}\"",
            s"\"canCross\": \"${canCross}\"   }"
          ).mkString(", ")
        )
        .mkString(", ") + "\n ]"
      val visualizerFlushes = "[" + flushesData
        .map(flushThem =>
          val (ts, dir1, dir2, count) = flushThem
          List(
            s"\n{  \"timestamp\": \"${ts}\"",
            s"\"fromDirection\": \"${dir1}\"",
            s"\"toDirection\": \"${dir2}\"",
            s"\"count\": \"${count}\"  }"
          ).mkString(", ")
        )
        .mkString(", ") + "\n ] "

      "\n\n{ " + List(
        s"\n \"vehiclesLeft\": ${vehiclesLeft}",
        s"\n \"duration\": ${time}",
        s"\n \"lightsData\": ${lightsArray}",
        s"\n \"crossingsData\": ${crossingsArray}",
        s"\n \"visualizerFlushes\": ${visualizerFlushes}"
      ).mkString(", ") + "\n }"
    end complexStepToString

    def loop(res: List[StepResult], acc: List[String]): String = res match
      case StepResult(vehicles, time, lights, crossing, flushes) :: rest =>

        val stepAsString = if simple then
          val vehArray = vehListToJsonArray(vehicles)
          s" {\n \"vehiclesLeft\": ${vehArray} \n}"
        else complexStepToString(vehicles, time, lights, crossing, flushes)

        loop(rest, acc :+ stepAsString)
      case Nil =>
        val statuses = acc.mkString(", \n")
        val result   = s"\"stepStatuses\": [\n${statuses}\n]"
        if enclose then enclosed(result) else result

    loop(results, List())
  end resultsToJson

  def arrivalsArray(arrivals: List[List[Vehicle]]): String =
    def singleArrival(ari: List[Vehicle]) =
      "[ " + ari
        .map(vehicle =>
          List( // note curly braces below
            s"\n { \"vahicleId\": \"${vehicle.vehicleId}\"",
            s"\"startRoad\": \"${vehicle.startRoad}\"",
            s"\"endRoad\": \"${vehicle.endRoad}\" }"
          ).mkString(", ")
        )
        .mkString(", ") + "\n ]"

    "[\n " + arrivals.map(singleArrival(_)).mkString(", \n") + "]"

  private def initialStateDict(
    data: (List[(Direction, Direction, Light)], List[(Direction, Boolean)])
  ): String =
    def lightsData = "[ \n" + data._1
      .map(change =>
        val (dir1, dir2, lightColor) = change
        List( // note curly braces below
          s" { \"fromDirection\": \"${dir1}\"",
          s"\"toDirection\": \"${dir2}\"",
          s"\"lightColor\": \"${lightColor}\"  } \n"
        ).mkString(", ")
      )
      .mkString(", ") + "\n ] "

    def crossingsData =
      "[ \n" + data._2
        .map(change =>
          val (dir1, canCross) = change
          List( // note curly braces below
            s"{ \"fromDirection\": \"${dir1}\"",
            s"\"canCross\": \"${canCross}\" } \n "
          ).mkString(", ")
        )
        .mkString(", ") + "\n ]"

    def initial =
      "{ " + List(
        s"\n \"lightsData\": ${lightsData}",
        s"\n \"crossingsData\": ${crossingsData}"
      ).mkString(", ") + "\n }"
    initial
  end initialStateDict

  def visualizatorPackagedJson(
    input: List[TrafficTask],
    output: List[StepResult],
    initial: (List[(Direction, Direction, Light)], List[(Direction, Boolean)])
  ): String =

    val arrivals     = arrivalsArray(CoreConversions.tasksToArrivals(input))
    val initialDict  = initialStateDict(initial)
    val stepStatuses = resultsToJson(output, false, false)

    val combined =
      "{ \"simulation\": {" + List(
        s"\n \"arrivals\": ${arrivals}",
        s"\n \"initialState\": ${initialDict}",
        s"\n ${stepStatuses}" // <-- we dont enclose here, it dict aleady
      ).mkString(", ") + "} \n }"
    combined

end JsonConversions
