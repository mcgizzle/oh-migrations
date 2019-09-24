package io.github.mcgizzle.argonaut

import argonaut.{DecodeJson, Json}
import io.github.mcgizzle.{DecodeFailure, Decoder}

trait ArgonautDecoder {
  implicit def argonautDecoder[A](implicit D: DecodeJson[A]): Decoder[Json, A] =
    Decoder
      .from[Json, A](j => D.decodeJson(j).result.left.map(x => DecodeFailure(x._1)))
}

object ArgonautDecoder extends ArgonautDecoder
