package com.github.mcgizzle.circe

import com.github.mcgizzle
import com.github.mcgizzle.Decoder
import io.circe.Json

trait CirceDecoder {
  implicit def circeDecoder[A: io.circe.Decoder]: Decoder[Json, A] =
    mcgizzle.Decoder.from[Json, A](j => io.circe.Decoder[A].decodeJson(j).toOption)
}

object CirceDecoder extends CirceDecoder
