/**
 * Copyright by Djarjo GmbH 2023
 */
package com.djarjo.jetson;

import com.djarjo.common.BaseConverter;
import com.djarjo.common.ReflectionHelper;
import com.djarjo.jetson.converter.JsonConverter;
import com.djarjo.text.RecursionException;
import com.google.common.flogger.FluentLogger;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.time.*;
import java.util.*;

/**
 * Encodes objects into a JSON string.
 * <p>
 * The JSON encoder handles basic values like numbers, strings, booleans, null, UUID,
 * {@code java.time} objects and lists, sets, maps of them directly.
 * Enumerations will be encoded by their {@code toJson()} method.
 * If this does not exist then their {@code toString()} method
 * will be used. Any other object is converted by recursively calling their getter methods
 * which are annotated with {@code @Json}. If no annotated getter is found then
 * {@code toJson()} will be invoked. If this method does not exist then an empty JSON
 * object "{}" will be returned.
 *
 * @author Hajo Lemcke
 * @since 2026-05-14 changed withNulls to skipNulls and added skipEmpty
 * @since 2017-01-02
 */
public class JsonEncoder {
  /**
   * Name of method in a class which will be used by default
   */
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

  // --- Setting: true skips members which are empty: String, List, Set, Map
  private boolean _skipEmpty = false;

  // --- Setting: true skips members with a {@code null} value
  private boolean _skipNull = false;

  /**
   * Empty no arg constructor
   */
  public JsonEncoder() {
  }

  /**
   * Instantiates and returns a new instance of this class to be used in streaming method
   * calls. This method only exists because Java cannot call a method during instantiation.
   *
   * @return new instance of JsonEncoder()
   */
  public static JsonEncoder encoder() {
    return new JsonEncoder();
  }

  /**
   * Encodes the given Java object into a JSON string.
   * <ul>
   * <li>A collection (array, list or set) will be encoded into a JSON list
   * enclosed in square brackets</li>
   * <li>A map will be encoded into a JSON object enclosed in curly braces</li>
   * <li>A POJO annotated with {@literal @Json} will be encoded as follows
   * 	 <ol><li>converter will be used if declared</li>
   * 	     <li>method {@literal toJson()} will be called if found in class</li>
   *       <li>all members (fields and getters) will be encoded
   *       unless annotated with {@literal @JsonTransient}</li></ol></li>
   * <li>A POJO <i>not</i> annotated with {@literal @Json} will be encoded by encoding
   *   all members (fields and getters) which are annotated with {@literal @Json}</li>
   * </ul>
   *
   * @param object to be encoded
   * @return JSON string or {@code null} if the parameter is {@code null} or an error has occurred
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
   * @param value to check
   * @return {@code true} if value is {@code null} or effectively empty
   */
  public boolean isEmpty( Object value ) {
    if ( value == null ) {
      return true;
    }
    if ( value.getClass().isArray() ) {
      return Array.getLength( value ) == 0;
    } else if ( value instanceof String str ) {
      if ( str.isBlank() ) return true;
      return value.equals( "null" );
    } else if ( value instanceof List ) {
      return ((List<?>) value).isEmpty();
    } else if ( value instanceof Map ) {
      return ((Map<?, ?>) value).isEmpty();
    } else if ( value instanceof Set ) {
      return ((Set<?>) value).isEmpty();
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
   * Skip members of an object which are empty instead of including them.
   *
   * @return encoder for streaming API
   */
  public JsonEncoder skipEmpty() {
    _skipEmpty = true;
    return this;
  }

  /**
   * Skip members of an object which are {@code null} instead of including them.
   *
   * @return encoder for streaming API
   */
  public JsonEncoder skipNull() {
    _skipNull = true;
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
   * Encodes to <a href="https://json5.org">JSON 5</a> but into a single line without spaces
   *
   * @return this for fluent API
   */
  public JsonEncoder toJson5Compact() {
    _indent = null;
    _prettyPrint = false;
    _toJson5 = true;
    return this;
  }

  /**
   * Encoder tries to <i>pretty-print</i> values in the resulting JSON string by
   * prefixing values with the string {@code indent}.
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
   * Encodes enumeration value.
   * <p>
   * If the enumeration class is annotated with {@literal @Json} then this will be used.
   * Otherwise, the enum value will be converted to its String representation.
   * </p>
   *
   * @param value enum item
   * @return encoded value
   */
  private String _encodeEnum( Enum<?> value ) {
    List<JsonAccessor> accessors = JsonCache.getAccessors( value.getClass() );
    return (accessors == null || accessors.isEmpty()) ? _encodeString( value.toString() )
        : accessors.get( 0 ).getValue( value ).toString();
  }

  /**
   * Appends "key:value" to builder.
   * <p>
   * Appends:
   * <ul><li>{@literal "key":value} if not empty</li>
   * <li>{@code null} if value is {@code null} and {@literal skipNull}
   * </p>
   *
   * @param builder current JSON text part
   * @param key key from map or POJO
   * @param value value of key
   */
  private void _encodeKeyValue( StringBuilder builder, String key, Object value ) {
    if ( (value == null) && _skipNull ) return;
    if ( isEmpty( value ) && _skipEmpty ) return;
    builder.append( "," );
    if ( _prettyPrint ) {
      builder.append( "\n" );
      builder.append( _indentation[_stack.size()] );
    }
    builder.append( _toJson5 ? key + (_prettyPrint ? ": " : ":") : "\"" + key + "\":" );
    builder.append( value );
  }

  /**
   * Encodes the given map into {@literal "{" key ":" value [", " key ":" value]* "}"
   */
  private String _encodeMap( Map<Object, Object> map ) {
    StringBuilder jsonStringBuilder = new StringBuilder();
    map.entrySet().stream()
        .sorted( Map.Entry.comparingByKey(
            Comparator.comparing( Object::toString ) ) )
        .forEach( e -> _encodeKeyValue(
            jsonStringBuilder,
            e.getKey().toString(),
            _encodeValue( e.getValue() )
        ) );
    return "{" + _stripLeadingComma( jsonStringBuilder ) + "}";
  }

  /**
   * Encodes the given plain old Java object (POJO) into a JSON object
   */
  @SuppressWarnings("unchecked")
  private String _encodePojo( Object pojo ) {
    StringBuilder builder = new StringBuilder();
    Class<?> pojoClass = pojo.getClass();
    List<JsonAccessor> accessors = JsonCache.getAccessors( pojoClass );
    if ( accessors == null || accessors.isEmpty() ) return "";

    //--- Handle class level annotation "converter" or "toJson()" right here
    if ( (accessors.size() == 1) && (accessors.getFirst().isClass()) ) {
      // --- use converter if given
      JsonConverter converter = accessors.getFirst().getConverter();
      if ( converter != null ) {
        return converter.encodeToJson( pojo );
      }

      //--- use method "toJson()" if it exists in pojo class
      Method toJson = ReflectionHelper.findMethod( pojoClass, METHOD_TO_JSON );
      if ( toJson != null ) {
        try {
          String json = (String) ReflectionHelper.getValueFromMember( pojo, toJson );
          return _encodeString( json );
        } catch ( IllegalAccessException | InvocationTargetException e ) {
          logger.atWarning().withCause( e )
              .log( "Cannot get value from 'toJson()' in %s", pojoClass.getName() );
          return null;
        }
      }
    }

    //--- now handle all fields and methods
    for ( JsonAccessor accessor : accessors ) {
      Object value = accessor.getValue( pojo );
      if ( accessor.mayEncode( value, _skipEmpty, _skipNull ) ) {
        String name = accessor.getJsonName();
        String encodedValue = _encodeValue( value );
        _encodeKeyValue( builder, name, encodedValue );
      }
    }
    return "{" + _stripLeadingComma( builder ) + "}";
  }

  /**
   * Encodes given string for JSON.
   *
   * @param str string
   * @return string enclosed in quotes
   */
  private String _encodeString( String str ) {
    StringBuilder builder = new StringBuilder();
    for ( int i = 0; i < str.length(); i++ ) {
      char c = str.charAt( i );
      switch ( c ) {
        case '"':
          builder.append( _toJson5 ? "\"" : "\\\"" );
          break;
        case '\\':
          builder.append( "\\\\" );
          break;
        case '\b':
          builder.append( "\\b" );
          break;
        case '\f':
          builder.append( "\\f" );
          break;
        case '\n':
          builder.append( "\\n" );
          break;
        case '\r':
          builder.append( "\\r" );
          break;
        case '\t':
          builder.append( "\\t" );
          break;
        default:
          if ( c < 0x20 ) {
            builder.append( "\\u00" );
            String hex = Integer.toHexString( c );
            if ( hex.length() == 1 ) {
              builder.append( '0' );
            }
            builder.append( hex );
          } else {
            builder.append( c );
          }
      }
    }
    String s = builder.toString();
    //--- if s contains single quote then even JSON-5 must put the string in double quotes
    return (s.contains( "'" ) || !_toJson5) ? "\"" + s + "\"" :
        "'" + s + "'";
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
    if ( value == null ) {
      return _skipNull ? null : "null";
    }
    if ( isEmpty( value ) ) {
      return _skipEmpty ? null
          : (value instanceof Collection) ? "[]"
            : (value instanceof Map) ? "{}"
              : "\"\"";
    }

    // --- Encode basic value
    if ( (value instanceof Boolean) || (value instanceof Byte) || (value instanceof Double)
        || (value instanceof Float) || (value instanceof Integer) || (value instanceof Long)
        || (value instanceof Short) ) {
      return value.toString();
    }

    // --- Encode value objects
    if ( (value instanceof BigDecimal) || (value instanceof Character)
        || (value instanceof Currency) || (value instanceof Duration)
        || (value instanceof Instant) || (value instanceof LocalDate)
        || (value instanceof LocalTime) || (value instanceof LocalDateTime)
        || (value instanceof Locale) || (value instanceof OffsetDateTime)
        || (value instanceof Period) || (value instanceof String)
        || (value instanceof UUID) || (value instanceof URI)
        || (value instanceof URL) || (value instanceof ZonedDateTime) ) {
      return _encodeString( value.toString() );
    }

    //--- Special handling for enums
    if ( value instanceof Enum e ) {
      return _encodeEnum( e );
    }

    // --- Encode recursive objects
    if ( _stack.contains( value ) ) {
      throw new RecursionException(
          "JSON Encoding Exception. Already encoded: " + value + "\n" + _stack );
    }
    _stack.add( value );
    String json;
    if ( value.getClass().isArray() ) {
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