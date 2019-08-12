import org.scalatest.{FlatSpec, Matchers}
import shapeless.Nat._

class DecodeAndMigrateTests extends FlatSpec with Matchers {

  "DecodeAndMigrate" should "decode and and migrate, choosing the latest decoder" in {

    case class UserV1(name: String)
    case class UserV2(name: String, favouriteColour: Option[String])

    trait User

    object User {
      implicit val v1: Versioned.Aux[User, _1, UserV1] = Versioned[User, _1, UserV1]
      implicit val v2: Versioned.Aux[User, _2, UserV2] = Versioned[User, _2, UserV2]
    }

    implicit val m1: MigrationFunction[UserV1, UserV2] = MigrationFunction(u1 => UserV2(u1.name, None))

    trait A

    implicit val d1: Decoder[A, UserV1] = new Decoder[A, UserV1] {
      def from(a: A): Option[UserV1] = Some(UserV1(""))
    }

    implicit val d2: Decoder[A, UserV2] = new Decoder[A, UserV2] {
      def from(a: A): Option[UserV2] = Some(UserV2("decoded", Some("red")))
    }

    val a: A = new A {}

    implicit val x = DecodeAndMigrateBuilder.base[User, A, _2, UserV2](a)
    implicit val y = DecodeAndMigrateBuilder.decodeOrRecurse[User, A, _1, _2, UserV1, UserV2](a)

    DecodeAndMigrate[User].decode[A, _1, _2](a) shouldBe Some(UserV2("decoded", Some("red")))

  }
  it should "choose an earlier decoder in later fails" in {
    case class UserV1(name: String)
    case class UserV2(name: String, favouriteColour: Option[String])

    trait User

    object User {
      implicit val v1: Versioned.Aux[User, _1, UserV1] = Versioned[User, _1, UserV1]
      implicit val v2: Versioned.Aux[User, _2, UserV2] = Versioned[User, _2, UserV2]
    }

    implicit val m1: MigrationFunction[UserV1, UserV2] = MigrationFunction(u1 => UserV2(u1.name, None))

    trait A

    implicit val d1: Decoder[A, UserV1] = new Decoder[A, UserV1] {
      def from(a: A): Option[UserV1] = Some(UserV1("User1"))
    }

    implicit val d2: Decoder[A, UserV2] = new Decoder[A, UserV2] {
      def from(a: A): Option[UserV2] = None
    }

    val a: A = new A {}

    implicit val x = DecodeAndMigrateBuilder.base[User, A, _2, UserV2](a)
    implicit val y = DecodeAndMigrateBuilder.decodeOrRecurse[User, A, _1, _2, UserV1, UserV2](a)

    DecodeAndMigrate[User].decode[A, _1, _2](a) shouldBe Some(UserV2("User1", None))
  }
}
