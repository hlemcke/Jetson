# Jetson

JSON codec designed similar to JPA.

Jetson encodes and decodes all basic values, lists, sets, maps and all your classes.

The decoder automatically decodes both JSON and JSON5.
To encode JSON5 use `Jetson.encoder().toJson5().encode( myObject )`.

This package also includes:

* `BeanHelper` for direct bean access and manipulation
* `ReflectionHelper` for deep class access using Java reflection
* `Tokenizer` to analyze text and split into tokens

## Usage

Main usage is to encode a Java class into a valid JSON string
and decode a JSON string back into a Java class.

To reduce boilerplate code _Jetson_ uses annotations
which can be applied on class, fields or getters.

For encoding to JSON additional features can be applied:

* .bytesToList() → encodes `byte[]` to a JSON list instead of a string in Base64 format
* .prettyPrint() → encodes to a pretty printed JSON string with indented entries
* .toJson5() → encodes to better human-readable JSON5 format
* .withNulls() → also encode null values instead of skipping them

## Example

```
@Json( accessType = Json.AccessType.FIELD )
class Pojo {
    private int code = 4711;
    
    @JsonTransient
    public Long id = 1234;

    @Json(name = "key" )
    public String name = "abc";

    @Json
    public List<SomeClass> myList = new ArrayList<>();
    
    public getCode() { return code; }
    public void setCode( int code ) { this.code = code; }
}
```

Using `String json = Jetson.encode( new Pojo())` produces the following JSON text.

```
{"code":4711,"key":"abc"}
```

The text does not contain field `id`, because it is transient. 

And the best thing is ... a JSON text can be decoded directly back into an object.
`Jetson.decodeIntoObject( "{'key':'def'}", new Pojo());` delivers:

```
Pojo
  code = 4711
  id = 1234
  name = "def"
```

## JSON Annotation - Attributes

The annotation `@Json` accepts these attributes:

* `converter` → expects a class implementing `JsonConverter`. Encoding and decoding will use the converter.
* `decode` → specifies decoding to `NEVER`, `ONLY_IF_EMPTY` or the default `ALWAYS`
* `encode` → specifies encoding to `ALWAYS`, `NEVER` or the default `ONLY_IF_NOT_EMPTY`
* `enumAccessor` → uses value from accessor (see below)
* `name` → replaces field name
* `mergeCollection` → `true` (default is `false`) will append decoding list entries to existing ones


## JSON Annotation - on Class

Annotation `@Json` can also be applied on class level.
The class level annotation specifies these attributes:

* `accessType` → specify `FIELD` to use fields or the default `PROPERTY`
* `converter` → encodes to a string and must create an object of this class from JSON string
* `decode` → will apply its value to all fields or properties.
  The value can be modified on each field or property by an individual `@Json` annotation
* `encode` → will apply its value to all fields or properties
  The value can be modified on each field or property by an individual `@Json` annotation
* `enumAccessor` → unused
* `name` → unused
* `mergeCollection` → unused

Priority of annotations:

1. If the annotation specifies a `converter` then this will be used for encoding and decoding
2. If the class contains method `String toJson()` then this will be invoked on encoding
3. If the class contains method `static $ThisClass fromJson( String jsonText )`
   then this will be invoked on decoding
4. otherwise all fields or properties will be used. In this case an individual
   {@literal @Json} annotation has higher priority and may overwrite attributes set at class level

## Functionalities

Converters for the following standard Java classes are already integrated
and are not required to be set:

* all basic types: boolean, byte, double, float, int, long, short and their classes
* all types from `java.time`
* `byte[]` gets encoded into Base64 and decoded from a Base64 string
* Java classes: BigDecimal, Currency, Locale, URI, URL, UUID

### Encoding Enumerations

To encode an enumeration attribute in a class, just annotate the field or getter with `@Json`.

If the enum contains some code which should be used for the JSON value like this:

```
enum UserLanguage {
  FRENCH ( "fr" ),
  GERMAN ( "de" ),
  ENGLISH ( "en" );
  public final code;
  UserLanguage( String code ) { this.code = code; } } 
```

Then just annotate the class attribute with `@Json( enumAccessor = "code" )`.
