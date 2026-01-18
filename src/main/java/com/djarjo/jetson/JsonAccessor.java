package com.djarjo.jetson;

import com.djarjo.common.BeanHelper;
import com.djarjo.common.ReflectionHelper;
import com.djarjo.jetson.converter.JsonConverter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

	public JsonAccessor {
		if ( field != null ) field.setAccessible( true );
		if ( getter != null ) getter.setAccessible( true );
		if ( setter != null ) setter.setAccessible( true );
	}

	public JsonConverter<?> getConverter() {
		if ( config.converter().equals( JsonConverter.class ) ) return null;
		try {
			return config.converter().getDeclaredConstructor().newInstance();
		} catch ( IllegalAccessException | InstantiationException |
							InvocationTargetException | NoSuchMethodException e ) {
			throw new RuntimeException( "Cannot instantiate " + config.converter().getName(),
					e );
		}
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

	public Object getValue( Object bean ) {
		if ( isClass() ) {
			throw new IllegalStateException( "Cannot get value from class " + clazz.getName() );
		}
		try {
			return isField() ? field.get( bean ) : getter.invoke( bean, (Object[]) null );
		} catch ( IllegalAccessException | InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
	}

	public boolean isClass() {
		return (field == null) && (getter == null);
	}

	public boolean isField() {
		return field != null;
	}

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

	public void setValue( Object bean, Object value ) {
		if ( isClass() ) {
			throw new IllegalStateException( "Cannot set value on class " + clazz.getName() );
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

	@Override
	public String toString() {
		return String.format( "%s %s → %s, config: %s", clazz.getSimpleName(),
				getName(), getJsonName(), config );
	}
}