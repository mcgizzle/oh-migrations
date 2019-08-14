lazy val modules: List[ProjectReference] = List(core, circe)

resolvers ++= Seq (
  "Maven Central Server" at "http://repo1.maven.org/maven2",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Releases"  at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)

val projName = "oh-migrations"
val orgName = "io.github.mcgizzle"

lazy val root = project.in(file("."))
  .settings(
    moduleName := projName,
    description := "A data migration library at the type-level",
    organization := orgName
  )
  .aggregate(modules: _*)

lazy val core = mkProject("core")
  .settings(
    description := s"Core library for $name")

val circeVersion = "0.11.1"

lazy val circe = mkProject("circe")
  .settings(
    description := s"Circe interop for $name",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  ).dependsOn(core)

lazy val commonSettings = Seq(
  Compile / scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:implicitConversions", "-language:higherKinds", "-language:postfixOps", "-language:reflectiveCalls",
    "-unchecked",
    "-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-value-discard",
    "-Ypartial-unification",
    "-Xfuture"),
  moduleName := projName,
  organization := orgName)

import ReleaseTransformations._

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
        "org.typelevel" %% "cats-core" % "2.0.0-RC1",
        "com.chuusai" %% "shapeless" % "2.3.3",
        "org.scalatest" %% "scalatest" % "3.0.5" % "test"),
      publishTo := sonatypePublishTo.value
    )
