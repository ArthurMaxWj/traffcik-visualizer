package traffcik.controllers

import traffcik.core.*
import traffcik.simulation.TrafficController

import scala.math.*
import scala.util.Random

object WeatherController:
  enum Weather:
    case Storm, Rainy, Windy, Cloudy, Sunny, Tropical

  object Weather:
    def names                                   = Weather.values.map(_.toString)
    def weatherOf(s: String, fallback: Weather) =
      if Weather.names.contains(s) then Weather.valueOf(s)
      else fallback

  def weatherToPropability(weather: Weather): Float =
    import Weather.*

    weather match
      case Storm    => 0.05
      case Rainy    => 0.2
      case Windy    => 0.4
      case Cloudy   => 0.6
      case Sunny    => 0.8
      case Tropical => 0.9

  def hourToProbability(hour: Int): Float =
    if hour >= 1 && hour < 5 then 0.01
    else if hour >= 5 && hour < 7 then 0.4
    else if hour >= 7 && hour < 10 then 0.8 // school, work
    else if hour >= 10 && hour < 13 then 0.2
    else if hour >= 13 && hour < 15 then 0.6
    else if hour >= 15 && hour < 18 then 0.8 // back from school, work
    else if hour >= 18 && hour < 21 then 0.5
    else if hour >= 21 && hour < 23 then 0.2
    else 0.1 // around midnigt

  /** non-FP function, uses random numbers as side-effect */
  def dynamicCrossingsFiring(weather: Weather)(hour: Int): Boolean =
    val rand = Random.nextInt(100)
    val prob = weatherToPropability(weather) * hourToProbability(hour)

    prob * 100 >= rand // f.e. 80% prob accepts all 0..80 numbers as passing check

  /** Arguments format: (maxTime, startHour, leftTurnTime, timePerCar)(weather, cutCrossingDelay)
    * maps to: DynamicController(f, hourInUnits, cutCrossingDelay, timePerCar, leftTurnTime,
    * maxTime, startHour) where 'f is WeatherController.dynamicCrossingsFiring(weather) (its curried
    * function)
    */
  def apply(
    maxTime: Int = 9,
    startHour: Int = 8,
    leftTurnTime: Int = 2,
    timePerCar: Int = 1
  )(weather: Weather, hourInUnits: Int = 4, cutCrossingDelay: Int = 3): DynamicController =
    new DynamicController(
      dynamicCrossingsFiring(weather),
      hourInUnits,
      cutCrossingDelay,
      timePerCar,
      leftTurnTime,
      maxTime,
      startHour
    )
end WeatherController
