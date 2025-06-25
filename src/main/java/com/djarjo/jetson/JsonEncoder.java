/**
 * Copyright by Djarjo GmbH 2023
 */
package com.djarjo.jetson;

import com.djarjo.common.Base64;
import com.djarjo.common.BaseConverter;
import com.djarjo.common.BeanHelper;
import com.djarjo.jetson.converter.JsonConverter;
import com.djarjo.text.RecursionException;
import com.google.common.flogger.FluentLogger;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Encodes objects into a Json string.
 * <p>
 * The JSON encoder handles numbers, strings, booleans, null, lists, sets, maps,
 * UUID, java.time objects directly. Enumerations will be encoded by their
 * {@code .toJson()} method. If this does not exist then their
 * {@code .toString()} method will be used. Any other object is converted by
 * recursively calling their getter methods which are annotated with
 * {@code @Json}. If no annotated getter is found then {@code .toJson()} will be
 * invoked. If this method does not exist
 *
 * @author Hajo Lemcke
 * @since 2017-01-02
 */
public class JsonEncoder {
	private final static FluentLogger logger = FluentLogger.forEnclosingClass();
	private final int MAX_INDENT = 10;
	private final String[] _indentation = new String[MAX_INDENT];
	// --- Setting: encode an array or list of bytes into a Base64 string
	private boolean _bytesToBase64 = true;
	// --- Setting: encode to JSON 5 {@link https://json5.org}
	private boolean _json5 = false;
	// --- Setting: indentation character(s)
	private String _indent = null;
	private boolean _prettyPrint = false;

	// --- Used to prevent circular references in object hierarchy
	private Set<Object> _stack;

	// --- Setting: if to encode members with a value of {@code null}
	private boolean _withNullMembers = false;

	/**
	 * Empty no arg constructor
	 */
	public JsonEncoder() {
	}

	/**
	 * Instantiates and returns a new instance of this class to be used in
	 * streaming method calls. This method only exists because Java cannot call
	 * a method during instantiation.
	 *
	 * @return new JsonEncoder()
	 */
	public static JsonEncoder encoder() {
		return new JsonEncoder();
	}

	/**
	 * Encodes the given Java object into a Json string.
	 * <p>
	 * If the given object is a collection like a list or a set then it will be
	 * encoded into a Json array enclosed in square brackets.
	 * </p>
	 * <p>
	 * Any other object will be encoded into a Json object enclosed in curly
	 * braces. The encoded Json string contains all fields (getters) which are
	 * annotated with {@link com.djarjo.jetson.Json @Json}.
	 *
	 * @param object The object to be encoded
	 * @return encoded object as a Json string or {@code null} if the parameter
	 * is {@code null} or an error has occurred
	 */
	public String encode(Object object) {
		_stack = new HashSet<>();
		return _encodeValue(object);
	}

	/**
	 * Gets characters used for <i>pretty-print</i>.
	 *
	 * @return indentation string or {@code null}
	 */
	public String getIndent() {
		return _indent;
	}

	/******************************************************************
	 * Checks whether an object is empty.
	 *
	 * <p>
	 * If the given parameter is null then {@code true} will be returned. This
	 * method works on the following types:
	 * <ul>
	 * <li>String is empty when its length is zero, when it only contains spaces
	 * and tabs or when the text is "null"</li>
	 * <li>Lists, Maps and Sets are considered empty when they do not contain
	 * any element</li>
	 * </ul>
	 *
	 * @param obj
	 *            bean to check
	 * @return true if object is effectively empty
	 */
	public boolean isEmpty(Object obj) {
		if (obj == null) {
			return true;
		}
		if (obj instanceof Array) {
			return ((Object[]) obj).length == 0;
		} else if (obj instanceof String) {
			if (((String) obj).isEmpty())
				return true;
			return obj.equals("null");
		} else if (obj instanceof List) {
			return ((List<?>) obj).isEmpty();
		} else if (obj instanceof Map) {
			return ((Map<?, ?>) obj).isEmpty();
		} else if (obj instanceof Set) {
			return ((Set<?>) obj).isEmpty();
		}
		return false;
	}

	/**
	 * If to encode an array or list of bytes into a Base64 string.
	 *
	 * @param toBase64 default = {@code true}
	 * @return encoder for streaming API
	 */
	public JsonEncoder withBytesToBase64(boolean toBase64) {
		_bytesToBase64 = toBase64;
		return this;
	}

	/**
	 * Encode to <a href="https://json5.org">JSON 5</a>
	 *
	 * @param json5 default = {@code false}
	 * @return encoder for streaming API
	 */
	public JsonEncoder toJson5(boolean json5) {
		_json5 = json5;
		if (json5 && (_indent == null)) {
			withPrettyPrint("  ");
		}
		return this;
	}

	/**
	 * Encode members of an object with a value of {@code null}
	 *
	 * @param withNulls default = {@code false}
	 * @return encoder for streaming API
	 */
	public JsonEncoder withNulls(boolean withNulls) {
		_withNullMembers = withNulls;
		return this;
	}

	/**
	 * Encoder tries to <i>pretty-print</i> values in the resulting Json string
	 * by prefixing values with the string {@code indent}.
	 * <p>
	 * This string must only contain one or more of space (U+0020) or tab
	 * (U+0009) characters. If indent is not empty then each single value in the
	 * resulting json string will be on its own line. Lines are separated by
	 * line feed characters (U+000a).
	 * </p>
	 *
	 * @param indent default = {@code null}
	 * @return encoder for streaming API
	 */
	public JsonEncoder withPrettyPrint(String indent) {
		for (int i = 0; i < indent.length(); i++) {
			if (indent.charAt(i) != ' ' && indent.charAt(i) != '\t') {
				indent = "  ";
				break;
			}
		}
		_indent = indent;
		_prettyPrint = true;
		// --- Prepare indentations for depth up to MAX_INDENT
		_indentation[0] = "";
		_indentation[1] = _indent;
		for (int i = 2; i < MAX_INDENT; i++) {
			_indentation[i] = _indentation[i - 1] + _indent;
		}
		return this;
	}

	/**
	 * Encodes the given array into {@literal "[" value ["," value]* "]"}.
	 * <p>
	 * If {@code bytesToBase64 = true} then an array of bytes will be encoded
	 * into a Base64 string instead.
	 * </p>
	 */
	private String _encodeArray(Object array) {
		Class<?> _elemClazz = array.getClass()
			  .getComponentType();
		if (_bytesToBase64 && (_elemClazz.equals(byte.class)
			  || _elemClazz.equals(Byte.class))) {
			return "\"" + Base64.encoder()
				  .encode((byte[]) array) + "\"";
		}
		StringBuilder jsonStringBuilder = new StringBuilder();
		Object[] _array = BaseConverter.convertToArray(array);
		for (Object obj : _array) {
			jsonStringBuilder.append(_prettyPrint ? ",\n" : ",");
			jsonStringBuilder.append(_encodeValueIndented(obj));
		}
		return "[" + _stripLeadingComma(jsonStringBuilder) + "]";
	}

	/**
	 * Encodes the given collection into {@literal "[" value ["," value]* "]"}
	 */
	private String _encodeCollection(Collection<?> collection) {
		StringBuilder jsonStringBuilder = new StringBuilder();
		for (Object obj : collection) {
			jsonStringBuilder.append(_prettyPrint ? ",\n" : ",");
			jsonStringBuilder.append(_encodeValueIndented(obj));
		}
		return "[" + _stripLeadingComma(jsonStringBuilder) + "]";
	}

	/**
	 * Appends "key:value" to builder if value is not null or IsWithNulls
	 *
	 * @param builder current json text part
	 * @param key     key from map or Pojo
	 * @param value   value of key
	 */
	private void _encodeKeyValue(StringBuilder builder, String key,
	                             Object value) {
		if (value != null || _withNullMembers) {
			builder.append(",");
			if (_prettyPrint) {
				builder.append("\n");
				builder.append(_indentation[_stack.size()]);
			}
			builder.append(_json5 ? key + ": " : "\"" + key + "\":");
			builder.append(value);
		}
	}

	/**
	 * Encodes the given map into {@literal "{" key ":" value [", " key ":"
	 * value]* "}"
	 */
	private String _encodeMap(Map<Object, Object> map) {
		StringBuilder jsonStringBuilder = new StringBuilder();
		Iterator<Map.Entry<Object, Object>> iter = map.entrySet()
			  .iterator();
		Map.Entry<Object, Object> entry = null;
		while (iter.hasNext()) {
			entry = iter.next();
			_encodeKeyValue(jsonStringBuilder, entry.getKey()
						.toString(),
				  _encodeValue(entry.getValue()));
		}
		return "{" + _stripLeadingComma(jsonStringBuilder) + "}";
	}

	/**
	 * Encodes the given plain old Java object (POJO) into {@literal "{" key ":"
	 * value [", " key ":" value]* "}"
	 * <p>
	 * If POJO contains the method {@code toJson()} then this will be used instead
	 * of checking fields and getters.
	 * </p>
	 */
	private String _encodePojo(Object pojo) {
		Json anno = null;
		StringBuilder builder = new StringBuilder();
		String name = null;
		Object value = null;

		// --- Encode fields
		Field[] fields = pojo.getClass()
			  .getFields();
		for (Field field : fields) {
			anno = field.getAnnotation(Json.class);
			if ((anno != null) && anno.encodable()) {
				try {
					field.setAccessible(true);
					value = field.get(pojo);
					name = field.getName();
					_encodePojoMember(builder, anno, name, value);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					logger.atWarning()
						  .withCause(e)
						  .log(
								"Cannot encode field " + field + " for " + value);
				}
			}
		}

		// --- Encode methods
		Method[] methods = BeanHelper.obtainGetters(pojo.getClass());
		for (Method method : methods) {
			anno = method.getAnnotation(Json.class);
			if ((anno != null) && anno.encodable()) {
				try {
					method.setAccessible(true);
					value = method.invoke(pojo, (Object[]) null);
					name = BeanHelper
						  .getVarnameFromMethodname(method.getName());
					_encodePojoMember(builder, anno, name, value);
				} catch (IllegalArgumentException | InvocationTargetException
				         | IllegalAccessException e) {
					logger.atWarning()
						  .log("JsonCodec._encodePojo( " + pojo
								+ " ). Cannot invoke " + method + ": "
								+ e.getMessage());
				}
			}
		}
		return "{" + _stripLeadingComma(builder) + "}";
	}

	/**
	 * Encodes the value of a member (from object field or method) into a Json
	 * string.
	 * <ol>
	 * <li>check {@code Json.access()}</li>
	 * <li>check {@code Json.converter()}</li>
	 * </ol>
	 *
	 * @param anno  Complete annotation with parameters
	 * @param name  Name of field
	 * @param value value to be encoded
	 */
	private void _encodePojoMember(StringBuilder builder, Json anno,
	                               String name, Object value) {

		// --- Use optional key from annotation
		if (!anno.key()
			  .equals(Json.defaultName)) {
			name = anno.key();
		}
		value = _encodeWithConverter(anno, value);
		_encodeKeyValue(builder, name, _encodeValue(value));
	}

	private String _encodeString(String str) {
		str = str.replace("\\", "\\\\");
		if (_json5) {
			return "'" + str + "'";
		}
		str = str.replace("\"", "\\\"");
		return "\"" + str + "\"";
	}

	private String _encodeValueIndented(Object value) {
		String encoded = _encodeValue(value);
		return _prettyPrint ? _indentation[_stack.size()] + encoded : encoded;
	}

	/**
	 * Encodes a Java object value. Performs following steps:
	 * <ol>
	 * <li>check {@code null} value</li>
	 * <li>check basic value:</li>
	 * <li>check {@code Json.converter()}</li>
	 * <li>perform recursion on array, collection, map or pojo</li>
	 * </ol>
	 *
	 * @param value Java object to be encoded
	 * @return part for a Json string
	 */
	@SuppressWarnings("unchecked")
	private String _encodeValue(Object value) {
		if (isEmpty(value)) {
			return _withNullMembers ? "null" : null;
		}

		// --- Encode basic value
		if (value instanceof Boolean || value instanceof Byte
			  || value instanceof Double || value instanceof Float
			  || value instanceof Integer || value instanceof Long
			  || value instanceof Short) {
			return value.toString();
		}
		// --- Encode derived value
		if (value instanceof BigDecimal || value instanceof Character
			  || value instanceof Currency || value instanceof Enum
			  || value instanceof LocalDate || value instanceof Locale
			  || value instanceof OffsetDateTime || value instanceof String
			  || value instanceof UUID || value instanceof URI
			  || value instanceof URL) {
			return _encodeString(value.toString());
		}

		// --- Encode recursive objects
		if (_stack.contains(value)) {
			throw new RecursionException("Json Encoding Exception."
				  + " Already encoded: " + value + "\n" + _stack);
		}
		_stack.add(value);
		String json = null;
		if (value.getClass()
			  .isArray()) {
			json = _encodeArray(value);
		} else if (value instanceof List) {
			json = _encodeCollection((Collection<?>) value);
		} else if (value instanceof AbstractCollection) {
			json = _encodeCollection((Collection<?>) value);
		} else if (value instanceof Map) {
			json = _encodeMap((Map<Object, Object>) value);
		} else {
			json = _encodePojo(value);
		}
		_stack.remove(value);
		return json;
	}

	/**
	 * Gets value from {@code Json.converter()} parameter.
	 *
	 * @param anno  Json annotation
	 * @param value value to be encoded
	 * @return converted value or given {@code value}
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private Object _encodeWithConverter(Json anno, Object value) {
		if ((anno != null) && (value != null)) {
			Class<? extends JsonConverter> annotatedConverter =
				  anno.converter();
			if (annotatedConverter != JsonConverter.class) {
				try {
					JsonConverter converter = annotatedConverter
						  .getDeclaredConstructor()
						  .newInstance();
					value = converter.encodeToJson(value);
				} catch (InstantiationException | IllegalAccessException
				         | IllegalArgumentException | InvocationTargetException
				         | NoSuchMethodException | SecurityException e) {
					logger.atWarning()
						  .withCause(e)
						  .log("JsonCodec._encodeWithConverter( " + anno
								+ ", '" + value + "') failed with "
								+ e.getMessage());
				}
			}
		}
		return value;
	}

	private String _stripLeadingComma(StringBuilder builder) {
		if (_prettyPrint) {
			builder.append("\n")
				  .append(_indentation[_stack.size() - 1]);
		}
		return ((builder.length() > 2) && (builder.charAt(0) == ','))
			  ? builder.substring(1)
			  : builder.toString();
	}
}