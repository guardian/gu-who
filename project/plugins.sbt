// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url("heroku-sbt-plugin-releases",
  url("https://dl.bintray.com/heroku/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.heroku" % "sbt-heroku" % "0.3.0")

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.7")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.2")
