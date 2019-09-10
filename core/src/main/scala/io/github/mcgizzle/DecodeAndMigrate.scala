package io.github.mcgizzle

import cats.implicits._
import cats.{Applicative, Id}
import shapeless.{Lazy, Nat, Succ}

final class DecodeAndMigrate[F[_], Origin] {
  def from[A, Start <: Nat, Target <: Nat](a: A)(implicit ev: DecodeAndMigrateBuilder[F, Origin, A, Start, Target]): F[Option[ev.Out]] = ev.decodeAndMigrate(a)
}

object DecodeAndMigrate {
  def apply[Origin]: DecodeAndMigrate[Id, Origin] = new DecodeAndMigrate[Id, Origin]
}
object DecodeAndMigrateF {
  def apply[F[_], Origin]: DecodeAndMigrate[F, Origin] = new DecodeAndMigrate[F, Origin]
}


sealed trait DecodeAndMigrateBuilder[F[_], Origin, A, Start <: Nat, Target <: Nat] {

  type Out

  def decodeAndMigrate(a: A): F[Option[Out]]

}

object DecodeAndMigrateBuilder  {

  type Aux[F[_], Origin, A, Start <: Nat, Target <: Nat, Out0] = DecodeAndMigrateBuilder[F, Origin, A, Start, Target] { type Out = Out0 }

  implicit def recurse[F[_]: Applicative, Origin, A, N <: Nat, Target <: Nat, D, DTarget]
  (implicit
   v: Versioned.Aux[Origin, N, D],
   ev: Decoder[A, D],
   m: MigrationBuilder.Aux[F, Origin, N, Target, D, DTarget],
   r: Lazy[DecodeAndMigrateBuilder.Aux[F, Origin, A, Succ[N], Target, DTarget]]
  ): DecodeAndMigrateBuilder.Aux[F, Origin, A, N, Target, DTarget] =
    new DecodeAndMigrateBuilder[F, Origin, A, N, Target] {
      type Out = DTarget
      def decodeAndMigrate(a: A): F[Option[DTarget]] =
        r.value.decodeAndMigrate(a).product(ev.decode(a).map(m.migrate).sequence).map { case (a, b) => a <+> b }
    }

  implicit def base[F[_]: Applicative, Origin, A, N <: Nat, DN]
  (implicit
   v: Versioned.Aux[Origin, N, DN],
   d: Decoder[A, DN]
  ): DecodeAndMigrateBuilder.Aux[F, Origin, A, N, N, DN] =
    new DecodeAndMigrateBuilder[F, Origin, A, N, N] {
      type Out = DN
      def decodeAndMigrate(a: A): F[Option[DN]] = Applicative[F].pure(d.decode(a))
  }
}



