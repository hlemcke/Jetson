/**
 * Copyright by Djarjo GmbH 2023
 */
package com.djarjo.jetson;

import com.djarjo.common.BaseConverter;
import com.djarjo.common.BeanHelper;
import com.djarjo.common.ReflectionHelper;
import com.djarjo.jetson.converter.JsonConverter;
import com.djarjo.text.RecursionException;
import com.google.common.flogger.FluentLogger;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Encodes objects into a JSON string.
 * <p>
 * The JSON encoder handles numbers, strings, booleans, null, lists, sets, maps, UUID,
 * {@code java.time} objects directly. Enumerations will be encoded by their
 * {@code toJson()} method. If this does not exist then their {@code toString()} method
 * will be used. Any other object is converted by recursively calling their getter methods
 * which are annotated with {@code @Json}. If no annotated getter is found then
 * {@code toJson()} will be invoked. If this method does not exist then an empty JSON
 * object "{}" will be returned.
 *
 * @author Hajo Lemcke
 * @since 2017-01-02
 */
public class JsonEncoder {
	/** METHOD_TO_JSON */
	public final static String METHOD_TO_JSON = "toJson";
	private final static FluentLogger logger = FluentLogger.forEnclosingClass();
	private final int MAX_INDENT = 10;
	private final String[] _indentation = new String[MAX_INDENT];

	// --- Setting: encode an array or list of bytes into a Base64 string
	private boolean _bytesToBase64 = true;

	// --- Setting: encode to JSON 5 {@link https://json5.org}
	private boolean _toJson5 = false;

	// --- Setting: indentation character(s)
	private String _indent = null;
	private boolean _prettyPrint = false;

	// --- Used to prevent circular references in object hierarchy
	private Set<Object> _stack;

	// --- Setting: true encodes members with a {@code null} value
	private boolean _withNulls = false;

	/**
	 * Empty no arg constructor
	 */
	public JsonEncoder() {
	}

	/**
	 * Instantiates and returns a new instance of this class to be used in streaming method
	 * calls. This method only exists because Java cannot call a method during
	 * instantiation.
	 *
	 * @return new JsonEncoder()
	 */
	public static JsonEncoder encoder() {
		return new JsonEncoder();
	}

	/**
	 * Encodes the given Java object into a JSON string.
	 * <ul>
	 * <li>A collection like a list or a set will be encoded into a JSON list
	 * enclosed in square brackets</li>
	 * <li>A map will be encoded into a JSON object enclosed in curly braces. {@code null}
	 * values will not be added to the map unless the encoder is invoked with
	 * {@code withNulls()}</li>
	 * <li>A POJO annotated with {@literal @Json} will be encoded as follows
	 * 	 <ol><li>a converter will be used if declared</li>
	 * 	     <li>method {@literal toJson()} will be called if found in class</li>
	 *       <li>all members (fields and getters) will be encoded
	 *       unless annotated with {@literal @JsonTransient}</li></ol></li>
	 * <li>A POJO <i>not</i> annotated with {@literal @Json} will be encoded by encoding
	 *   all members (fields and getters) which are annotated with {@literal @Json}</li>
	 * </ul>
	 *
	 * @param object The object to be encoded
	 * @return encoded object as a JSON string or {@code null} if the parameter is
	 * {@code null} or an error has occurred
	 */
	public String encode( Object object ) {
		_stack = new HashSet<>();
		return _encodeValue( object );
	}

	/**
	 * Gets characters used for <i>pretty-print</i>.
	 *
	 * @return indentation string or {@code null}
	 */
	public String getIndent() {
		return _indent;
	}

	/**
	 * Checks whether an object is empty.
	 *
	 * <p>
	 * If the given parameter is null then {@code true} will be returned. This method works
	 * on the following types:
	 * <ul>
	 * <li>String is empty when its length is zero, when it only contains spaces
	 * and tabs or when the text is "null"</li>
	 * <li>Lists, Maps and Sets are considered empty when they do not contain
	 * any element</li>
	 * </ul>
	 *
	 * @param obj bean to check
	 * @return true if object is effectively empty
	 */
	public boolean isEmpty( Object obj ) {
		if ( obj == null ) {
			return true;
		}
		if ( obj.getClass().isArray() ) {
			return Array.getLength( obj ) == 0;
		} else if ( obj instanceof String str ) {
			if ( str.isBlank() ) return true;
			return obj.equals( "null" );
		} else if ( obj instanceof List ) {
			return ((List<?>) obj).isEmpty();
		} else if ( obj instanceof Map ) {
			return ((Map<?, ?>) obj).isEmpty();
		} else if ( obj instanceof Set ) {
			return ((Set<?>) obj).isEmpty();
		}
		return false;
	}

	/**
	 * Encodes an array or list of bytes into a JSON list of comma separated integer values
	 * instead of a Base64 string.
	 *
	 * @return this for fluent API
	 */
	public JsonEncoder bytesToList() {
		_bytesToBase64 = false;
		return this;
	}

	/**
	 * Encode to <a href="https://json5.org">JSON 5</a>
	 *
	 * @return this for fluent API
	 */
	public JsonEncoder toJson5() {
		_toJson5 = true;
		if ( _indent == null ) {
			prettyPrint( "  " );
		}
		return this;
	}

	/**
	 * Encode members of an object with a value of {@code null} instead of skipping them.
	 *
	 * @return encoder for streaming API
	 */
	public JsonEncoder withNulls() {
		_withNulls = true;
		return this;
	}

	/**
	 * Encoder tries to <i>pretty-print</i> values in the resulting JSON string by
	 * prefixing
	 * values with the string {@code indent}.
	 * <p>
	 * This string must only contain one or more of space (U+0020) or tab (U+0009)
	 * characters. If indent is not empty then each single value in the resulting JSON
	 * string will be on its own line. Lines are separated by line feed characters
	 * (U+000a).
	 * </p>
	 *
	 * @param indent default = {@code null}
	 * @return encoder for streaming API
	 */
	public JsonEncoder prettyPrint( String indent ) {
		for ( int i = 0; i < indent.length(); i++ ) {
			if ( indent.charAt( i ) != ' ' && indent.charAt( i ) != '\t' ) {
				indent = "  ";
				break;
			}
		}
		_indent = indent;
		_prettyPrint = true;
		// --- Prepare indentations for depth up to MAX_INDENT
		_indentation[0] = "";
		_indentation[1] = _indent;
		for ( int i = 2; i < MAX_INDENT; i++ ) {
			_indentation[i] = _indentation[i - 1] + _indent;
		}
		return this;
	}

	/**
	 * Encodes the given array into {@literal "[" value ["," value]* "]"}.
	 * <p>
	 * If {@code bytesToBase64 = true} then an array of bytes will be encoded into a Base64
	 * string instead.
	 * </p>
	 */
	private String _encodeArray( Object array ) {
		Class<?> _elemClazz = array.getClass()
				.getComponentType();
		if ( _bytesToBase64 && (_elemClazz.equals( byte.class ) || _elemClazz.equals(
				Byte.class )) ) {
			return "\"" + Base64.getUrlEncoder()
					.withoutPadding()
					.encodeToString( (byte[]) array ) + "\"";
		}
		StringBuilder jsonStringBuilder = new StringBuilder();
		Object[] _array = BaseConverter.convertToArray( array );
		for ( Object obj : _array ) {
			jsonStringBuilder.append( _prettyPrint ? ",\n" : "," );
			jsonStringBuilder.append( _encodeValueIndented( obj ) );
		}
		return "[" + _stripLeadingComma( jsonStringBuilder ) + "]";
	}

	/**
	 * Encodes the given collection into {@literal "[" value ["," value]* "]"}
	 */
	private String _encodeCollection( Collection<?> collection ) {
		StringBuilder jsonStringBuilder = new StringBuilder();
		for ( Object obj : collection ) {
			jsonStringBuilder.append( _prettyPrint ? ",\n" : "," );
			jsonStringBuilder.append( _encodeValueIndented( obj ) );
		}
		return "[" + _stripLeadingComma( jsonStringBuilder ) + "]";
	}

	/**
	 * Appends "key:value" to builder if value is not null or IsWithNulls
	 *
	 * @param builder current json text part
	 * @param key key from map or POJO
	 * @param value value of key
	 */
	private void _encodeKeyValue( StringBuilder builder, String key, Object value ) {
		if ( value != null || _withNulls ) {
			builder.append( "," );
			if ( _prettyPrint ) {
				builder.append( "\n" );
				builder.append( _indentation[_stack.size()] );
			}
			builder.append( _toJson5 ? key + ": " : "\"" + key + "\":" );
			builder.append( value );
		}
	}

	/**
	 * Encodes the given map into {@literal "{" key ":" value [", " key ":" value]* "}"
	 */
	private String _encodeMap( Map<Object, Object> map ) {
		StringBuilder jsonStringBuilder = new StringBuilder();
		Iterator<Map.Entry<Object, Object>> iter = map.entrySet()
				.iterator();
		Map.Entry<Object, Object> entry;
		while ( iter.hasNext() ) {
			entry = iter.next();
			_encodeKeyValue( jsonStringBuilder, entry.getKey()
					.toString(), _encodeValue( entry.getValue() ) );
		}
		return "{" + _stripLeadingComma( jsonStringBuilder ) + "}";
	}

	/**
	 * Encodes the given plain old Java object (POJO) into a JSON object
	 */
	private String _encodePojo( Object pojo ) {
		boolean accessFields = false;
		boolean allMembers = false;
		StringBuilder builder = new StringBuilder();
		String name;
		Class<?> clazz = pojo.getClass();
		Object value = null;

		// --- Check annotation on class level
		Json anno = clazz.getAnnotation( Json.class );
		if ( anno != null ) {
			// --- use converter if given
			JsonConverter converter = Jetson.getConverter( anno );
			if ( converter != null ) {
				return converter.encodeToJson( pojo );
			}

			//--- use method "toJson()" if it exists in pojo class
			Method toJson = ReflectionHelper.findMethod( clazz, METHOD_TO_JSON );
			if ( toJson != null ) {
				try {
					return ReflectionHelper.getValueFromMember( pojo, toJson ).toString();
				} catch ( IllegalAccessException | InvocationTargetException e ) {
					logger.atWarning().withCause( e )
							.log( "Cannot get value from 'toJson' in %s", clazz.getName() );
					return null;
				}
			}
			if ( JsonAccessType.FIELD.equals( anno.accessType() ) ) {
				accessFields = true;
			}
			allMembers = true;
		}

		// --- Encode fields
		Field[] fields = clazz.getFields();
		for ( Field field : fields ) {
			if ( _isToEncode( field, allMembers, accessFields ) ) {
				try {
					anno = field.getAnnotation( Json.class );
					field.setAccessible( true );
					name = field.getName();
					value = field.get( pojo );
					_encodePojoMember( builder, anno, name, value );
				} catch ( IllegalArgumentException | IllegalAccessException e ) {
					logger.atWarning()
							.withCause( e )
							.log( "Cannot encode field " + field + " for " + value );
				}
			}
		}

		// --- Encode methods
		Method[] methods = BeanHelper.obtainGetters( pojo.getClass() );
		for ( Method method : methods ) {
			if ( _isToEncode( method, allMembers, !accessFields ) ) {
				try {
					anno = method.getAnnotation( Json.class );
					method.setAccessible( true );
					name = ReflectionHelper.getVarNameFromMethodName( method.getName() );
					value = method.invoke( pojo, (Object[]) null );
					_encodePojoMember( builder, anno, name, value );
				} catch ( IllegalAccessException | IllegalArgumentException |
									InvocationTargetException e ) {
					logger.atWarning()
							.withCause( e )
							.log( "Cannot invoke %s on %s", method, clazz );
				}
			}
		}
		return "{" + _stripLeadingComma( builder ) + "}";
	}

	boolean _isToEncode( AccessibleObject member, boolean allMembers,
			boolean accessMember ) {
		Json anno = member.getAnnotation( Json.class );
		return ((anno != null && anno.encodable())
				|| (anno == null && allMembers && accessMember && !member.isAnnotationPresent(
				JsonTransient.class )));
	}

	/**
	 * Encodes the value of a member (from object field or method) into a JSON string.
	 * <ol>
	 * <li>check {@code Json.access()}</li>
	 * <li>check {@code Json.converter()}</li>
	 * </ol>
	 *
	 * @param anno Complete annotation with attributes
	 * @param name Name of field
	 * @param value value to be encoded
	 */
	private void _encodePojoMember( StringBuilder builder, Json anno, String name,
			Object value ) {

		//--- Check annotation (if present) attributes
		if ( anno != null && value != null ) {
			if ( !anno.key().equals( Json.defaultName ) ) {
				name = anno.key();
			}
			JsonConverter converter = Jetson.getConverter( anno );
			if ( converter != null ) {
				value = converter.encodeToJson( value );
			}
		}
		_encodeKeyValue( builder, name, _encodeValue( value ) );
	}

	private String _encodeString( String str ) {
		str = str.replace( "\\", "\\\\" );
		if ( _toJson5 ) {
			return "'" + str + "'";
		}
		str = str.replace( "\"", "\\\"" );
		return "\"" + str + "\"";
	}

	private String _encodeValueIndented( Object value ) {
		String encoded = _encodeValue( value );
		return _prettyPrint ? _indentation[_stack.size()] + encoded : encoded;
	}

	/**
	 * Encodes a Java object value. Performs the following steps in given order:
	 * <ol>
	 * <li>check {@code null} value</li>
	 * <li>check basic values: bool, all numerical, String, UUID, java.time.* </li>
	 * <li>check {@code Json.converter()}</li>
	 * <li>on class annotation use methods {@code toJson} and {@code fromJson} if they
	 * exist</li>
	 * <li>perform recursion on array, collection, map and POJO</li>
	 * </ol>
	 *
	 * @param value Java object to be encoded
	 * @return part for the final JSON string
	 */
	@SuppressWarnings("unchecked")
	private String _encodeValue( Object value ) {
		if ( isEmpty( value ) ) {
			return _withNulls ? "null" : null;
		}

		// --- Encode basic value
		if ( value instanceof Boolean || value instanceof Byte || value instanceof Double
				|| value instanceof Float || value instanceof Integer || value instanceof Long
				|| value instanceof Short ) {
			return value.toString();
		}
		// --- Encode derived value
		if ( value instanceof BigDecimal || value instanceof Character
				|| value instanceof Currency || value instanceof Enum
				|| value instanceof LocalDate || value instanceof Locale
				|| value instanceof OffsetDateTime || value instanceof String
				|| value instanceof UUID || value instanceof URI || value instanceof URL ) {
			return _encodeString( value.toString() );
		}

		// --- Encode recursive objects
		if ( _stack.contains( value ) ) {
			throw new RecursionException(
					"Json Encoding Exception." + " Already encoded: " + value + "\n" + _stack );
		}
		_stack.add( value );
		String json;
		if ( value.getClass()
				.isArray() ) {
			json = _encodeArray( value );
		} else if ( value instanceof Map ) {
			json = _encodeMap( (Map<Object, Object>) value );
		} else if ( value instanceof Collection<?> ) {
			json = _encodeCollection( (Collection<?>) value );
		} else {
			json = _encodePojo( value );
		}
		_stack.remove( value );
		return json;
	}

	private String _stripLeadingComma( StringBuilder builder ) {
		if ( _prettyPrint ) {
			builder.append( "\n" )
					.append( _indentation[_stack.size() - 1] );
		}
		return ((builder.length() > 2) && (builder.charAt( 0 ) == ',')) ?
				builder.substring( 1 ) : builder.toString();
	}
}