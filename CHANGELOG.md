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
