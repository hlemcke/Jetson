package com.djarjo.common;

import com.djarjo.text.TextHelper;
import com.google.common.flogger.FluentLogger;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;

/**
 * This class solely consists of static methods which convert a value from one type to
 * another one. If the input type is of type string then the parser methods from
 * {@link com.djarjo.text.TextHelper TextHelper} will be used.
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
     * Converts array to list
     *
     * @param array a
     * @return list
     */
    public static List<?> arrayToList(Object[] array) {
        if (array == null) {
            return null;
        }
        List<Object> list = new ArrayList<>(array.length);
        Collections.addAll(list, array);
        return list;
    }

    /**
     * Converts 8 bytes from {@code bytes} starting at {@code offset} into a long value.
     *
     * @param bytes  byte array
     * @param offset index to highest byte
     * @return long value or {@code null}
     */
    public static Long bytesToLong(byte[] bytes, int offset) {
        if (bytes == null) return null;
        long result = 0L;
        int end = (bytes.length - offset < Long.BYTES) ? bytes.length
                : offset + Long.BYTES;
        for (int i = offset; i < end; i++) {
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
    public static Object[] convertToArray(Object arrayOfPrimitiveTypes) {
        if (arrayOfPrimitiveTypes == null) {
            return null;
        }
        Class<?> valKlass = arrayOfPrimitiveTypes.getClass();
        Object[] outputArray = null;

        for (Class<?> arrKlass : _ARRAY_PRIMITIVE_TYPES) {
            if (valKlass.isAssignableFrom(arrKlass)) {
                int arrlength = Array.getLength(arrayOfPrimitiveTypes);
                outputArray = new Object[arrlength];
                for (int i = 0; i < arrlength; ++i) {
                    outputArray[i] = Array.get(arrayOfPrimitiveTypes, i);
                }
                break;
            }
        }
        if (outputArray == null) { // not a primitive type array
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
    public static Enum convertToEnum(Class type, String name) {
        String[] parts = name.split("\\.");
        name = parts[parts.length - 1];
        try {
            Method find = ReflectionHelper.findMethod(type, "find");
            if (find != null) {
                return (Enum) find.invoke(null, name);
            }
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException | IllegalAccessException |
                 InvocationTargetException ex) {
            String message = String.format("Enumeration %s has no value %s", type, name);
            logger.atWarning().withCause(ex).log(message);
            throw new RuntimeException(message, ex);
        }
    }

    /**
     * Converts {@code value} to given target {@code type} if possible.
     *
     * @param value value
     * @param type  target type
     * @return value converted to target type
     */
    public static Object convertToType(Object value, Type type) {
        //--- null has no type
        if (value == null) return null;

        if (type.equals(value.getClass()) || type.equals(Object.class) ||
                (type instanceof TypeVariable)) {
            return value;
        }
        if (value instanceof Map) return value;

        //--- handle target collection
        if (value instanceof Collection coll) {
            if (ReflectionHelper.isArray(type)) return coll.toArray();
            return ReflectionHelper.isSet(type) ? new HashSet<>(coll)
                    : value;
        }

        if (ReflectionHelper.isEnum(type)) {
            return convertToEnum((Class<? extends Enum>) type, (String) value);
        }

        if (ReflectionHelper.isList(type)) {
            return value;
        } else if (ReflectionHelper.isByteArray(type) && (value instanceof String)) {
            return Base64.decoder().decode((String) value);
        } else if (ReflectionHelper.isArray(type)) {
            if (value.getClass().isArray()) {
                return value;
            }
            logger.atWarning().log("Target type is array of type '%s'." +
                    " Cannot convert \"%s\" %s", type, value, value.getClass());
        }
        return _getConverters().getOrDefault(type, v -> {
            throw new IllegalArgumentException("Unsupported type:" + type);
        }).apply(value);
    }

    /**
     * Checks if {@code type} can be converted (is a <em>basic value</em>) which can be
     * converted with {@link #convertToType(Object, Type)}
     *
     * @param type type
     * @return true or false
     */
    public static boolean isConvertible(Type type) {
        if (type == null) return true;
        Class<?> clazz = ReflectionHelper.getRawClass(type);
        return clazz != null && (_getConverters().containsKey(
                clazz) || ReflectionHelper.isEnum(type));
    }

    /**
     * Converts a List into a dynamically-typed array. The array type is derived from the
     * class of the first element in the list.
     *
     * @param list The source list to convert. Can contain any type of element.
     * @return An array (T[]) of type T, or null if the list is null or empty.
     */
    public static Object listToArray(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        Class<?> componentType = list.getFirst().getClass();
        int size = list.size();
        Object array = Array.newInstance(componentType, size);
        for (int i = 0; i < size; i++) {
            Array.set(array, i, list.get(i));
        }
        return array;
    }

    /**
     * Converts the given long value to an array with 8 bytes. MSB first.
     *
     * @param value Long value (64bit)
     * @return 8 bytes or {@code null}
     */
    public static byte[] longToByteArray(Long value) {
        if (value == null) {
            return null;
        }
        byte[] result = new byte[Long.BYTES];
        for (int i = Long.BYTES - 1; i >= 0; i--) {
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
    public static List<Byte> longToByteList(Long value) {
        if (value == null) {
            return null;
        }
        List<Byte> bytes = new ArrayList<>();
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            bytes.addFirst((byte) (value & 0xFF));
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
    public static Boolean toBoolean(Object value) {
        return switch (value) {
            case Boolean b -> b;
            case String s -> TextHelper.parseBoolean(s);
            case Number n -> n.doubleValue() != 0.0;
            default -> {
                logger.atWarning().log("Cannot convert to Boolean: '%s'", value);
                yield false;
            }
        };
    }

    /**
     * Converts {@code value} to a {@code BigDecimal}
     *
     * @param value Byte, Double, Float, Integer, Long, Short or String
     * @return new instance of {@code BigDecimal} or {@code null} if not convertible
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal big) return big;
        if (value instanceof String) {
            return TextHelper.parseBigDecimal((String) value);
        }
        try {
            return switch (value) {
                case Byte b -> BigDecimal.valueOf(b);
                case Double v -> BigDecimal.valueOf(v);
                case Float v -> BigDecimal.valueOf(v);
                case Integer i -> BigDecimal.valueOf(i);
                case Long l -> BigDecimal.valueOf(l);
                case Short i -> BigDecimal.valueOf(i);
                default -> null;
            };
        } catch (NumberFormatException e) {
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
    public static Byte toByte(Object value) {
        return switch (value) {
            case null -> null;
            case Byte b -> b;
            case Number n -> n.byteValue();
            case String s -> TextHelper.parseByte(s);
            default -> {
                logger.atWarning().log("Cannot convert to Byte: '%s'", value);
                yield null;
            }
        };
    }

    /**
     * Converts {@code value} to old Date.
     *
     * @param value to be converted
     * @return Date or {@code null}
     */
    public static Date toDate(Object value) {
        if (value == null) return null;
        if (value instanceof Date date) return date;
        if (value instanceof Instant i) return Date.from(i);
        if (value instanceof LocalDate ld) {
            return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof LocalDateTime ldt) {
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof OffsetDateTime odt) return Date.from(odt.toInstant());
        if (value instanceof ZonedDateTime zdt) return Date.from(zdt.toInstant());
        if (value instanceof String s) return toDate(TextHelper.parseDate(s));
        logger.atWarning().log("Cannot convert to Date: '%s'", value);
        return null;
    }

    /**
     * Converts {@code value} to a Double.
     *
     * @param value to be converted
     * @return {@code null} if not convertible to a Double
     */
    public static Double toDouble(Object value) {
        return switch (value) {
            case null -> null;
            case Double d -> d;
            case Number n -> n.doubleValue();
            case String s -> TextHelper.parseDouble(s);
            default -> {
                logger.atWarning().log("Cannot convert to Double: '%s'", value);
                yield null;
            }
        };
    }

    /**
     * Converts value to Duration
     *
     * @param value Duration or String or null
     * @return Duration or null
     */
    public static Duration toDuration(Object value) {
        if (value == null) return null;
        if (value instanceof Duration d) return d;
        if (value instanceof String s) return Duration.parse(s);
        return null;
    }

    /**
     * Converts {@code value} to a Float.
     *
     * @param value to be converted
     * @return {@code null} if not convertible to a Float
     */
    public static Float toFloat(Object value) {
        if (value == null) return null;
        if (value instanceof Float f) return f;
        if (value instanceof Number n) return n.floatValue();
        if (value instanceof String s) return TextHelper.parseFloat(s);
        logger.atWarning().log("Cannot convert to Float: '%s'", value);
        return null;
    }

    /**
     * Converts {@code value} to an {@code Instant}.
     *
     * @param value to be converted
     * @return new instance of {@code Instant} or {@code null} if not convertible
     */
    public static Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant i) return i;
        if (value instanceof String s) return Instant.parse(s);
        logger.atWarning().log("Cannot convert to Instant: '%s'", value);
        return null;
    }

    /**
     * Converts {@code value} to an {@code Integer}.
     *
     * @param value to be converted
     * @return new instance of {@code Integer} or {@code null} if not convertible
     */
    public static Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) return TextHelper.parseInteger(s);
        logger.atWarning().log("Cannot convert to Integer: '%s'", value);
        return null;
    }

    /**
     * Converts {@code value} to {@code LocalDate}
     *
     * @param value to be converted
     * @return new instance of {@code LocalDate} or {@code null} if not convertible
     */
    public static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate l) return l;
        if (value instanceof OffsetDateTime o) return o.toLocalDate();
        if (value instanceof String s) return TextHelper.parseDate(s);
        logger.atWarning().log("Cannot convert '%s' to LocalDate", value);
        return null;
    }

    /**
     * Converts {@code value} to {@code LocalTime}
     *
     * @param value to be converted
     * @return new instance of {@code LocalTime} or {@code null} if not convertible
     */
    public static LocalTime toLocalTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalTime l) return l;
        if (value instanceof OffsetDateTime o) return o.toLocalTime();
        if (value instanceof String s) return TextHelper.parseTime(s);
        logger.atWarning().log("Cannot convert '%s' to LocalTime", value);
        return null;
    }

    /**
     * Converts {@code value} to {@code Locale}
     *
     * @param value to be converted
     * @return new instance of {@code Locale} or {@code null} if not convertible
     */
    public static Locale toLocale(Object value) {
        if (value == null) return null;
        if (value instanceof Locale l) return l;
        if (value instanceof String s) return Locale.forLanguageTag(s);
        logger.atWarning().log("Cannot convert to Locale: '%s'", value);
        return null;
    }

    /**
     * Converts {@code value} to a {@code Long}.
     *
     * @param value to be converted
     * @return new instance of {@code Long} or {@code null} if not convertible
     */
    public static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return TextHelper.parseLong(s);
        logger.atWarning().log("Cannot convert to Long: '%s'", value);
        return null;
    }

    /**
     * Converts {@code value} to a {@code OffsetDateTime} object
     *
     * @param value to be converted
     * @return new instance of {@code OffsetDateTime} or {@code null} if not convertible
     */
    public static OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime o) return o;
        if (value instanceof String s) return TextHelper.parseDateTime(s);
        logger.atWarning().log("Cannot convert to OffsetDateTime: '%s'", value);
        return null;
    }

    /**
     * Converts {@code value} to a {@code Period} object
     *
     * @param value to be converted
     * @return new instance of {@code Period} or {@code null} if not convertible
     */
    public static Period toPeriod(Object value) {
        if (value == null) return null;
        if (value instanceof Period p) return p;
        if (value instanceof String s) return Period.parse(s);
        logger.atWarning().log("Cannot convert to Period: '%s'", value);
        return null;
    }

    /**
     * Converts {@code value} to a Short object
     *
     * @param value to be converted
     * @return {@code null} if not convertible to Short
     */
    public static Short toShort(Object value) {
        if (value == null) return null;
        if (value instanceof Short s) return s;
        if (value instanceof Number n) return n.shortValue();
        if (value instanceof String s) return TextHelper.parseShort(s);
        logger.atWarning().log("Cannot convert to Short: '%s'", value);
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
    public static String toString(BigDecimal value) {
        return (value == null) ? "" : value.toString();
    }

    /**
     * Converts given object to a timezone.
     *
     * @param value to convert (could be {@code null})
     * @return new instance of {@code ZoneId} or {@code null}
     */
    public static ZoneId toZoneId(Object value) {
        return switch (value) {
            case Duration d -> toZoneIdFromMinutes((int) d.toMinutes());
            case String s -> TextHelper.parseTimeZone(s);
            case OffsetDateTime o -> o.getOffset();
            case ZoneId tz -> tz;
            case null, default -> null;
        };
    }

    /**
     * Converts {@code minutes} to {@code ZoneId}
     *
     * @param minutes well ... minutes
     * @return new instance of {@code ZoneId}
     */
    public static ZoneId toZoneIdFromMinutes(int minutes) {
        return ZoneOffset.ofTotalSeconds(minutes * 60);
    }

    /**
     * Converts {@code value} to a UUID.
     *
     * @param value null or an uuid in string format or a Base-64 encoded uuid
     * @return {@code null} if not convertible to UUID
     */
    public static UUID toUuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID u) return u;
        if (value instanceof String s) return TextHelper.parseUuid(s);
        logger.atWarning().log("Cannot convert to UUID: '%s'", value);
        return null;
    }

    /**
     * Converts given uuid into URL safe Base64 encoded string.
     *
     * @param uuid value to be encoded to Base64
     * @return Base64 encoded string (URL safe)
     */
    public static String uuidToBase64(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        byte[] bitesHigh =
                BaseConverter.longToByteArray(uuid.getMostSignificantBits());
        byte[] bitesLow =
                BaseConverter.longToByteArray(uuid.getLeastSignificantBits());
        byte[] bites = new byte[Long.BYTES * 2];
        System.arraycopy(bitesHigh, 0, bites, 0, Long.BYTES);
        System.arraycopy(bitesLow, 0, bites, Long.BYTES, Long.BYTES);
        return Base64.encoder().encode(bites);
    }

    /**
     * Converts {@code value} to an {@code ZonedDateTime}.
     *
     * @param value to be converted
     * @return new instance of {@code ZonedDateTime} or {@code null} if not convertible
     */
    public static ZonedDateTime toZonedDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof ZonedDateTime z) return z;
        if (value instanceof String s) return ZonedDateTime.parse(s);
        logger.atWarning().log("Cannot convert to ZonedDateTime: '%s'", value);
        return null;
    }

    private static Map<Class<?>, Function<Object, Object>> _getConverters() {
        if (_converters.isEmpty()) {
            _converters.put(BigDecimal.class, BaseConverter::toBigDecimal);
            _converters.put(boolean.class, BaseConverter::toBoolean);
            _converters.put(Boolean.class, BaseConverter::toBoolean);
            _converters.put(byte.class, BaseConverter::toByte);
            _converters.put(Byte.class, BaseConverter::toByte);
            _converters.put(Currency.class, v -> Currency.getInstance(v.toString()));
            _converters.put(Date.class, BaseConverter::toDate);
            _converters.put(double.class, BaseConverter::toDouble);
            _converters.put(Double.class, BaseConverter::toDouble);
            _converters.put(Duration.class, BaseConverter::toDuration);
            _converters.put(float.class, BaseConverter::toFloat);
            _converters.put(Float.class, BaseConverter::toFloat);
            _converters.put(int.class, BaseConverter::toInteger);
            _converters.put(Integer.class, BaseConverter::toInteger);
            _converters.put(Instant.class, BaseConverter::toInstant);
            _converters.put(long.class, BaseConverter::toLong);
            _converters.put(Long.class, BaseConverter::toLong);
            _converters.put(LocalDate.class, BaseConverter::toLocalDate);
            _converters.put(LocalTime.class, BaseConverter::toLocalTime);
            _converters.put(Locale.class, v -> toLocale(v.toString()));
            _converters.put(OffsetDateTime.class, BaseConverter::toOffsetDateTime);
            _converters.put(Period.class, BaseConverter::toPeriod);
            _converters.put(String.class, v -> v);
            _converters.put(TimeZone.class, BaseConverter::toZoneId);
            _converters.put(UUID.class, BaseConverter::toUuid);
            _converters.put(ZonedDateTime.class, BaseConverter::toZonedDateTime);
            _converters.put(ZoneId.class, BaseConverter::toZonedDateTime);
        }
        return _converters;
    }
}