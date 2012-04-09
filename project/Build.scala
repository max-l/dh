import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "play-test"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
        "postgresql" % "postgresql" % "8.4-701.jdbc4",
        "org.squeryl" %% "squeryl" % "0.9.5-RC2",
        "org.pegdown" % "pegdown" % "1.1.0"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
        /*
      lessEntryPoints <<= baseDirectory(base => (
        //(base / "app" / "assets" / "stylesheets" / "bootstrap.less") +++
        (base / "app" / "assets" / "stylesheets" ** "responsive.less")
      ))
      */
    )
}
