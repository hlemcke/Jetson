package com.djarjo.codec;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.djarjo.text.TextHelper;
import com.google.common.flogger.FluentLogger;

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

	public final static UUID base64ToUuid( String text ) {
		if ( text == null ) {
			return null;
		}
		byte[] bytes = Base64.getDecoder().decode( text );
		long high = BaseConverter.bytesToLong( bytes, 0 );
		long low = BaseConverter.bytesToLong( bytes, Long.BYTES );
		return new UUID( high, low );
	}

	/**
	 * Converts 8 bytes from {@code bytes} starting at {@code offset} into a
	 * long value.
	 *
	 * @param bytes
	 *            byte array
	 * @param offset
	 *            index to highest byte
	 * @return long value or {@code null}
	 */
	public final static Long bytesToLong( byte[] bytes, int offset ) {
		if ( bytes == null ) {
			return null;
		}
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
	 * Converts Boolean to boolean.
	 *
	 * @param value
	 *            {@code true}, {@code false} or {@code null}
	 * @return {@code false} if value is {@code null} else value
	 */
	public final static boolean convert2boolean( Boolean value ) {
		return (value == null) ? false : value;
	}

	/**
	 * Converts the given object to an integer value. If {@code value} is
	 * {@code null} or not a number then 0 will be returned.
	 *
	 * @param value
	 * @return integer value
	 */
	public final static int convert2int( Object value ) {
		Integer intVal = convertToInteger( value );
		return (intVal == null) ? 0 : intVal;
	}

	/**
	 * Converts the given string to a basic integer value. If the string is not
	 * a number then 0 will be returned. If the value of the string is greater
	 * than Integer.MAX_VALUE then MAX_VALUE will be returned. If it is smaller
	 * than Integer.MIN_VALUE then MIN_Value will be returned.
	 *
	 * @param value
	 *            The text to be converted
	 * @return a basic integer value
	 * @see #convertToInteger(String)
	 */
	public final static int convert2int( String value ) {
		Long lval = stringToLong( value );
		if ( lval == null )
			return 0;
		if ( lval < Integer.MIN_VALUE )
			return Integer.MIN_VALUE;
		if ( lval > Integer.MAX_VALUE )
			return Integer.MAX_VALUE;
		int ival = (int) (lval % Integer.MAX_VALUE);
		return ival;
	}

	/**
	 * Converts an array of primitive types in an array of objects. An arrays of
	 * objects will just be returned.
	 *
	 * @param arrayOfPrimitiveTypes
	 *            array of byte, char, ...
	 * @return array of objects or {@code null} if the given parameter is
	 *         {@code null}
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

	public static BigDecimal convertToBigDecimal( Object value ) {
		if ( value == null ) {
			return null;
		}
		BigDecimal result = null;
		if ( value instanceof Long ) {
			result = BigDecimal.valueOf( (Long) value );
		} else if ( value instanceof Double ) {
			result = BigDecimal.valueOf( (Double) value );
		} else if ( value instanceof String ) {
			result = new BigDecimal( (String) value );
		}
		return result;
	}

	public static Boolean convertToBoolean( Object value ) {
		if ( value == null ) {
			return null;
		}
		Boolean result = null;
		if ( value instanceof Boolean ) {
			result = (Boolean) value;
		} else if ( value instanceof String ) {
			result = TextHelper.parseBoolean( (String) value );
		}
		return result;
	}

	/**
	 * Converts `name` to the enumclass `type`.
	 *
	 * If `name` is "className.valueName" like in Dart then only "valueName"
	 * will be used.
	 *
	 * @param type
	 *            the enum class
	 * @param name
	 *            string representation of an enum value
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Enum convertToEnum( Class type, String name ) {
		int index = name.indexOf( '.' );
		if ( index > 0 ) {
			name = name.substring( index + 1 );
		}
		Enum value = null;
		try {
			value = Enum.valueOf( type, name );
		} catch (IllegalArgumentException ex) {
			logger.atWarning().withCause( ex )
					.log( "Enumeration %s has no value %s", type, name );
		}
		return value;
	}

	/**
	 * Converts the given {@code value} to a Float object. Converts Boolean
	 * {@code false} to 0 and Boolean {@code true} to 1.
	 *
	 * @param value
	 *            value to be converted (can be {@code null})
	 * @return {@code null} if {@code value} is {@code null} or not a number
	 *         else returns {@code value} as a new Float object
	 */
	public static Float convertToFloat( Object value ) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof String ) {
			return TextHelper.parseFloat( (String) value );
		}
		if ( value instanceof Boolean ) {
			if ( (Boolean) value ) {
				return 1f;
			}
			return 0f;
		}
		if ( value instanceof Double ) {
			double dbl = (double) value;
			return (float) dbl;
		}
		if ( value instanceof Byte || value instanceof Short
				|| value instanceof Integer || value instanceof Long ) {
			return (Float) value;
		}
		logger.atWarning().log( "Cannot convert to Integer: %s", value );
		return null;
	}

	/**
	 * Converts {@code value} to an Integer.
	 *
	 * @param value
	 *            value to convert
	 * @return {@code null} if not convertible to an Integer
	 */
	public static Integer convertToInteger( Object value ) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Integer ) {
			return ((Integer) value);
		}
		if ( value instanceof String ) {
			return convertToInteger( (String) value );
		}
		logger.atWarning().log( "Cannot convert to Integer: %s", value );
		return null;
	}

	/**
	 * Converts {@code text} to an Integer.
	 *
	 * @param text
	 *            integer in string format
	 * @return {@code null} if not convertible to an Integer
	 */
	public static Integer convertToInteger( String text ) {
		return TextHelper.parseInteger( text );
	}

	public static Locale convertToLocale( Object object ) {
		if ( object == null ) {
			return null;
		}
		if ( object instanceof String ) {
			return stringToLocale( (String) object );
		}
		logger.atWarning().log( "Cannot convert object to Locale: %s", object );
		return null;
	}

	public static Long convertToLong( Object object ) {
		if ( object == null ) {
			return null;
		}
		if ( object instanceof Integer ) {
			return ((Long) object);
		}
		if ( object instanceof Long ) {
			return (Long) object;
		}
		if ( object instanceof String ) {
			return stringToLong( (String) object );
		}
		logger.atWarning().log( "Cannot convert object to Long: %s", object );
		return null;
	}

	public static Short convertToShort( String text ) {
		return TextHelper.parseShort( text );
	}

	/**
	 * Converts the given int value to a string.
	 *
	 * @param value
	 * @return int as string
	 */
	public static String convertToString( int value ) {
		return "" + value;
	}

	/**
	 * Converts the given BigDecimal to a string. A value of {@code null} will
	 * return an empty string.
	 *
	 * @param value
	 *            The BigDecimal or {@code null}
	 * @return big decimal as possibly empty string
	 */
	public static String convertToString( BigDecimal value ) {
		return (value == null) ? "" : value.toString();
	}

	public static String convertToString( Double value ) {
		return (value == null ? "" : value.toString());
	}

	public static String convertToString( Object object ) {
		return (object == null ? "" : object.toString());
	}

	/******************************************************************
	 * Converts {@code value} to given target {@code type} if possible.
	 *
	 * @param value
	 *            value
	 * @param type
	 *            target type
	 * @return value converted to target type or just value
	 */
	public static Object convertToType( Object value, Class<?> type ) {
		if ( value == null ) { // null has no type
			return null;
		} else if ( type.equals( value.getClass() )
				|| type.equals( Object.class ) ) {
			return value;
		} else if ( value instanceof Map ) {
			return value;
		} else if ( value instanceof AbstractCollection ) {
			return value;
		} else if ( type == boolean.class ) {
			return convert2boolean(
					TextHelper.parseBoolean( value.toString() ) );
		} else if ( type == Boolean.class ) {
			return TextHelper.parseBoolean( value.toString() );
		} else if ( type == BigDecimal.class ) {
			return convertToBigDecimal( value );
		} else if ( type == Currency.class ) {
			return Currency.getInstance( value.toString() );
		} else if ( type == float.class ) {
			return convertToFloat( value );
		} else if ( type == int.class ) {
			return convert2int( value.toString() );
		} else if ( type == long.class ) {
			return string2long( value.toString() );
		} else if ( type == LocalDate.class ) {
			return stringToDate( value.toString() );
		} else if ( type == Locale.class ) {
			return convertToLocale( value.toString() );
		} else if ( type == OffsetDateTime.class ) {
			return OffsetDateTime.parse( value.toString() );
		} else if ( type == Integer.class ) {
			return TextHelper.parseInteger( value.toString() );
		} else if ( type == Long.class ) {
			return TextHelper.parseLong( value.toString() );
		} else if ( type == String.class ) {
			return value;
		} else if ( type == UUID.class ) {
			return stringToUUID( (String) value );
		} else if ( Enum.class.isAssignableFrom( type ) ) {
			value = convertToEnum( type, (String) value );
		} else if ( type.isArray() ) {
			if ( value.getClass().isArray() ) {
				return value;
			}
			if ( value instanceof List ) {
				return ((List<?>) value).toArray();
			}
			logger.atWarning()
					.log( "Target type is array of type '" + type
							+ "'. Cannot convert \"" + value + "\" "
							+ value.getClass() );
		} else {
			logger.atWarning().log( "Missing conversion to '" + type
					+ "' for \"" + value + "\" " + value.getClass() );
		}
		return value;
	}

	public final static UUID convertToUuid( Object value ) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof UUID ) {
			return (UUID) value;
		}
		if ( value instanceof String ) {
			return convertToUuid( (String) value );
		}
		return null;
	}

	/**
	 * Converts {@code value} to a UUID.
	 *
	 * @param value
	 *            string specifying a uuid
	 * @return {@code null} if {@code value} is null or not a valid uuid
	 *         representation
	 */
	public final static UUID convertToUuid( String value ) {
		UUID uuid = null;
		if ( value != null ) {
			try {
				uuid = UUID.fromString( value );
			} catch (IllegalArgumentException ex) {
			}
		}
		return uuid;
	}

	public final static BigDecimal intToBigDecimal( Integer value ) {
		return (value == null) ? null : new BigDecimal( value );
	}

	public final static BigDecimal longToBigDecimal( Long value ) {
		return (value == null) ? null : new BigDecimal( value );
	}

	/**
	 * Converts the given long value to an array with 8 bytes. MSB first.
	 *
	 * @param value
	 *            Long value (64bit)
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
	 * @param value
	 *            Long value (64bit)
	 * @return list with 8 bytes or {@code null}
	 */
	public static List<Byte> longToByteList( Long value ) {
		if ( value == null ) {
			return null;
		}
		List<Byte> bytes = new ArrayList<>();
		for ( int i = Long.BYTES - 1; i >= 0; i-- ) {
			bytes.add( 0, Byte.valueOf( (byte) (value & 0xFF) ) );
			value >>= 8;
		}
		return bytes;
	}

	/**
	 * Converts the given long value to a string.
	 *
	 * @param value
	 *            The value which may be {@code null}
	 * @return value as a possible empty string
	 */
	public static String longToString( Long value ) {
		return (value == null ? "" : value.toString());
	}

	public final static BigDecimal stringToBigDecimal( String value ) {
		return (value == null) ? null : TextHelper.parseBigDecimal( value );
	}

	/**
	 * Converts the given text to a new instance of {@code LocalDate}
	 *
	 * @param text
	 *            date and time in ISO-8601 format
	 * @return new instance of {@code LocalDate} or {@code null}
	 */
	public static LocalDate stringToDate( String text ) {
		return TextHelper.parseDate( text );
	}

	/**
	 * Converts the given text to a new instance of {@code OffsetDateTime}
	 *
	 * @param text
	 *            date and time in ISO-8601 format
	 * @return new instance of {@code OffsetDateTime} or {@code null}
	 */
	public static OffsetDateTime stringToDateTime( String text ) {
		return TextHelper.parseDateTime( text );
	}

	public static Double stringToDouble( String text ) {
		if ( text == null || text.length() == 0 ) {
			return null;
		}
		Double value = Double.valueOf( text );
		return value;
	}

	public static Locale stringToLocale( String text ) {
		return (text == null) ? null : Locale.forLanguageTag( text );
	}

	/******************************************************************
	 * Parses the given text as a long integer. Skips leading blanks. Reads
	 * digits for the integer until the first non-digit.
	 *
	 * @param text
	 *            Text to be parsed
	 * @return long integer (64bit) or {@code null} if the text does not start
	 *         with digits
	 */
	public static Long stringToLong( String text ) {
		return TextHelper.parseLong( text );
	}

	/**
	 * Null-safe
	 *
	 * @param text
	 *            digits
	 * @return 0 if {@code text == null} else long value
	 */
	public final static long string2long( String text ) {
		Long lval = stringToLong( text );
		return (lval == null) ? 0 : lval;
	}

	public static UUID stringToUUID( String text ) {
		return (text == null) ? null : UUID.fromString( text );
	}

	/**
	 * Converts given uuid into URL safe Base64 encoded string.
	 *
	 * @param uuid
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

	/**
	 * null-safe
	 *
	 * @param value
	 *            uuid or {@code null}
	 * @return UUID.toString() or {@code null}
	 */
	public static String uuidToString( UUID value ) {
		return (value == null ? "" : value.toString());
	}

	/******************************************************************
	 * All arrays of primitive types
	 */
	private final static Class<?>[] _ARRAY_PRIMITIVE_TYPES =
			{ int[].class, float[].class, double[].class, boolean[].class,
					byte[].class, short[].class, long[].class, char[].class };
}