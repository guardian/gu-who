name := "gu-who"

version := "1.0-SNAPSHOT"

scalaVersion := "2.13.10"

updateOptions := updateOptions.value.withCachedResolution(true)

buildInfoKeys := Seq[BuildInfoKey](
  name,
  "gitCommitId" -> Option(System.getenv("SOURCE_VERSION")).getOrElse("unknown")
)

buildInfoPackage := "app"

lazy val root = (project in file(".")).enablePlugins(PlayScala, BuildInfoPlugin)

resolvers ++= Resolver.sonatypeOssRepos("releases")

libraryDependencies ++= Seq(
  filters,
  ws,
  "com.softwaremill.macwire" %% "macros" % "2.5.8" % Provided, // slight finesse: 'provided' as only used for compile
  "org.webjars" % "bootstrap" % "3.3.5",
  "com.adrianhurt" %% "play-bootstrap" % "1.6.1-P28-B3",
  "com.madgag" %% "play-git-hub" % "5.3",
  "org.kohsuke" % "github-api" % "1.313",
  "com.github.nscala-time" %% "nscala-time" % "2.32.0",
  "com.madgag.scala-git" %% "scala-git-test" % "4.6" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
)     

Compile / doc / sources := Seq.empty

Compile / packageDoc / publishArtifact := false
