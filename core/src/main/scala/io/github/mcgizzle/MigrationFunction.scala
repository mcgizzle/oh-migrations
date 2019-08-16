package io.github.mcgizzle

import cats.Id
import cats.data.Kleisli

object MigrationFunction {
  def apply[Data1, Data2](f: Data1 => Data2): Data1 +=> Data2 = Kleisli[Id, Data1, Data2](f)
}
