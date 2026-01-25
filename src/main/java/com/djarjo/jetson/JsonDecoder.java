/**
 *
 */
package com.djarjo.jetson;

import com.djarjo.common.BaseConverter;
import com.djarjo.common.ReflectionHelper;
import com.djarjo.jetson.converter.JsonConverter;
import com.djarjo.text.*;
import com.google.common.flogger.FluentLogger;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;

/**
 * Decodes a JSON string into a target object.
 *
 * @author Hajo Lemcke
 * @see <a href="http://www.json.org/">Json</a>
 * @since 2019-06-03 decoding into collection will replace existing value
 * @since 2020-05-19 decode enumeration value auto-strips leading class name if present
 */
public class JsonDecoder {
	/**
	 * Name of optional method in class
	 */
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
	 * Decodes the given JSON string. Finding a "{" will automatically create a new
	 * HashMap.
	 * Finding a "[" will automatically create a new ArrayList. Any other value will
	 * just be
	 * returned.
	 *
	 * @param jsonString JSON string to be decoded
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
		return (result != null) ? result : _decodeJsonValue( target, null );
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
				TokenizerOption.SKIP_LINEBREAK,
				TokenizerOption.SKIP_WHITESPACE );
		Token token = _tokenizer.nextToken();
		if ( token.symbol != Symbol.LEFT_BRACKET ) {
			String s = _makeErrorInfo( "Syntax error. '[' expected to parse a list" );
			throw new ParseException( s, 0 );
		}
		return (List<T>) _decodeJsonList( new ArrayList<T>(), itemClass );
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
	 * Merges items from JSON string into an existing collection instead of replacing the
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
	 * Decodes a JSON string starting with "[". Returns the given {@code target} or creates
	 * a new ArrayList filled with values from the JSON string until "]". Values in the
	 * JSON
	 * array must be basic types or {@code valueType} must be given.
	 *
	 * @param target The target object for the values in the collection
	 * @param valueType type of the values (required if not a basic type)
	 * @return given {@code target} or a new {@code ArrayList}
	 * @throws IllegalAccessException what it says
	 * @throws ParseException what it says
	 */
	private Object _decodeJsonList( Object target, Type valueType )
			throws IllegalAccessException, ParseException {

		//--- Build a new list or clear current one
		List<Object> tempValues = new ArrayList<>();
		Type itemType = ReflectionHelper.getGenericInnerType( valueType );

		while ( true ) {
			Token token = _tokenizer.nextToken();

			// --- Safely skip comma even if in front of bracket
			if ( token.symbol == Symbol.COMMA ) token = _tokenizer.nextToken();
			if ( token.symbol == Symbol.RIGHT_BRACKET ) break;
			if ( token.symbol == Symbol.EOF ) {
				String s = _makeErrorInfo( "Syntax error. Value or ']' expected" );
				throw new ParseException( s, _tokenizer.getPosition() );
			}
			Object value = _decodeJsonValue( null, itemType );
			if ( itemType != null ) {
				value = BaseConverter.convertToType( value, itemType );
			}
			tempValues.add( value );
		}
		Class<?> rawClass = ReflectionHelper.getRawClass( valueType );

		// CASE A: Target/Type is an Array
		if ( rawClass != null && rawClass.isArray() ) {
			Class<?> componentType = rawClass.getComponentType();
			Object array = Array.newInstance( componentType, tempValues.size() );
			for ( int i = 0; i < tempValues.size(); i++ ) {
				Array.set( array, i,
						BaseConverter.convertToType( tempValues.get( i ), componentType ) );
			}
			return array;
		}

		// CASE B: Target/Type is a Collection
		Collection<Object> collectionTarget = (target instanceof Collection)
				? (Collection<Object>) target
				: new ArrayList<>();

		if ( !_mergeCollections ) collectionTarget.clear();
		collectionTarget.addAll( tempValues );
		return collectionTarget;
	}

	/**
	 * Decodes a JSON string which has started with "{". Decoding ends when finding the
	 * matching "}". Entries are decoded in between. Each entry looks like
	 *
	 * @param target The target object for the values
	 * @param targetType type if target is null
	 * @return target or new map filled from JSON string
	 * @throws IllegalAccessException if access to an object field or setter fails
	 * @throws ParseException if json string has a format error
	 */
	@SuppressWarnings("unchecked")
	private Object _decodeJsonObject( Object target, Type targetType )
			throws IllegalAccessException, ParseException {

		// --- New target if none given
		if ( target == null ) {
			target = (targetType == null) ? new HashMap<>() : ReflectionHelper.createInstance(
					targetType );
		}

		// --- Loop through JSON object until "}"
		while ( true ) {
			Token token = _tokenizer.nextToken();
			if ( token.symbol == Symbol.COMMA ) token = _tokenizer.nextToken();
			if ( token.symbol == Symbol.RIGHT_BRACE ) break;

			//--- get key and consume ":"
			String key = _obtainNameFromJson();

			// --- Next token must be the value. Delay target instantiation
			if ( target instanceof Map map ) {
				map.put( key, _decodeJsonValue( null, null ) );
				continue;
			}

			//--- Check if there is an accessor
			JsonAccessor accessor = JsonCache.findAccessorByName( target.getClass(), key );
			if ( accessor == null ) {
				if ( (_logLevel4MissingAttribute != Level.OFF) && (!_keysToSkip.contains(
						key )) ) {
					String message = _makeErrorInfo(
							"Name '" + key + "' does not exist in " + target.getClass() );
					logger.at( _logLevel4MissingAttribute ).log( message );
				}
				_skipValue();
				continue;
			}
			// RECURSION: Pass current value of field so nested lists/beans are reused
			Object currentValue = accessor.getValue( target );
			Object decodedValue = _decodeJsonValue( currentValue, accessor.getType() );

			// BaseConverter handles String -> UUID conversion here
			accessor.setValue( target, decodedValue );
		}
		return target;
	}

	/**
	 * Analyzes the value from the current token and acts accordingly.
	 *
	 * @param target target object or null
	 * @param targetType type of target
	 * @return value from decoded JSON string
	 * @throws IllegalAccessException on target access error
	 * @throws ParseException on json format error
	 */
	@SuppressWarnings("unchecked")
	private Object _decodeJsonValue( Object target, Type targetType )
			throws IllegalAccessException, ParseException {
		Token token = _tokenizer.getToken();

		//--- 1. Check Class-level @Json annotation
		Class<?> rawClass = (target != null) ? target.getClass()
				: ReflectionHelper.getRawClass( targetType );
		if ( (rawClass != null) && rawClass.isAnnotationPresent( Json.class ) ) {
			Json jsonAnno = rawClass.getAnnotation( Json.class );
			if ( jsonAnno != null ) {
				Object object = _handleClassAnnotation(
						ReflectionHelper.createInstance( targetType ) );
				if ( object != null ) {
					return object;
				}
			}
		}

		return switch ( token.symbol ) {
			case LEFT_BRACE -> _decodeJsonObject( target, targetType );
			case LEFT_BRACKET -> _decodeJsonList( target, targetType );
			case STRING -> token.value;
			case VALUE_DECIMAL -> Double.parseDouble( token.value );
			case VALUE_DOUBLE -> token.getAsDouble();
			case VALUE_HEX -> TextHelper.parseHex( token.value );
			case VALUE_INTEGER -> token.getAsLong();
			case WORD -> _decodeJsonWord( token );
			default -> {
				String s = _makeErrorInfo( "Unknown token " + token );
				throw new ParseException( s, _tokenizer.getPosition() );
			}
		};
	}

	private Object _decodeJsonWord( Token token ) throws ParseException {
		String name = token.value;
		if ( name.equalsIgnoreCase( "false" ) ) return Boolean.FALSE;
		if ( name.equalsIgnoreCase( "null" ) ) return null;
		if ( name.equalsIgnoreCase( "true" ) ) return Boolean.TRUE;
		String s = _makeErrorInfo( "Unknown value: '" + name + "'" );
		throw new ParseException( s, _tokenizer.getPosition() );
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
	 * position of the error in the JSON string and its first 100 chars.
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
	 * Parses the JSON name in a JSON object, consumes following colon (':') and value.
	 *
	 * @return name
	 */
	private String _obtainNameFromJson() {
		Token token = _tokenizer.getToken();

		// --- "name" expected or just name if JSON5
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
		_tokenizer.nextToken();
		return name;
	}

	/**
	 * Advance tokenizer to end of JSON collection: "]"
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
	 * Skips the current JSON value without using it
	 */
	private void _skipValue() {
		Token token = _tokenizer.getToken();
		if ( token.symbol == Symbol.LEFT_BRACKET ) {
			_skipCollection();
		} else if ( token.symbol == Symbol.LEFT_BRACE ) {
			_skipMap();
		}
	}
}