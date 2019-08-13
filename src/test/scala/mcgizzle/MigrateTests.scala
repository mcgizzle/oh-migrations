package mcgizzle

import org.scalatest.{FlatSpec, Matchers}
import shapeless.Nat._
import shapeless.test.illTyped

class MigrateTests extends FlatSpec with Matchers {

  case class UserV1(name: String, age: Int)
  case class Name(value: String)
  case class UserV2(name: Name, age: Int)
  case class Age(vale: Int)
  case class UserV3(name: Name, age: Age)

  trait User

  object User {
    implicit val version1: Versioned.Aux[User, _1, UserV1] = Versioned[User, _1, UserV1]
    implicit val version2: Versioned.Aux[User, _2, UserV2] = Versioned[User, _2, UserV2]
    implicit val version3: Versioned.Aux[User, _3, UserV3] = Versioned[User, _3, UserV3]
  }

  "Migrating based on versions" should "work for a short chain" in {

    implicit val V1toV2: MigrationFunction[UserV1, UserV2] = MigrationFunction(a => UserV2(Name(a.name), a.age))
    implicit val V2toV3: MigrationFunction[UserV2, UserV3] = MigrationFunction(a => UserV3(a.name, Age(a.age)))

    Migrate[User].from[_1, _2].apply(UserV1("John", 7)) shouldBe UserV2(Name("John"), 7)
    Migrate[User].from[_1, _3].apply(UserV1("John", 7)) shouldBe UserV3(Name("John"), Age(7))

  }

  it should "work for a long chain" in {

    case class UserV4(name: Name, age: Age, email: Option[String])
    case class UserV5(name: Name, age: Age, email: Option[String], address: Option[String])
    case class FirstName(value: String)
    case class LastName(value: String)
    case class UserV6(firstName: FirstName, lastName: LastName, age: Age, email: Option[String], address: Option[String])
    case class Address(value: Option[String])
    case class UserV7(firstName: FirstName, lastName: LastName, age: Age, email: Option[String], address: Address)
    case class ContactInfo(email: Option[String], address: Address)
    case class UserV8(firstName: FirstName, lastName: LastName, age: Age, contactInfo: ContactInfo)

    trait User_

    object User_ {
      implicit val version1: Versioned.Aux[User_, _1, UserV1] = Versioned[User_, _1, UserV1]
      implicit val version2: Versioned.Aux[User_, _2, UserV2] = Versioned[User_, _2, UserV2]
      implicit val version3: Versioned.Aux[User_, _3, UserV3] = Versioned[User_, _3, UserV3]
      implicit val version4: Versioned.Aux[User_, _4, UserV4] = Versioned[User_, _4, UserV4]
      implicit val version5: Versioned.Aux[User_, _5, UserV5] = Versioned[User_, _5, UserV5]
      implicit val version6: Versioned.Aux[User_, _6, UserV6] = Versioned[User_, _6, UserV6]
      implicit val version7: Versioned.Aux[User_, _7, UserV7] = Versioned[User_, _7, UserV7]
      implicit val version8: Versioned.Aux[User_, _8, UserV8] = Versioned[User_, _8, UserV8]
    }

    implicit val V1toV2: MigrationFunction[UserV1, UserV2] = MigrationFunction(a => UserV2(Name(a.name), a.age))
    implicit val V2toV3: MigrationFunction[UserV2, UserV3] = MigrationFunction(a => UserV3(a.name, Age(a.age)))
    implicit val V3toV4: MigrationFunction[UserV3, UserV4] = MigrationFunction(a => UserV4(a.name, a.age, None))
    implicit val V4toV5: MigrationFunction[UserV4, UserV5] = MigrationFunction(a => UserV5(a.name, a.age, None, None))
    implicit val V5toV6: MigrationFunction[UserV5, UserV6] = MigrationFunction{ a =>
      val split = a.name.value.split(" ")
      UserV6(FirstName(split.head), LastName(split.last), a.age, None, None)
    }
    implicit val V6toV7: MigrationFunction[UserV6, UserV7] = MigrationFunction( a => UserV7(a.firstName, a.lastName, a.age, a.email, Address(a.address)))
    implicit val V7toV8: MigrationFunction[UserV7, UserV8] = MigrationFunction( a => UserV8(a.firstName, a.lastName, a.age, ContactInfo(a.email, a.address)))


    Migrate[User_].from[_1, _8].apply(UserV1("John Smith", 43)) shouldBe UserV8(FirstName("John"), LastName("Smith"), Age(43), ContactInfo(None, Address(None)))
  }

  it should "not work for a non-existent version numbers" in {

    illTyped("""mcgizzle.Migrate[Origin].migrate[_0, _3].apply(Adam("hi")) shouldBe Bob("hi")""")

  }

  it should "not work for wrong tag" in {

    trait Dog

    implicit val V1toV2: MigrationFunction[UserV1, UserV2] = MigrationFunction(a => UserV2(Name(a.name), a.age))
    implicit val V2toV3: MigrationFunction[UserV2, UserV3] = MigrationFunction(a => UserV3(a.name, Age(a.age)))

    illTyped("""mcgizzle.Migrate[Dog].from[_0, _2].apply(Adam("hi")) shouldBe Bob("hi")""", "could not find implicit value for parameter m.*")

  }

  it should "fail going back a version" in {
    implicit val V1toV2: MigrationFunction[UserV1, UserV2] = MigrationFunction(a => UserV2(Name(a.name), a.age))
    implicit val V2toV3: MigrationFunction[UserV2, UserV3] = MigrationFunction(a => UserV3(a.name, Age(a.age)))

    illTyped("""mcgizzle.Migrate[User].from[_2, _1].apply(UserV2(Name(""), 4))""", "could not find implicit value for parameter m.*")
  }

}
