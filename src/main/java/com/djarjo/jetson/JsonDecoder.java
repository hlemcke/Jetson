/**
 *
 */
package com.djarjo.jetson;

import com.djarjo.common.BaseConverter;
import com.djarjo.common.BeanHelper;
import com.djarjo.common.ReflectionHelper;
import com.djarjo.jetson.converter.JsonConverter;
import com.djarjo.text.*;
import com.google.common.flogger.FluentLogger;

import java.lang.reflect.*;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;

/**
 * Decodes a Json string into a target object.
 *
 * @author Hajo Lemcke
 * @see <a href="http://www.json.org/">Json</a>
 * @since 2019-06-03 decoding into collection will replace existing value
 * @since 2020-05-19 decode enumeration value auto-strips leading class name if present
 */
public class JsonDecoder {
	public final static String METHOD_FROM_JSON = "fromJson";
	private final static FluentLogger logger = FluentLogger.forEnclosingClass();

	private Set<String> _keysToSkip = null;
	private Level _logLevel4MissingAttribute = Level.OFF;
	private boolean _mergeCollections = false;
	private Tokenizer _tokenizer = null;

	/**
	 * Empty no argument constructor.
	 */
	public JsonDecoder() {
	}

	/**
	 * Instantiates and returns a new instance of this class to be used in streaming method
	 * calls.
	 *
	 * @return new instance of JsonDecoder
	 */
	public static JsonDecoder decoder() {
		return new JsonDecoder();
	}

	/**
	 * Decodes the given Json string. Finding a "{" will automatically create a new
	 * HashMap.
	 * Finding a "[" will automatically create a new ArrayList. Any other value will
	 * just be
	 * returned.
	 *
	 * @param jsonString Json string to be decoded
	 * @return a new list or a new map
	 * @throws IllegalAccessException if a decoded value cannot be set into the target
	 * object
	 * @throws ParseException in case of an error in the JSON string
	 */
	public Object decode(
			String jsonString ) throws IllegalAccessException, ParseException {
		return decodeIntoObject( jsonString, null );
	}

	/**
	 * Decodes the given JSON string and writes its values into the given target object.
	 * <p>
	 * Decoding process:
	 * </p>
	 * <ol>
	 *   <li>If target is {@code null} then a new Map will be created, filled and
	 *   returned</li>
	 *   <li>If the target class is annotated with {@literal @Json} then:
	 *     <ul><li>converter will be used if present</li>
	 *       <li>all setters will be used (default) unless
	 *       {@code accessType=JsonAccessType.FIELD} is set to use all public fields</li>
	 *       <li>Members (fields and setters) annotated with {@literal @JsonTransient}
	 *       will be skipped</li>
	 *     </ul></li>
	 *   <li>If target class is <b>not</b> annotated with {@literal @Json} then all members
	 *   (fields and getters) will be used which are annotated with {@literal @Json}</li>
	 * </ol>
	 * <p>
	 * Fields (getters) in target class must be annotated with
	 * {@link com.djarjo.jetson.Json @Json}.
	 * </p>
	 *
	 * @param json JSON text to decode
	 * @param target target object to write values into
	 * @return given target or new instance of list or map
	 * @throws IllegalAccessException if a decoded value cannot be set
	 * @throws ParseException in case of an error in the JSON string
	 */
	public Object decodeIntoObject( String json,
			Object target ) throws ParseException, IllegalAccessException {
		if ( json == null || json.isBlank() ) {
			return null;
		}
		_tokenizer = new Tokenizer( json,
				TokenizerOption.SKIP_DOUBLESLASH_UNTIL_EOL,
				TokenizerOption.SKIP_LINEBREAK, TokenizerOption.SKIP_WHITESPACE );
		_tokenizer.nextToken();
		Object result = _handleClassAnnotation( target );
		return (result != null) ? result : _decodeValue( target, null, null );
	}

	/**
	 * Decodes the given {@code jsonString} into a "List&lt;itemClass&gt;".
	 *
	 * @param <T> Type of collection
	 * @param jsonString must start with "["
	 * @param itemClass Type of elements in list
	 * @return new list
	 * @throws ParseException if not a JSON list
	 * @throws IllegalAccessException if field in list items not accessible
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> decodeList( String jsonString,
			Class<T> itemClass ) throws ParseException, IllegalAccessException {
		if ( jsonString == null || jsonString.isEmpty() ) {
			return null;
		}
		_tokenizer = new Tokenizer( jsonString,
				TokenizerOption.SKIP_DOUBLESLASH_UNTIL_EOL,
				TokenizerOption.SKIP_LINEBREAK, TokenizerOption.SKIP_WHITESPACE );
		Token token = _tokenizer.nextToken();
		if ( token.symbol != Symbol.LEFT_BRACKET ) {
			String s = _makeErrorInfo( "Syntax error. '[' expected to parse a list" );
			throw new ParseException( s, 0 );
		}
		return (List<T>) _decodeList( new ArrayList<T>(), itemClass );
	}

	/**
	 * Add keys to be skipped if they do not exist in target object.
	 * <p>
	 * Only used if logLevel4MissingAttribute is not OFF
	 *
	 * @param keys one or more keys to be skipped
	 * @return this for fluent API
	 */
	public JsonDecoder keysToSkip( String... keys ) {
		_keysToSkip = (_keysToSkip == null) ? new HashSet<>() : _keysToSkip;
		Collections.addAll( _keysToSkip, keys );
		return this;
	}

	/**
	 * Merges items from Json string into an existing collection instead of replacing the
	 * collection.
	 *
	 * @return this for fluent API
	 */
	public JsonDecoder mergeCollections() {
		_mergeCollections = true;
		return this;
	}

	/**
	 * Sets log level to report a missing setter (defaults to OFF).
	 *
	 * @param level Level to use for reporting
	 * @return this for fluent API
	 */
	public JsonDecoder logLevel4MissingAttribute( Level level ) {
		this._logLevel4MissingAttribute = level;
		return this;
	}

	/**
	 * Clones the given collection into a new {@code ArrayList}. Used for decoding
	 * {@code json} into an existing collection.
	 *
	 * @param toBeCloned Collection to be cloned
	 * @return ArrayList with items from given collection
	 */
	private Collection<?> _cloneCollection( Collection<Object> toBeCloned ) {
		return new ArrayList<>( toBeCloned );
	}

	/**
	 * Decodes a Json string starting with "[". Returns the given {@code target} or creates
	 * a new ArrayList filled with values from the Json string until "]". Values in the
	 * Json
	 * array must be basic types or {@code valueType} must be given.
	 *
	 * @param target The target object for the values in the collection
	 * @param valueType type of the values (required if not a basic type)
	 * @return given {@code target} or a new {@code ArrayList}
	 * @throws IllegalAccessException what it says
	 * @throws ParseException what it says
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private Collection _decodeList( Collection target,
			Type valueType ) throws IllegalAccessException, ParseException {
		Token token;
		target = (target == null) ? new ArrayList<>() : target;

		//--- Build a new list
		List<Object> listFromJson = new ArrayList<>();
		while ( true ) {
			token = _tokenizer.nextToken();
			if ( token.symbol == Symbol.COMMA ) {
				token = _tokenizer.nextToken();
			}
			if ( token.symbol == Symbol.RIGHT_BRACKET ) {
				break; // --- Safely skip , in front of }
			}
			if ( token.symbol == Symbol.EOF ) {
				String s = _makeErrorInfo( "Syntax error. Value or ']' expected" );
				throw new ParseException( s, _tokenizer.getPosition() );
			}
			//--- extract json list element (if map) for optional second parse
			int startIndex = (token.symbol == Symbol.LEFT_BRACE) ? token.position : -1;
			//--- Value from Json string
			Object value = _decodeValue( null, null, valueType );
			if ( valueType != null ) {
				value = BaseConverter.convertToType( value, (Class<?>) valueType );
			}
			boolean isMerged = false;
			if ( startIndex >= 0 ) {
				int endIndex = _tokenizer.getPosition();
				String jsonElement = _tokenizer.getText().substring( startIndex,
						endIndex );
				logger.atFiner()
						.log( "start=%d, end = %d, text=%s", startIndex, endIndex,
								jsonElement );
				//--- Search for this object (if it is an object) in target
				for ( Object element : target ) {
					if ( value.equals( element ) ) {
						JsonDecoder.decoder().decodeIntoObject( jsonElement, element );
						isMerged = true;
						break;
					}
				}
			}
			//--- Append value to new list ensures order from json string
			if ( !_mergeCollections || !isMerged ) {
				listFromJson.add( value );
			}
		}
		// --- Transfer json elements into target
		if ( !_mergeCollections ) {
			target.clear();
		}
		target.addAll( listFromJson );
		return target;
	}

	/**
	 * Decodes the Json string for a map. String has started with "{". Map elements are
	 * encoded {@code name ":" value}. Elements are separated by a comma. Values can be of
	 * basic type, LocalDate, OffsetDateTime, UUID or enumeration.
	 *
	 * @param target map to be filled. {@code null} will create a new {@code HashMap}
	 * @param keyType Type of the key. Can be {@code null} for basic types
	 * @param valueType Type of the value. Can be {@code null} for basic types
	 * @return given {@code target} or a new instance of {@code HashMap}
	 * @throws ParseException on json string error
	 * @throws IllegalAccessException on target access error
	 */
	private Map<Object, Object> _decodeMap( Tokenizer tokenizer,
			Map<Object, Object> target,
			Type keyType, Type valueType ) throws ParseException,
			IllegalAccessException {
		Object key = null, value = null;
		if ( target == null ) {
			target = new HashMap<>();
		}
		while ( true ) {
			key = _obtainNameFromJson();
			if ( key == null ) {
				break;
			}
			if ( keyType != null ) {
				key = BaseConverter.convertToType( key, (Class<?>) keyType );
			}
			value = _decodeValue( null, null, valueType );
			target.put( key, value );
		}
		return target;
	}

	/**
	 * Decodes a JSON string which has started with "{". Decoding ends when finding the
	 * matching "}". Entries are decoded in between. Each entry looks like
	 *
	 * @param target The target object for the values
	 * @param keyType type of key for a map
	 * @param valueType generic type of map or collection
	 * @return target or new map filled from JSON string
	 * @throws IllegalAccessException if access to an object field or setter fails
	 * @throws ParseException if json string has a format error
	 */
	@SuppressWarnings("unchecked")
	private Object _decodeObject( Object target, Type keyType,
			Type valueType ) throws IllegalAccessException, ParseException {
		boolean accessFields = false;
		boolean allMembers = false;
		Map<String, Member> members;
		String name;
		Token token;
		Object value;

		// --- New target if none given
		if ( target == null ) {
			if ( valueType == null || valueType == Object.class ) {
				return _decodeMap( _tokenizer, null, null, valueType );
			}
			target = ReflectionHelper.createInstanceFromType( valueType );
		}

		// --- target is a map
		if ( Map.class.isAssignableFrom( target.getClass() ) ) {
			return _decodeMap( _tokenizer, (Map<Object, Object>) target, keyType,
					valueType );
		}

		//--- Check if class is annotated with @Json
		Json classAnno = target.getClass().getAnnotation( Json.class );
		if ( classAnno != null ) {
			Object object = _handleClassAnnotation( target );
			if ( object != null ) {
				return object;
			}
			if ( JsonAccessType.FIELD.equals( classAnno.accessType() ) ) {
				accessFields = true;
			}
			allMembers = true;
		}

		members = _obtainMembers( target );

		// --- Loop through JSON object until "}"
		while ( true ) {
			name = _obtainNameFromJson();
			if ( name == null ) {
				break;
			}
			Member member = members.get( name );
			if ( member == null ) {
				if ( (_logLevel4MissingAttribute != Level.OFF) && (!_keysToSkip.contains(
						name )) ) {
					String message =
							_makeErrorInfo(
									"Name '" + name + "' does not exist in " + target.getClass() );
					logger.at( _logLevel4MissingAttribute )
							.log( message );
				}
				_skipValue();
				continue;
			}
			// --- Next token must be the value. Delay target instantiation
			token = _tokenizer.getToken();
			if ( token.symbol == Symbol.LEFT_BRACKET || token.symbol == Symbol.LEFT_BRACE ) {
				value = _decodeObjectValue( target, member );
			} else {
				value = _decodeValue( null, null, null );
			}
			_setValue( target, member, value );
		}
		return target;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private Object _decodeObjectValue( Object target,
			Member member ) throws IllegalAccessException, ParseException {
		Object newTarget;
		Type valueType = null, keyType = null, type = null;

		// --- Determine target type
		if ( member instanceof Field ) {
			type = ((Field) member).getGenericType();
			((Field) member).setAccessible( true );
			newTarget = ((Field) member).get( target );
		} else {
			type = ((Method) member).getGenericReturnType();
			newTarget = BeanHelper.getValue( target, ((Method) member) );
		}

		// --- Generic type
		if ( type instanceof ParameterizedType pType ) {
			String rawTypeName = pType.getRawType()
					.getTypeName();
			if ( Collection.class.getName()
					.equals( rawTypeName ) || List.class.getName()
					.equals( rawTypeName ) ) {
				if ( newTarget == null ) {
					newTarget = new ArrayList<>();
				}
				valueType = ReflectionHelper.getActualTypeArgument( pType, 0 );
			} else if ( Set.class.getName()
					.equals( rawTypeName ) ) {
				if ( newTarget == null ) {
					newTarget = new HashSet<>();
				}
				valueType = ReflectionHelper.getActualTypeArgument( pType, 0 );
			} else if ( Map.class.getName()
					.equals( rawTypeName ) ) {
				if ( newTarget == null ) {
					newTarget = new HashMap<>();
				}
				keyType = ReflectionHelper.getActualTypeArgument( pType, 0 );
				valueType = ReflectionHelper.getActualTypeArgument( pType, 1 );
			} else if ( EnumMap.class.getName()
					.contentEquals( rawTypeName ) ) {
				keyType = ReflectionHelper.getActualTypeArgument( pType, 0 );
				valueType = ReflectionHelper.getActualTypeArgument( pType, 1 );
				if ( newTarget == null ) {
					newTarget = new EnumMap( (Class<?>) keyType );
				}
			}
		}
		if ( newTarget == null ) {
			valueType = type;
		}
		return _decodeValue( newTarget, keyType, valueType );
	}

	/**
	 * Analyzes the value from the current token.
	 * <p>
	 * The returned object can be:
	 * <p>
	 * <ul>
	 * <li>Boolean</li>
	 * <li>Byte, Short, Integer and Long</li>
	 * <li>String</li>
	 * <li>Object</li>
	 * </ul>
	 * an array of these types or a complete new map.
	 * </p>
	 *
	 * @param target target object
	 * @param keyType type of key in a map ({@code null} if target is no map)
	 * @param valueType type of value
	 * @return value from decoded JSON string
	 * @throws IllegalAccessException on target access error
	 * @throws ParseException on json format error
	 */
	private Object _decodeValue( Object target, Type keyType,
			Type valueType ) throws IllegalAccessException, ParseException {
		Object retval;
		Token token = _tokenizer.getToken();

		//--- Handle class annotation for value
		if ( valueType != null ) {
			Json classAnno = ReflectionHelper.getRawClass( valueType )
					.getAnnotation( Json.class );
			if ( classAnno != null ) {
				Object object = _handleClassAnnotation(
						ReflectionHelper.createInstanceFromType( valueType ) );
				if ( object != null ) {
					return object;
				}
			}
		}

		if ( token.symbol == Symbol.LEFT_BRACKET ) { // JSON List
			if ( target != null && target.getClass().isArray() ) {
				List<?> list = BaseConverter.arrayToList( (Object[]) target );
				if ( valueType == null ) {
					valueType = target.getClass().getComponentType();
				}
				retval = _decodeList( list, valueType );
				retval = BaseConverter.listToArray( list );
			} else {
				retval = _decodeList( (Collection<?>) target, valueType );
			}
		} else if ( token.symbol == Symbol.LEFT_BRACE ) { // JSON Object
			retval = _decodeObject( target, keyType, valueType );
		} else if ( token.symbol == Symbol.WORD ) {
			String name = token.value;
			if ( name.equalsIgnoreCase( "false" ) ) {
				retval = Boolean.FALSE;
			} else if ( name.equalsIgnoreCase( "null" ) ) {
				retval = null;
			} else if ( name.equalsIgnoreCase( "true" ) ) {
				retval = Boolean.TRUE;
			} else {
				String s = _makeErrorInfo( "Unknown value: '" + name + "'" );
				throw new ParseException( s, _tokenizer.getPosition() );
			}
		}

		// --- Value is an integer
		else if ( token.symbol == Symbol.VALUE_INTEGER ) {
			retval = token.getAsLong();
		}

		// --- Value is hexadecimal
		else if ( token.symbol == Symbol.VALUE_HEX ) {
			retval = TextHelper.parseHex( token.value );
		}

		// --- Value is a decimal or double
		else if ( token.symbol == Symbol.VALUE_DECIMAL ) {
			retval = Double.parseDouble( token.value );
		}

		// --- Value is a decimal
		else if ( token.symbol == Symbol.VALUE_DOUBLE ) {
			retval = token.getAsDouble();
		}

		// --- Value is a string
		else if ( token.symbol == Symbol.STRING ) {
			retval = token.value;
		} else {
			String s = _makeErrorInfo( "Unknown token " + token );
			throw new ParseException( s, _tokenizer.getPosition() );
		}
		return retval;
	}

	/**
	 * Cuts out JSON object from tokenizer current position until matching character
	 *
	 * @return substring of JSON text
	 */
	private String _cutOutUntilMatching() {
		Token token = _tokenizer.getToken();
		if ( token.symbol == Symbol.STRING ) {
			return token.value;
		}
		token = _tokenizer.clipUntilMatching();
		return token.value;
	}

	/**
	 * Handles a target class which is annotated with {@literal @Json}.
	 * <p>
	 * If {@literal @Json} specifies a converter then the current section from the
	 * tokenizer
	 * will be cut out and given to converters decode method.
	 * </p><p>
	 * If the class contains a method named {@code fromJson(String)} then the current
	 * section from the tokenizer will be cut out and given to that method.
	 * </p>
	 *
	 * @param target current bean
	 * @return value from converter or {@code fromJson} or {@code null} if none exists
	 */
	@SuppressWarnings("rawtypes")
	private Object _handleClassAnnotation( Object target ) {
		// --- token must be a string
		Token token = _tokenizer.getToken();
		if ( token.symbol != Symbol.STRING ) {
			return null;
		}

		// --- Check if class is annotated with @Json
		if ( target == null ) {
			return null;
		}
		Json classAnno = target.getClass().getAnnotation( Json.class );
		if ( classAnno == null ) {
			return null;
		}

		// --- use converter if given
		JsonConverter converter = Jetson.getConverter( classAnno );
		if ( converter != null ) {
			return converter.decodeFromJson( token.value );
		}

		//--- use method "fromJson()" if it exists in POJO class
		Method fromJson = ReflectionHelper.findMethod( target.getClass(), METHOD_FROM_JSON );
		if ( fromJson != null ) {
			try {
				return fromJson.invoke( target, token.value );
			} catch ( IllegalAccessException | InvocationTargetException e ) {
				logger.atWarning().withCause( e )
						.log( "Cannot invoke %s.fromJson( %s )", target.getClass().getName(),
								token.value );
			}
		}
		return null;
	}

	/**
	 * Creates a new string starting with the given text. Followed by information about the
	 * position of the error in the Json string and its first 100 chars.
	 *
	 * @param prefix leading string
	 * @return error info
	 */
	private String _makeErrorInfo( String prefix ) {
		String jsonPart = _tokenizer.getText();
		int len = jsonPart.length();
		jsonPart = jsonPart.substring( 0, (Math.min( len, 100 )) );
		return prefix + " near " + _tokenizer.getClipping() + " at " + _tokenizer.getPosition() + " in Json " + jsonPart;
	}

	/**
	 * Obtains all fields within the given target which are annotated with
	 * {@link com.djarjo.jetson.Json @Json}.
	 *
	 * @param target The target object
	 * @return Map with Json names and members (fields and getters) as targets for Json
	 * values
	 */
	private Map<String, Member> _obtainMembers( Object target ) {
		Map<String, Member> members = new HashMap<>();

		// --- Obtain fields
		for ( Field field : target.getClass()
				.getFields() ) {
			Json anno = field.getAnnotation( Json.class );
			if ( anno != null ) {
				String name = anno.name();
				if ( name.equals( Json.defaultName ) ) {
					name = field.getName();
				}
				members.put( name, field );
			}
		}

		// --- Obtain methods
		for ( Method method : target.getClass()
				.getMethods() ) {
			Json anno = method.getAnnotation( Json.class );
			if ( anno != null ) {
				String name = anno.name();
				if ( name.equals( Json.defaultName ) ) {
					name = ReflectionHelper.getVarNameFromMethodName( method.getName() );
				}
				members.put( name, method );
			}
		}
		return members;
	}

	/**
	 * Parses the JSON string within a map or an object. Returns {@code null} when finding
	 * the closing "}". Skips ",". Parses {@code name} (which will be returned). Skips ":".
	 * Parses "value" without evaluating it.
	 *
	 * @return name or {@code null} if Json element is "}"
	 */
	private String _obtainNameFromJson() {
		Token token = _tokenizer.nextToken();

		// --- '}' ends parsing
		if ( token.symbol == Symbol.RIGHT_BRACE ) {
			return null;
		}

		// --- Skip comma ',' as separator between elements
		if ( token.symbol == Symbol.COMMA ) {
			token = _tokenizer.nextToken();
		}
		if ( token.symbol == Symbol.RIGHT_BRACE ) {
			return null; // --- Safely skip , in front of }
		}

		// --- "name" expected
		if ( (token.symbol != Symbol.STRING) && (token.symbol != Symbol.WORD) ) {
			throw new JsonParseException(
					_makeErrorInfo( "Syntax error. Name or '}' expected" ) );
		}
		String name = token.value;

		// --- ':' expected
		token = _tokenizer.nextToken();
		if ( token.symbol != Symbol.COLON ) {
			throw new JsonParseException(
					_makeErrorInfo( "Syntax error. Colon ':' expected" ) );
		}

		// --- Next token must be the value.
		token = _tokenizer.nextToken();
		return name;
	}

	/**
	 * Sets the given value into the {@code member} of object {@code target}. If the Json
	 * annotation at {@code member} contains {@code converter=SomeClass} this converter
	 * will
	 * be used to decode the Json string into the value for the target.
	 *
	 * @param target t
	 * @param member m
	 * @param value v
	 * @throws IllegalAccessException ex
	 */
	@SuppressWarnings("unchecked")
	private void _setValue( Object target, Member member,
			Object value ) throws IllegalAccessException {

		Json anno = (member instanceof Field) ?
				((Field) member).getAnnotation( Json.class ) :
				((Method) member).getAnnotation( Json.class );

		if ( anno.decodable() ) {
			// --- Use "converter" if present
			if ( anno.converter() != JsonConverter.class ) {
				value = _useConverterToDecode( anno, value );
			} else if ( ReflectionHelper.isEnum( member ) ) {
				String accessor = anno.enumAccessor();
				if ( !accessor.equals( Json.defaultName ) ) {
					Class<? extends Enum> enumClass = (Class<? extends Enum>)
							ReflectionHelper.getType( member );
					value = TextHelper.findEnum( value, enumClass, null, accessor );
				}
			}

			// --- Set value into field or use setter method
			ReflectionHelper.setValue( target, member, 0, value );
		}
	}

	/**
	 * Advance tokenizer to end of json collection: "]"
	 */
	private void _skipCollection() {
		_tokenizer.clipUntilSymbol( Symbol.RIGHT_BRACKET );
	}

	/**
	 * Advance tokenizer to end of json map: "}"
	 */
	private void _skipMap() {
		_tokenizer.clipUntilSymbol( Symbol.RIGHT_BRACE );
	}

	/**
	 * Skips the current Json value without using it
	 */
	private void _skipValue() {
		Token token = _tokenizer.getToken();
		if ( token.symbol == Symbol.LEFT_BRACKET ) {
			_skipCollection();
		} else if ( token.symbol == Symbol.LEFT_BRACE ) {
			_skipMap();
		}
	}

	@SuppressWarnings("rawtypes")
	private Object _useConverterToDecode( Json anno,
			Object value ) throws IllegalAccessException {
		if ( (anno != null) && (value != null) && (anno.converter() != JsonConverter.class) ) {
			Class<? extends JsonConverter> annotatedConverter = anno.converter();
			try {
				JsonConverter converter = annotatedConverter.getDeclaredConstructor()
						.newInstance();
				value = converter.decodeFromJson( (String) value );
			} catch ( InstantiationException | IllegalArgumentException |
								InvocationTargetException | NoSuchMethodException |
								SecurityException e ) {
				IllegalAccessException ex = new IllegalAccessException( _makeErrorInfo(
						"Cannot "
								+ "instantiate converter: " + annotatedConverter ) );
				ex.initCause( e );
				throw ex;
			}
		}
		return value;
	}
}