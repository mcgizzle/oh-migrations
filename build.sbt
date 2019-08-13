import sbt._

name := "my-gr8-library"

version := "0.0.1"

scalaVersion := "2.12.9"

lazy val modules: List[ProjectReference] = List(core, circe)

lazy val root = project.in(file("."))
  .settings(
    name := s"$name",
    description := "A data migration library at the type-level",
    version := s"$version"
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
    "-Xfuture"))

def mkProject(p: String) =
  Project(p, file(p))
    .settings(commonSettings)
    .settings(
      moduleName := s"$name-$p",
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % "2.0.0-RC1",
        "com.chuusai" %% "shapeless" % "2.3.3",
        "org.scalatest" %% "scalatest" % "3.0.5" % "test")
    )