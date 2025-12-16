# Jetson

JSON codec with annotations and converters. Includes tokenizer with parser and basic converters.

Jetson encodes and decodes not only basic values, arrays, lists, maps but also any Java class.

## Usage

Main usage is to encode a Java class into a valid JSON string
and decode a JSON string back into a Java class.

To reduce boilerplate code _Jetson_ uses annotations
which can be applied on fields or getters.

On encoding to JSON additional features can be applied:

* .bytesToList() → encodes `byte[]` to a JSON list instead of a string in Base64 format
* .prettyPrint() → encodes to a pretty printed JSON string with indented entries
* .toJson5() → encodes to human-readable JSON5 format
* .withNulls() → also encode null values instead of skipping them

## Example

```
class Pojo {
    private int code = 4711;
    public Long id = 1234;
    @Json
    public String name = "abc";

    @Json
    public List<SomeClass> myList = new ArrayList<>();
    
    @Json
    public getCode() { return code; }
    public void setCode( int code ) { this.code = code; }
}
```

Using `String jsonText = Jetson.encode( new Pojo())` produces the following Json text.

```
{"code":4711,"name":"abc"}
```

The text does not contain field "id", because it is not annotated.

And the best thing is ... a JSON text can be decoded directly back into an object.
`Jetson.decodeIntoObject( "{'name':'def'}", new Pojo());` delivers:

```
Pojo
  code = 4711
  id = 1234
  name = "def"
```

## Json annotation with attributes

The annotation `@Json` can be applied to members (fields and getter methods) accepting these attributes:

* converter → expects a class implementing `JsonConverter`. Encoding and decoding will use the converter.
* decodable → Default true. `false` will not decode this field from json string
* encodable → Default true. `false` will not encode this field into json string
* key → replaces field name with this key like `@Json( key = "another" )` will produce `"another":...` during encoding.
  Decoding also awaits key "another"
* mergeCollection →

Annotation `@Json` can also be applied on class level. All attributes are still valid.

## Functionalities

Converters for the following standard Java classes are already integrated and are not required to be set on such
attributes in a class:

* all basic types: boolean, byte, double, float, int, long, short and their classes
* all from java.time
* byte[] gets encoded into Base64 and decoded from a Base64 string
* Java classes: BigDecimal, Currency, Locale, URI, URL, UUID
