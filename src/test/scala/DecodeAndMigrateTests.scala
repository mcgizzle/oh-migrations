import org.scalatest.{FlatSpec, Matchers}
import shapeless.Nat._

class DecodeAndMigrateTests extends FlatSpec with Matchers {

  trait A

  val producerOfAs: A = new A {}
  case class UserV1(name: String)
  case class UserV2(name: String, favouriteColour: Option[String])
  case class UserV3(name: String, favouriteColour: Option[String], isFun: Boolean)

  trait User

  object User {
    implicit val v1: Versioned.Aux[User, _1, UserV1] = Versioned[User, _1, UserV1]
    implicit val v2: Versioned.Aux[User, _2, UserV2] = Versioned[User, _2, UserV2]
    implicit val v3: Versioned.Aux[User, _3, UserV3] = Versioned[User, _3, UserV3]
  }

  implicit val m1: MigrationFunction[UserV1, UserV2] = MigrationFunction(u1 => UserV2(u1.name, None))

  "DecodeAndMigrate" should "choose the latest decoder" in {

    implicit val d1: Decoder[A, UserV1] = Decoder.from(_ => Some(UserV1("")))
    implicit val d2: Decoder[A, UserV2] = Decoder.from(_ => Some(UserV2("decoded", Some("red"))))


    implicit val y0 = DecodeAndMigrateBuilder.base[User, A, _2, UserV2](producerOfAs)
    implicit val y1 = DecodeAndMigrateBuilder.decodeOrRecurse[User, A, _1, _2, UserV1, UserV2](producerOfAs)

    DecodeAndMigrate[User].decode[A, _1, _2](producerOfAs) shouldBe Some(UserV2("decoded", Some("red")))

  }
  it should "migrate from earlier decoder" in {

    implicit val d1: Decoder[A, UserV1] = Decoder.from(_ => Some(UserV1("User1")))
    implicit val d2: Decoder[A, UserV2] = Decoder.from(_ => None)

    implicit val y0 = DecodeAndMigrateBuilder.base[User, A, _2, UserV2](producerOfAs)
    implicit val y1 = DecodeAndMigrateBuilder.decodeOrRecurse[User, A, _1, _2, UserV1, UserV2](producerOfAs)

    DecodeAndMigrate[User].decode[A, _1, _2](producerOfAs) shouldBe Some(UserV2("User1", None))
  }
  it should "choose latest and then migrate" in {

    implicit val m2: MigrationFunction[UserV2, UserV3] = MigrationFunction(u2 => UserV3(u2.name, Some("blue"), false))

    implicit val d1: Decoder[A, UserV1] = Decoder.from(_ => Some(UserV1("User1")))
    implicit val d2: Decoder[A, UserV2] = Decoder.from(_ => Some(UserV2("I choose V2!", None)))
    implicit val d3: Decoder[A, UserV3] = Decoder.from(_ => None)

    implicit val y0 = DecodeAndMigrateBuilder.base[User, A, _3, UserV3](producerOfAs)
    implicit val y1 = DecodeAndMigrateBuilder.decodeOrRecurse[User, A, _2, _3, UserV2, UserV3](producerOfAs)
    implicit val y2 = DecodeAndMigrateBuilder.decodeOrRecurse[User, A, _1, _3, UserV1, UserV3](producerOfAs)

    DecodeAndMigrate[User].decode[A, _1, _3](producerOfAs) shouldBe Some(UserV3("I choose V2!", Some("blue"), false))
  }
}
