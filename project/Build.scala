import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "photo-album"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore, javaJdbc, javaEbean,
    "com.typesafe" %% "play-plugins-mailer" % "2.1-RC2",
    "org.slf4j" % "slf4j-simple" % "1.7.2" % "test",
    "postgresql" % "postgresql" % "9.1-901.jdbc4",
    "commons-codec" % "commons-codec" % "1.7",
    "org.apache.commons" % "commons-lang3" % "3.1",
    "commons-lang" % "commons-lang" % "2.5"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
