name := "gu-who"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.4"

herokuAppName in Compile := "gu-who"

herokuJdkVersion in Compile := "1.8"

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
  "org.webjars" % "bootstrap" % "3.2.0",
  "org.kohsuke" % "github-api" % "1.59",
  "com.github.nscala-time" %% "nscala-time" % "1.4.0",
  "com.squareup.okhttp" % "okhttp" % "2.0.0",
  "com.squareup.okhttp" % "okhttp-urlconnection" % "2.0.0",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3-1",
  "org.eclipse.jgit" % "org.eclipse.jgit" % "3.5.1.201410131835-r",
  "com.madgag.scala-git" %% "scala-git" % "2.7",
  "com.madgag.scala-git" %% "scala-git-test" % "2.7" % "test"
)     

sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

