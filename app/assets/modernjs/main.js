function overrideLane(lane) {
    let directions = ["north", "south", "east", "west"]
    let opposite = {
        "north": "south",
        "south": "north",
        "east": "west",
        "west": "east"
    }

    let shouldNotOverride = directions.some(d => lane == `${d}-${opposite[d]}`)
    if (shouldNotOverride) {
        return lane // already ok
    }

    return directions.map(d => {
        if (lane.startsWith(d)) {

            return `${d}-${opposite[d]}`
        }
        return false
    }).find(d => d) // a nonfalse
}

function refreshVehiclesPlacement(fmaps) {
    let vehicleGroups = [fmaps.inLaneMap, fmaps.outLaneMap, fmaps.goneMap]
    let prefixes = ["veh-lane", "veh-out-lane", "veh-gone-lane"]
    let lanes = Object.keys(fmaps.inLaneMap) // all have the same keys

    vehicleGroups.forEach((group, idx) => {
        let prefix = prefixes[idx]

        lanes.forEach((la) =>
            document.getElementById(`${prefix}-${la}`).innerHTML = "" // clear it
        )

        lanes.forEach((la) => {
            let howMany = group[la]
            let bulletChar = String.fromCharCode(8226)
            let content = new Array(howMany).fill(bulletChar).join("")

    
            let ola = la
            if (prefix == "veh-lane" && window.viconf["onelane"]) {
                ola = overrideLane(la)
                document.getElementById(`${prefix}-${ola}`).innerHTML += content
            } else document.getElementById(`${prefix}-${la}`).innerHTML = content
        })
    })
    

}


function fireLight(fromDirection, toDirection, lightColor) {
    let [from, to] = [fromDirection.toLowerCase(), toDirection.toLowerCase()]
    let color = lightColor.toLowerCase()

    let eid = `lane-${from}-${to}`
    document.getElementById(eid).style.backgroundColor = color
}

function enableCrossing(fromDirection, canCross) {
    let crossing = fromDirection.toLowerCase()
    let isGreen = canCross == "true"
    let color = isGreen ? "green" : "red"

    let eid = `crossing-${crossing}`
    document.getElementById(eid).style.borderColor = color
}

function loadFromInitialState(initailStateJson) {
    let lightsData = initailStateJson["lightsData"]
    let crossingsData = initailStateJson["crossingsData"]

    lightsData.forEach(d => fireLight(d["fromDirection"], d["toDirection"], d["lightColor"]))
    crossingsData.forEach(d => enableCrossing(d["fromDirection"], d["canCross"]))

}


function processEvent(event, etype, timeTill, howMuchIsSecond, fmaps) {
        setTimeout(() => {
            switch(etype) {
                case "light":
                    fireLight(event["fromDirection"], event["toDirection"], event["lightColor"])
                break
                case "crossing":
                    enableCrossing(event["fromDirection"], event["canCross"])
                break
                case "flush":
                    executeFlush(event, fmaps.inLaneMap, fmaps.outLaneMap)
                    refreshVehiclesPlacement(fmaps)
                default:
                // ???
            }
        }, timeTill*howMuchIsSecond)
}

function processVisualizerEventsSeqentally(lightsData, crossingsData, visualizerFlushes, vehiclesLeft, arrivals, howMuchIsSecond, fmaps) {
    const allEvents = [lightsData, crossingsData, visualizerFlushes]
    const eventTypes = ["light", "crossing", "flush"]


    let ordering = allEvents.map((array, idx) => {
        if (array.length > 0) return [parseInt(array[0]["timestamp"]), idx]
        else return [null, idx]
    })

    let [timeTill, whichFirst] = ordering.reduce((accumulator, currentValue) => {
        if (accumulator[0] == null) return currentValue
        else if (currentValue[0] == null) return accumulator
        else return accumulator[0] < currentValue[0] ?  accumulator : currentValue
    }, ordering[0] || [null, null])


    if (timeTill != null){
        let event = allEvents[whichFirst].shift() // <-- we simulate Queue

        // please note, there is no setTimeout here as all events
        //  have their timeouts already computed on backends and use them later
        processEvent(event, eventTypes[whichFirst], timeTill, howMuchIsSecond, fmaps)
        processVisualizerEventsSeqentally(...allEvents, vehiclesLeft, arrivals, howMuchIsSecond, fmaps)

    }
}

function simulationMainLoop(howMuchIsSecond, stepStatuses, arrivals, stepIndex, fmaps) {
    moveAlongFlushes(fmaps.outLaneMap, fmaps.goneMap)

    let step = stepStatuses.shift() // <-- queue of steps
    if  (!step) return

    let lightsData = step["lightsData"]
    let crossingsData = step["crossingsData"]
    let visualizerFlushes = step["visualizerFlushes"]
    
    let vehiclesLeft = step["vehiclesLeft"]

    executeArrivals(arrivals, stepIndex, fmaps.inLaneMap)
    refreshVehiclesPlacement(fmaps)

    processVisualizerEventsSeqentally(lightsData, crossingsData, visualizerFlushes, vehiclesLeft, arrivals, howMuchIsSecond, fmaps)
    let stepTotalTime = parseInt(step["duration"])*howMuchIsSecond
    console.log(`STEP: ${stepTotalTime} miliseconds`)


    // we need to process next step only after the previous one finished:
    setTimeout(() =>{
        simulationMainLoop(howMuchIsSecond, stepStatuses, arrivals, stepIndex + 1, fmaps)
    }, stepTotalTime)
}

function visualize(json, config) {
    window.viconf = config // not ideal but effective
    let sim = json["simulation"]
    let arrivals = sim["arrivals"]
    let initialState = sim["initialState"]
    let stepStatuses = sim["stepStatuses"]

    let fmaps = {
        inLaneMap: flushMap(),
        outLaneMap: flushMap(),
        goneMap: flushMap()
    }

    loadFromInitialState(initialState)
    // test(stepStatuses, arrivals) // FIX: testing
    simulationMainLoop(parseInt(window.viconf["ms"]), stepStatuses, arrivals, 0, fmaps)

}

// LOGIC FOR VEHICLES VISUALIZATION:

function laneBy(fromDirection, toDirection) {
    let [fromd, tod] = [fromDirection.toLowerCase(), toDirection.toLowerCase()]
    return `${fromd}-${tod}`
}

function flushMap() {
    let lanes = [
        "north-south",
        "north-west",
        "north-east",
        "south-north",
        "south-west",
        "south-east",
        "west-south",
        "west-north",
        "west-east",
        "east-south",
        "east-west",
        "east-north"
    ]

    let dict = {}
    lanes.forEach(l => dict[l] = 0)

    return dict
}

function executeArrivals(arrivals, idx, inLaneMap) {
    let arr = arrivals[idx]
    arr.forEach(
        vehic => {
            inLaneMap[laneBy(vehic["startRoad"], vehic["endRoad"])] += 1
        }
    )
}

function executeFlush(event, inLaneMap, outLaneMap) {
    let amount = parseInt(event["count"])
    let fromd = event["fromDirection"]
    let tod = event["toDirection"]

    inLaneMap[laneBy(fromd, tod)] -= amount
    outLaneMap[laneBy(tod, fromd)] += amount
}

function moveAlongFlushes(outLaneMap, goneMap) {
    let keys = Object.keys(outLaneMap)
    keys.forEach(key => {
        goneMap[key] = outLaneMap[key]
        outLaneMap[key] = 0
    })
}

function logDictCopy(dict) { /* Because Goole's console.log updates dicts */
    console.log({...dict})
}


window.visualize = visualize