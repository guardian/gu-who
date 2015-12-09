name := "gu-who"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

updateOptions := updateOptions.value.withCachedResolution(true)

buildInfoKeys := Seq[BuildInfoKey](
  name,
  BuildInfoKey.constant("gitCommitId", Option(System.getenv("SOURCE_VERSION")) getOrElse(try {
    "git rev-parse HEAD".!!.trim
  } catch {
    case e: Exception => "unknown"
  }))
)

buildInfoPackage := "app"

lazy val root = (project in file(".")).enablePlugins(PlayScala, BuildInfoPlugin)

resolvers ++= Seq(
  // Resolver.mavenLocal,
  Resolver.sonatypeRepo("releases")
)

libraryDependencies ++= Seq(
  cache,
  filters,
  ws,
  "com.typesafe.akka" %% "akka-agent" % "2.3.2",
  "org.webjars" % "bootstrap" % "3.3.5",
  "com.adrianhurt" %% "play-bootstrap3" % "0.4.4-P24",
  "com.madgag" %% "play-git-hub" % "2.3",
  "com.github.nscala-time" %% "nscala-time" % "2.6.0",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3-1",
  "com.madgag.scala-git" %% "scala-git-test" % "3.3" % "test",
  "org.scalatestplus" %% "play" % "1.4.0-M4" % "test"
)     

sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false
