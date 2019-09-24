package io.github.mcgizzle.argonaut

import org.scalatest.{FlatSpec, Matchers}
import argonaut._
import io.github.mcgizzle
import shapeless.test.illTyped

class ArgonautDecoderTests extends FlatSpec with Matchers {

  "Decoder" should "provide an implicit Decoder instance given a circe one" in {

      case class JSON(a: Int, b: String)
      object JSON {
        implicit def decoder: DecodeJson[JSON] = null
      }

      implicitly[mcgizzle.Decoder[Json, JSON]]
    }
  it should "work for a complex type" in {

      case class J2(value: String)
      object J2 {
        implicit def decoder: DecodeJson[J2] = null
      }
      case class J1(l: List[J2])
      object J1 {
        implicit def decoder: DecodeJson[J1] = null
      }
      case class JSON(a: Int, b: String, c: J1, d: J2)
      object JSON {
        implicit def decoder: DecodeJson[JSON] = null

      }
      implicitly[mcgizzle.Decoder[Json, JSON]]
    }
  it should "not provide an implicit Decoder instance if no circe one is found" in {

      case class JSON(a: Int, b: String)
      illTyped("implicitly[mcgizzle.Decoder[Json, JSON]]", "could not find implicit value for parameter e.*")
    }

}
