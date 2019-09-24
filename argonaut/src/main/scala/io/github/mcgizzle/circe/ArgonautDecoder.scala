package io.github.mcgizzle.circe

import argonaut.{DecodeJson, Json}
import io.github.mcgizzle.{DecodeFailure, Decoder}

trait ArgonautDecoder {
  implicit def circeDecoder[A](implicit D: DecodeJson[A]): Decoder[Json, A] =
    Decoder
      .from[Json, A](j => D.decodeJson(j).result.left.map(x => DecodeFailure(x._1)))
}

object ArgonautDecoder extends ArgonautDecoder
