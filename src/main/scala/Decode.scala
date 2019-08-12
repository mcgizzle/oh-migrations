import shapeless.{Nat, Succ}
import shapeless.ops.nat.Pred
import cats.Alternative
import cats.implicits._

trait Decoder[A, B] {

  def from(a: A): Option[B]

}

class DecodeAndMigrate[Origin] {
  def decode[A, Start <: Nat, Target <: Nat, TargetD](a: A)(implicit ev: DecodeAndMigrateBuilder[Origin, A, Start, Target, TargetD]): Option[TargetD] = ev.decodeAndMigrate(a)
}

object DecodeAndMigrate {
  def apply[Origin]: DecodeAndMigrate[Origin] = new DecodeAndMigrate[Origin]
}

trait DecodeAndMigrateBuilder[Origin, A, Start <: Nat, Target <: Nat, TargetD] {

  def decodeAndMigrate(a: A): Option[TargetD]

}

object DecodeAndMigrateBuilder {

  def decode[Origin, A, Start <: Nat, Target <: Nat, TargetD](a: A)(implicit m: DecodeAndMigrateBuilder[Origin, A, Start, Target, TargetD]): Option[TargetD] = m.decodeAndMigrate(a)

  implicit def base[Origin, A, Target <: Nat, D](a: A)(implicit ev: Decoder[A, D]): DecodeAndMigrateBuilder[Origin, A, Target, Target, D] =
    new DecodeAndMigrateBuilder[Origin,A, Target, Target, D] {
      def decodeAndMigrate(a: A): Option[D] = ev.from(a)
  }

  implicit def decodeOrRecurse[Origin, A, N <: Nat, Target <: Nat, D, TargetD](a: A)
  (implicit ev: Decoder[A, D],
   m: MigrationBuilder.Aux[Origin, N, Target, D, TargetD],
   r: DecodeAndMigrateBuilder[Origin, A, Succ[N], Target, TargetD]): DecodeAndMigrateBuilder[Origin, A, N, Target, TargetD] =
    new DecodeAndMigrateBuilder[Origin, A,  N, Target, TargetD] {
      def decodeAndMigrate(a: A): Option[TargetD] = r.decodeAndMigrate(a) <+> m.migrateOption(ev.from(a))
    }

}



