package com.djarjo.common;

import com.google.common.flogger.FluentLogger;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

public class ReflectionHelper {
	public final static List<String> getterPrefixes = List.of( "get", "has", "is" );
	public final static List<String> setterPrefixes = List.of( "set", "with" );
	private final static FluentLogger logger = FluentLogger.forEnclosingClass();

	/**
	 *
	 * @param clazz
	 * @param name
	 * @return Field or {@code null}
	 */
	public static Field findField( Class<?> clazz, String name ) {
		Field[] fields = clazz.getDeclaredFields();
		for ( Field field : fields ) {
			if ( field.getName().equals( name ) ) {
				return field;
			}
		}
		return null;
	}

	/**
	 * Gets a new instance from the given type.
	 *
	 * @param generic any type
	 * @return new instance or {@code null}
	 */
	public static Object createInstanceFromType( Type generic ) {
		if ( !(generic instanceof Class<?> cls) ) {
			return null;
		}
		// Check for interfaces or abstract classes which cannot be instantiated directly
		if ( cls.isInterface() || Modifier.isAbstract(
				cls.getModifiers() ) ) {
			logger.atWarning()
					.log( "Cannot create instance of interface or abstract class: %s",
							cls.getName() );
			return null;
		}
		try {
			Constructor<?> constructor = cls.getDeclaredConstructor();
			constructor.setAccessible( true );
			return constructor.newInstance();
		} catch ( IllegalAccessException | InstantiationException | IllegalArgumentException |
							InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			logger.atWarning().log( "Cannot create instance of %s", cls );
			throw new RuntimeException( e );
		}
	}

	/**
	 * Finds the method for the given property name following bean standard.
	 * <p>
	 * The method must start with one of ["get", "has", "is"] followed by the given
	 * property
	 * name. The first character of the property name is converted to upper case before
	 * comparison.
	 * </p>
	 *
	 * @param beanClass The class to find the getter method in
	 * @param propertyName The name of the property
	 * @return Returns the method or <em>null</em> if not found
	 */
	public static Method findGetter( Class<?> beanClass, String propertyName ) {
		Method[] methods = beanClass.getMethods();
		return findGetter( methods, propertyName );
	}

	/**
	 * Finds the method for the given property name following bean standard.
	 * <p>
	 * The method must start with one of ["get", "has", "is"] followed by the given
	 * property
	 * name. The first character of the property name is converted to upper case before
	 * comparison.
	 * </p>
	 *
	 * @param methods Methods
	 * @param propertyName The name of the property
	 * @return Returns the method or <em>null</em> when not found
	 */
	public static Method findGetter( Method[] methods, String propertyName ) {
		assert methods != null : "No methods given";
		if ( propertyName == null || propertyName.isEmpty() ) {
			return null;
		}
		String name = "" + Character.toUpperCase( propertyName.charAt( 0 ) );
		if ( propertyName.length() > 1 ) {
			name += propertyName.substring( 1 );
		}
		String methodName = null;
		for ( Method method : methods ) {
			methodName = method.getName();
			for ( String prefix : getterPrefixes ) {
				if ( methodName.startsWith( prefix ) && name
						.equals( methodName.substring( prefix.length() ) ) ) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * Finds the setter method for the given property name following bean standard.
	 * <p>
	 * The method must start with "set" followed by the given property name. The first
	 * character of the property name is converted to upper case before comparison.
	 * </p>
	 *
	 * @param beanClass The class to find the getter method in
	 * @param propertyName The name of the property
	 * @return method or <em>null</em> if not found
	 */
	public static Method findSetter( Class<?> beanClass, String propertyName ) {
		assert beanClass != null : "No class given";
		String name = makeMethodName( "set", propertyName );
		if ( name == null ) {
			return null;
		}
		for ( Method method : beanClass.getDeclaredMethods() ) {
			if ( name.equals( method.getName() ) ) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Obtains the setter method from the given getter method.
	 *
	 * @param getter method
	 * @return setter method or <em>null</em> if there is none
	 */
	public static Method findSetter( Method getter ) {
		if ( isSetter( getter ) ) {
			return getter;
		}
		String methodName = getter.getName();
		String name = getVarNameFromMethodName( getter.getName() );
		if ( name != null ) {
			return findSetter( getter.getDeclaringClass(), name );
		}
		return null;
	}

	/**
	 * Finds the setter method for the given property name following bean standard.
	 * <p>
	 * The method must start with "set" followed by the given property name. The first
	 * character of the property name is converted to upper case before comparison.
	 * </p>
	 *
	 * @param methods Methods
	 * @param propertyName The name of the property
	 * @return method or <em>null</em> if not found
	 */
	public static Method findSetter( Method[] methods, String propertyName ) {
		assert methods != null : "No methods given";
		if ( propertyName == null || propertyName.isEmpty() ) {
			return null;
		}
		String name = makeMethodName( "set", propertyName );
		for ( Method method : methods ) {
			if ( name.equals( method.getName() ) ) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Gets generic type by {@code index}.
	 *
	 * @param pType type
	 * @param index index to generic like 0 for List
	 * @return generic type
	 */
	public static Type getActualTypeArgument( ParameterizedType pType, int index ) {
		Type[] actualTypeArguments = pType.getActualTypeArguments();
		if ( 0 <= index && index < actualTypeArguments.length ) {
			String typeArgument = actualTypeArguments[index].getTypeName();
			try {
				return Thread.currentThread()
						.getContextClassLoader()
						.loadClass( typeArgument );
			} catch ( ClassNotFoundException | IllegalArgumentException |
								SecurityException e ) {
				logger.atWarning()
						.withCause( e )
						.log( "Cannot obtain generic type %d from '%s'", index, typeArgument );
			}
		}
		return null;
	}

	public static Class<?> getMemberType( Member member ) {
		if ( member instanceof Field field ) {
			return field.getType();
		}
		if ( member instanceof Method method ) {
			return method.getReturnType();
		}
		return member.getClass();
	}


	public static Object getValueByIndex( Object bean, int index ) {
		if ( bean == null ) return null;
		Class<?> beanType = bean.getClass();

		if ( List.class.isAssignableFrom( beanType ) ) {
			List<?> list = (List<?>) bean;
			return ((0 <= index) && (index < list.size())) ? list.get( index ) : null;
		}
		if ( beanType.isArray() ) {
			int length = Array.getLength( bean );
			return ((0 <= index) && (index < length)) ? Array.get( bean, index ) : null;
		}
		return null;
	}

	public static Object getValueFromMember( Object bean,
			Member member ) throws IllegalAccessException, InvocationTargetException {
		if ( member instanceof Field field ) {
			return field.get( bean );
		}
		//--- Use getter
		Method getter = obtainGetterFromSetter( bean.getClass(), (Method) member );
		if ( getter == null ) {
			throw new RuntimeException(
					String.format( "No getter in %s from %s", bean, member ) );
		}
		return getter.invoke( bean, (Object[]) null );
	}

	/**
	 * Extracts the name of the variable from the name of the method. The method name must
	 * start with any one of [{@code get}, {@code has}, {@code is}, {@code set},
	 * {@code with}] followed by the name of the variable starting with an uppercase char.
	 *
	 * @param methodName name of the method
	 * @return name of the variable or {@code null}
	 */
	public static String getVarNameFromMethodName( String methodName ) {
		String[] prefixes = {"get", "has", "is", "set", "with"};
		for ( String prefix : prefixes ) {
			if ( methodName.startsWith( prefix ) ) {
				int len = prefix.length();
				return Character.toLowerCase( methodName.charAt( len ) )
						+ methodName.substring( len + 1 );
			}
		}
		return null;
	}

	/**
	 * Instantiates property.
	 * <p>
	 * Attention: this method creates a new instance for the member. It should only be
	 * called if the current value is {@code null}!
	 * </p>
	 * <p>
	 * If {@code member} is of type {@code list} then it becomes a new {@code ArrayList}.
	 * Its generic will be instantiated, added to the list and returned.
	 * </p><p>
	 * If {@code member} is of type {@code aray} then it becomes a new array of size 1. Its
	 * generic will be instantiated, added to the array and returned.
	 * </p>
	 *
	 * @param bean contains member
	 * @param member setter method or field
	 * @return new instance
	 */
	public static Object instantiateProperty( Object bean, Member member ) {
		Class<?> memberType = getMemberType( member );

		try {
			//--- Special handling for lists and arrays
			if ( (List.class.isAssignableFrom( memberType )) || memberType.isArray() ) {
				return _instantiateListProperty( bean, member, memberType );
			}
			if ( memberType.isInterface() || Modifier.isAbstract(
					memberType.getModifiers() ) ) {
				throw new RuntimeException(
						"Cannot auto-instantiate interface or abstract class: " + memberType.getName() );
			}

			//--- Standard Pojo
			return createInstanceFromType( memberType );
		} catch ( IllegalAccessException | InvocationTargetException e ) {
			logger.atFinest().log( "Cannot access %s in bean %s", member, bean );
			throw new RuntimeException( e );
		}
	}

	public static Object instantiateGeneric( Member member ) {
		ParameterizedType generic;
		if ( member instanceof Field field ) {
			generic = (ParameterizedType) field.getGenericType();
		} else if ( member instanceof Method method ) {
			generic = (ParameterizedType) method.getGenericReturnType();
		} else {
			String msg = "Member must be field or method but is " + member;
			logger.atFinest().log( msg );
			throw new RuntimeException( msg );
		}
		Type valueType = ReflectionHelper.getActualTypeArgument( generic, 0 );
		return ReflectionHelper.createInstanceFromType( valueType );
	}

	private static Object _instantiateListProperty( Object bean, Member member,
			Class<?> memberType ) throws IllegalAccessException, InvocationTargetException {
		Object newInstance = instantiateGeneric( member );

		//--- Create instance of list or array and add new instance
		Object list = null;
		if ( memberType.isArray() ) {
			Class<?> componentType = memberType.getComponentType();
			list = Array.newInstance( componentType, 1 );
			Array.set( list, 0, newInstance );
		} else {
			List<Object> l = new ArrayList<>();
			l.add( newInstance );
			list = l;
		}
		setValue( bean, member, list );
		return newInstance;
	}

	public static boolean isSetter( Method method ) {
		if ( method == null ) return false;
		String methodName = method.getName();
		for ( String prefix : setterPrefixes ) {
			if ( methodName.startsWith( prefix ) && method.getParameterCount() == 1 ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a method name. The returned method name starts with the optional prefix and
	 * is followed by the propertyName whose first character is converted to uppercase.
	 *
	 * @param prefix The prefix for the method like "get", "has", "is" or "set"
	 * @param propertyName The name of a property
	 * @return method name or <em>null</em>
	 */
	public static String makeMethodName( String prefix, String propertyName ) {
		if ( propertyName == null || propertyName.isEmpty() ) {
			return null;
		}
		if ( prefix == null ) {
			prefix = "";
		}
		String name =
				prefix + Character.toUpperCase( propertyName.charAt( 0 ) );
		if ( propertyName.length() > 1 ) {
			name += propertyName.substring( 1 );
		}
		return name;
	}

	public static Method obtainGetterFromSetter( Class<?> clazz, Method setter ) {
		String baseName = getVarNameFromMethodName( setter.getName() );
		return findGetter( clazz, baseName );
	}

	/**
	 * Sets {@code member} in {@code bean} to {@code value}.
	 *
	 * @param bean bean
	 * @param member field or method
	 * @param value new value
	 * @throws IllegalAccessException e
	 */
	public static void setValue( Object bean, Member member,
			Object value ) throws IllegalAccessException {
		if ( member instanceof Field field ) {
			Class<?> type = field.getType();
			Object convertedValue = BaseConverter.convertToType( value, type );
			field.setAccessible( true );
			field.set( bean, convertedValue );
			return;
		}
		if ( member instanceof Method method ) {
			Method setter = findSetter( method );
			if ( setter == null ) {
				throw new RuntimeException(
						String.format( "No setter found in bean %s from %s", bean, method ) );
			}
			Class<?>[] types = setter.getParameterTypes();
			if ( types.length != 1 ) {
				String msg = String.format(
						"Method must be a standard setter with one parameter but has %d: %s",
						types.length, method );
				throw new RuntimeException( msg );
			}
			Object convertedValue = BaseConverter.convertToType( value, types[0] );
			setter.setAccessible( true );
			try {
				setter.invoke( bean, convertedValue );
			} catch ( InvocationTargetException e ) {
				throw new IllegalAccessException(
						String.format( "Cannot set '%s' in bean '%s' with method '%s'",
								convertedValue, bean, setter ) );
			}
		} else {
			throw new RuntimeException( "Member must be field or method but is " + member );
		}
	}

	@SuppressWarnings("unchecked")
	public static void setValueAt( Object listOrArray, int index, Object value ) {
		Class<?> type = listOrArray.getClass();
		if ( type.isArray() ) {
			Array.set( listOrArray, index, value );
		} else {
			List<Object> l = (List<Object>) listOrArray;
			if ( index < l.size() ) {
				l.set( index, value );
			} else {
				l.add( value );
			}
		}
	}
}
