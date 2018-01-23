name := "timerboard-net-sso-scala"

version := "0.1"

scalaVersion := "2.12.4"

val http4sVersion = "0.18.0-M8"
val fs2Version    = "0.10.0-M10"
val circeVersion  = "0.9.0-M3"
val catsVersion   = "1.0.0-RC2"
val doobieVersion = "0.5.0-M13"
val tsecVersion = "0.0.1-M6"

// JSON parser
libraryDependencies ++= Seq("circe-core", "circe-generic", "circe-parser", "circe-jawn").map("io.circe" %% _ % circeVersion)
// http layers
libraryDependencies ++= Seq("http4s-core", "http4s-dsl", "http4s-blaze-server", "http4s-twirl", "http4s-blaze-client").map("org.http4s" %% _ % http4sVersion)
libraryDependencies += "org.http4s" %% "http4s-circe" % http4sVersion excludeAll(ExclusionRule(organization = "io.circe"))
// templates
enablePlugins(SbtTwirl)
// YAML
libraryDependencies += "io.circe" %% "circe-yaml" % "0.7.0-M2"
// command line parser
libraryDependencies ++= Seq("com.monovore" %% "decline" % "0.4.0")
// databases
libraryDependencies ++= Seq("doobie-core", "doobie-postgres").map("org.tpolecat" %% _ % doobieVersion)
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.25"
// auth
resolvers += "jmcardon at bintray" at "https://dl.bintray.com/jmcardon/tsec"
val tsecV = "0.0.1-M6"
libraryDependencies ++= Seq(
  "io.github.jmcardon" %% "tsec-common" % tsecV,
  "io.github.jmcardon" %% "tsec-password" % tsecV,
  "io.github.jmcardon" %% "tsec-symmetric-cipher" % tsecV,
  "io.github.jmcardon" %% "tsec-mac" % tsecV,
  "io.github.jmcardon" %% "tsec-signatures" % tsecV,
  "io.github.jmcardon" %% "tsec-md" % tsecV,
  "io.github.jmcardon" %% "tsec-jwt-mac" % tsecV,
  "io.github.jmcardon" %% "tsec-jwt-sig" % tsecV,
  "io.github.jmcardon" %% "tsec-http4s" % tsecV
)


// testing
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"
libraryDependencies += "com.github.docker-java" % "docker-java" % "3.0.3" % "test"

