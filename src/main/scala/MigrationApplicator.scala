import shapeless.{Nat, Succ}

class Migrate[Origin] {

  def from[V1 <: Nat, V2 <: Nat](implicit m: MigrationVersionApplicator[Origin, V1, V2]): m.Data1 => m.Data2 = m.migrate(_)

  def fromD[Data1, Data2](implicit m: MigrationApplicator[Origin, Data1, Data2]): MigrationApplicator[Origin, Data1, Data2] = m

}

object Migrate {
  def apply[Origin]: Migrate[Origin] = new Migrate[Origin]
}

trait MigrationVersionApplicator[Origin, Start <: Nat, End <: Nat] {

  type Data1
  type Data2

  def migrate(d: Data1): Data2

}

object MigrationVersionApplicator {

  type Aux[Origin, Start <: Nat, End <: Nat, D1, D2] = MigrationVersionApplicator[Origin, Start, End] {
    type Data1 = D1
    type Data2 = D2
  }

  implicit def base[Origin, Start <: Nat, D, DNext]
  (implicit
   v: Versioned.Aux[Origin, Start, D],
   v1: Versioned.Aux[Origin, Succ[Start], DNext],
   f: MigrationFunction[D, DNext]): MigrationVersionApplicator.Aux[Origin, Start, Succ[Start], D, DNext] = new MigrationVersionApplicator[Origin, Start, Succ[Start]] {
    type Data1 = D
    type Data2 = DNext
    def migrate(d: Data1): Data2 = f.apply(d)
  }

  implicit def recurse[Origin, Start <: Nat, End <: Nat, DStart, DNext, DEnd]
  (implicit
   v1: Versioned.Aux[Origin, Start, DStart],
   v2: Versioned.Aux[Origin, Succ[Start], DNext],
   v3: Versioned.Aux[Origin, End, DEnd],
   f: MigrationFunction[DStart, DNext],
   r: MigrationVersionApplicator.Aux[Origin, Succ[Start], End, DNext, DEnd],
  ): MigrationVersionApplicator.Aux[Origin, Start, End, DStart, DEnd] = new MigrationVersionApplicator[Origin, Start, End] {
    type Data1 = DStart
    type Data2 = DEnd

    def migrate(d: DStart): Data2 = r.migrate(f.apply(d))
  }

}

trait MigrationApplicator[Origin, Data1, Data2] {
  def migrate(d: Data1): Data2
}

object MigrationApplicator {

  implicit def base[Origin, Data1, Data2, V <: Nat]
  (implicit
   v1: Versioned.Aux[Origin, V, Data1],
   v2: Versioned.Aux[Origin, Succ[V], Data2],
   f: MigrationFunction[Data1, Data2]): MigrationApplicator[Origin, Data1, Data2] = new MigrationApplicator[Origin, Data1, Data2] {
    def migrate(d: Data1): Data2 = f.apply(d)
  }

  implicit def recurse[Origin, Data1, DataN_1, DataN, VN_1 <: Nat]
  (implicit
   m: MigrationApplicator[Origin, Data1, DataN_1],
   vN_1: Versioned.Aux[Origin, VN_1, DataN_1],
   vN: Versioned.Aux[Origin, Succ[VN_1], DataN],
   f: MigrationFunction[DataN_1, DataN]
  ): MigrationApplicator[Origin, Data1, DataN] = new MigrationApplicator[Origin, Data1, DataN] {
    def migrate(d: Data1): DataN = f.apply(m.migrate(d))
  }
}

