## 2.3.4+2

* Added toInstant() to list of base converters

## 2.3.4+1

* Fixed problem parsing times now correctly sets nanos

## 2.3.4

* added decodeIntoObject with generic

## 2.3.3

* fixed decoding JSON string into a record using static method `toJson()`

## 2.3.2

* changed `JsonAccessType` to integrated enum `Json.AccessType`
* added Base32 encoding and decoding
* multiple documentation updates

## 2.2.2

* fixed enumeration decodings
* fixed decoding JSON list into set

## 2.2.1

* fixed JsonDecoder when class is annotated
* `ReflectionHelper` no heavily uses `Type` instead of `Member`

## 2.2.0

* removed attributes from `@Json`: _decodable_ and _encodable_. Use __decode__ and __encode__ instead
* Added internal `JsonAccessor` for any annotation on class, method or field
* Added internal `JsonCache` to cache classes already scanned for JSON annotations

## 2.1.4

* Added `MemberAccessor` to bundle field or getter/setter-pair

## 2.1.3

* Fixed bug to correctly compute length of byte[] when decoding a Base64 string

## 2.1.2

* Added findFields in BeanHelper
* Modified method parameters and return values from arrays to lists

## 2.1.1

* Fixed bug to automatically decode a Base64 string into `byte[]`
* Re-inserted Base64 which automatically decodes a string either being in URL-safe or standard format

## 2.1.0

* Jetson now directly encodes and decodes enumerations without requiring a converter

## 2.0.2

* Added missing `java.time` classes to automatic encoding / decoding

## 2.0.1

* Fixed bug when decoding a JSON list into an ancient Java array

## 2.0.0

* Classes can now be annotated with @Json to encode all members. Class level annotation uses accessType to distinguish
  between fields and properties
* New annotation @JsonTransient to be used on fields and properties to suppress decoding and encoding
* Added method `decodeToList( String, Class)` to support decoding lists outside of pojos (which have no generic)

## 1.1.0

* Decoding a collection into an object overwrites current collection
* chain method `mergeCollections()` on decoder will merge existing values and append new ones

## 1.0.0

* Initial release
