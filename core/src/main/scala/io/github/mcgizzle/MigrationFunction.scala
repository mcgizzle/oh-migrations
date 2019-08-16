package io.github.mcgizzle

trait MigrationFunction[Data1, Data2] {
  def apply(d: Data1): Data2
}


object MigrationFunction {

  type +=>[Data1, Data2] = MigrationFunction[Data1, Data2]

  def apply[Data1, Data2](f: Data1 => Data2): MigrationFunction[Data1, Data2] = new MigrationFunction[Data1, Data2] {
    def apply(d: Data1): Data2 = f(d)
  }

}