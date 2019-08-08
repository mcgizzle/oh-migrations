import shapeless.{Nat, Succ}

class Migrate[Origin] {
  def from[Data1, Data2](implicit m: MigrationApplicator[Origin, Data1, Data2]): MigrationApplicator[Origin, Data1, Data2] = m
}

object Migrate {
  def apply[Origin]: Migrate[Origin] = new Migrate[Origin]
}

trait MigrationApplicator[Origin, Data1, Data2] {
  def migrate(d: Data1): Data2
}

object MigrationApplicator {

  implicit def base[Origin, Data1, Data2, V <: Nat](
                                             implicit v1: Versioned[Origin, Data1, V],
                                             v2: Versioned[Origin, Data2, Succ[V]],
                                             f: MigrationFunction[Data1, Data2]
                                           ): MigrationApplicator[Origin, Data1, Data2] = new MigrationApplicator[Origin, Data1, Data2] {
    def migrate(d: Data1): Data2 = f.apply(d)
  }

  implicit def recurse[Origin, Data1, DataN_1, DataN, VN_1 <: Nat]
  (
    implicit m: MigrationApplicator[Origin, Data1, DataN_1],
    vN_1: Versioned[Origin, DataN_1, VN_1],
    vN: Versioned[Origin, DataN, Succ[VN_1]],
    f: MigrationFunction[DataN_1, DataN]
  ): MigrationApplicator[Origin, Data1, DataN] = new MigrationApplicator[Origin, Data1, DataN] {
    def migrate(d: Data1): DataN = f.apply(m.migrate(d))
  }
}
