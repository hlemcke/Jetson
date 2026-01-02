package com.djarjo.common;

import com.google.common.flogger.FluentLogger;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Provides low level methods to access beans.
 * <p>
 * Whenever a {@code list} is documented then the code also works for {@code arrays}.
 * </p>
 */
public class ReflectionHelper {
	public final static List<String> getterPrefixes = List.of( "get", "has", "is" );
	public final static List<String> setterPrefixes = List.of( "add", "set", "with" );
	private final static FluentLogger logger = FluentLogger.forEnclosingClass();

	/**
	 * Gets a new instance from the given type.
	 *
	 * @param type any type
	 * @return new instance or {@code null}
	 */
	public static Object createInstanceFromType( Type type ) {
		if ( !(type instanceof Class<?> cls) ) {
			return null;
		}
		if ( isList( getRawClass( type ) ) ) {
			return new ArrayList<>();
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

	public static Method findMethod( Class<?> clazz, String name ) {
		for ( Method method : clazz.getMethods() ) {
			if ( method.getName().equals( name ) ) {
				return method;
			}
		}
		return null;
	}

	public static Method findMethod( Method[] methods, String propertyName,
			List<String> prefixes ) {
		assert methods != null : "No methods given";
		assert !prefixes.isEmpty() : "No prefixes given";
		if ( propertyName == null || propertyName.isEmpty() ) {
			return null;
		}
		String name = "" + Character.toUpperCase( propertyName.charAt( 0 ) );
		if ( propertyName.length() > 1 ) {
			name += propertyName.substring( 1 );
		}
		String methodName;
		for ( Method method : methods ) {
			methodName = method.getName();
			for ( String prefix : prefixes ) {
				if ( methodName.startsWith( prefix ) && name
						.equals( methodName.substring( prefix.length() ) ) ) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * Finds field by given name in class.
	 *
	 * @param clazz class
	 * @param name field name
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
	 * Finds the getter method for the given property name following bean standard.
	 * <p>
	 * The method must start with one of ["get", "has", "is"] followed by the given
	 * property
	 * name and must not have any parameters.
	 * </p>
	 *
	 * @param beanClass class to find the getter method in
	 * @param propertyName name of the property
	 * @return getter method or {@code null}
	 */
	public static Method findGetter( Class<?> beanClass, String propertyName ) {
		Method[] methods = beanClass.getMethods();
		return findMethod( methods, propertyName, getterPrefixes );
	}

	/**
	 * Finds the setter method for the given property name following bean standard.
	 * <p>
	 * The method must start with one of ["get", "has", "is"] followed by
	 * {@code propertyName} and must have exactly one parameter.
	 * </p>
	 *
	 * @param beanClass class to find the setter method in
	 * @param propertyName The name of the property
	 * @return setter method or {@code null}
	 */

	public static Method findSetter( Class<?> beanClass, String propertyName ) {
		Method[] methods = beanClass.getMethods();
		//--- Check if method without setter prefix exists
		for ( Method method : methods ) {
			if ( method.getName().equals( propertyName ) ) {
				return method;
			}
		}
		//--- No method with given name => check all standard setter prefixes
		return findMethod( methods, propertyName, setterPrefixes );
	}

	/**
	 * Gets actual type.
	 *
	 * @param genericType type
	 * @param index index into list of generic types (normally 0)
	 * @return type
	 */
	public static Type getActualTypeArgument( Type genericType, int index ) {
		if ( genericType instanceof ParameterizedType paramType ) {
			Type[] actualTypes = paramType.getActualTypeArguments();
			return ((index < 0) || (index >= actualTypes.length)) ? null : actualTypes[index];
		}
		if ( genericType instanceof Class<?> cls && cls.isArray() ) {
			return cls.getComponentType();
		}
		return null;
	}

	public static Class<?> getMemberType( Member member ) {
		if ( member instanceof Field field ) {
			return field.getType();
		}
		if ( member instanceof Method method ) {
			return isSetter( method ) ? method.getParameterTypes()[0] : method.getReturnType();
		}
		return member.getClass();
	}


	public static Class<?>[] getParameterTypes( Member member ) {
		return (member instanceof Field field) ?
				new Class<?>[]{field.getType(), field.getGenericType().getClass()} :
				((Method) member).getParameterTypes();
	}

	public static Class<?> getRawClass( Type type ) {
		if ( type instanceof Class ) {
			return (Class<?>) type;
		} else if ( type instanceof ParameterizedType ) {
			// Extracts the raw type from a generic type (e.g., List<String> -> List)
			return (Class<?>) ((ParameterizedType) type).getRawType();
		}
		// TODO add checks for GenericArrayType, WildcardType, etc., for completeness
		return null;
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
		Method getter = obtainGetterFromMethod( bean.getClass(), (Method) member );
		if ( getter == null ) {
			throw new RuntimeException(
					String.format( "No getter in %s from %s", bean, member ) );
		}
		return getter.invoke( bean, (Object[]) null );
	}

	/**
	 * Extracts the name of the variable from the name of the method. The method name must
	 * start with any one of [{@code get}, {@code has}, {@code is}, {@code set},
	 * {@code with}] followed by the name of the variable.
	 *
	 * @param methodName name of the method
	 * @return name of the variable or {@code null}
	 */
	public static String getVarNameFromMethodName( String methodName ) {
		List<String> prefixes = new ArrayList<>( getterPrefixes );
		prefixes.addAll( setterPrefixes );
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
	 * Instantiates the generic class of an array or a list and writes it into
	 *
	 * @param bean pojo needed for array resize
	 * @param member in pojo needed to set resized array
	 * @param arrayOrList array or list object
	 * @param index -1 to append
	 * @return pojo created and set into array or list
	 * @throws IllegalAccessException e
	 */
	public static Object instantiateGeneric( Object bean,
			Member member, Object arrayOrList, int index ) throws IllegalAccessException {
		Type genericType;
		if ( member instanceof Field field ) {
			genericType = field.getGenericType();
		} else if ( member instanceof Method method ) {
			genericType = isGetter( method ) ? method.getGenericReturnType() :
					method.getGenericParameterTypes()[0];
		} else {
			String msg = "Member must be field or method but is " + member;
			logger.atFinest().log( msg );
			throw new RuntimeException( msg );
		}
		Type actualType = ReflectionHelper.getActualTypeArgument( genericType, 0 );
		Object pojo = ReflectionHelper.createInstanceFromType( actualType );

		//--- Append to end of list or array
		int currentSize = _getSize( arrayOrList );
		if ( (index < 0) || (currentSize <= index) ) {
			_appendValueToList( bean, member, arrayOrList, pojo );
		} else {
			_writeValueToList( arrayOrList, index, pojo );
		}
		return pojo;
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
			Object pojo = createInstanceFromType( memberType );
			_writeValueToMember( bean, member, pojo );
			return pojo;
		} catch ( IllegalAccessException e ) {
			logger.atFinest().log( "Cannot access %s in bean %s", member, bean );
			throw new RuntimeException( e );
		}
	}

	public static boolean isArrayOrList( Class<?> clazz ) {
		return (clazz != null) && (isList( clazz ) || clazz.isArray());
	}

	/**
	 * Checks if member is a byte array
	 *
	 * @param member field or getter
	 * @return {@code true} if a byte array
	 */
	public static boolean isByteArray( Member member ) {
		return (member instanceof Field field) ? field.getType() == byte[].class
				: member instanceof Method method && method.getReturnType() == byte[].class;
	}

	/**
	 * Checks if member is an enumeration.
	 *
	 * @param member field or getter
	 * @return {@code true} if an enumeration
	 */
	public static boolean isEnum( Member member ) {
		if ( member instanceof Field field ) {
			return field.getType().isEnum();
		}
		if ( member instanceof Method method ) {
			Class<?> returnType = method.getReturnType();
			return returnType.isEnum() || Enum.class.isAssignableFrom( returnType );
		}
		return false;
	}

	public static boolean isList( Class<?> clazz ) {
		return clazz != null && List.class.isAssignableFrom( clazz );
	}

	/**
	 * Checks if given method is a getter.
	 * <p>
	 * A getter:
	 * <ul><li>has no parameters</li>
	 * <li>must not return void</li>
	 * <li>must start with {@code get}, {@code has} or {@code is}</li></ul>
	 *
	 * @param method method
	 * @return {@code true} if method is a getter
	 */
	public static boolean isGetter( Method method ) {
		if ( (method == null)
				|| (method.getParameterCount() != 0)
				|| (method.getReturnType() == void.class)
				|| "getClass".equals( method.getName() ) ) {
			return false;
		}
		String methodName = method.getName();
		for ( String prefix : getterPrefixes ) {
			if ( methodName.startsWith( prefix ) ) {
				if ( Character.isUpperCase( methodName.charAt( prefix.length() ) ) ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks if given method is a setter.
	 * <p>
	 * A setter:
	 * <ul><li>has exactly one parameter</li>
	 * <li>must start with {@code add}, {@code set} or {@code with}</li></ul>
	 *
	 * @param method method
	 * @return {@code true} if method is a setter
	 */
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
	 * @param prefix The prefix for the method like "get", "has", "is", "set" or "with".
	 * Best to use entry from {@link #getterPrefixes} or {@link #setterPrefixes}
	 * @param propertyName The name of a property
	 * @return method name or {@code null} if {@code propertyName} is empty
	 */
	public static String makeMethodName( String prefix, String propertyName ) {
		if ( propertyName == null || propertyName.isEmpty() ) {
			return null;
		}
		prefix = (prefix == null) ? "" : prefix;
		String name = prefix + Character.toUpperCase( propertyName.charAt( 0 ) );
		if ( propertyName.length() > 1 ) {
			name += propertyName.substring( 1 );
		}
		return name;
	}

	public static Method obtainGetterFromMethod( Class<?> clazz, Method method ) {
		if ( isGetter( method ) ) {
			return method;
		}
		String name = getVarNameFromMethodName( method.getName() );
		return (name == null) ? null : findGetter( clazz, name );
	}

	/**
	 * Obtains the setter from given method.
	 *
	 * @param method method
	 * @return setter method or {@code null}
	 */
	public static Method obtainSetterFromMethod( Class<?> clazz, Method method ) {
		if ( isSetter( method ) ) {
			return method;
		}
		String name = getVarNameFromMethodName( method.getName() );
		return (name == null) ? null : findSetter( clazz, name );
	}

	/**
	 * Sets {@code member} in {@code bean} to {@code value}.
	 * <p>
	 * If {@code member} is a list and {@code value} is not then the list will be gotten
	 * (or
	 * created) and {@code value} will be added to it.
	 * </p>
	 *
	 * @param bean bean
	 * @param member field or method
	 * @param value new value
	 * @throws IllegalAccessException e
	 */
	public static void setValue( Object bean, Member member, int index,
			Object value ) throws IllegalAccessException {

		//--- Handle Field
		if ( member instanceof Field field ) {
			_setValueUsingField( bean, field, index, value );
			return;
		}

		//--- Handle setter method
		if ( member instanceof Method method ) {
			Method setter = obtainSetterFromMethod( bean.getClass(), method );
			if ( setter == null ) {
				throw new RuntimeException(
						String.format( "No setter found in bean %s from %s", bean, method ) );
			}
			_setValueUsingSetter( bean, setter, index, value );
		} else {
			throw new RuntimeException( "Member must be field or method but is " + member );
		}
	}

	private static int _getSize( Object value ) {
		return (value == null) ? 0 : isList( value.getClass() )
				? ((List<?>) value).size() : (value.getClass().isArray())
				? Array.getLength( value ) : 0;
	}

	/**
	 * Instantiates array (with length 1)  or list and writes it into the bean.
	 *
	 * @param bean bean
	 * @param member field or setter
	 * @param memberType array or list
	 * @return array or list
	 * @throws IllegalAccessException e
	 */
	private static Object _instantiateListProperty( Object bean, Member member,
			Class<?> memberType ) throws IllegalAccessException {
		if ( memberType.isArray() ) {
			Class<?> componentType = memberType.getComponentType();
			Object[] array = (Object[]) Array.newInstance( componentType, 1 );
			_writeValueToMember( bean, member, array );
			return array;
		}
		List<Object> l = new ArrayList<>();
		_writeValueToMember( bean, member, l );
		return l;
	}

	/**
	 * Sets {@code value} into {@code member} in {@code bean}.
	 * <p>{@code member} type must be list or array</p>
	 *
	 * @param bean object
	 * @param member field or setter
	 * @param index -1 to append or index 0 .. n
	 * @param value value to set at index
	 */
	private static void _setValueAt( Object bean, Member member, int index,
			Object value ) throws IllegalAccessException {
		assert value != null;
		Class<?>[] types = getParameterTypes( member );

		//--- If value itself is a list then directly write into member
		if ( isArrayOrList( value.getClass() ) ) {
			_writeValueToMember( bean, member, value );
			return;
		}

		//--- Handle case with index
		try {
			Object currentValue = getValueFromMember( bean, member );
			if ( currentValue == null ) {
				currentValue = createInstanceFromType( types[0] );
				_writeValueToMember( bean, member, currentValue );
			}
			int currentSize = _getSize( currentValue );

			//--- Append to end of list or array
			if ( (index < 0) || (currentSize <= index) ) {
				_appendValueToList( bean, member, currentValue, value );
				return;
			}

			_writeValueToList( currentValue, index, value );
		} catch ( InvocationTargetException e ) {
			throw new IllegalAccessException( e.getMessage() );
		}
	}

	private static void _setValueUsingField( Object bean, Field field, int index,
			Object value ) throws IllegalAccessException {
		if ( value == null ) {
			field.setAccessible( true );
			field.set( bean, null );
			return;
		}
		Class<?>[] types = getParameterTypes( field );

		//--- If field is 'byte[]' and value is String then decode from Base64 to bytes
		if ( (value instanceof String) && (types[0] == byte[].class) ) {
			Object convertedValue = Base64.getDecoder().decode( (String) value );
			field.setAccessible( true );
			field.set( bean, convertedValue );
			return;
		}

		if ( (List.class.isAssignableFrom( types[0] )) || types[0].isArray() ) {
			_setValueAt( bean, field, index, value );
			return;
		}
		Object convertedValue = BaseConverter.convertToType( value, types[0] );
		field.setAccessible( true );
		field.set( bean, convertedValue );
	}

	private static void _setValueUsingSetter( Object bean, Method setter, int index,
			Object value ) throws IllegalAccessException {
		Class<?>[] types = setter.getParameterTypes();
		if ( !isSetter( setter ) ) {
			String msg = String.format(
					"Method must be a standard setter with one parameter but has %d: %s",
					types.length, setter );
			throw new RuntimeException( msg );
		}

		try {
			if ( value == null ) {
				setter.invoke( bean, (Object) null );
				return;
			}

			//--- If field is 'byte[]' and value is String then decode from Base64 to bytes
			if ( (value instanceof String) && (types[0] == byte[].class) ) {
				value = Base64.getDecoder().decode( (String) value );
			} else if ( isArrayOrList( types[0] ) ) {
				_setValueAt( bean, setter, index, value );
				return;
			}

			Object convertedValue = BaseConverter.convertToType( value, types[0] );
			setter.setAccessible( true );
			setter.invoke( bean, convertedValue );
		} catch ( IllegalAccessException | InvocationTargetException e ) {
			throw new IllegalAccessException(
					String.format( "Cannot set '%s' in bean '%s' with method '%s'",
							value, bean, setter ) );
		}
	}

	@SuppressWarnings("unchecked")
	private static void _appendValueToList( Object bean, Member member, Object listOrArray,
			Object value ) throws IllegalAccessException {
		if ( isList( listOrArray.getClass() ) ) {
			List<Object> l = (List<Object>) listOrArray;
			l.add( value );
			return;
		}
		//--- Increment array
		Object[] array = (Object[]) listOrArray;
		int largerSize = array.length;
		Object[] largerArray = Arrays.copyOf( array, largerSize );
		largerArray[largerSize - 1] = value;

		//--- replace old array with larger one
		_writeValueToMember( bean, member, largerArray );
	}

	@SuppressWarnings("unchecked")
	private static void _writeValueToList( Object listOrArray, int index, Object value ) {
		if ( isList( listOrArray.getClass() ) ) {
			List<Object> l = (List<Object>) listOrArray;
			l.set( index, value );
			return;
		}
		Object[] array = (Object[]) listOrArray;
		array[index] = value;
	}

	private static void _writeValueToMember( Object bean, Member member, Object value )
			throws IllegalAccessException {
		if ( member instanceof Field field ) {
			field.set( bean, value );
			return;
		}
		if ( member instanceof Method setter ) {
			try {
				setter.invoke( bean, value );
			} catch ( InvocationTargetException e ) {
				throw new IllegalAccessException( e.getMessage() );
			}
		}
	}
}