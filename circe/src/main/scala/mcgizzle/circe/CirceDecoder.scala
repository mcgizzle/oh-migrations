package mcgizzle.circe

import io.circe.Json

trait CirceDecoder {
  implicit def circeDecoder[A: io.circe.Decoder]: mcgizzle.Decoder[Json, A] =
    mcgizzle.Decoder.from[Json, A](j => io.circe.Decoder[A].decodeJson(j).toOption)
}

object CirceDecoder extends CirceDecoder
