name := "gu-who"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.5"

updateOptions := updateOptions.value.withCachedResolution(true)

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](
  name,
  BuildInfoKey.constant("gitCommitId", Option(System.getenv("BUILD_VCS_NUMBER")) getOrElse(try {
    "git rev-parse HEAD".!!.trim
  } catch {
    case e: Exception => "unknown"
  }))
)

buildInfoPackage := "app"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  filters,
  ws,
  "org.webjars" % "bootstrap" % "3.3.2-1",
  "org.kohsuke" % "github-api" % "1.62" exclude("org.jenkins-ci", "annotation-indexer"),
  "com.github.nscala-time" %% "nscala-time" % "1.8.0",
  "com.squareup.okhttp" % "okhttp" % "2.2.0",
  "com.squareup.okhttp" % "okhttp-urlconnection" % "2.2.0",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3-1",
  "org.eclipse.jgit" % "org.eclipse.jgit" % "3.6.2.201501210735-r",
  "com.madgag.scala-git" %% "scala-git" % "2.9",
  "com.madgag.scala-git" %% "scala-git-test" % "2.9" % "test"
)     

sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false
