package com.djarjo.common;

import com.google.common.flogger.FluentLogger;

import java.lang.reflect.*;
import java.util.*;

/**
 * Provides low level methods to access beans.
 * <p>
 * Whenever a {@code list} is documented then the code also works for {@code arrays}.
 * </p>
 */
public class ReflectionHelper {

	/**
	 * Prefixes for getter methods
	 */
	public final static Set<String> GETTER_PREFIXES = Set.of( "can", "get", "has", "is",
			"may" );

	/**
	 * Prefixes for setter methods
	 */
	public final static Set<String> SETTER_PREFIXES = Set.of( "add", "set", "with" );

	private final static FluentLogger logger = FluentLogger.forEnclosingClass();

	/**
	 * Exists only to comply with Javadoc
	 */
	public ReflectionHelper() {
	}

	/**
	 * Gets a new instance from the given type.
	 *
	 * @param type any type
	 * @return new instance or {@code null}
	 */
	public static Object createInstance( Type type ) {
		if ( type == null ) return null;

		if ( isArray( type ) ) return _createArrayWithOneItem( type );
		if ( isList( type ) ) return new ArrayList<>();

		try {
			Class<?> clazz = getRawClass( type );
			if ( clazz == null || clazz.isInterface() || Modifier.isAbstract(
					clazz.getModifiers() ) ) {
				// Check common interfaces
				if ( clazz != null ) {
					if ( Map.class.isAssignableFrom( clazz ) ) return new HashMap<>();
					if ( Set.class.isAssignableFrom( clazz ) ) return new HashSet<>();
				}
				throw new InstantiationException(
						"Cannot instantiate abstract/interface: " + type.getTypeName() );
			}

			Constructor<?> constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible( true );
			return constructor.newInstance();
		} catch ( Exception e ) {
			String msg = "Instantiation failed for " + type.getTypeName();
			logger.atWarning().withCause( e ).log( msg );
			throw new RuntimeException( msg, e );
		}
	}

	/**
	 * Finds field by given name in class.
	 *
	 * @param clazz class
	 * @param name field name
	 * @return Field or {@code null}
	 */
	public static Field findField( Class<?> clazz, String name ) {
		try {
			return clazz.getDeclaredField( name );
		} catch ( NoSuchFieldException e ) {
			return (clazz.getSuperclass() != null) ? findField( clazz.getSuperclass(),
					name ) : null;
		}
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
		return findMethodWithPrefixes( methods, propertyName, GETTER_PREFIXES );
	}

	/**
	 * Finds method in class.
	 *
	 * @param clazz class which should contain method
	 * @param name name of method
	 * @return method or null
	 */
	public static Method findMethod( Class<?> clazz, String name ) {
		return Arrays.stream( clazz.getMethods() )
				.filter( m -> m.getName().equals( name ) )
				.findFirst().orElse( null );
	}

	/**
	 * Finds method by name in given methods.
	 *
	 * @param methods methods from class
	 * @param prop name of property
	 * @param prefixes list of allowed prefixes
	 * @return method or null
	 */
	private static Method findMethodWithPrefixes( Method[] methods, String prop,
			Set<String> prefixes ) {
		if ( prop == null || prop.isEmpty() ) return null;
		String suffix = Character.toUpperCase(
				prop.charAt( 0 ) ) + (prop.length() > 1 ? prop.substring( 1 ) : "");

		for ( Method m : methods ) {
			for ( String pre : prefixes ) {
				if ( m.getName().equals( pre + suffix ) ) return m;
			}
		}
		return null;
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
		// Exact name match (Fluent API style)
		Method exact = findMethod( beanClass, propertyName );
		if ( exact != null && exact.getParameterCount() == 1 ) return exact;
		return findMethodWithPrefixes( methods, propertyName, SETTER_PREFIXES );
	}

	public static Type getGenericInnerType( Type type ) {
		if ( type instanceof Class<?> cls && cls.isArray() ) return cls.getComponentType();
		if ( type instanceof GenericArrayType gat ) return gat.getGenericComponentType();
		if ( type instanceof ParameterizedType pt && pt.getActualTypeArguments().length > 0 ) {
			return pt.getActualTypeArguments()[0];
		}
		return null;
	}

	/**
	 * Gets all parameters types of member
	 *
	 * @param member field or getter
	 * @return types of parameters
	 */
	public static Class<?>[] getParameterTypes( Member member ) {
		return (member instanceof Field field) ?
				new Class<?>[]{field.getType(), field.getGenericType().getClass()} :
				((Method) member).getParameterTypes();
	}

	public static Class<?> getRawClass( Type type ) {
		if ( type instanceof Class<?> cls ) return cls;
		if ( type instanceof ParameterizedType pt ) return (Class<?>) pt.getRawType();
		return null;
	}

	/**
	 * Gets value
	 *
	 * @param arrayOrList list or array
	 * @param index index
	 * @return value at index
	 */
	public static Object getValueByIndex( Object arrayOrList, int index ) {
		if ( arrayOrList == null ) return null;
		Class<?> beanType = arrayOrList.getClass();

		if ( List.class.isAssignableFrom( beanType ) ) {
			List<?> list = (List<?>) arrayOrList;
			return ((0 <= index) && (index < list.size())) ? list.get( index ) : null;
		}
		if ( beanType.isArray() ) {
			int length = Array.getLength( arrayOrList );
			return ((0 <= index) && (index < length)) ? Array.get( arrayOrList, index ) : null;
		}
		return null;
	}

	/**
	 * Gets value from member
	 *
	 * @param bean object
	 * @param member field or getter
	 * @return value (can be null)
	 * @throws IllegalAccessException if access not allowed
	 * @throws InvocationTargetException if member cannot be invoked
	 */
	public static Object getValueFromMember( Object bean,
			Member member ) throws IllegalAccessException, InvocationTargetException {
		if ( member instanceof Field field ) {
			field.setAccessible( true );
			return field.get( bean );
		}
		//--- Use getter
		Method getter = obtainGetterFromMethod( bean.getClass(), (Method) member );
		if ( getter == null ) {
			throw new RuntimeException(
					String.format( "No getter in %s from %s", bean, member ) );
		}
		getter.setAccessible( true );
		return getter.invoke( bean, (Object[]) null );
	}

	/**
	 * Extracts the name of the variable from the name of the method. The method name must
	 * start with any one of [{@code get}, {@code has}, {@code is}, {@code set},
	 * {@code with}] followed by the name of the variable.
	 *
	 * @param methodName name of the method
	 * @return name of the variable o r {@code null}
	 */
	public static String getVarNameFromMethodName( String methodName ) {
		List<String> prefixes = new ArrayList<>( GETTER_PREFIXES );
		prefixes.addAll( SETTER_PREFIXES );
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
	 * Checks if class is array or list
	 *
	 * @param type class or an array of a basic type
	 * @return true if array or list
	 */
	public static boolean isArray( Type type ) {
		return (type instanceof Class) ? ((Class<?>) type).isArray() :
				type instanceof GenericArrayType;
	}

	/**
	 * Checks if type is a byte array
	 *
	 * @param type type of field or getter
	 * @return {@code true} if a byte array
	 */
	public static boolean isByteArray( Type type ) {
		return getRawClass( type ) == byte[].class;
	}

	/**
	 * Checks if {@code type} is an enumeration.
	 *
	 * @param type type
	 * @return {@code true} if an enumeration
	 */
	public static boolean isEnum( Type type ) {
		//--- A Type must be a Class to potentially be an Enum
		return (type instanceof Class<?> cls) && cls.isEnum();
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
		for ( String prefix : GETTER_PREFIXES ) {
			if ( methodName.startsWith( prefix ) ) {
				if ( Character.isUpperCase( methodName.charAt( prefix.length() ) ) ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks if {@code type} is a list
	 *
	 * @param type type
	 * @return true if list
	 */
	public static boolean isList( Type type ) {
		return (type instanceof Class<?> cls) ? List.class.isAssignableFrom( cls )
				: type instanceof ParameterizedType pt && isList( pt.getRawType() );
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
		for ( String prefix : SETTER_PREFIXES ) {
			if ( methodName.startsWith( prefix ) && method.getParameterCount() == 1 ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns field name from method if method is a getter.
	 *
	 * @param method getter
	 * @return field name without prefix starts with lowercase char or {@code null}
	 */
	public static String makeFieldName( Method method ) {
		if ( method == null ) return "";
		String name = method.getName();
		for ( String prefix : GETTER_PREFIXES ) {
			if ( name.startsWith( prefix ) ) {
				name = name.substring( prefix.length() );
				if ( !name.isEmpty() && Character.isUpperCase( name.charAt( 0 ) ) ) {
					return Character.toLowerCase( name.charAt( 0 ) ) + name.substring( 1 );
				}

			}
		}
		return "";
	}

	/**
	 * Returns a method name. The returned method name starts with the optional prefix and
	 * is followed by the propertyName whose first character is converted to uppercase.
	 *
	 * @param prefix The prefix for the method like "get", "has", "is", "set" or "with".
	 * Best to use entry from {@link #GETTER_PREFIXES} or {@link #SETTER_PREFIXES}
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

	/**
	 * Obtains getter even if setter is given
	 *
	 * @param clazz class
	 * @param method getter or setter
	 * @return getter or null
	 */
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
	 * @param clazz class containing method
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
	 * If {@code member} is a list and {@code value} is not then the list will be read (or
	 * created) and {@code value} will be added to it.
	 * </p>
	 *
	 * @param bean bean
	 * @param accessor field or method
	 * @param index index if member is a list or an array
	 * @param value new value
	 * @throws IllegalAccessException e
	 */
	public static void setValue( Object bean, MemberAccessor accessor, int index,
			Object value ) throws IllegalAccessException {
		if ( accessor.isField() ) {
			_setValueUsingField( bean, accessor.field(), index, value );
		} else {
			_setValueUsingSetter( bean, accessor.setter(), index, value );
		}
	}

	private static void _appendValueToArrayOrList( Object bean, Member member,
			Object arrayOrList, Object value ) throws IllegalAccessException {
		if ( isArray( arrayOrList.getClass() ) ) {
			int largerSize = Array.getLength( arrayOrList ) + 1;
			Object[] largerArray = Arrays.copyOf( (Object[]) arrayOrList, largerSize );
			largerArray[largerSize - 1] = value;

			//--- replace old array with larger one
			_writeValueToMember( bean, member, largerArray );
			return;
		}
		((List) arrayOrList).add( value );
	}

	private static Object _createArrayWithOneItem( Type type ) {
		Type componentType = getGenericInnerType( type );
		Class<?> cls = getRawClass( componentType != null ? componentType : Object.class );
		return Array.newInstance( cls != null ? cls : Object.class, 1 );
	}

	private static int _getSize( Object value ) {
		return (value == null) ? 0 : isList( value.getClass() )
				? ((List<?>) value).size() : (value.getClass().isArray())
				? Array.getLength( value ) : 0;
	}

	/**
	 * Sets {@code value} into {@code member} in {@code bean}.
	 * <p>{@code member} type must be an array or a list</p>
	 *
	 * @param bean object
	 * @param member field or setter
	 * @param index -1 to append or valid index
	 * @param value value to set at index
	 */
	private static void _setValueAt( Object bean, Member member, int index,
			Object value ) throws IllegalAccessException {
		assert value != null;
		Class<?>[] types = getParameterTypes( member );
		try {
			Object arrayOrList = getValueFromMember( bean, member );
			if ( arrayOrList == null ) {
				arrayOrList = createInstance( types[0] );
				_writeValueToMember( bean, member, arrayOrList );
			}
			int currentSize = _getSize( arrayOrList );

			//--- Append to end of list or array
			if ( (index < 0) || (currentSize <= index) ) {
				_appendValueToArrayOrList( bean, member, arrayOrList, value );
				return;
			}
			_writeValueToArrayOrList( arrayOrList, index, value );
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
			Object convertedValue = Base64.decoder().decode( (String) value );
			field.setAccessible( true );
			field.set( bean, convertedValue );
			return;
		}

		if ( (List.class.isAssignableFrom( types[0] )) || isArray( types[0] ) ) {
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
				value = Base64.decoder().decode( (String) value );
			} else if ( (List.class.isAssignableFrom( types[0] )) || isArray( types[0] ) ) {
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
	private static void _writeValueToArrayOrList( Object listOrArray, int index,
			Object value ) {
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