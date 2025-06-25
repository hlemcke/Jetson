/**
 *
 */
package com.djarjo.jetson;

import java.text.ParseException;

/**
 * Jetson wraps {@link com.djarjo.jetson.JsonDecoder JsonDecoder} and
 * {@link com.djarjo.jetson.JsonEncoder JsonEncoder} by using their default
 * settings.
 *
 * <h2>Json Specification</h2>
 *
 * <pre>
 * &lt;json&gt;    ::= &lt;value&gt;
 * &lt;array&gt;   ::= '[' ']' | '[' &lt;values&gt; ']'
 * &lt;object&gt;  ::= '{' &lt;elements&gt; '}'
 * &lt;member&gt;  ::= '&quot;'name'&quot;' ':' &lt;value&gt;
 * &lt;members&gt; ::= &lt;member&gt; | &lt;member&gt; ',' &lt;member&gt;
 * &lt;value&gt;   ::= &lt;array&gt; | &lt;number&gt; | &lt;object&gt; | &lt;string&gt; | 'false' | 'null' | 'true'
 * &lt;values&gt;  ::= &lt;value&gt; | &lt;value&gt; ',' &lt;values&gt;
 * </pre>
 *
 * @author Hajo Lemcke
 * @since 2023-01-05 Initial version
 */
public class Jetson {

	// --- Setting: encode to JSON 5 {@link https://json5.org}
	private boolean _json5 = false;

	// --- Setting: encode an array or list of bytes into a Base64 string
	private boolean _bytesToBase64 = true;
	// --- Setting: if to encode members with a value of {@code null}
	private boolean _withNulls = false;
	// --- Setting: indentation character(s)
	private String _indent = null;

	/**
	 * Constructor without parameters
	 */
	public Jetson() {
	}

	/**
	 * Create a new instance
	 *
	 * @return new instance
	 */
	public static Jetson newInstance() {
		return new Jetson();
	}

	/**
	 * Decodes the given Json string into a basic value, a list or a map.
	 * <p>
	 * Finding a "{" will automatically create a new HashMap. Finding a "[" will
	 * automatically create a new ArrayList. Any other value will just be
	 * returned.
	 * </p>
	 *
	 * @param jsonString Json string to be decoded
	 * @return new instance of basic value, list or map
	 * @throws IllegalAccessException will not occur (Java exception definition problem)
	 * @throws ParseException         in case of an error in the Json string
	 */
	static public Object decode(String jsonString)
		  throws IllegalAccessException, ParseException {
		return JsonDecoder.decoder()
			  .decode(jsonString);
	}

	/**
	 * Decodes given {@code jsonString} and writes values into {@code target}
	 * object.
	 * <p>
	 * Fields in target object must be annotated with {@code @Json}.
	 * </p>
	 * <p>
	 * Uses default settings of {@link com.djarjo.jetson.JsonDecoder
	 * JsonDecoder}.
	 * </p>
	 *
	 * @param jsonString Json string to decode
	 * @param target     target object with annotated getter methods
	 * @return target updated with values from jsonString
	 * @throws IllegalAccessException if a decoded value cannot be set into the target object
	 * @throws ParseException         in case of an error in the Json string
	 */
	static public Object decode(String jsonString, Object target)
		  throws IllegalAccessException, ParseException {
		JsonDecoder.decoder()
			  .decode(jsonString, target);
		return target;
	}

	/**
	 * Gets a new instance of JsonDecoder
	 *
	 * @return new instance of JsonDecoder
	 */
	public static JsonDecoder decoder() {
		return new JsonDecoder();
	}

	/**
	 * Encodes object into a Json text.
	 *
	 * @param object to be encoded
	 * @return encoded object
	 */
	static public String encode(Object object) {
		return JsonEncoder.encoder()
			  .encode(object);
	}

	/**
	 * Encodes object to Json5
	 *
	 * @param object to be encoded. Requires annotations "@Json"
	 * @return encoded object in Json5 format
	 */
	static public String encodeJson5(Object object) {
		return JsonEncoder.encoder()
			  .toJson5(true)
			  .encode(object);
	}

	/**
	 * Automatically encodes byte arrays to BASE64 (default).
	 *
	 * @param with {@code false} will suppress encoding to base64
	 * @return this for fluent API
	 */
	public Jetson withBytesToBase64(boolean with) {
		this._bytesToBase64 = with;
		return this;
	}

	/**
	 * Encodes values from objects having a value of {@code null}.
	 *
	 * @param with default is {@code false}
	 * @return this for fluent API
	 */
	public Jetson withNulls(boolean with) {
		this._withNulls = with;
		return this;
	}

	/**
	 * Pretty prints the encoded json string by writing each item on a separate
	 * line and indenting it with the given {@code indent}.
	 * <p>
	 * A value of {@code null} will encode into a one line string
	 * </p>
	 *
	 * @param indent default is no indentation
	 * @return this for fluent API
	 */
	public Jetson withPrettyPrint(String indent) {
		this._indent = indent;
		return this;
	}

	/**
	 * Gets a new instance of JsonEncoder
	 *
	 * @return new instance of JsonEncoder
	 */
	public JsonEncoder encoder() {
		return JsonEncoder.encoder()
			  .toJson5(_json5)
			  .withBytesToBase64(_bytesToBase64)
			  .withNulls(_withNulls)
			  .withPrettyPrint(_indent);
	}

	/**
	 * Encodes to JSON-5 syntax.
	 *
	 * @param to {@code true} encodes to JSON-5
	 * @return this for fluent API
	 */
	public Jetson toJson5(boolean to) {
		this._json5 = to;
		return this;
	}

	@Override
	public String toString() {
		return String.format("Encoder %s %s %s %s", _json5 ? "toJson5" : "",
			  _bytesToBase64 ? "toBase64" : "", _withNulls ? "withNulls" : "",
			  (_indent == null) ? "" : "indent='" + _indent + "'");
	}
}