import shapeless.{Nat, Succ}

trait MigrationFunction[Data1, Data2] {
  def apply(d: Data1): Data2
}

object MigrationFunction {

  def apply[Data1, Data2](f: Data1 => Data2): MigrationFunction[Data1, Data2] = new MigrationFunction[Data1, Data2] {
    def apply(d: Data1): Data2 = f(d)
  }

  implicit def identity[Data]: MigrationFunction[Data, Data] = new MigrationFunction[Data, Data] {
    def apply(d: Data): Data = d
  }
}

trait MigrationVersionFunction[Origin, From <: Nat] {
  def apply(d: Versioned[Origin, From]): Versioned[Origin, Succ[From]]
}

object MigrationVersionFunction {

  def apply[Origin, From <: Nat](f: Versioned[Origin, From] => Versioned[Origin, Succ[From]]): MigrationVersionFunction[Origin, From] =
    new MigrationVersionFunction[Origin, From] {
      def apply(d: Versioned[Origin, From]): Versioned[Origin, Succ[From]] = f(d)
  }

}