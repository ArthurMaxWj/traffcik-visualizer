import scala.sys.process._

name := """traffcikvisualizer"""
organization := "com.amwojcik"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.7.1"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test

libraryDependencies += "org.scalameta" % "sbt-scalafmt_2.12_1.0" % "2.5.4"

lazy val readyjs = taskKey[Unit]("Compiles JavaScript")
readyjs := { "npx babel app/assets/modernjs --out-dir public/javascripts" ! }

lazy val lintjs = taskKey[Unit]("Runs linter on JavaScript")
lintjs := { "npx eslint app/assets/modernjs/**" ! }