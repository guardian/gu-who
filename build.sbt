name := "gu-who"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.kohsuke" % "github-api" % "1.50-SNAPSHOT"
)     

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

play.Project.playScalaSettings
