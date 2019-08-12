import shapeless.ops.nat.Pred
import shapeless.{Lazy, Nat, Succ}

trait Decoder[A, B] {

  def from(b: B): Option[A]

}

object Replae {

  def base[A, B](b: B)(ev: Decoder[A, B]) = ev.from(b)

  def recurse[Origin, Current, Next, Start, B](b: B)(ev: MigrationBuilder[Origin, Start, End]) = null

}
trait DecodeAndMigrateBuilder[Origin, Start <: Nat, End <: Nat] {

  type Data1
  type Data2

  def decodeAndMigrate(d: Data1): Data2

}

object MigrationBuilder extends LowPriority {

  type Aux[Origin, Start <: Nat, End <: Nat, D1, D2] = MigrationBuilder[Origin, Start, End] {
    type Data1 = D1
    type Data2 = D2
  }

  implicit def recurse[Origin, Start <: Nat, Current <: Nat, Prev <: Nat, DCurrent, DPrev, DStart]
  (implicit
   ev: Pred.Aux[Current, Prev],
   v1: Versioned.Aux[Origin, Start, DStart],
   v2: Versioned.Aux[Origin, Prev, DPrev],
   v3: Versioned.Aux[Origin, Current, DCurrent],
   f: MigrationFunction[DPrev, DCurrent],
   r: Lazy[MigrationBuilder.Aux[Origin, Start, Prev, DStart, DPrev]],
  ): MigrationBuilder.Aux[Origin, Start, Current, DStart, DCurrent] = new MigrationBuilder[Origin, Start, Current] {
    type Data1 = DPrev
    type Data2 = DCurrent

    def migrate(d: DPrev): DCurrent = r.value.migrate(f.apply(d))
  }

  implicit def base[Origin, V <: Nat, Prev <: Nat, D, DPrev]
  (implicit
   ev: Pred.Aux[V, Prev],
   v: Versioned.Aux[Origin, V, D],
   v1: Versioned.Aux[Origin, Prev, DPrev],
   e: Decoder
   f: MigrationFunction[DPrev, D]): MigrationBuilder.Aux[Origin, Prev, V, DPrev, D] = new MigrationBuilder[Origin, Prev, V] {
    type Data1 = DPrev
    type Data2 = D
    def migrate(d: DPrev): D = f.apply(d)
  }
}



