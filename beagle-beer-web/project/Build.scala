import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "beagle-beer-web"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    "com.typesafe.play" %% "play-slick" % "0.3.3"
//      anorm
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    net.virtualvoid.sbt.graph.Plugin.graphSettings: _*
  )

}
