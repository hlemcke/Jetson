package com.djarjo.common;

import com.djarjo.text.TextHelper;
import com.google.common.flogger.FluentLogger;

import javax.lang.model.element.ElementKind;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.time.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods for beans.
 *
 * @author Hajo Lemcke
 * @since 2025-05-17 modified to use ReflectionHelper
 * @since 2021-12-28 method "set" now uses generics from List
 */
public class BeanHelper {
	public static final Set<Class<?>> BASIC_TYPES = new HashSet<>( Arrays.asList(
			Boolean.class,
			Character.class,
			Currency.class,
			Duration.class,         // e.g., "PT1H30M" (Time-based: 1 hour 30 mins)
			Instant.class,          // e.g. "2025-12-20T10:15:30Z"
			LocalDate.class,
			LocalTime.class,
			LocalDateTime.class,
			Locale.class,
			OffsetDateTime.class,
			Period.class,           // e.g., "P2Y3M" (Date-based: 2 years 3 months)
			String.class,
			UUID.class,
			ZonedDateTime.class ) );
	private final static FluentLogger logger = FluentLogger.forEnclosingClass();
	private static final Comparator<Object> keyComparator =
			Comparator.comparing( Object::toString );
	private static final Pattern indexedPropertyPattern =
			Pattern.compile( "^(\\w+)\\[([0-9]+|\\+)]$" );
	private static final Comparator<Method> methodNameComparator =
			Comparator.comparing( Method::getName );

	/**
	 * Useless public constructor implemented for Javadoc only
	 */
	public BeanHelper() {
	}

	/**
	 * Obtains all fields from the bean which have a getter method and store them in the
	 * map. The key is the name of the getter (lower case without the leading 'get') and
	 * the
	 * value is the value returned from the getter method.
	 *
	 * @param bean Java object to be described
	 * @return new instance of HashMap
	 * @throws IllegalAccessException if access is prohibited
	 * @throws IllegalArgumentException if getter name is wrong
	 * @throws InvocationTargetException if getter must not be invoked
	 */
	public static Map<String, Object> describe( Object bean )
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		Method[] getters = obtainGetters( bean.getClass() );
		return describe( bean, getters );
	}

	/**
	 * Describes the given bean into a map. The description contains the properties of the
	 * bean which can be obtained through the given getter methods.
	 *
	 * @param bean Java object to be described
	 * @param getters list of getter methods
	 * @return new instance of HashMap
	 * @throws IllegalAccessException if access is prohibited
	 * @throws IllegalArgumentException if getter name is wrong
	 * @throws InvocationTargetException if getter must not be invoked
	 */
	public static Map<String, Object> describe( Object bean, Method[] getters )
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {

		// ----- Prepare resulting map
		Map<String, Object> map = new HashMap<>();

		// --- Put values into map
		String key = null;
		Object value = null;
		for ( Method method : getters ) {
			key = ReflectionHelper.getVarNameFromMethodName( method.getName() );
			method.setAccessible( true );
			value = method.invoke( bean, (Object[]) null );
			map.put( key, value );
		}
		return map;
	}

	/**
	 * Obtains all fields from the bean and stores them in a map where key is the name of
	 * the field.
	 *
	 * @param bean The bean to describe
	 * @return map with field names and their values
	 * @throws IllegalAccessException if access is prohibited
	 */
	public static Map<String, Object> describeFields( Object bean )
			throws IllegalAccessException {
		Map<String, Object> map = new HashMap<>();

		// --- Put values into map
		String key = null;
		Object value = null;
		Field[] fields = bean.getClass()
				.getDeclaredFields();
		for ( Field field : fields ) {
			key = field.getName();
			if ( Modifier.isStatic( field.getModifiers() ) ) {
				continue;
			}
			field.setAccessible( true );
			value = field.get( bean );
			map.put( key, value );
		}
		return map;
	}

	/**
	 * Extracts the name of the package out of the full name of the Java file or class.
	 *
	 * @param fullName The full name of a class or interface
	 * @return package name
	 */
	public static String extractPackageFromName( String fullName ) {
		String str = fullName;
		if ( fullName.endsWith( ".class" ) ) {
			str = fullName.substring( 0, fullName.length() - 6 );
		} else if ( fullName.endsWith( ".java" ) ) {
			str = fullName.substring( 0, fullName.length() - 5 );
		}
		// --- adapt path to package
		str = str.replace( '/', '.' );
		str = str.replace( '\\', '.' );
		int i = str.lastIndexOf( '.' );
		if ( i > 0 ) {
			str = str.substring( 0, i );
		}
		return str;
	}

	/**
	 * Finds methods with the given annotation.
	 *
	 * @param cls The class in which to find methods
	 * @param annoClass The annotation which must be present on the method
	 * @return annotated methods or <em>null</em> if none found
	 */
	public static Method[] findMethods( Class<?> cls,
			Class<? extends Annotation> annoClass ) {
		List<Method> annotatedMethods = new ArrayList<>();
		Method[] methods = cls.getMethods();
		for ( Method method : methods ) {
			if ( method.isAnnotationPresent( annoClass ) ) {
				annotatedMethods.add( method );
			}
		}
		Method[] array = null;
		if ( !annotatedMethods.isEmpty() ) {
			array = new Method[annotatedMethods.size()];
			array = annotatedMethods.toArray( array );
		}
		return array;
	}

	/**
	 * Gets the value from a field or method in the given bean. Any exception will be
	 * logged
	 * with level WARNING and {@code null} will be returned.
	 *
	 * @param bean bean which contains the element
	 * @param kind type of the element (FIELD or METHOD)
	 * @param name name of the element
	 * @return value of the element
	 */
	public static Object getValue( Object bean, ElementKind kind,
			String name ) {
		Object value = null;
		try {
			if ( kind == ElementKind.FIELD ) {
				Field field = bean.getClass()
						.getDeclaredField( name );
				field.setAccessible( true );
				value = field.get( bean );
			} else if ( kind == ElementKind.METHOD ) {
				Method method = bean.getClass()
						.getDeclaredMethod( name, (Class<?>[]) null );
				method.setAccessible( true );
				value = method.invoke( bean, (Object[]) null );
			}
		} catch ( IllegalAccessException | IllegalArgumentException
							| InvocationTargetException | NoSuchFieldException
							| SecurityException | NoSuchMethodException e ) {
			String msg = "Error reading value from " + name + " in bean "
					+ bean.getClass();
			logger.atWarning()
					.withCause( e )
					.log( msg );
		}
		return value;
	}

	/**
	 * Gets the value from the given member (field or method). A method must not have any
	 * parameters. Any exception will be logged with level WARNING and {@code null} will be
	 * returned.
	 *
	 * @param bean The bean which contains the element
	 * @param element The field or method in the bean
	 * @return Returns the value of the element
	 */
	public static Object getValue( Object bean, AnnotatedElement element ) {
		Object value = null;
		try {
			if ( element instanceof Field field ) {
				field.setAccessible( true );
				value = field.get( bean );
			} else if ( element instanceof Method method ) {
				method.setAccessible( true );
				int count = method.getParameterCount();
				if ( count != 0 ) {
					throw new IllegalArgumentException( "Method " + method
							+ " may not have parameters but has " + count );
				}
				value = method.invoke( bean, (Object[]) null );
			}
		} catch ( IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e ) {
			String msg = "Error reading value from " + element;
			logger.atWarning()
					.withCause( e )
					.log( msg );
		}
		return value;
	}

	/**
	 * Injects {@code value} into {@code object}.{@code fieldName}
	 *
	 * @param bean target object
	 * @param fieldName name of target field in bean
	 * @param value value to set
	 * @throws IllegalAccessException if access is prohibited
	 * @throws IllegalArgumentException if type of value is wrong
	 * @throws NoSuchFieldException if bean does not have an attribute named
	 * {@code fieldName}
	 * @throws SecurityException if access is prohibited
	 */
	public static void inject( Object bean, String fieldName, Object value )
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = bean.getClass();
		Field internalField = clazz.getDeclaredField( fieldName );
		internalField.setAccessible( true );
		internalField.set( bean, value );
	}

	/**
	 * Determines if an object should be treated as a single "basic" value rather than a
	 * complex object that needs recursion.
	 */
	public static boolean isBasicType( Object value ) {
		if ( value == null ) return true;
		Class<?> clazz = value.getClass();
		if ( BASIC_TYPES.contains( clazz ) ) return true;
		if ( value instanceof Number ) return true;
		return clazz.isEnum();
	}

	/**
	 * Makes a class name from the given name by making the first character upper case.
	 *
	 * @param name Name of a method or field
	 * @return name with first char made upper case
	 */
	public static String makeClassName( String name ) {
		return Character.toUpperCase( name.charAt( 0 ) ) + name.substring( 1 );
	}

	/**
	 * Makes a field name from the given string with the following algorithm:
	 * <ol>
	 * <li>If the string contains no lower case letter then the whole string
	 * will be converted to lower case (constant name e.g. from an enumeration)
	 * </li>
	 * <li>The first character becomes lower case</li>
	 * <li>Each underscore '_' will be removed and the letter following it
	 * becomes upper case</li>
	 * </ol>
	 * Examples:
	 * <table>
	 * <caption>Examples</caption>
	 * <tr>
	 * <td>SOME_ENUM_ENTRY</td>
	 * <td>-&gt; someEnumEntry</td>
	 * </tr>
	 * <tr>
	 * <td>MYCONSTANT</td>
	 * <td>-&gt; myconstant</td>
	 * </tr>
	 * <tr>
	 * <td>SomeClass</td>
	 * <td>-&gt; someClass</td>
	 * </tr>
	 * </table>
	 *
	 * @param name some name
	 * @return field name
	 */
	public static String makeFieldName( String name ) {
		boolean hasLowerCase = false;
		int i = 0;
		for ( i = 0; i < name.length(); i++ ) {
			if ( Character.isLowerCase( name.charAt( i ) ) ) {
				hasLowerCase = true;
				break;
			}
		}
		if ( !hasLowerCase ) {
			name = name.toLowerCase();
		} else {
			name = Character.toLowerCase( name.charAt( 0 ) )
					+ name.substring( 1 );
		}

		StringBuilder fieldName = new StringBuilder( name );
		for ( i = 0; i < fieldName.length(); i++ ) {
			if ( fieldName.charAt( i ) == '_' ) {
				fieldName.deleteCharAt( i );
				fieldName.setCharAt( i,
						Character.toUpperCase( fieldName.charAt( i ) ) );
			}
		}
		return fieldName.toString();
	}

	/**
	 * Obtains all getter methods of the given class sorted by name.
	 * <p>
	 * A getter method is defined by:
	 * <ol>
	 * <li>The method name starts with 'get', 'has' or 'is'</li>
	 * <li>The first char following 'get', 'has' or 'is' is upper case</li>
	 * <li>The method has no parameter</li>
	 * </ol>
	 * <p>
	 * Returns a map where the key is the property name. That is the string
	 * following 'get' with the first char made lower case. The value assigned
	 * to the key is the getter method itself.
	 * </p>
	 *
	 * @param cls The Java class to obtain the getter methods from
	 * @return array of getter methods
	 */
	public static Method[] obtainGetters( Class<?> cls ) {
		List<Method> getters = new ArrayList<>();
		Method[] methods = cls.getMethods();
		for ( Method method : methods ) {
			if ( ReflectionHelper.isGetter( method ) ) {
				getters.add( method );
			}
		}
		getters.sort( methodNameComparator );
		return getters.toArray( new Method[0] );
	}

	/**
	 * Obtains all setter methods of the given class.
	 * <p>
	 * A setter method is defined by:
	 * <ol>
	 * <li>The method name starts with 'set'</li>
	 * <li>The first char following 'set' is upper case</li>
	 * <li>The method has exactly one single parameter</li>
	 * </ol>
	 *
	 * @param clazz The Java class to obtain the setter methods from
	 * @param withInheritance {@code true} includes inherited classes
	 * @return array of setter methods
	 */
	public static Method[] obtainSetters( Class<?> clazz,
			boolean withInheritance ) {
		List<Method> setters = new ArrayList<>();
		Method[] methods = withInheritance ? clazz.getMethods() : clazz.getDeclaredMethods();
		for ( Method method : methods ) {
			if ( ReflectionHelper.isSetter( method ) ) {
				setters.add( method );
			}
		}
		return setters.toArray( new Method[0] );
	}

	/**
	 * Obtains all setter methods of the given class.
	 *
	 * @param clazz The Java class to obtain the setter methods from
	 * @return array of setter methods
	 */
	public static Method[] obtainSetters( Class<?> clazz ) {
		return obtainSetters( clazz, false );
	}

	/**
	 * Populates a Java object from a map.
	 *
	 * @param bean Java object to populate
	 * @param setters array of setter methods
	 * @param map map with values
	 * @throws IllegalAccessException if access is prohibited
	 * @throws IllegalArgumentException if getter name is wrong
	 * @throws InvocationTargetException if getter must not be invoked
	 * @throws ParseException if map has wrong structure
	 */
	public static void populate( Object bean, Method[] setters,
			Map<String, Object> map )
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, ParseException {
		populate( bean, setters, map, false );
	}

	/**
	 * Sets properties of the given bean which have a setter method. The property must have
	 * the same name as the key of an entry from the map.
	 *
	 * @param bean fields will be populated from the map by calling its setters
	 * @param setters Setter methods. Use null to obtain them here
	 * @param map keys must have bean property names
	 * @param withInherited {@code true} includes inherited classes
	 * @throws IllegalArgumentException if value has incorrect type
	 * @throws IllegalAccessException if access to setter is prohibited
	 * @throws InvocationTargetException in setter cannot be invoked
	 * @throws ParseException if a parsing error occurs
	 */
	public static void populate( Object bean, Method[] setters,
			Map<String, Object> map, boolean withInherited )
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, ParseException {
		logger.atFiner().log( bean + ", " + map );

		// --- No setters given => obtain here
		if ( setters == null ) {
			setters = obtainSetters( bean.getClass(), withInherited );
		}

		// --- Loop through setter methods
		String key;
		for ( Method setter : setters ) {
			key = ReflectionHelper.getVarNameFromMethodName( setter.getName() );
			if ( map.containsKey( key ) ) {
				ReflectionHelper.setValue( bean, setter, 0, map.get( key ) );
			}
		}
	}

	/**
	 * Pretty prints {@code bean} by converting it to JSON 5 format with each key on a
	 * separate line.
	 *
	 * @param bean to be pretty printed
	 * @param withNullValues {@code false} will drop properties having a null value
	 * @return string with one property per line
	 */
	public static String prettyPrint( Object bean, boolean withNullValues ) {
		return prettyPrint( bean, 0, withNullValues );
	}

	/**
	 * Returns a string with one property per line of the given {@code bean}. Only uses
	 * public getter methods.
	 *
	 * @param bean object to be pretty printed
	 * @param maxDepth only print to this depth (0 = no limit)
	 * @param withNullValues {@code true} prints properties with a null value
	 * @return string with one property per line
	 */
	public static String prettyPrint( Object bean, int maxDepth,
			boolean withNullValues ) {
		String s = _prettyPrintIndent( bean, maxDepth, withNullValues, 0,
				new HashSet<Object>() );
		s = (s.startsWith( "\n" ) ? s.substring( 1 ) : s);
		return s;
	}

	private static String _prettyPrintIndent( Object bean, int maxDepth,
			boolean withNullValues, int curDepth, Set<Object> path ) {
		if ( bean == null ) {
			return withNullValues ? "null" : null;
		}
		if ( bean instanceof Boolean || bean instanceof Byte
				|| bean instanceof Short || bean instanceof Integer
				|| bean instanceof Long ) {
			return bean.toString();
		}
		if ( bean instanceof Float || bean instanceof Double
				|| bean instanceof BigDecimal
				|| bean instanceof BigInteger ) {
			return bean.toString();
		}
		if ( bean instanceof Character || bean instanceof CharSequence ) {
			return "\"" + bean + "\"";
		}
		if ( bean instanceof LocalDate
				|| bean instanceof Locale
				|| bean instanceof LocalDateTime
				|| bean instanceof OffsetDateTime
				|| bean instanceof UUID ) {
			return "\"" + bean + "\"";
		}
		if ( bean instanceof Enum ) {
			return bean.toString();
		}
		// --- current output
		StringBuilder s = new StringBuilder();

		// --- Array
		if ( bean.getClass()
				.isArray() ) {
			for ( int i = 0; i < Array.getLength( bean ); i++ ) {
				s.append( _prettyPrintIndent( Array.get( bean, i ), maxDepth,
								withNullValues, curDepth + 1, path ) )
						.append( ", " );
			}
			return s.isEmpty() ? "[]" : "[ " + s + " ]";
		}

		// --- Collection
		if ( bean instanceof Collection ) {
			for ( Object item : (Collection<?>) bean ) {
				s.append( _prettyPrintIndent( item, maxDepth, withNullValues,
								curDepth + 1, path ) )
						.append( ", " );
			}
			return s.isEmpty() ? "[]" : "[ " + s + " ]";
		}

		// --- Prepare indentation
		final String spaces = "                              ";
		String indent = spaces.substring( 0, curDepth * 2 );

		// --- Map
		Object value = null;
		if ( bean instanceof Map<?, ?> map ) {
			Object[] keys = map.keySet()
					.toArray( new Object[map.size()] );
			Arrays.sort( keys, keyComparator );
			for ( Object key : keys ) {
				value = _prettyPrintIndent( map.get( key ), maxDepth,
						withNullValues, curDepth + 1, path );
				s.append( (withNullValues || value != null)
						? String.format( "%s%s: %s,\n", indent, key, value )
						: "" );
			}
			return s.isEmpty() ? "{}" : "{\n " + s + indent + " }";
		}

		// --- Bean -> prevent recursion and deep dive
		if ( path.contains( bean ) || (curDepth > 11)
				|| (maxDepth > 0 && curDepth >= maxDepth) ) {
			return "\"" + bean + "\"";
		}
		path.add( bean );
		Method[] getters = BeanHelper.obtainGetters( bean.getClass() );
		for ( Method getter : getters ) {
			try {
				value = getter.invoke( bean, (Object[]) null );
				s.append( (withNullValues || value != null)
						? indent + ReflectionHelper.getVarNameFromMethodName( getter.getName() )
						+ ": "
						+ _prettyPrintIndent( value, maxDepth,
						withNullValues, curDepth + 1, path )
						+ ",\n"
						: "" );
			} catch ( IllegalAccessException | IllegalArgumentException
								| InvocationTargetException e ) {
				s.append( "\"" )
						.append( bean )
						.append( "\"" );
			}
		}
		path.remove( bean );
		return ((!s.isEmpty()) || withNullValues) ? "{\n" + s + indent + "}"
				: null;
	}

	/**
	 * Sets a property of the given bean.
	 * <p>
	 * The property must either have a setter method following bean standards or a field
	 * with this name. The setter must be named like "setAbc( Object value )". The
	 * <em>propertyName</em> defines the property like "abc".
	 * </p>
	 * <h4>Nested Properties</h4>
	 * <p>
	 * Beans may be nested. In this case <em>propertyName</em> must be a slash-separated
	 * list of names like "nestedBean/attr1" to set {@code value} into property
	 * <em>attr1</em> of <em>nestedBean</em>.
	 * </p>
	 * <h4>List Properties</h4>
	 * <p>
	 * {@code value} can also be written into a list property. If {@code value} itself is a
	 * list then it will directly be written into the property.
	 * </p><p>
	 * Otherwise {@code value} will be converted into the lists generic type. If the (list)
	 * property is {@code null} then the list will be created first and written into the
	 * property. Then {@code value} will be written into the list:
	 * <ol>
	 *   <li>If no index is given (like in "someList") then index 0 will be used</li>
	 *   <li>If index is "+" then {@code value} will be appended to end of list</li>
	 *   <li>Otherwise {@code value} will be written into the list at <em>index</em>.
	 *   If <em>index</em> is out of range then {@code value} will be appended</li>
	 * </ol>
	 *
	 * @param bean The bean in which to set the property to the given value
	 * @param propertyPath single property like "attr1" or nested like "someBean/attr2"
	 * @param value The new value for the property
	 * @return {@code true} when the field was found and the value could be set
	 */
	public static boolean setValue( Object bean, String propertyPath, Object value ) {
		if ( bean == null ) return false;

		String[] props = propertyPath.split( "/" );
		String prop = "";
		int index = 0;
		Object currentBean = bean;
		try {
			//--- loop property path (may contain lists)
			for ( int i = 0; i < props.length; i++ ) {
				index = 0;
				prop = props[i];
				if ( prop.contains( "[" ) ) {
					Matcher matcher = indexedPropertyPattern.matcher( prop );
					if ( matcher.matches() ) {
						prop = matcher.group( 1 );
						String indexText = matcher.group( 2 );
						index = "+".equals( indexText ) ? -1 : TextHelper.parseInteger( indexText );
					}
				}

				//--- Setter or fallback to field
				Member member = ReflectionHelper.findSetter( currentBean.getClass(), prop );
				if ( member == null ) {
					member = ReflectionHelper.findField( currentBean.getClass(), prop );
					if ( member == null ) {
						String message = String.format( "No field '%s' in bean %s", prop, bean );
						logger.atFiner().log( message );
						throw new RuntimeException( new NoSuchFieldException( message ) );
					}
				}
				((AccessibleObject) member).setAccessible( true );

				//--- Set value into last property from path
				if ( i == props.length - 1 ) {
					ReflectionHelper.setValue( currentBean, member, index, value );
					return true;
				}

				//--- Obtain current pojo from property path
				Object currentValue = ReflectionHelper.getValueFromMember( currentBean, member );

				//--- Current property is null -> create it
				if ( currentValue == null ) {
					currentValue = ReflectionHelper.instantiateProperty( currentBean, member );
				}

				if ( ReflectionHelper.isArrayOrList( currentValue.getClass() ) ) {
					//--- Obtain item or create it
					Object listItem = ReflectionHelper.getValueByIndex( currentValue, index );
					//--- list is empty -> create item
					if ( listItem == null ) {
						listItem = ReflectionHelper.instantiateGeneric( bean, member, currentValue,
								index );
					}
					currentValue = listItem;
				}
				currentBean = currentValue;
			}
		} catch ( IllegalAccessException e ) {
			logger.atFiner().log( "Cannot access '%s' in bean %s", prop, bean );
			throw new RuntimeException( e );
		} catch ( InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
		return false;
	}
}