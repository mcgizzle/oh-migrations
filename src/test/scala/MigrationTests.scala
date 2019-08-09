import Versioned.Aux
import org.scalatest.{FlatSpec, Matchers}
import shapeless.{Nat, _0}
import shapeless.Nat._
import shapeless.test.illTyped

class MigrationTests extends FlatSpec with Matchers {

  case class UserV1(name: String, age: Int)
  case class Name(value: String)
  case class UserV2(name: Name, age: Int)
  case class Age(vale: Int)
  case class UserV3(name: Name, age: Age)

  trait User

  object User {
    implicit val version0: Versioned.Aux[User, _1, UserV1] = Versioned[User, _1, UserV1]
    implicit val version1: Versioned.Aux[User, _2, UserV2] = Versioned[User, _2, UserV2]
    implicit val version2: Versioned.Aux[User, _3, UserV3] = Versioned[User, _3, UserV3]
  }

  "Migrating based on versions" should "work for a chain" in {

    implicit val adam2Bob: MigrationFunction[UserV1, UserV2] = MigrationFunction(a => UserV2(Name(a.name), a.age))
    implicit val bob2Carol: MigrationFunction[UserV2, UserV3] = MigrationFunction(a => UserV3(a.name, Age(a.age)))

    Migrate[User].from[_1, _2].apply(UserV1("John", 7)) shouldBe UserV2(Name("John"), 7)
    Migrate[User].from[_1, _3].apply(UserV1("John", 7)) shouldBe UserV3(Name("John"), Age(7))

  }

  it should "not work for a non-existent version numbers" in {

    illTyped("""Migrate[Origin].migrate[_0, _3].apply(Adam("hi")) shouldBe Bob("hi")""")

  }

  it should "not work for wrong tag" in {

    trait Dog

    illTyped("""Migrate[Dog].from[_0, _2].apply(Adam("hi")) shouldBe Bob("hi")""", "could not find implicit value for parameter m.*")

  }

  "Migrating based on data type" should "work for a chain" in {

    implicit val adam2Bob: MigrationFunction[UserV1, UserV2] = MigrationFunction(a => UserV2(Name(a.name), a.age))
    implicit val bob2Carol: MigrationFunction[UserV2, UserV3] = MigrationFunction(a => UserV3(a.name, Age(a.age)))

    Migrate[User].fromD[UserV1, UserV3].migrate(UserV1("John", 7)) shouldBe UserV3(Name("John"), Age(7))

  }
  it should "fail if a version tag is missing" in {
    trait User_

    object User_ {
      implicit val version0: Versioned.Aux[User_, _0, UserV1] = Versioned[User_, _0, UserV1]
      implicit val version1: Versioned.Aux[User_, _1, UserV2] = Versioned[User_, _1, UserV2]
    }

    implicit val adam2Bob: MigrationFunction[UserV1, UserV2] = MigrationFunction(a => UserV2(Name(a.name), a.age))
    implicit val bob2Carol: MigrationFunction[UserV2, UserV3] = MigrationFunction(a => UserV3(a.name, Age(a.age)))

    illTyped("Migrate[User_].fromD[UserV1, UserV3]", "could not find implicit value for parameter m.*")
    illTyped("Migrate[User_].fromD[UserV2, UserV3]", "diverging implicit expansion.*")
  }
  it should "fail if a migration function is missing" in {

    implicit val adam2Bob: MigrationFunction[UserV1, UserV2] = MigrationFunction(a => UserV2(Name(a.name), a.age))

    illTyped("Migrate[User].fromD[UserV1, UserV3]", "could not find implicit value for parameter m.*")
    illTyped("Migrate[User].fromD[UserV2, UserV3]", "diverging implicit expansion.*")
  }

  it should "fail if trying to migrate to an older version" in {

    trait User_

    object User_ {
      implicit val version2: Versioned.Aux[User_, _2, UserV1] = Versioned[User_, _2, UserV1]
      implicit val version1: Versioned.Aux[User_, _1, UserV2] = Versioned[User_, _1, UserV2]
      implicit val version0: Versioned.Aux[User_, _0, UserV3] = Versioned[User_, _0, UserV3]
    }

    implicit val adam2Bob: MigrationFunction[UserV1, UserV2] = MigrationFunction(a => UserV2(Name(a.name), a.age))
    implicit val bob2Carol: MigrationFunction[UserV2, UserV3] = MigrationFunction(a => UserV3(a.name, Age(a.age)))

    illTyped("Migrate[User_].fromD[UserV1, UserV3]", "diverging implicit expansion.*")
    illTyped("Migrate[User_].fromD[UserV2, UserV3]", "diverging implicit expansion.*")
  }

}
