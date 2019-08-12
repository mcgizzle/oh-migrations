
trait MigrationFunction[Data1, Data2] {
  def apply(d: Data1): Data2
  def maybeMigrate(d: Option[Data1]): Option[Data2] = d.map(apply(_))
}

object MigrationFunction {

  def apply[Data1, Data2](f: Data1 => Data2): MigrationFunction[Data1, Data2] = new MigrationFunction[Data1, Data2] {
    def apply(d: Data1): Data2 = f(d)
  }

  implicit def id[D]: MigrationFunction[D, D] = new MigrationFunction[D, D] {
    def apply(d: D): D = d
  }

}