import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "clearvote"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
        "postgresql" % "postgresql" % "8.4-701.jdbc4",
        "org.squeryl" %% "squeryl" % "0.9.5-2",
        "org.pegdown" % "pegdown" % "1.1.0",
        "javax.mail" % "mail" % "1.4"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(

      lessEntryPoints <<= baseDirectory(base => (
        (base / "app" / "assets" / "stylesheets" / "bootstrap.less") +++
        (base / "app" / "assets" / "stylesheets" / "ballot.less") +++
        (base / "app" / "assets" / "stylesheets" / "timepicker.less")
      ))
    )
}
