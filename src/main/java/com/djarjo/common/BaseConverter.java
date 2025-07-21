package com.djarjo.common;

import com.djarjo.text.TextHelper;
import com.google.common.flogger.FluentLogger;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;

/**********************************************************************
 * This class solely consists of static methods which convert a value from one
 * type to another one. If the input type is of type string then the parser
 * methods from {@link com.djarjo.text.TextHelper TextHelper} will be used.
 * <p>
 * All methods are reentrant and multi-thread save.
 * </p>
 *
 * @author Hajo Lemcke Dec. 2014
 * @since 2020-08-17 added bytesToLong and longToBytes
 */
public class BaseConverter {
	private final static FluentLogger logger = FluentLogger.forEnclosingClass();

	/***
	 * All arrays of primitive types
	 */
	private final static Class<?>[] _ARRAY_PRIMITIVE_TYPES =
			{int[].class, float[].class, double[].class, boolean[].class,
					byte[].class, short[].class, long[].class, char[].class};
	private static final Map<Class<?>, Function<Object, Object>> _converters =
			new HashMap<>();

	/**
	 * Useless public constructor implemented for Javadoc only
	 */
	public BaseConverter() {
	}

	/**
	 * Converts 8 bytes from {@code bytes} starting at {@code offset} into a long value.
	 *
	 * @param bytes byte array
	 * @param offset index to highest byte
	 * @return long value or {@code null}
	 */
	public static Long bytesToLong( byte[] bytes, int offset ) {
		if ( bytes == null ) return null;
		long result = 0L;
		int end = (bytes.length - offset < Long.BYTES) ? bytes.length
				: offset + Long.BYTES;
		for ( int i = offset; i < end; i++ ) {
			result <<= Byte.SIZE;
			result |= (bytes[i] & 0xFF);
		}
		return result;
	}

	/**
	 * Converts an array of primitive types in an array of objects. An arrays of objects
	 * will just be returned.
	 *
	 * @param arrayOfPrimitiveTypes array of byte, char, ...
	 * @return array of objects or {@code null} if the given parameter is {@code null}
	 */
	public static Object[] convertToArray( Object arrayOfPrimitiveTypes ) {
		if ( arrayOfPrimitiveTypes == null ) {
			return null;
		}
		Class<?> valKlass = arrayOfPrimitiveTypes.getClass();
		Object[] outputArray = null;

		for ( Class<?> arrKlass : _ARRAY_PRIMITIVE_TYPES ) {
			if ( valKlass.isAssignableFrom( arrKlass ) ) {
				int arrlength = Array.getLength( arrayOfPrimitiveTypes );
				outputArray = new Object[arrlength];
				for ( int i = 0; i < arrlength; ++i ) {
					outputArray[i] = Array.get( arrayOfPrimitiveTypes, i );
				}
				break;
			}
		}
		if ( outputArray == null ) { // not a primitive type array
			outputArray = (Object[]) arrayOfPrimitiveTypes;
		}
		return outputArray;
	}

	/**
	 * Converts `name` to the enumeration class `type`.
	 * <p>
	 * If `name` is "className.valueName" like in Dart then only "valueName" will be used.
	 *
	 * @param type the enum class
	 * @param name string representation of an enum value
	 * @return enumeration value or {@code null}
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Enum convertToEnum( Class type, String name ) {
		int index = name.indexOf( '.' );
		if ( index > 0 ) {
			name = name.substring( index + 1 );
		}
		Enum value = null;
		try {
			value = Enum.valueOf( type, name );
		} catch ( IllegalArgumentException ex ) {
			logger.atWarning()
					.withCause( ex )
					.log( "Enumeration %s has no value %s", type, name );
		}
		return value;
	}

	/******************************************************************
	 * Converts {@code value} to given target {@code type} if possible.
	 *
	 * @param value
	 *            value
	 * @param type
	 *            target type
	 * @return value converted to target type
	 */
	public static Object convertToType( Object value, Class<?> type ) {
		if ( value == null ) { // null has no type
			return null;
		} else if ( type.equals( value.getClass() )
				|| type.equals( Object.class ) ) {
			return value;
		} else if ( value instanceof Map ) {
			return value;
		} else if ( value instanceof Collection ) {
			return value;
		} else if ( Enum.class.isAssignableFrom( type ) ) {
			return convertToEnum( type, (String) value );
		} else if ( type.isArray() ) {
			if ( value.getClass().isArray() ) {
				return value;
			}
			if ( value instanceof List ) {
				return ((List<?>) value).toArray();
			}
			logger.atWarning().log( "Target type is array of type '%s'." +
					" Cannot convert \"%s\" %s", type, value, value.getClass() );
		}
		return _getConverters().getOrDefault( type, v -> {
			throw new IllegalArgumentException( "Unsupported type:" + type );
		} ).apply( value );
	}

	/**
	 * Converts the given long value to an array with 8 bytes. MSB first.
	 *
	 * @param value Long value (64bit)
	 * @return 8 bytes or {@code null}
	 */
	public static byte[] longToByteArray( Long value ) {
		if ( value == null ) {
			return null;
		}
		byte[] result = new byte[Long.BYTES];
		for ( int i = Long.BYTES - 1; i >= 0; i-- ) {
			result[i] = (byte) (value & 0xFF);
			value >>= 8;
		}
		return result;
	}

	/**
	 * Converts the given long value to a list with 8 bytes. MSB first.
	 *
	 * @param value Long value (64bit)
	 * @return list with 8 bytes or {@code null}
	 */
	public static List<Byte> longToByteList( Long value ) {
		if ( value == null ) {
			return null;
		}
		List<Byte> bytes = new ArrayList<>();
		for ( int i = Long.BYTES - 1; i >= 0; i-- ) {
			bytes.add( 0, (byte) (value & 0xFF) );
			value >>= 8;
		}
		return bytes;
	}

	/**
	 * Converts {@code value} to a Boolean object
	 *
	 * @param value to be converted
	 * @return {@code null} if not convertible to Boolean
	 */
	public static Boolean toBoolean( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof String s ) return TextHelper.parseBoolean( s );
		if ( value instanceof Number n ) return n.doubleValue() != 0.0;
		logger.atWarning().log( "Cannot convert to Boolean: '%s'", value );
		return false;
	}

	/**
	 * Converts {@code value} to a BigDecimal
	 *
	 * @param value Byte, Double, Float, Integer, Long, Short or String
	 * @return new instance of BigDecimal or {@code null}
	 */
	public static BigDecimal toBigDecimal( Object value ) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof String ) {
			return TextHelper.parseBigDecimal( (String) value );
		}
		try {
			if ( value instanceof Byte ) {
				return BigDecimal.valueOf( (Byte) value );
			}
			if ( value instanceof Double ) {
				return BigDecimal.valueOf( (Double) value );
			}
			if ( value instanceof Float ) {
				return BigDecimal.valueOf( (Float) value );
			}
			if ( value instanceof Integer ) {
				return BigDecimal.valueOf( (Integer) value );
			}
			if ( value instanceof Long ) {
				return BigDecimal.valueOf( (Long) value );
			}
			if ( value instanceof Short ) {
				return BigDecimal.valueOf( (Short) value );
			}
		} catch ( NumberFormatException e ) {
			// --- Just return null
		}
		return null;
	}

	/**
	 * Converts {@code value} to a Byte object
	 *
	 * @param value to be converted
	 * @return {@code null} if not convertible to Byte
	 */
	public static Byte toByte( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof Number n ) return n.byteValue();
		if ( value instanceof String s ) return TextHelper.parseByte( s );
		logger.atWarning().log( "Cannot convert to Byte: '%s'", value );
		return null;
	}

	/**
	 * Converts {@code value} to a Double.
	 *
	 * @param value value to convert
	 * @return {@code null} if not convertible to a Double
	 */
	public static Double toDouble( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof Number n ) return n.doubleValue();
		if ( value instanceof String s ) return TextHelper.parseDouble( s );
		logger.atWarning().log( "Cannot convert to Double: '%s'", value );
		return null;
	}

	/**
	 * Converts {@code value} to a Float.
	 *
	 * @param value value to convert
	 * @return {@code null} if not convertible to a Float
	 */
	public static Float toFloat( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof Number n ) return n.floatValue();
		if ( value instanceof String s ) return TextHelper.parseFloat( s );
		logger.atWarning().log( "Cannot convert to Float: '%s'", value );
		return null;
	}

	/**
	 * Converts {@code value} to an Integer.
	 *
	 * @param value value to convert
	 * @return {@code null} if not convertible to an Integer
	 */
	public static Integer toInteger( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof Number n ) return n.intValue();
		if ( value instanceof String s ) return TextHelper.parseInteger( s );
		logger.atWarning().log( "Cannot convert to Integer: '%s'", value );
		return null;
	}

	/**
	 * Converts {@code value} to LocalDate
	 *
	 * @param value value to convert
	 * @return {@code null} if not convertible to LocalDate
	 */
	public static LocalDate toLocalDate( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof OffsetDateTime o ) return o.toLocalDate();
		if ( value instanceof String s ) return TextHelper.parseDate( s );
		logger.atWarning().log( "Cannot convert to LocalDate: '%s'", value );
		return null;
	}

	/**
	 * Converts {@code value} to LocalTime
	 *
	 * @param value value to convert
	 * @return {@code null} if not convertible to LocalTime
	 */
	public static LocalTime toLocalTime( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof OffsetDateTime o ) return o.toLocalTime();
		if ( value instanceof String s ) return TextHelper.parseTime( s );
		logger.atWarning().log( "Cannot convert to LocalTime: '%s'", value );
		return null;
	}

	/**
	 * Converts {@code value} to Locale
	 *
	 * @param value value to convert
	 * @return {@code null} if not convertible to Locale
	 */
	public static Locale toLocale( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof String s ) return Locale.forLanguageTag( s );
		logger.atWarning().log( "Cannot convert to Locale: '%s'", value );
		return null;
	}

	/**
	 * Converts {@code value} to a Long.
	 *
	 * @param value value to convert
	 * @return {@code null} if not convertible to Long
	 */
	public static Long toLong( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof Number n ) return n.longValue();
		if ( value instanceof String s ) return TextHelper.parseLong( s );
		logger.atWarning().log( "Cannot convert to Long: '%s'", value );
		return null;
	}

	/**
	 * Converts {@code value} to a OffsetDateTime object
	 *
	 * @param value to be converted
	 * @return {@code null} if not convertible to OffsetDateTime
	 */
	public static OffsetDateTime toOffsetDateTime( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof OffsetDateTime o ) return o;
		if ( value instanceof String s ) return TextHelper.parseDateTime( s );
		logger.atWarning().log( "Cannot convert to OffsetDateTime: '%s'", value );
		return null;
	}

	/**
	 * Converts {@code value} to a Short object
	 *
	 * @param value to be converted
	 * @return {@code null} if not convertible to Short
	 */
	public static Short toShort( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof Number n ) return n.shortValue();
		if ( value instanceof String s ) return TextHelper.parseShort( s );
		logger.atWarning().log( "Cannot convert to Short: '%s'", value );
		return null;
	}

	/**
	 * Converts given value to a string. A value of {@code null} will return an empty
	 * string.
	 * <p>
	 * Null-safe
	 *
	 * @param value Any value, even {@code null}
	 * @return string representation of value or an empty string
	 */
	public static String toString( BigDecimal value ) {
		return (value == null) ? "" : value.toString();
	}

	/**
	 * Converts {@code value} to a UUID.
	 *
	 * @param value null or an uuid in string format or a Base-64 encoded uuid
	 * @return {@code null} if not convertible to UUID
	 */
	public static UUID toUuid( Object value ) {
		if ( value == null ) return null;
		if ( value instanceof UUID u ) return u;
		if ( value instanceof String s ) return TextHelper.parseUuid( s );
		logger.atWarning().log( "Cannot convert to UUID: '%s'", value );
		return null;
	}

	/**
	 * Converts given uuid into URL safe Base64 encoded string.
	 *
	 * @param uuid value to be encoded to Base64
	 * @return Base64 encoded string (URL safe)
	 */
	public static String uuidToBase64( UUID uuid ) {
		if ( uuid == null ) {
			return null;
		}
		byte[] bitesHigh =
				BaseConverter.longToByteArray( uuid.getMostSignificantBits() );
		byte[] bitesLow =
				BaseConverter.longToByteArray( uuid.getLeastSignificantBits() );
		byte[] bites = new byte[Long.BYTES * 2];
		System.arraycopy( bitesHigh, 0, bites, 0, Long.BYTES );
		System.arraycopy( bitesLow, 0, bites, Long.BYTES, Long.BYTES );
		return Base64.getUrlEncoder().withoutPadding().encodeToString( bites );
	}

	private static Map<Class<?>, Function<Object, Object>> _getConverters() {
		if ( _converters.isEmpty() ) {
			_converters.put( boolean.class, BaseConverter::toBoolean );
			_converters.put( Boolean.class, BaseConverter::toBoolean );
			_converters.put( BigDecimal.class, BaseConverter::toBigDecimal );
			_converters.put( byte.class, BaseConverter::toByte );
			_converters.put( Byte.class, BaseConverter::toByte );
			_converters.put( Currency.class, v -> Currency.getInstance( v.toString() ) );
			_converters.put( double.class, BaseConverter::toDouble );
			_converters.put( Double.class, BaseConverter::toDouble );
			_converters.put( float.class, BaseConverter::toFloat );
			_converters.put( Float.class, BaseConverter::toFloat );
			_converters.put( int.class, BaseConverter::toInteger );
			_converters.put( Integer.class, BaseConverter::toInteger );
			_converters.put( long.class, BaseConverter::toLong );
			_converters.put( Long.class, BaseConverter::toLong );
			_converters.put( LocalDate.class, BaseConverter::toLocalDate );
			_converters.put( Locale.class, v -> toLocale( v.toString() ) );
			_converters.put( OffsetDateTime.class, v -> OffsetDateTime.parse( v.toString() ) );
			_converters.put( String.class, v -> v );
			_converters.put( UUID.class, BaseConverter::toUuid );
		}
		return _converters;
	}
}