package io.github.mcgizzle.circe

import io.circe.Json
import io.github
import io.github.mcgizzle.{DecodeFailure, Decoder}

trait CirceDecoder {
  implicit def circeDecoder[A: io.circe.Decoder]: Decoder[Json, A] =
    github.mcgizzle.Decoder
      .from[Json, A](j => io.circe.Decoder[A].decodeJson(j).left.map(x => DecodeFailure(x.message)))
}

object CirceDecoder extends CirceDecoder
