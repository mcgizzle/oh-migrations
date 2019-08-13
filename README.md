# my-gr8-library

Data migrations through implicit function composition at the type-level.

This library provides the ability to version and migrate ADT's.

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

// We must version all our data types in tge chain
object App {
    
    import mcgizzle._
    import shapeless.nat._
    
    trait User {
      implicit val v1: Versioned[User, _1] = mcgizzle.Versioned[User, _1, UserV1]
      implicit val v2: Versioned[User, _2] = mcgizzle.Versioned[User, _2, UserV2]
      implicit val v3: Versioned[User, _3] = mcgizzle.Versioned[User, _3, UserV3]
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

}
```

