package io.github.mcgizzle

sealed trait Decoder[A, B] {
  def decode(in: A): Either[DecodeFailure, B]
}
object Decoder {
  def from[A, B](f: A => Either[DecodeFailure, B]): Decoder[A, B] =
    new Decoder[A, B] {
      def decode(in: A): Either[DecodeFailure, B] = f(in)
    }
}
