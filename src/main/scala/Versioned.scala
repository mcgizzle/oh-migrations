import shapeless.{Nat, _0}

trait Versioned[Origin, Data, V <: Nat]

object Versioned {
  def apply[Origin, Data, V <: Nat]: Versioned[Origin, Data, V] = null

  implicit def version0[Origin]: Versioned[Origin, Origin, _0] = null
}
