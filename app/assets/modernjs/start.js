console.log("Hello in TraffcikVisualizer! All ready to go")

function obtainConfiguration() {
    let elem = document.getElementById("visualizer-main")

    return {
        "onelane": elem.getAttribute("data-conf-oneline") || "", // empty means its off, don't default it
        "ms": elem.getAttribute("data-conf-ms") || "1000"
    }
}

function getExample() {
    let xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            visualize(JSON.parse(xhttp.responseText), obtainConfiguration())
        }
    }
    xhttp.open("GET", "/assets/json/generated-output.json", true)
    xhttp.send()
}


getExample()
