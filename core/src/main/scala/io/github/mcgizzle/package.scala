package io.github

import cats.Id
import cats.data.Kleisli

package object mcgizzle {

  type +=>[Data1, Data2] = Kleisli[Id, Data1, Data2]

}
