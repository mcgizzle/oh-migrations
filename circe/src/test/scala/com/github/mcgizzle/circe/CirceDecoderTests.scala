package com.github.mcgizzle.circe

import org.scalatest.{FlatSpec, Matchers}
import com.github.mcgizzle
import io.circe._
import shapeless.test.illTyped

class CirceDecoderTests extends FlatSpec with Matchers {

  "Decoder" should "provide an implicit Decoder instance given a circe one" in {
    import io.circe.generic.auto._

    case class JSON(a: Int, b: String)

    implicitly[mcgizzle.Decoder[Json, JSON]]
  }
  it should "work for a complex type" in {
    import io.circe.generic.auto._

    case class J2(value: String)
    case class J1(l: List[J2])
    case class JSON(a: Int, b: String, c: J1, d: J2)

    implicitly[mcgizzle.Decoder[Json, JSON]]
  }
  it should "not provide an implicit Decoder instance if no circe one is found" in {

    case class JSON(a: Int, b: String)
    illTyped("implicitly[mcgizzle.Decoder[Json, JSON]]", "could not find implicit value for parameter e.*")
  }

}

