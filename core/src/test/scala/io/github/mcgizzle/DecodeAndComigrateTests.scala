package io.github.mcgizzle

import cats.data.Kleisli
import cats.{Applicative, Functor, Monad}
import org.scalatest.{FlatSpec, Matchers}
import shapeless.Nat._

class DecodeAndComigrateTests extends FlatSpec with Matchers {

  trait A

  val producerOfAs: A = new A {}
  case class UserV1(name: String)
  case class UserV2(name: String, favouriteColour: Option[String])
  case class UserV3(name: String, favouriteColour: Option[String], isFun: Boolean)
  case class FavouriteColour(value: String)
  case class UserV4(name: String, favouriteColour: FavouriteColour, isFun: Boolean)
  case class FunLevel(value: Int)
  case class UserV5(name: String, favouriteColour: FavouriteColour, isFun: FunLevel)
  case class Name(value: String)
  case class UserV6(name: Name, favouriteColour: FavouriteColour, isFun: FunLevel)

  trait User

  object User {
    implicit val v1 = Versioned[User, _1, UserV1]
    implicit val v2 = Versioned[User, _2, UserV2]
    implicit val v3 = Versioned[User, _3, UserV3]
    implicit val v4 = Versioned[User, _4, UserV4]
    implicit val v5 = Versioned[User, _5, UserV5]
    implicit val v6 = Versioned[User, _6, UserV6]
  }

  implicit val m1: UserV2 +=> UserV1 = MigrationFunction(u1 => UserV1(u1.name))

  "mcgizzle.DecodeAndComigrate" should "choose the latest decoder" in {

    implicit val d1: Decoder[A, UserV1] = Decoder.from[A, UserV1](_ => Some(UserV1("")))
    implicit val d2: Decoder[A, UserV2] = Decoder.from[A, UserV2](_ => Some(UserV2("decoded", Some("red"))))

    DecodeAndComigrate[User].from[A, _2, _1](producerOfAs) shouldBe Some(UserV2("decoded", Some("red")))

  }
  it should "migrate from earlier decoder" in {

    implicit val d1: Decoder[A, UserV1] = Decoder.from(_ => Some(UserV1("User1")))
    implicit val d2: Decoder[A, UserV2] = Decoder.from(_ => None)

    DecodeAndComigrate[User].from[A, _2, _1](producerOfAs) shouldBe Some(UserV1("User1"))
  }
  it should "choose latest and then migrate" in {

    implicit val m2: UserV3 +=> UserV2 = MigrationFunction(u3 => UserV2(u3.name, u3.favouriteColour))

    implicit val d1: Decoder[A, UserV1] = Decoder.from(_ => Some(UserV1("User1")))
    implicit val d2: Decoder[A, UserV2] = Decoder.from(_ => Some(UserV2("I choose V2!", None)))
    implicit val d3: Decoder[A, UserV3] = Decoder.from(_ => None)

    DecodeAndComigrate[User].from[A, _3, _1](producerOfAs) shouldBe Some(UserV1("I choose V2!"))
  }
  it should "work for a long chain" in {

    implicit val m2: UserV3 +=> UserV2 = MigrationFunction(u3 => UserV2(u3.name, u3.favouriteColour))
    implicit val m3: UserV4 +=> UserV3 = MigrationFunction(u4 => UserV3(u4.name, Some(u4.favouriteColour.value), u4.isFun))
    implicit val m4: UserV5 +=> UserV4 = MigrationFunction(u5 => UserV4(u5.name, u5.favouriteColour, if(u5.isFun.value >= 50) true else false))
    implicit val m5: UserV6 +=> UserV5 = MigrationFunction(u6 => UserV5(u6.name.value, u6.favouriteColour, u6.isFun))

    implicit val d1: Decoder[A, UserV1] = Decoder.from(_ => Some(UserV1("User1")))
    implicit val d2: Decoder[A, UserV2] = Decoder.from(_ => Some(UserV2("I choose V2!", None)))
    implicit val d3: Decoder[A, UserV3] = Decoder.from(_ => None)
    implicit val d4: Decoder[A, UserV4] = Decoder.from(_ => Some(UserV4("Willy Wonka", FavouriteColour("green"), true)))
    implicit val d5: Decoder[A, UserV5] = Decoder.from(_ => None)
    implicit val d6: Decoder[A, UserV6] = Decoder.from(_ => None)

    DecodeAndComigrate[User].from[A, _6, _1](producerOfAs) shouldBe Some(UserV1(""))

  }
  "DecodeAndComigrateF" should "parse effectful migrations for any arbritrary effect" in {

    case class Effect[A](run: A)

    var effectApplied = false

    implicit def functor: Functor[Effect] = new Functor[Effect] {
      def map[A, B](fa: Effect[A])(f: A => B): Effect[B] = Effect(f(fa.run))
    }

    implicit def applicative: Applicative[Effect] = new Applicative[Effect] {
      def pure[A](x: A): Effect[A] = Effect(x)

      def ap[A, B](ff: Effect[A => B])(fa: Effect[A]): Effect[B] = Effect(ff.run(fa.run))
    }

    implicit def monad: Monad[Effect] = new Monad[Effect] {
      def pure[A](x: A): Effect[A] = applicative.pure(x)

      def flatMap[A, B](fa: Effect[A])(f: A => Effect[B]): Effect[B] = {
        effectApplied = true
        f(fa.run)
      }

      def tailRecM[A, B](a: A)(f: A => Effect[Either[A, B]]): Effect[B] = ???
    }

    implicit val m1 = MigrationFunctionF[Effect].from{ u2: UserV2 => Effect(UserV1(u2.name))}
    implicit val m2 = MigrationFunctionF[Effect].from{ u3: UserV3 => Effect(UserV2(u3.name, u3.favouriteColour))}

    implicit val d1: Decoder[String, UserV1] = Decoder.from(_ => Some(UserV1("User1")))
    implicit val d2: Decoder[String, UserV2] = Decoder.from(_ => Some(UserV2("I choose V2!", None)))
    implicit val d3: Decoder[String, UserV3] = Decoder.from(_ => None)

    monad.flatMap(Effect(""))(s => DecodeAndComigrateF[Effect, User].from[String, _3, _1](s)).run shouldBe Some(UserV1(""))

    effectApplied shouldBe true

  }
}
