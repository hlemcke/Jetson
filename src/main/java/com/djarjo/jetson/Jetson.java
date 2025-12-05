/**
 *
 */
package com.djarjo.jetson;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Jetson wraps {@link com.djarjo.jetson.JsonDecoder JsonDecoder} and
 * {@link com.djarjo.jetson.JsonEncoder JsonEncoder}.
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

	private Jetson() {
	}

	/**
	 * Decodes the given Json string into a basic value, a list or a map.
	 * <p>
	 * Finding a "{" will automatically create a new HashMap. Finding a "[" will
	 * automatically create a new ArrayList. Any other value will just be returned.
	 * </p>
	 *
	 * @param jsonString Json string to be decoded
	 * @return new instance of basic value, list or map
	 * @throws IllegalAccessException will not occur (Java exception definition problem)
	 * @throws ParseException in case of an error in the Json string
	 */
	public static Object decode(
			String jsonString ) throws IllegalAccessException, ParseException {
		return JsonDecoder.decoder()
				.decode( jsonString );
	}

	/**
	 * Decodes given {@code jsonString} and writes values into {@code target} object.
	 * <p>
	 * Fields in target object must be annotated with {@code @Json}.
	 * </p>
	 * <p>
	 * Uses default settings of {@link com.djarjo.jetson.JsonDecoder JsonDecoder}.
	 * </p>
	 *
	 * @param jsonString Json string to decode
	 * @param target target object with annotated getter methods
	 * @return target updated with values from jsonString
	 * @throws IllegalAccessException if a decoded value cannot be set into the target
	 * object
	 * @throws ParseException in case of an error in the Json string
	 */
	public static Object decodeIntoObject( String jsonString,
			Object target ) throws IllegalAccessException, ParseException {
		JsonDecoder.decoder()
				.decodeIntoObject( jsonString, target );
		return target;
	}

	/**
	 * Decodes given {@code jsonString} into a list
	 *
	 * <p>
	 * Creates a new instance of {@code ArrayList} and writes values into new objects of
	 * type {@code itemClass}.
	 * </p>
	 *
	 * @param jsonString Json string to decode
	 * @param itemClass Class of objects in list
	 * @param <T> Type of elements in list
	 * @return list
	 * @throws IllegalAccessException when writing values into item object is not allowed
	 * @throws ParseException in case of an error in the Json string
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> decodeToList( String jsonString,
			Class<?> itemClass ) throws ParseException, IllegalAccessException {
		return (List<T>) JsonDecoder.decoder().decodeList( jsonString, itemClass );
	}

	/**
	 * Decodes given {@code jsonString} into a map.
	 * <p>
	 * Utility method includes casting to map and suppresses compiler warning for unchecked
	 * conversion.
	 * </p>
	 *
	 * @param jsonString Json string to decode
	 * @return map
	 * @throws IllegalAccessException will not happen here
	 * @throws ParseException in case of an error in the Json string
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> decodeToMap(
			String jsonString ) throws ParseException, IllegalAccessException {
		return (Map<String, Object>) JsonDecoder.decoder().decode( jsonString );
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
	public static String encode( Object object ) {
		return JsonEncoder.encoder()
				.encode( object );
	}

	/**
	 * Gets a new instance of JsonEncoder
	 *
	 * @return new instance of JsonEncoder
	 */
	public static JsonEncoder encoder() {
		return new JsonEncoder();
	}

	/**
	 * Encodes byte arrays to a list instead of a BASE64 string.
	 *
	 * @return Encoder for fluent API
	 */
	public static JsonEncoder bytesToList() {
		return JsonEncoder.encoder()
				.bytesToList();
	}

	/**
	 * Decoding a Json string will merge collection items from string into already existing
	 * items of a collection.
	 *
	 * @return decoder for fluent API
	 */
	public static JsonDecoder mergeCollection() {
		return JsonDecoder.decoder()
				.mergeCollections();
	}

	/**
	 * Pretty prints the encoded json string by writing each item on a separate line and
	 * indenting it with the given {@code indent}.
	 * <p>
	 * A value of {@code null} will encode into a one line string
	 * </p>
	 *
	 * @param indent default is no indentation
	 * @return this for fluent API
	 */
	public static JsonEncoder prettyPrint( String indent ) {
		return JsonEncoder.encoder()
				.prettyPrint( indent );
	}

	/**
	 * Encodes to JSON-5 syntax.
	 *
	 * @return this for fluent API
	 */
	public static JsonEncoder toJson5() {
		return JsonEncoder.encoder()
				.toJson5();
	}

	/**
	 * Encodes values from objects having a value of {@code null} instead of skipping them.
	 *
	 * @return this for fluent API
	 */
	public static JsonEncoder withNulls() {
		return JsonEncoder.encoder()
				.withNulls();
	}
}