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
  def from[A, Start <: Nat, Target <: Nat](a: A)(implicit ev: DecodeAndMigrateBuilder[Origin, A, Start, Target]): Option[ev.Out] = ev.decodeAndMigrate(a)
}

object DecodeAndMigrate {
  def apply[Origin]: DecodeAndMigrate[Origin] = new DecodeAndMigrate[Origin]
}

trait DecodeAndMigrateBuilder[Origin, A, Start <: Nat, Target <: Nat] {

  type Out

  def decodeAndMigrate(a: A): Option[Out]

}

object DecodeAndMigrateBuilder  {

  type Aux[Origin, A, Start <: Nat, Target <: Nat, Out0] = DecodeAndMigrateBuilder[Origin, A, Start, Target] { type Out = Out0 }

  implicit def recurse[Origin, A, N <: Nat, Target <: Nat, D, DTarget]
  (implicit
   v: Versioned.Aux[Origin, N, D],
   ev: Decoder[A, D],
   m: MigrationBuilder.Aux[Origin, N, Target, D, DTarget],
   r: Lazy[DecodeAndMigrateBuilder.Aux[Origin, A, Succ[N], Target, DTarget]],
  ): DecodeAndMigrateBuilder.Aux[Origin, A, N, Target, DTarget] =
    new DecodeAndMigrateBuilder[Origin, A, N, Target] {
      type Out = DTarget
      def decodeAndMigrate(a: A): Option[DTarget] = r.value.decodeAndMigrate(a) <+> m.migrateOption(ev.decode(a))
    }

  implicit def base[Origin, A, N <: Nat, DN]
  (implicit
   v: Versioned.Aux[Origin, N, DN],
   d: Decoder[A, DN]
  ): DecodeAndMigrateBuilder.Aux[Origin, A, N, N, DN] =
    new DecodeAndMigrateBuilder[Origin, A, N, N] {
      type Out = DN
      def decodeAndMigrate(a: A): Option[DN] = d.decode(a)
  }
}



