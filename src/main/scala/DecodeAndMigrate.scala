import shapeless.{=:!=, Lazy, Nat, Succ}
import cats.implicits._

trait Decoder[A, B] {
  def decode(in: A): Option[B]
}
object Decoder {
  def from[A, B](f: A => Option[B]): Decoder[A, B] =
    new Decoder[A, B] {
      def decode(in: A): Option[B] = f(in)
    }
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

  implicit def base[Origin, A, N <: Nat, DN](a: A)
  (implicit
   d: Decoder[A, DN]): DecodeAndMigrateBuilder.Aux[Origin, A, N, N, DN] =
    new DecodeAndMigrateBuilder[Origin, A, N, N] {
      type Out = DN
      def decodeAndMigrate(a: A): Option[DN] = d.decode(a)
  }

  implicit def decodeOrRecurse[Origin, A, N <: Nat, Target <: Nat, D, DTarget](a: A)
  (implicit
   r: DecodeAndMigrateBuilder.Aux[Origin, A, Succ[N], Target, DTarget],
   ev: Decoder[A, D],
   m: MigrationBuilder.Aux[Origin, N, Target, D, DTarget],
  ): DecodeAndMigrateBuilder.Aux[Origin, A, N, Target, DTarget] =
    new DecodeAndMigrateBuilder[Origin, A, N, Target] {
      type Out = DTarget
      def decodeAndMigrate(a: A): Option[DTarget] = r.decodeAndMigrate(a) <+> m.migrateOption(ev.decode(a))
    }

}



