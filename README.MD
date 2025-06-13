# TraffcikVisualizer (by A.M. Wójcik)
This is an application created to showcase my abilities in Scala and some JS. It has two sides to it, the front "Visualizer" and the core logic "Traffcik" (combined words 'traffick' and my last name "Wójcik").

Traffcik takes input and generates output, then that output is used by Visualizer to create a simulation you can watch in your browser. 

**Important note:** If you need to use just output or only in format specified in the exercise's description, read below on assembling a fat JAR with  sbt assembly and run it with option '-simple' as the third argument, as explained in section below. I suggest trying it out in the browser instead, with 'jsut json' link on UI.

# Starting the application
## In web
Use the following commands:
```
npm 
sbt update

sbt readyjs
sbt run
```
And go to **localhost:9000** to view it. Command `sbt readyjs` compiles JavaScript with Babel.js, always use it whenever you change your JS code.

## In console
First install it (`cd` is required):
```
cd asconsole
sh build.sh /somelecation
```
Here '/somelocation' is a folder where u want it to be installed. You can just drop folder on the bash terminal.
Then go to that subdirectory's traffcik directory `cd /somelocation/traffcik` and run:
```
sbt run
```
which should output error a bout lack of arguments. Now I suggest creating a fatJAR with sbt and running it instead:
```
sbt assembly
java -cp <path> traffcik.simulation.Start input.json output.json -simple
```
where '<path>' is whatever 'sbt assembly command' pointed to. Option '-simple' isn't necessary, but it removes all extra data used by the frontend so it matches the simple format described in the exercise description. More on that in the "Format Information" section below.

# What do you see in UI?
In UI the dots represent cars and dotted lines are pedestrian crossings. 

The color of each lane represents a traffic light, not the actual number of lanes. Middle lane is  for cars going forward,  left lane is for cars turning left, etc. This allows for two views: view in order (checkbox folding lanes can be found on UI) and by-direction, helpful in understanding where cars are headed. If you are confused by the latest, keep the checkbox checked.

# Application structure
### traffcik.core
Defines core classes and data structures used by other classes. Also defines LanesQue, a queue of cars for each direction.

### traffcik.simulation
Defines TraffickManager which transforms tasks into Vehicles and them into LanesQue, then feeds that to a specific controller. Here you also find methods related to JSON in JsonConversions.

### traffcik.controllers
traffcik.simulation.TrafficController's subclasses, each havs their own idea about how intersections work. Some allow turns left, others do not, some allow for web simulations while others do not, etc. You can play with each by altering the 'HemeController.control' method.

The default is the latest and most complete controller, called *WeatherControler*. It computes whether crosswalks are green/busy by probability commutated from hours in the day (changes during simulation) and whether selected from UI. It is only an abstraction over *DynamicController*,  controller that allows you to decide whether crosswalks are fired green for each step by using a function passed to it.

On the other hand, the simplest controller working with UI is *FullTwoPhazesController*, which takes *all* cars on a given axis and flushes them all at once. There are still simpler non-UI controllers to play with in 'app/traffcik/controllers/basicOnes.scala'.

# Tests
Sadly  I didn't have enough time to write tests for everything, but you can run my tests with `sbt test`, works the same way in console and web.

# Code conventions
Besides weird naming like LanesQue, slurpIn and spitOut, that emphasize something about the given method, I also use Ruby-style special comments such as `TODO:`, `NOTE:` `FIX:`, and `OPTIMIZE:` in both JS and Scala code. 

Also note that there is documentation found in `/** ... */` for most important classes.

# Linters
I used 'sbt-scalafmt' configured for Scala 3. Run it from sbt with `scalafmt`.

# Formats Information:
Input JSON format has always the same structure:
```
{
  "commands": [
    {
      "type": "addVehicle",
      "vehicleId": "vehicle1",
      "startRoad": "south",
      "endRoad": "north"
    }, {
      "type": "step"
    }
}
```
Here we have two types of commands, 'step' and 'addVehicle'. All traffic controllers support them. You can mix these two types of commands as you prefer. 'vehicleId' at this moment must be nonempty, but not necessarily unique.

Output format has a simple form (see about option `simple` in sections above) that doesn't work in UI and we is a superset of that version that does work with UI (via packaged format, see below). Simple version looks as follows:
```
{
  "stepStatuses": [
    {
      "leftVehicles": [
        "vehicle1", 
        "vehicle2"
      ]
    }, {
      "leftVehicles": []
    }
  ]
}
```

Here you see outputs for each step of simulation. Step is defined individually by every controller, so it can be a one phaze of traffic lights or all at once., depending on which controller we are currently using. Usually however, controllers have two phazes each for one axis, North-South and East-West, then they swap. That can, however, include turns left and other aspects of each case.

The UI based JSON also contains information about crosswalks, light color changes, and describes when each vehicle moves.

The JSON used by UI is actually packaged version that combines information from both input and output.

