import com.gu.riffraff.artifact.BuildInfo

name := "gu-who"

version := "1.0-SNAPSHOT"

scalaVersion := "2.13.10"

buildInfoPackage := "app"

enablePlugins(RiffRaffArtifact, BuildInfoPlugin)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffArtifactResources := Seq(
  (assembly/assemblyOutputPath).value -> s"${name.value}/${name.value}.jar",
  file("cdk/cdk.out/GoogleSearchIndexingObservatory-PROD.template.json") -> s"cdk.out/GoogleSearchIndexingObservatory-PROD.template.json",
  file("cdk/cdk.out/riff-raff.yaml") -> s"riff-raff.yaml"
)

resolvers ++= Resolver.sonatypeOssRepos("releases")

libraryDependencies ++= Seq(
  "com.madgag.play-git-hub" %% "core" % "5.10",
  "com.madgag.play-git-hub" %% "testkit" % "5.10" % Test,
  "org.kohsuke" % "github-api" % "1.314",
  "com.github.nscala-time" %% "nscala-time" % "2.32.0",
  "com.madgag.scala-git" %% "scala-git-test" % "4.6" % Test,

  "com.amazonaws" % "aws-lambda-java-core" % "1.2.2",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.2",
  "org.slf4j" % "log4j-over-slf4j" % "2.0.5", //  log4j-over-slf4j provides `org.apache.log4j.MDC`, which is dynamically loaded by the Lambda runtime
  "ch.qos.logback" % "logback-classic" % "1.4.5",
  "com.lihaoyi" %% "upickle" % "2.0.0",
)

buildInfoPackage := "com.gu.who"
buildInfoKeys := {
  lazy val buildInfo = BuildInfo(baseDirectory.value)
  Seq[BuildInfoKey](
    "buildNumber" -> buildInfo.buildIdentifier,
    "gitCommitId" -> buildInfo.revision,
    "buildTime" -> System.currentTimeMillis
  )
}