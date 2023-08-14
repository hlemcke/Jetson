# Jetson
Json codec with annotations and converters. Includes tokenizer with parser and basic converters.

Jetson encodes and decodes not only basic values, arrays, lists, maps but also any Java class.

## Usage
Main usage is to encode Java classes into a valid Json string and decode a Json string back into Java classes.
To reduce boilerplate code _Jetson_ uses annotations which can be applied on fields or getters.

## Example

```
class Pojo {
    @Json
    public String name = "abc";
    public Long id = 1234;
    private int code = 4711;
    @Json
    public getCode() { return code; }
    public void setCode( int code ) { this.code = code; }
}
```

Using `String jsonText = Jetcon.encode( new Pojo())` produces the following Json text.
The text does not contain field "id", because it is not annotated.

```
{"name":"abc","code":4711}
```

And the best thing is ... a Json text can be decoded directly back into an object.
`Jetson.decode( "{'name':'def'}", new Pojo());` delivers:

```
Pojo
  name = "def"
  code = 4711
```

## Json annotation with attributes
The annotation `@Json` can be applied to fields and (getter) methods. It accepts these attributes
* key -> replaces fieldname with this key like `@Json( key = "another" )` will produce `"another":...` during encoding. Decoding also awaits key "another"
* encodable -> Default true. `false` will not encode this field
* decodable -> Default true. `false` will not decode this field
* converter -> expects a class implementing `JsonConverter`. Encoding and decoding will use the converter.


