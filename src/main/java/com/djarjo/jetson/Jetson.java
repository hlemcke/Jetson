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
 *
 * @author Hajo Lemcke
 * @since 2023-01-05 Initial version
 */
public class Jetson {

	/**
	 * Decodes the given Json string into a basic value, a list or a map.
	 * <p>
	 * Finding a "{" will automatically create a new HashMap. Finding a "[" will
	 * automatically create a new ArrayList. Any other value will just be
	 * returned.
	 * </p>
	 *
	 * @param jsonString
	 *            Json string to be decoded
	 * @return new instance of basic value, list or map
	 * @throws IllegalAccessException
	 *             will not occur (Java exception definition problem)
	 * @throws ParseException
	 *             in case of an error in the Json string
	 */
	static public Object decode( String jsonString )
			throws IllegalAccessException, ParseException {
		return JsonDecoder.decoder().decode( jsonString );
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
	 * @param jsonString
	 *            Json string to decode
	 * @param target
	 *            target object with annotated getter methods
	 * @throws IllegalAccessException
	 *             if a decoded value cannot be set into the target object
	 * @throws ParseException
	 *             in case of an error in the Json string
	 */
	static public void decode( String jsonString, Object target )
			throws IllegalAccessException, ParseException {
		JsonDecoder.decoder().decode( jsonString, target );
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
	 * @param object
	 *            to be encoded
	 * @return encoded object
	 */
	static public String encode( Object object ) {
		return JsonEncoder.encoder().encode( object );
	}

	/**
	 * Encodes object to Json5
	 *
	 * @param object
	 *            to be encoded. Requires annotations "@Json"
	 * @return encoded object in Json5 format
	 */
	static public String encodeJson5( Object object ) {
		return JsonEncoder.encoder().withJson5( true ).encode( object );
	}

	/**
	 * Gets a new instance of JsonEncoder
	 *
	 * @return new instance of JsonEncoder
	 */
	public static JsonEncoder encoder() {
		return new JsonEncoder();
	}
}