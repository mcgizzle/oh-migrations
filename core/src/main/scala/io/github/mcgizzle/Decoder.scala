package io.github.mcgizzle

sealed trait Decoder[A, B] {
  def decode(in: A): Option[B]
}
object Decoder {
  def from[A, B](f: A => Option[B]): Decoder[A, B] =
    new Decoder[A, B] {
      def decode(in: A): Option[B] = f(in)
    }
}
