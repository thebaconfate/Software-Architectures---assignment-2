name := """assignment-2"""
organization := "vub.student"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.3.1"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
libraryDependencies += "com.github.jwt-scala" %% "jwt-play-json" % "9.4.5"
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.4"
libraryDependencies += "org.playframework" %% "play-json" % "3.0.1"



// Adds additional packages into Twirl
//TwirlKeys.templateImports += "vub.student.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "vub.student.binders._"
