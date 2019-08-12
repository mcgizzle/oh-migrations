import shapeless.{Nat, Succ}
import cats.implicits._

trait Decoder[A, B] {

  def from(a: A): Option[B]

}

class DecodeAndMigrate[Origin] {
  def decode[A, Start <: Nat, Target <: Nat](a: A)(implicit ev: DecodeAndMigrateBuilder[Origin, A, Start, Target]): Option[ev.Out] = ev.decodeAndMigrate(a)
}

object DecodeAndMigrate {
  def apply[Origin]: DecodeAndMigrate[Origin] = new DecodeAndMigrate[Origin]
}

trait DecodeAndMigrateBuilder[Origin, A, Start <: Nat, Target <: Nat] {

  type Out

  def decodeAndMigrate(a: A): Option[Out]

}

object DecodeAndMigrateBuilder {

  type Aux[Origin, A, Start <: Nat, Target <: Nat, Out0] = DecodeAndMigrateBuilder[Origin, A, Start, Target] { type Out = Out0 }


  implicit def base[Origin, A, Target <: Nat, D](a: A)(implicit ev: Decoder[A, D], v: Versioned.Aux[Origin, Target, D]): DecodeAndMigrateBuilder.Aux[Origin, A, Target, Target, D] =
    new DecodeAndMigrateBuilder[Origin,A, Target, Target] {
      type Out = D
      def decodeAndMigrate(a: A): Option[D] = ev.from(a)
  }

  implicit def decodeOrRecurse[Origin, A, N <: Nat, Target <: Nat, D, TargetD](a: A)
  (implicit ev: Decoder[A, D],
   v1: Versioned.Aux[Origin, N, D],
   v2: Versioned.Aux[Origin, Target, TargetD],
   m: MigrationBuilder.Aux[Origin, N, Target, D, TargetD],
   r: DecodeAndMigrateBuilder.Aux[Origin, A, Succ[N], Target, TargetD]): DecodeAndMigrateBuilder.Aux[Origin, A, N, Target, TargetD] =
    new DecodeAndMigrateBuilder[Origin, A, N, Target] {
      type Out = TargetD
      def decodeAndMigrate(a: A) = r.decodeAndMigrate(a) <+> m.migrateOption(ev.from(a))
    }

}



