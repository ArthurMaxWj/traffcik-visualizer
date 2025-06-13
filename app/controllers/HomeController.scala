package controllers

import javax.inject.*
import play.api.*
import play.api.mvc.*

import play.api.data.*
import play.api.data.Forms.*

import java.io.PrintWriter

import traffcik.core.*
import traffcik.simulation.JsonConversions
import traffcik.simulation.TrafficManager
import traffcik.controllers.*

import WeatherController.Weather
import WeatherController.Weather.*

/** This controller creates an `Action` to handle HTTP requests to the application's home page.
  */
@Singleton
class HomeController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController:

  /** Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method will be called when the
    * application receives a `GET` request with a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def simulation(onelane: String, ms: String) = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.simulation(onelane, ms))
  }

  val userForm = Form(
    tuple(
      "commands" -> text,
      "onelane"  -> optional(text),
      "ms"       -> text,
      "weather"  -> text
    )
  )

  private def control(weather: Weather) =
    val alwaysGreen        = (hour: Int) => true
    val greenAfterFewUnits = (hour: Int) => hour > 8
    // new FullTwoPhazesController(6, 1) // 6 cars, 1 sec for each
    // new LQFixedExtraTwoPhazesController(1, 8) // 1 car per sec, max 8 secs (croorects to 5 as minimum)
    // new LeftTurnController(1, 3, 8) // also 3 secs per left tun
    // new LeftTurnController(1, 10, 20) // here it stopes for a while
    // new DynamicController(alwaysGreen, 1000, 4, 1, 1, 9) // 1000 is hour, 1 delay on green
    // new DynamicController(alwaysGreen, 3, 5, 1, 1, 9) // 3 is hour, 5 delay on green

    // new DynamicController(greenAfterFewUnits, 100000, 1, 1, 10, 20) // shouuld work smilarly to: new LeftTurnController(1, 10, 20)
    // new DynamicController(greenAfterFewUnits, 1, 4, 1, 1, 9) // at first no dealy, then yes
    // ew DynamicController(greenAfterFewUnits, 1, 4, 1, 1, 9, 7) // here 7 is starting hour

    // has many options but feafults them. Here:
    // 9 is maxTime, Sunny is Weather and 3 is dealay on crosswalk enabled)
    // WeatherController(9)(Sunny, 5, 4)

    WeatherController(9)(weather, 5, 4)

  def sendCommands = Action { implicit request: Request[AnyContent] =>
    def writeFile(name: String, contents: String): Boolean =
      val pw      = new PrintWriter(name)
      var success = false // lets go imperative for IO

      try
        pw.write(contents)
        success = true
      catch case _ => () // TODO: add specyfic error messages
      finally pw.close

      success

    val data = userForm.bindFromRequest()
    if data.hasErrors then Ok("Error: form you send was corrupted")
    else
      val (commands, onelane, ms, weather) = data.get
      val tasksData                        = JsonConversions.tasksFromJson(commands)
      if !tasksData._1 then
        val errors = tasksData._3.mkString("\n")
        Ok(s"Errors in JSON: ${errors}")
      else
        val cont = control(Weather.weatherOf(weather, Sunny))

        val result     = TrafficManager(tasksData._2, cont).run
        val jsonResult =
          JsonConversions.visualizatorPackagedJson(tasksData._2, result, cont.initialTrafficState)

        val done = writeFile("public/json/generated-output.json", jsonResult)
        if !done then Ok("Error: Coudn't save resuls")
        else
          // val msStr = ms.getOrElse("")
          val ol = onelane.getOrElse("")
          Redirect(routes.HomeController.simulation(ol, ms))

          // TODO: remove these, they are are for testing:
          // val simpleJson = JsonConversions.resultsToJson(result, true)
          // Ok(simpleJson)
      end if
    end if
  }

  def justJson = Action { implicit request: Request[AnyContent] =>
    val data = userForm.bindFromRequest()
    if data.hasErrors then Ok("Error: form you send was corrupted")
    else
      val (commands, onelane, ms, weather) = data.get
      val tasksData                        = JsonConversions.tasksFromJson(commands)
      if !tasksData._1 then
        val errors = tasksData._3.mkString("\n")
        Ok(s"Errors in JSON: ${errors}")
      else
        val cont = control(Weather.weatherOf(weather, Sunny))

        val result     = TrafficManager(tasksData._2, cont).run
        val simpleJson = JsonConversions.resultsToJson(result, true)
        Ok(simpleJson)
  }
end HomeController
