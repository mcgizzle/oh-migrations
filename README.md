# oh-migrations

Data migrations through implicit function composition at the type-level.

This library provides the ability to version, decode and migrate ADT's.

## Install

For core functionality add the following to your `build.sbt`:

`"io.github.mcgizzle" %% "oh-migrations-core" % "0.0.1"`

to interop with [circe](https://github.com/circe/circe) add:

`"io.github.mcgizzle" %% "oh-migrations-circe" % "0.0.1"`

## Example


```scala
// Lets say you have a stringly typed User data type.
case class UserV1(firstName: String, lastName: String)

// You then decide to add some type information through the use of value classes.

case class FirstName(value: String)
case class LastName(value: String)
case class UserV2(firstName: FirstName, lastName: LastName)

// Next it is decided that we need a way to uniquely identify `Users` through a UUID, 
// and while we are at it, combine the names.

import java.util.UUID

case class Name(value: String)
case class UserV3(name: Name, id: UUID)

// We must version all our data types in the chain
object App {
    
    import shapeless.nat._
    
    trait User {
      implicit val v1 = Versioned[User, _1, UserV1]
      implicit val v2 = Versioned[User, _2, UserV2]
      implicit val v3 = Versioned[User, _3, UserV3]
    }

    def dbStuffToGetUUID(user: UserV1): UUID = ???

    // Now we provide migrations from n to n + 1
    
    implicit val m1: MigrationFunction[UserV1, UserV2] = u1 => 
      UserV2(FirstName(u1.firstName), LastName(u1.lastName))

    implicit val m2: MigrationFunction[UserV2, UserV3] = u1 => 
      UserV3(Name(u1.firstName.value + " " + u1.lastName.value), dbStuffToGetUUID(u1))
    
    // Here we get our migration composition (1 -> 3) for free
    val u1 = UserV1("Frederick", "Wiley")
    Migrate[User].from[_1, _3].apply(u1) == UserV3(Name("Frederick Wiley"), dbStuffToGetUUID(u1))

    // Even more interesting is the DecodeAndMigrate type-class which provides the ability
    // to attempt to decode our User from the latest version and then migrate it to our
    // desired version
    
    // We provide Decoders for each version of User
    implicit val d1: Decoder[String, UserV1] = Decoder.from(_ => None)   
    implicit val d2: Decoder[String, UserV2] = Decoder.from(_ => Some(UserV2(FirstName("Decoded"), LastName("By UserV2"))))   
    implicit val d3: Decoder[String, UserV3] = Decoder.from(_ => None)   

    // It decodes a UserV2 as it is the latest available and then migrates it to UserV3
    DecodeAndMigrate[User].from[String, _1, _3]("{ json value for example}") shouldBe Some(UserV3(Name("Decoded By UserV2")))

    // This functionality can be easily interoped with circe using oh-migrations-circe
    
    import io.circe._
    import io.circe.generic.auto._
    import com.github.mcgizzle.circe._
    
    DecodeAndMigrate[User].from[Json, _1, _3](UserV2(FirstName("Decoded"), LastName("By Circe")).asJson) shouldBe Some(UserV3(Name("Decoded By Circe")))

}
```

