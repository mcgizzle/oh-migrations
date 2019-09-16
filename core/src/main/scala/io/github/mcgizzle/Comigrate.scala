package io.github.mcgizzle

import cats.{FlatMap, Id}
import cats.data.Kleisli
import io.github.mcgizzle
import shapeless.{Lazy, Nat, Succ}

final class Comigrate[F[_], Origin] {

  def from[Start <: Nat, End <: Nat](implicit m: ComigrationBuilder[F, Origin, Start, End]): m.Data1 => F[m.Data2] =
    m.migrate(_)

}

object Comigrate {
  def apply[Origin]: Comigrate[Id, Origin] = new Comigrate[Id, Origin]
}

sealed trait ComigrationBuilder[F[_], Origin, Start <: Nat, End <: Nat] {

  type Data1
  type Data2

  def migrate(d: Data1): F[Data2]
}

object ComigrationBuilder {

  import cats.syntax.flatMap._

  type Aux[F[_], Origin, Start <: Nat, End <: Nat, D1, D2] = ComigrationBuilder[F, Origin, Start, End] {
    type Data1 = D1
    type Data2 = D2
  }

  implicit def recurse[F[_]: FlatMap, Origin, Low <: Nat, High <: Nat, DLow, DSuccLow, DHigh](
      implicit
      v1: Versioned.Aux[Origin, Low, DLow],
      v2: Versioned.Aux[Origin, Succ[Low], DSuccLow],
      v3: Versioned.Aux[Origin, High, DHigh],
      f: Kleisli[F, DSuccLow, DLow],
      r: Lazy[ComigrationBuilder.Aux[F, Origin, High, Succ[Low], DHigh, DSuccLow]]
  ): ComigrationBuilder.Aux[F, Origin, High, Low, DHigh, DLow] = new ComigrationBuilder[F, Origin, High, Low] {
    type Data1 = DHigh
    type Data2 = DLow

    def migrate(d: DHigh): F[DLow] = r.value.migrate(d).flatMap(f.run)
  }

  implicit def base[F[_], Origin, V <: Nat, D, DPrev](
      implicit
      v: Versioned.Aux[Origin, V, DPrev],
      v1: Versioned.Aux[Origin, Succ[V], D],
      f: Kleisli[F, D, DPrev]
  ): ComigrationBuilder.Aux[F, Origin, Succ[V], V, D, DPrev] = new ComigrationBuilder[F, Origin, Succ[V], V] {
    type Data1 = D
    type Data2 = DPrev
    def migrate(d: D): F[DPrev] = f(d)
  }
}
