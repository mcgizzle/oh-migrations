package com.github.mcgizzle

import shapeless.{Nat, Succ}

class Migrate[Origin] {

  def from[Start <: Nat, End <: Nat](implicit m: MigrationBuilder[Origin, Start, End]): m.Data1 => m.Data2 = m.migrate(_)

}

object Migrate {
  def apply[Origin]: Migrate[Origin] = new Migrate[Origin]
}

trait MigrationBuilder[Origin, Start <: Nat, End <: Nat] {

  type Data1
  type Data2

  def migrate(d: Data1): Data2
}

object MigrationBuilder {

  type Aux[Origin, Start <: Nat, End <: Nat, D1, D2] = MigrationBuilder[Origin, Start, End] {
    type Data1 = D1
    type Data2 = D2
  }

  implicit def recurse[Origin, Start <: Nat, N <: Nat, DStart, DN, DEnd]
  (implicit
   v1: Versioned.Aux[Origin, Start, DStart],
   v2: Versioned.Aux[Origin, Succ[N], DEnd],
   v3: Versioned.Aux[Origin, N, DN],
   f: MigrationFunction[DN, DEnd],
   r: MigrationBuilder.Aux[Origin, Start, N, DStart, DN],
  ): MigrationBuilder.Aux[Origin, Start, Succ[N], DStart, DEnd] = new MigrationBuilder[Origin, Start, Succ[N]] {
    type Data1 = DStart
    type Data2 = DEnd

    def migrate(d: DStart): DEnd = f(r.migrate(d))
  }

  implicit def base[Origin, V <: Nat, D, DNext]
  (implicit
   v: Versioned.Aux[Origin, V, D],
   v1: Versioned.Aux[Origin, Succ[V], DNext],
   f: MigrationFunction[D, DNext]): MigrationBuilder.Aux[Origin, V, Succ[V], D, DNext] = new MigrationBuilder[Origin, V, Succ[V]] {
    type Data1 = D
    type Data2 = DNext
    def migrate(d: D): DNext = f.apply(d)
  }
}




