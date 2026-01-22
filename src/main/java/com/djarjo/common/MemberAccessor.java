package com.djarjo.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

/**
 * Accessor using either a field or methods {@code getter} and {@code setter}
 *
 * @param field should be {@code null} if getter given
 * @param getter required if {@code field} is {@code null}
 * @param setter required when {@code getter} is given and {@code setValue} to be called
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
		if ( !hasField && !hasGetter && !hasSetter ) {
			throw new IllegalArgumentException(
					"Must provide at least a field, a getter or a setter" );
		}
		if ( hasField ) {
			field.setAccessible( true );
		}
		if ( hasGetter ) {
			getter.setAccessible( true );
		}
		if ( hasSetter ) {
			setter.setAccessible( true );
		}
	}

	public Annotation[] getAnnotations() {
		return isField() ? field.getAnnotations() : getter.getAnnotations();
	}

	public Member getMember() {
		return isField() ? field : getter;
	}

	public String getName() {
		return isField() ? field.getName() : getter.getName();
	}

	public Type getType() {
		return isField() ? field.getGenericType()
				: (getter != null) ? getter.getGenericReturnType()
				: (setter != null) ? setter.getGenericParameterTypes()[0]
				: Object.class;
	}

	public Object getValue( Object bean ) {
		try {
			return isField() ? field.get( bean )
					: (getter != null) ? getter.invoke( bean, (Object[]) null )
					: null;
		} catch ( IllegalAccessException | InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
	}

	public boolean isAnnotationPresent(
			Class<? extends Annotation> annotation ) {
		return isField() ? field.isAnnotationPresent(
				annotation ) : getter.isAnnotationPresent( annotation );
	}

	public boolean isField() {
		return (field != null);
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

	@Override
	public String toString() {
		return (field != null) ? field.getName() : getter.getName();
	}
}
