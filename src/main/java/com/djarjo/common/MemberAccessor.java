package com.djarjo.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Accessor using either a field or methods (getter and setter)
 *
 * @param field Field should be {@code null} if getter given
 * @param getter Method required if {@code field} is {@code null}
 * @param setter Method required when getter is given and setValue to be called
 */
public record MemberAccessor(Field field, Method getter, Method setter) {

	public MemberAccessor {
		boolean hasField = (field != null);
		boolean hasGetter = (getter != null);
		boolean hasSetter = (setter != null);
		if ( hasField && hasGetter ) {
			throw new IllegalArgumentException(
					"Provide EITHER a field OR a getter/setter combination, not both" );
		}
		if ( !hasField && !hasGetter ) {
			throw new IllegalArgumentException(
					"Must provide EITHER a field OR a getter/setter" );
		}
		if ( hasField ) {
			field.setAccessible( true );
		} else {
			getter.setAccessible( true );
			if ( hasSetter ) {
				setter.setAccessible( true );
			}
		}
	}

	public Annotation[] getAnnotations() {
		return isField() ? field.getAnnotations() : getter.getAnnotations();
	}

	public String getName() {
		return isField() ? field.getName() : getter.getName();
	}

	public Object getValue( Object bean ) {
		try {
			return isField() ? field.get( bean ) : getter.invoke( bean, (Object[]) null );
		} catch ( IllegalAccessException | InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
	}

	public boolean isAnnotationPresent(
			Class<? extends Annotation> annotation ) {
		return isField() ? field.isAnnotationPresent(
				annotation ) : getter.isAnnotationPresent( annotation );
	}

	public void setValue( Object bean, Object value ) {
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

	public boolean isField() {
		return (field != null);
	}

	@Override
	public String toString() {
		return (field != null) ? field.getName() : getter.getName();
	}
}
