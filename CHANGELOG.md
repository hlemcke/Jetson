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
