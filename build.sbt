name := "gu-who"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.madgag" % "github-api" % "1.49.99.0.2",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2",
  "org.eclipse.jgit" % "org.eclipse.jgit" % "3.3.0.201403021825-r",
  "com.madgag" %% "scala-git" % "1.11.1",
  "com.madgag" %% "scala-git-test" % "1.11.1" % "test"
)     

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

play.Project.playScalaSettings

