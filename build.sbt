name := "gu-who"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.kohsuke" % "github-api" % "1.49"
)     

play.Project.playScalaSettings
