package com.djarjo.jetson;

import com.djarjo.common.BaseConverter;
import com.djarjo.common.BeanHelper;
import com.djarjo.common.ReflectionHelper;
import com.djarjo.jetson.converter.JsonConverter;
import com.djarjo.text.TextHelper;
import com.google.common.flogger.FluentLogger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Accessor for a Java type annotated with {@literal @Json}.
 * <p>
 * The annotation could be on:
 * <ul><li>class → either with {@code converter}, class has method {@code toJson()} or
 *   just all getters / setters are used unless annotation contains {@literal
 *   accessType=JsonAccessType.FIELD}. In this case {@code field}, {@code getter}
 *   and {@code setter} are all {@code null}</li>
 *   <li>method → must be public</li>
 *   <li>field → could be private</li>
 *   </ul>
 *
 * @param clazz the class with annotation(s)
 * @param config attributes from {@literal @Json} annotation
 * @param field field
 * @param getter getter method
 * @param setter setter method
 */
public record JsonAccessor(Class<?> clazz, JsonConfig config, Field field, Method getter,
													 Method setter) {
	private final static FluentLogger logger = FluentLogger.forEnclosingClass();

	/** Makes each given member accessible */
	public JsonAccessor {
		if ( field != null ) field.setAccessible( true );
		if ( getter != null ) getter.setAccessible( true );
		if ( setter != null ) setter.setAccessible( true );
	}

	/**
	 * Gets a new instance of {@code JsonConverter} if the annotation specifies one.
	 *
	 * @return converter or {@code null}
	 */
	public JsonConverter<?> getConverter() {
		try {
			return hasConverter() ?
					config.converter().getDeclaredConstructor().newInstance() : null;
		} catch ( IllegalAccessException | InstantiationException |
							InvocationTargetException | NoSuchMethodException e ) {
			throw new RuntimeException( "Cannot instantiate " + config.converter().getName(),
					e );
		}
	}

	/**
	 * Gets enum accessor if annotation specifies it.
	 *
	 * @return enum accessor
	 */
	public String getEnumAccessor() {
		return config.enumAccessor();
	}

	/**
	 * Gets name used by JSON codec.
	 *
	 * @return name for encoding / decoding
	 */
	public String getJsonName() {
		return !config.name().equals( Json.defaultName ) ? config.name()
				: isField() ? field.getName()
				: isMethod() ? ReflectionHelper.makeFieldName( getter )
				: clazz.getSimpleName();
	}

	/**
	 * Gets name of class, field or getter
	 *
	 * @return member name
	 */
	public String getName() {
		return isField() ? field.getName()
				: isMethod() ? getter.getName() : clazz.getSimpleName();
	}

	/**
	 * Gets type of member
	 *
	 * @return type
	 */
	public Type getType() {
		return isField() ? field.getGenericType()
				: isMethod() ? getter.getGenericReturnType()
				: clazz;
	}

	/**
	 * Gets value from {@code bean} using this accessor.
	 * <p>
	 * If accessor specifies a converter, then the converted value will be returned.
	 * </p><p>
	 * If property is an enumeration and accessor specifies an {@code enumAccessor} then
	 * that one will be returned.
	 * </p>
	 *
	 * @param bean the bean
	 * @return value from accessor
	 */
	public Object getValue( Object bean ) {
		if ( isClass() ) {
			throw new IllegalStateException( "Cannot get value from class " + clazz.getName() );
		}
		try {
			Object value = isField() ? field.get( bean ) : getter.invoke( bean,
					(Object[]) null );
			if ( hasConverter() ) {
				JsonConverter converter = getConverter();
				return converter.encodeToJson( value );
			}
			if ( hasEnumAccessor() ) {
				return _useEnumAccessor( (Enum<?>) value );
			}
			return value;
		} catch ( IllegalAccessException | InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Checks for converter
	 *
	 * @return {@code true} if converter specified
	 */
	public boolean hasConverter() {
		return !config.converter().equals( JsonConverter.class );
	}

	/**
	 * Checks for enumeration accessor
	 *
	 * @return {@code true} if enum accessor specified
	 */
	public boolean hasEnumAccessor() {
		return !config.enumAccessor().equals( Json.defaultName );
	}

	/**
	 * Checks for class
	 *
	 * @return {@code true} if accessor contains a class
	 */
	public boolean isClass() {
		return (field == null) && (getter == null);
	}

	/**
	 * Checks for field
	 *
	 * @return {@code true} if accessor contains a field
	 */
	public boolean isField() {
		return field != null;
	}

	/**
	 * Checks for method
	 *
	 * @return {@code true} if accessor contains a method
	 */
	public boolean isMethod() {
		return (field == null) && (getter != null);
	}

	/**
	 * Checks if decoding may take place.
	 *
	 * @return {@code true} if decode is allowed
	 */
	public boolean mayDecode( Object value ) {
		if ( isClass() ) return true;
		return switch ( config.decode() ) {
			case ALWAYS -> true;
			case NEVER -> false;
			case ONLY_IF_EMPTY -> BeanHelper.isEmpty( value );
		};
	}

	/**
	 * Checks if encoding may take place.
	 *
	 * @return {@code true} if encode is allowed
	 */
	public boolean mayEncode( Object value, boolean withNulls ) {
		if ( isClass() ) return true;
		return switch ( config.encode() ) {
			case ALWAYS -> true;
			case NEVER -> false;
			case ONLY_IF_NOT_EMPTY -> withNulls || BeanHelper.isNotEmpty( value );
		};
	}

	/**
	 * Sets {@code value} in {@code bean}.
	 * <p>
	 * If accessor specifies a converter, then the converted value will be set. This
	 * requires the value to be a JSON string.
	 * </p>
	 *
	 * @param bean the bean
	 * @param value the value
	 */
	@SuppressWarnings("unchecked")
	public void setValue( Object bean, Object value ) {
		if ( isClass() ) {
			throw new IllegalStateException( "Cannot set value on class " + clazz.getName() );
		}
		if ( bean == null ) return;
		if ( hasConverter() ) {
			JsonConverter converter = getConverter();
			value = converter.decodeFromJson( (String) value );
		} else if ( ReflectionHelper.isEnum( getType() ) ) {
			value = TextHelper.findEnum( value, (Class<? extends Enum>) getType(), null,
					getEnumAccessor() );
		} else {
			value = BaseConverter.convertToType( value, getType() );
		}
		try {
			if ( isField() ) {
				field.set( bean, value );
			} else {
				setter.invoke( bean, value );
			}
		} catch ( IllegalAccessException | IllegalArgumentException |
							InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
	}

	private Object _useEnumAccessor( Enum<?> value ) {
		if ( value == null ) return null;
		String enumAccessor = config().enumAccessor();
		if ( enumAccessor.equals( Json.defaultName ) ) return value;
		Class<? extends Enum> enumClass = value.getClass();
		try {
			Field field = enumClass.getDeclaredField( enumAccessor );
			field.setAccessible( true );
			return field.get( value );
		} catch ( IllegalAccessException | NoSuchFieldException ignored ) {
			//--- No field => lookup getter method
			try {
				Method method = enumClass.getMethod( enumAccessor, (Class<?>[]) null );
				method.setAccessible( true );
				return method.invoke( value, (Object[]) null );
			} catch ( NoSuchMethodException | IllegalAccessException |
								InvocationTargetException e ) {
				logger.atFine().log( "Enum %s has no accessor: ", enumClass, enumAccessor );
			}
		}
		return value;
	}

	@Override
	public String toString() {
		return String.format( "%s %s → %s, config: %s", clazz.getSimpleName(),
				getName(), getJsonName(), config );
	}
}