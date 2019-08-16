package io.github.mcgizzle

import cats.Id
import cats.data.Kleisli

object MigrationFunction {
  def apply[Data1, Data2](f: Data1 => Data2): Data1 +=> Data2 = Kleisli[Id, Data1, Data2](f)
}

class MigrationFunctionF[F[_]] {
  def from[Data1, Data2](f: Data1 => F[Data2]): Kleisli[F, Data1, Data2] = Kleisli[F, Data1, Data2](f)
}
object MigrationFunctionF {
  def apply[F[_]]: MigrationFunctionF[F] = new MigrationFunctionF()
}

