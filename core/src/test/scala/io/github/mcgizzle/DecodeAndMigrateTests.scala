package io.github.mcgizzle

import cats.data.Kleisli
import cats.{Applicative, Functor, Monad}
import org.scalatest.{FlatSpec, Matchers}
import shapeless.Nat._

class DecodeAndMigrateTests extends FlatSpec with Matchers {

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

  implicit val m1: UserV1 +=> UserV2 = MigrationFunction(u1 => UserV2(u1.name, None))

  "mcgizzle.DecodeAndMigrate" should "choose the latest decoder" in {

      implicit val d1: Decoder[A, UserV1] = Decoder.from[A, UserV1](_ => Right(UserV1("")))
      implicit val d2: Decoder[A, UserV2] = Decoder.from[A, UserV2](_ => Right(UserV2("decoded", Some("red"))))

      DecodeAndMigrate[User].from[A, _1, _2](producerOfAs) shouldBe Right(UserV2("decoded", Some("red")))

    }
  it should "migrate from earlier decoder" in {

      implicit val d1: Decoder[A, UserV1] = Decoder.from(_ => Right(UserV1("User1")))
      implicit val d2: Decoder[A, UserV2] = Decoder.from(_ => Left(DecodeFailure("error")))

      DecodeAndMigrate[User].from[A, _1, _2](producerOfAs) shouldBe Right(UserV2("User1", None))
    }
  it should "choose latest and then migrate" in {

      implicit val m2: UserV2 +=> UserV3 = MigrationFunction(u2 => UserV3(u2.name, Some("blue"), false))

      implicit val d1: Decoder[A, UserV1] = Decoder.from(_ => Right(UserV1("User1")))
      implicit val d2: Decoder[A, UserV2] = Decoder.from(_ => Right(UserV2("I choose V2!", None)))
      implicit val d3: Decoder[A, UserV3] = Decoder.from(_ => Left(DecodeFailure("error")))

      DecodeAndMigrate[User].from[A, _1, _3](producerOfAs) shouldBe Right(UserV3("I choose V2!", Some("blue"), false))
    }
  it should "work for a long chain" in {

      implicit val m2: UserV2 +=> UserV3 = MigrationFunction(u2 => UserV3(u2.name, Some("blue"), false))
      implicit val m3: UserV3 +=> UserV4 =
        MigrationFunction(u3 => UserV4(u3.name, FavouriteColour(u3.favouriteColour.getOrElse("Black")), false))
      implicit val m4: UserV4 +=> UserV5 =
        MigrationFunction(u4 => UserV5(u4.name, u4.favouriteColour, FunLevel(if (u4.isFun) 100 else 0)))
      implicit val m5: UserV5 +=> UserV6 = MigrationFunction(u5 => UserV6(Name(u5.name), u5.favouriteColour, u5.isFun))

      implicit val d1: Decoder[A, UserV1] = Decoder.from(_ => Right(UserV1("User1")))
      implicit val d2: Decoder[A, UserV2] = Decoder.from(_ => Right(UserV2("I choose V2!", None)))
      implicit val d3: Decoder[A, UserV3] = Decoder.from(_ => Left(DecodeFailure("error")))
      implicit val d4: Decoder[A, UserV4] =
        Decoder.from(_ => Right(UserV4("Willy Wonka", FavouriteColour("green"), true)))
      implicit val d5: Decoder[A, UserV5] = Decoder.from(_ => Left(DecodeFailure("error")))
      implicit val d6: Decoder[A, UserV6] = Decoder.from(_ => Left(DecodeFailure("error")))

      DecodeAndMigrate[User].from[A, _1, _6](producerOfAs) shouldBe Right(
        UserV6(Name("Willy Wonka"), FavouriteColour("green"), FunLevel(100))
      )

    }
  it should "provide the latest error" in {
      implicit val m2: UserV2 +=> UserV3 = MigrationFunction(u2 => UserV3(u2.name, Some("blue"), false))
      implicit val m3: UserV3 +=> UserV4 =
        MigrationFunction(u3 => UserV4(u3.name, FavouriteColour(u3.favouriteColour.getOrElse("Black")), false))

      implicit val d1: Decoder[A, UserV1] = Decoder.from(_ => Left(DecodeFailure("d1 error")))
      implicit val d2: Decoder[A, UserV2] = Decoder.from(_ => Left(DecodeFailure("d2 error")))
      implicit val d3: Decoder[A, UserV3] = Decoder.from(_ => Left(DecodeFailure("d3 error")))

      DecodeAndMigrate[User].from[A, _1, _3](producerOfAs).left.map(_.toString) shouldBe Left(
        DecodeFailure("d3 error").toString
      )
    }
  "DecodeAndMigrateF" should "parse effectful migrations for any arbritrary effect" in {

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

      implicit val m1 = MigrationFunctionF[Effect].from { u1: UserV1 =>
        Effect(UserV2(u1.name, None))
      }
      implicit val m2 = MigrationFunctionF[Effect].from { u2: UserV2 =>
        Effect(UserV3(u2.name, u2.favouriteColour, true))
      }

      implicit val d1: Decoder[String, UserV1] = Decoder.from(_ => Right(UserV1("User1")))
      implicit val d2: Decoder[String, UserV2] = Decoder.from(_ => Right(UserV2("I choose V2!", None)))
      implicit val d3: Decoder[String, UserV3] = Decoder.from(_ => Left(DecodeFailure("error")))

      monad.flatMap(Effect(""))(s => DecodeAndMigrateF[Effect, User].from[String, _1, _3](s)).run shouldBe Right(
        UserV3("I choose V2!", None, true)
      )

      effectApplied shouldBe true

    }
}
