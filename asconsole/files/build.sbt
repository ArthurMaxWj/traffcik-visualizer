scalaVersion := "3.7.1"

name := "traffcik"

libraryDependencies += "org.playframework" %% "play-json" % "3.0.4"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % "test"

libraryDependencies += "org.scalameta" % "sbt-scalafmt_2.12_1.0" % "2.5.4"

// renaming my fatJar file, use `sbt asembly` to get it
lazy val myProject = (project in file("."))
  .settings(
    assembly / assemblyJarName := "traffcikByAmwojcik.jar",
    assembly / mainClass := Some("traffcik.simulation.hello"),
    assembly / assemblyMergeStrategy := { // conflict handling
        case x if x.endsWith("module-info.class") => MergeStrategy.discard
        case x => MergeStrategy.first
    }
  )
  