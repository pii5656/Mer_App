//scalaVersion := "2.11.5"

//val scalazVersion = "7.1.0"

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % "3.3.0"
)
import Test._
/*
resolvers ++= Seq(
  // other resolvers here
  // if you want to use snapshot builds (currently 0.12-SNAPSHOT), use this.
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Akka Snapshot epository" at "http://repo.akka.io/snapshots/"

)
 */

initialCommands in console := "import Test._"
