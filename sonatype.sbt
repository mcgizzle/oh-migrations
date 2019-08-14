sonatypeProfileName := "io.github.mcgizzle"

// License of your choice
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("mcgizzle", "oh-migrations", "mcgroas@tcd.ie"))

ThisBuild / publishMavenStyle := true

developers := List(
  Developer(id="mcgizzle", name="Sean McGroarty", email="mcgroas@tcd.ie", url=url("https://github.com/mcgizzle"))
)
