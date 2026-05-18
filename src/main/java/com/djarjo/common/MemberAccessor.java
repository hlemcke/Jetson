package com.djarjo.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

/**
 * Accessor using a field or methods {@code getter} and/ or {@code setter}
 *
 * @param field name of field. Required if there is no method
 * @param getter required if {@code field} is {@code null}
 * @param setter required when {@code getter} is given and {@code setValue} to be called
 */
public record MemberAccessor(Field field, Method getter, Method setter) {

  public MemberAccessor {
    boolean hasField = (field != null);
    boolean hasGetter = (getter != null);
    boolean hasSetter = (setter != null);
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
    return hasField() ? field.getAnnotations() : getter.getAnnotations();
  }

  public Member getMember() {
    return hasField() ? field : getter;
  }

  public String getName() {
    return hasField() ? field.getName() : getter.getName();
  }

  public Type getType() {
    return hasField() ? field.getGenericType()
        : (getter != null) ? getter.getGenericReturnType()
          : (setter != null) ? setter.getGenericParameterTypes()[0]
            : Object.class;
  }

  public Object getValue( Object bean ) {
    try {
      return hasField() ? field.get( bean )
          : (getter != null) ? getter.invoke( bean, (Object[]) null )
            : null;
    } catch ( IllegalAccessException | InvocationTargetException e ) {
      throw new RuntimeException( e );
    }
  }

  public boolean hasField() {
    return (field != null);
  }

  public boolean hasGetter() {
    return (getter != null);
  }

  public boolean hasSetter() {
    return (setter != null);
  }

  public boolean isAnnotationPresent(
      Class<? extends Annotation> annotation ) {
    return hasField() ? field.isAnnotationPresent(
        annotation ) : getter.isAnnotationPresent( annotation );
  }

  public void setValue( Object bean, Object value ) {
    try {
      if ( hasField() ) {
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
