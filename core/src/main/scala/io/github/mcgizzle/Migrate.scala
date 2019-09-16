package io.github.mcgizzle

import cats.{FlatMap, Id}
import cats.data.Kleisli
import shapeless.{Nat, Succ}

final class Migrate[F[_], Origin] {

  def from[Start <: Nat, End <: Nat](implicit m: MigrationBuilder[F, Origin, Start, End]): m.Data1 => F[m.Data2] =
    m.migrate(_)

}

object Migrate {
  def apply[Origin]: Migrate[Id, Origin] = new Migrate[Id, Origin]
}

sealed trait MigrationBuilder[F[_], Origin, Start <: Nat, End <: Nat] {

  type Data1
  type Data2

  def migrate(d: Data1): F[Data2]
}

object MigrationBuilder {

  import cats.syntax.flatMap._

  type Aux[F[_], Origin, Start <: Nat, End <: Nat, D1, D2] = MigrationBuilder[F, Origin, Start, End] {
    type Data1 = D1
    type Data2 = D2
  }

  implicit def recurse[F[_]: FlatMap, Origin, Start <: Nat, N <: Nat, DStart, DN, DEnd](
      implicit
      v1: Versioned.Aux[Origin, Start, DStart],
      v2: Versioned.Aux[Origin, Succ[N], DEnd],
      v3: Versioned.Aux[Origin, N, DN],
      f: Kleisli[F, DN, DEnd],
      r: MigrationBuilder.Aux[F, Origin, Start, N, DStart, DN]
  ): MigrationBuilder.Aux[F, Origin, Start, Succ[N], DStart, DEnd] = new MigrationBuilder[F, Origin, Start, Succ[N]] {
    type Data1 = DStart
    type Data2 = DEnd

    def migrate(d: DStart): F[DEnd] = r.migrate(d).flatMap(f.run)
  }

  implicit def base[F[_], Origin, V <: Nat, D, DNext](
      implicit
      v: Versioned.Aux[Origin, V, D],
      v1: Versioned.Aux[Origin, Succ[V], DNext],
      f: Kleisli[F, D, DNext]
  ): MigrationBuilder.Aux[F, Origin, V, Succ[V], D, DNext] = new MigrationBuilder[F, Origin, V, Succ[V]] {
    type Data1 = D
    type Data2 = DNext
    def migrate(d: D): F[DNext] = f(d)
  }
}
