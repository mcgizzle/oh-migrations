package io.github.mcgizzle

import cats.{FlatMap, Id}
import cats.data.Kleisli
import io.github.mcgizzle
import shapeless.{Nat, Succ}

final class Comigrate[F[_], Origin] {

  def from[Start <: Nat, End <: Nat](implicit m: ComigrationBuilder[F, Origin, Start, End]): m.Data1 => F[m.Data2] = m.migrate(_)

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

  implicit def recurse[F[_]: FlatMap, Origin, Start <: Nat, N <: Nat, DStart, DPrev, DEnd]
  (implicit
   v1: Versioned.Aux[Origin, Start, DStart],
   v2: Versioned.Aux[Origin, Succ[N], DEnd],
   v3: Versioned.Aux[Origin, N, DPrev],
   f: Kleisli[F, DPrev, DEnd],
   r: ComigrationBuilder.Aux[F, Origin, Start, N, DStart, DPrev]
  ): MigrationBuilder.Aux[F, Origin, Start, Succ[N], DStart, DEnd] = new ComigrationBuilder[F, Origin, Start, Succ[N]] {
    type Data1 = DStart
    type Data2 = DEnd

    def migrate(d: DStart): F[DEnd] = r.migrate(d).flatMap(f.run)
  }

  implicit def base[F[_], Origin, V <: Nat, D, DPrev]
  (implicit
   v: Versioned.Aux[Origin, V, DPrev],
   v1: Versioned.Aux[Origin, Succ[V], D],
   f: Kleisli[F, D, DPrev]): ComigrationBuilder.Aux[F, Origin, Succ[V], V, D, DPrev] = new ComigrationBuilder[F, Origin, Succ[V], V] {
    type Data1 = D
    type Data2 = DPrev
    def migrate(d: D): F[DPrev] = f(d)
  }
}




