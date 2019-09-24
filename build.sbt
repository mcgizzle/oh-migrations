lazy val modules: List[ProjectReference] = List(core, circe, argonaut)

resolvers ++= Seq(
  "Maven Central Server" at "http://repo1.maven.org/maven2",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)

val projName = "oh-migrations"
val orgName = "io.github.mcgizzle"

lazy val root = project.in(file("."))
  .settings(
    scalaVersion := "2.13.0",
    moduleName := projName,
    description := "A data migration library at the type-level",
    organization := orgName,
    publishArtifact := false
  )
  .aggregate(modules: _*)

lazy val core = mkProject("core")
  .settings(
    description := s"Core library for $name")

val circeVersion = "0.12.0-RC4"

lazy val circe = mkProject("circe")
  .settings(
    description := s"Circe interop for $name",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  ).dependsOn(core)

val argonautVersion = "6.2.3"

lazy val argonaut = mkProject("argonaut")
  .settings(
    description := s"Argonaut interop for $name",
    libraryDependencies ++= Seq(
      "io.argonaut" %% "argonaut" % argonautVersion
    )
  ).dependsOn(core)

import xerial.sbt.Sonatype._

lazy val commonSettings = Seq(
  scalaVersion := "2.13.0",
  moduleName := projName,
  organization := orgName,
  Compile / scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:implicitConversions", "-language:higherKinds", "-language:postfixOps", "-language:reflectiveCalls",
    "-unchecked",
    "-Xfatal-warnings",
    "-Ywarn-dead-code",
    "-Ywarn-value-discard"),
  sonatypeProfileName := "io.github.mcgizzle",
  sonatypeProjectHosting := Some(GitHubHosting("mcgizzle", "oh-migrations", "mcgroas@tcd.ie")),
  scalafmtOnCompile := true,
  developers := List(
    Developer(id = "mcgizzle", name = "Sean McGroarty", email = "mcgroas@tcd.ie", url = url("https://github.com/mcgizzle"))
  ),
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")))

ThisBuild / publishMavenStyle := true

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseCrossBuild := true // true if you cross-build the project for multiple Scala versions
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)

def mkProject(p: String) =
  Project(p, file(p))
    .settings(commonSettings)
    .settings(
      moduleName += s"-$p",
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % "2.0.0",
        "com.chuusai" %% "shapeless" % "2.3.3",
        "org.scalatest" %% "scalatest" % "3.0.8" % "test"),
      publishTo := sonatypePublishTo.value
    )
