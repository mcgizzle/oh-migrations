package io.github.mcgizzle

import cats.data.Kleisli
import cats.implicits._
import cats.{Applicative, Id, Monad}
import shapeless.{Lazy, Nat, Succ}

final class DecodeAndComigrate[F[_], Origin] {
  def from[A, Start <: Nat, Target <: Nat](a: A)(implicit ev: DecodeAndComigrateBuilder[F, Origin, A, Start, Target]): F[Option[ev.Out]] = ev.decodeAndComigrate(a)
}

object DecodeAndComigrate {
  def apply[Origin]: DecodeAndComigrate[Id, Origin] = new DecodeAndComigrate[Id, Origin]
}
object DecodeAndComigrateF {
  def apply[F[_], Origin]: DecodeAndComigrate[F, Origin] = new DecodeAndComigrate[F, Origin]
}


sealed trait DecodeAndComigrateBuilder[F[_], Origin, A, Start <: Nat, Target <: Nat] {

  type Out

  def decodeAndComigrate(a: A): F[Option[Out]]

}

object DecodeAndComigrateBuilder  {

  type Aux[F[_], Origin, A, Start <: Nat, Target <: Nat, Out0] = DecodeAndComigrateBuilder[F, Origin, A, Start, Target] { type Out = Out0 }

  /*/

    3 -> 1

    recurse[3, 1]
      v[3]
      d[1]
      m[3, 1]
      r[3, 2]

    recurse[3, 2]
      v[3]
      d[2]
      m[2, 1]
      r[3, 3]

    base[3, 3]
      v[3]
      d[3]

   */



  implicit def recurse[F[_]: Monad, Origin, A, High <: Nat, Low <: Nat, DHigh, DLow, DSuccLow]
  (implicit
   v: Versioned.Aux[Origin, High, DHigh],
   v1: Versioned.Aux[Origin, Low, DLow],
   v2: Versioned.Aux[Origin, Succ[Low], DSuccLow],
   ev: Decoder[A, DLow],
   m: ComigrationBuilder.Aux[F, Origin, High, Low, DHigh, DLow],
   f: Kleisli[F, DSuccLow, DLow],
   r: Lazy[DecodeAndComigrateBuilder.Aux[F, Origin, A, High, Succ[Low], DSuccLow]]
  ): DecodeAndComigrateBuilder.Aux[F, Origin, A, High, Low, DLow] =
    new DecodeAndComigrateBuilder[F, Origin, A, High, Low] {
      type Out = DLow
      def decodeAndComigrate(a: A): F[Option[DLow]] = r.value.decodeAndComigrate(a).flatMap(_.map(f.run))(_ <+> ev.decode(a))


     // r.value.decodeAndComigrate(a).flatMap(_.map(m.migrate).sequence).map(_ <+> ev.decode(a))
//        r.value.decodeAndComigrate(a).map(_ <+> ev.decode(a).map(m.migrate))
    }

  implicit def base[F[_]: Applicative, Origin, A, N <: Nat, DN]
  (implicit
   v: Versioned.Aux[Origin, N, DN],
   d: Decoder[A, DN]
  ): DecodeAndComigrateBuilder.Aux[F, Origin, A, N, N, DN] =
    new DecodeAndComigrateBuilder[F, Origin, A, N, N] {
      type Out = DN
      def decodeAndComigrate(a: A): F[Option[DN]] = Applicative[F].pure(d.decode(a))
  }
}



