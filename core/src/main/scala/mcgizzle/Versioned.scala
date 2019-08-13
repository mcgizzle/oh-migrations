package mcgizzle

import shapeless.Nat

trait Versioned[Origin, V <: Nat] {
  type Data
}

object Versioned {
  type Aux[Origin, V <: Nat, Data0] = Versioned[Origin, V]{ type Data = Data0 }

  def apply[Origin, V <: Nat, D]: Versioned.Aux[Origin, V, D] = new Versioned[Origin, V] {type Data = D}

}
