import org.scalatest.{FlatSpec, Matchers}
import shapeless._0
import shapeless.Nat._
import shapeless.test.illTyped

class MigrationTests extends FlatSpec with Matchers {
  case class Adam(value: String)
  case class Bob(value: String)
  case class Carol(value: String)

  "migrate" should "work for a chain of versions" in {
    trait Origin

    object Origin {
      implicit val version0: Versioned[Origin, Adam, _0] = Versioned[Origin, Adam, _0]
      implicit val version1: Versioned[Origin, Bob, _1] = Versioned[Origin, Bob, _1]
      implicit val version2: Versioned[Origin, Carol, _2] = Versioned[Origin, Carol, _2]
    }

    implicit val adam2Bob: MigrationFunction[Adam, Bob] = MigrationFunction(a => Bob(a.value))
    implicit val bob2Carol: MigrationFunction[Bob, Carol] = MigrationFunction(a => Carol(a.value))

    val m = Migrate[Origin].from[Adam, Carol]
    m.migrate(Adam("hi")) shouldBe Carol("hi")
  }
  "migrate" should "fail if a version tag is missing" in {
    trait Origin

    object Origin {
      implicit val version0: Versioned[Origin, Adam, _0] = Versioned[Origin, Adam, _0]
      implicit val version1: Versioned[Origin, Bob, _1] = Versioned[Origin, Bob, _1]
    }

    implicit val adam2Bob: MigrationFunction[Adam, Bob] = MigrationFunction(a => Bob(a.value))
    implicit val bob2Carol: MigrationFunction[Bob, Carol] = MigrationFunction(a => Carol(a.value))

    illTyped("Migrate[Origin].from[Adam, Carol]", "could not find implicit value for parameter m.*")
    illTyped("Migrate[Origin].from[Bob, Carol]", "diverging implicit expansion.*")
  }
  "migrate" should "fail if a migration function is missing" in {
    trait Origin

    object Origin {
      implicit val version0: Versioned[Origin, Adam, _0] = Versioned[Origin, Adam, _0]
      implicit val version1: Versioned[Origin, Bob, _1] = Versioned[Origin, Bob, _1]
      implicit val version2: Versioned[Origin, Carol, _2] = Versioned[Origin, Carol, _2]
    }

    implicit val adam2Bob: MigrationFunction[Adam, Bob] = MigrationFunction(a => Bob(a.value))

    illTyped("Migrate[Origin].from[Adam, Carol]", "could not find implicit value for parameter m.*")
    illTyped("Migrate[Origin].from[Bob, Carol]", "diverging implicit expansion.*")
  }
  "migrate" should "fail if trying to migrate to an older version" in {
    trait Origin

    object Origin {
      implicit val version2: Versioned[Origin, Adam, _2] = Versioned[Origin, Adam, _2]
      implicit val version1: Versioned[Origin, Bob, _1] = Versioned[Origin, Bob, _1]
      implicit val version0: Versioned[Origin, Carol, _0] = Versioned[Origin, Carol, _0]
    }

    implicit val adam2Bob: MigrationFunction[Adam, Bob] = MigrationFunction(a => Bob(a.value))
    implicit val bob2Carol: MigrationFunction[Bob, Carol] = MigrationFunction(a => Carol(a.value))

    illTyped("Migrate[Origin].from[Adam, Carol]", "diverging implicit expansion.*")
    illTyped("Migrate[Origin].from[Bob, Carol]", "diverging implicit expansion.*")
  }

}
