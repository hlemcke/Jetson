/**
 *
 */
package com.djarjo.jetson;

import com.djarjo.common.BaseConverter;
import com.djarjo.common.BeanHelper;
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
 * @since 2020-05-19 decode enumeration value auto-strips leading class name if
 * present
 */
public class JsonDecoder {
	private final static FluentLogger logger = FluentLogger.forEnclosingClass();

	private Level _logLevel4MissingAttribute = Level.OFF;

	private Set<String> _keysToSkip = null;

	private Tokenizer _tokenizer = null;

	/**
	 * Empty no argument constructor.
	 */
	public JsonDecoder() {
	}

	/**
	 * Instantiates and returns a new instance of this class to be used in
	 * streaming method calls. This method only exists because Java cannot call
	 * a method during instantiation.
	 *
	 * @return new JsonDecoder()
	 */
	public static JsonDecoder decoder() {
		return new JsonDecoder();
	}

	/**
	 * Decodes the given Json string. Finding a "{" will automatically create a
	 * new HashMap. Finding a "[" will automatically create a new ArrayList. Any
	 * other value will just be returned.
	 *
	 * @param jsonString Json string to be decoded
	 * @return a new list or a new map
	 * @throws IllegalAccessException if a decoded value cannot be set into the target object
	 * @throws ParseException         in case of an error in the Json string
	 */
	public Object decode(String jsonString)
		  throws IllegalAccessException, ParseException {
		return decode(jsonString, null);
	}

	/**
	 * Decodes the given Json string and writes its values into the given target
	 * object.
	 * <p>
	 * Fields (getters) in target class must be annotated with
	 * {@link com.djarjo.jetson.Json @Json}.
	 * </p>
	 *
	 * @param jsonString Json string to decode
	 * @param target     target object for the values from the Json string
	 * @return given target or new instance of list or map
	 * @throws IllegalAccessException if a decoded value cannot be set into the target object
	 * @throws ParseException         in case of an error in the Json string
	 */
	public Object decode(String jsonString, Object target)
		  throws ParseException, IllegalAccessException {
		if (jsonString == null || jsonString.isEmpty()) {
			return null;
		}
		_tokenizer = new Tokenizer(jsonString,
			  TokenizerOption.SKIP_DOUBLESLASH_UNTIL_EOL,
			  TokenizerOption.SKIP_LINEBREAK,
			  TokenizerOption.SKIP_WHITESPACE);
		_tokenizer.nextToken();
		return _decodeValue(target, null, null);
	}

	/**
	 * Decodes the given {@code jsonString} into a "List&lt;itemClass&gt;".
	 *
	 * @param <T>        Type of collection
	 * @param jsonString must start with "["
	 * @param itemClass  Type of elements in list
	 * @return new list
	 * @throws ParseException         if not a Json list
	 * @throws IllegalAccessException if field in list items not accessible
	 */
	@SuppressWarnings("unchecked")
	public <T extends Object> Collection<T> decodeList(String jsonString,
	                                                   Class<T> itemClass) throws ParseException, IllegalAccessException {
		if (jsonString == null || jsonString.isEmpty()) {
			return null;
		}
		_tokenizer = new Tokenizer(jsonString,
			  TokenizerOption.SKIP_DOUBLESLASH_UNTIL_EOL,
			  TokenizerOption.SKIP_LINEBREAK,
			  TokenizerOption.SKIP_WHITESPACE);
		Token token = _tokenizer.nextToken();
		if (token.symbol != Symbol.LEFT_BRACKET) {
			String s = _makeErrorInfo(
				  "Syntax error. '[' expected to parse a list");
			throw new ParseException(s, 0);
		}
		return _decodeList(null, itemClass);
	}

	/**
	 * If smaller than Level.OFF then a key in the Json string which has no
	 * matching setter in the target bean will be logged with that level.
	 *
	 * @return Level
	 */
	public Level getLogLevel4MissingAttribute() {
		return _logLevel4MissingAttribute;
	}

	/**
	 * Sets log level to reports a missing setter (defaults to OFF).
	 *
	 * @param level Level to use for reporting
	 * @return this for fluent API
	 */
	public JsonDecoder setLogLevel4MissingAttribute(Level level) {
		this._logLevel4MissingAttribute = level;
		return this;
	}

	/**
	 * Gets keys to be skipped on missing key action.
	 *
	 * @return keys to be skipped
	 */
	public Set<String> getKeysToSkip() {
		if (_keysToSkip == null) {
			_keysToSkip = new HashSet<>();
		}
		return _keysToSkip;
	}

	/**
	 * Add keys to be skipped if they do not exist in target object.
	 * <p>
	 * Only used if logLevel4MissingAttribute is not OFF
	 *
	 * @param keys one or more keys to be skipped
	 * @return this for fluent API
	 */
	public JsonDecoder withKeysToSkip(String... keys) {
		getKeysToSkip();
		Collections.addAll(_keysToSkip, keys);
		return this;
	}

	/**
	 * Clones the given collection into a new {@code ArrayList}. Used for
	 * decoding {@code json} into an existing collection.
	 *
	 * @param toBeCloned Collection to be cloned
	 * @return ArrayList with items from given collection
	 */
	private Collection<?> _cloneCollection(Collection<Object> toBeCloned) {
		return new ArrayList<>(toBeCloned);
	}

	/**
	 * Decodes a Json string starting with "[". Returns the given {@code target}
	 * or a new ArrayList filled with values from the Json string until "]".
	 * Values in the Json array must be basic types or {@code valueType} must be
	 * given.
	 *
	 * @param target    The target object for the values in the collection
	 * @param valueType type of the values (required if not a basic type)
	 * @return given {@code target} or a new {@code ArrayList}
	 * @throws IllegalAccessException what it says
	 * @throws ParseException         what it says
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private Collection _decodeList(Collection target, Type valueType)
		  throws IllegalAccessException, ParseException {
		Token token = null;
		Collection targetClone = null;
		if (target == null) {
			target = new ArrayList<>();
		} else {
			targetClone = _cloneCollection(target);
		}
		while (true) {
			token = _tokenizer.nextToken();
			if (token.symbol == Symbol.COMMA) {
				token = _tokenizer.nextToken();
			}
			if (token.symbol == Symbol.RIGHT_BRACKET) {
				break; // --- Safely skip , in front of }
			}
			if (token.symbol == Symbol.EOF) {
				String s =
					  _makeErrorInfo("Syntax error. Value or ']' expected");
				throw new ParseException(s, _tokenizer.getPosition());
			}
			Object value = _decodeValue(null, null, valueType);
			if (valueType != null) {
				value = BaseConverter.convertToType(value,
					  (Class<?>) valueType);
			}
			// --- Replace existing items in given target
			if (targetClone != null) {
				target.remove(value); // remove before adding
				targetClone.remove(value); // only orphans will remain
			}
			target.add(value);
		}
		// --- Remove orphans in given target
		if (targetClone != null && !targetClone.isEmpty()) {
			Iterator<Object> iter = targetClone.iterator();
			while (iter.hasNext()) {
				target.remove(iter.next());
			}
		}
		return target;
	}

	/**
	 * Decodes the Json string for a map. String has started with "{". Map
	 * elements are encoded {@code name ":" value}. Elements are separated by a
	 * comma. Values can be of basic type, LocalDate, OffsetDateTime, UUID or
	 * enumeration.
	 *
	 * @param target    map to be filled. {@code null} will create a new
	 *                  {@code HashMap}
	 * @param keyType   Type of the key. Can be {@code null} for basic types
	 * @param valueType Type of the value. Can be {@code null} for basic types
	 * @return given {@code target} or a new instance of {@code HashMap}
	 * @throws ParseException
	 * @throws IllegalAccessException
	 */
	private Map<Object, Object> _decodeMap(Tokenizer tokenizer,
	                                       Map<Object, Object> target, Type keyType, Type valueType)
		  throws ParseException, IllegalAccessException {
		Object key = null, value = null;
		if (target == null) {
			target = new HashMap<>();
		}
		while (true) {
			key = _obtainNameFromJson();
			if (key == null) {
				break;
			}
			if (keyType != null) {
				key = BaseConverter.convertToType(key, (Class<?>) keyType);
			}
			tokenizer.getToken();
			value = _decodeValue(null, null, valueType);
			target.put(key, value);
		}
		return target;
	}

	/**
	 * Decodes a Json string which has started with "{". Decoding ends when
	 * finding the matching "}". Elements are decoded in between. Each element
	 * looks like
	 *
	 * <pre> * "name" : value
	 * </pre>
	 * <p>
	 * Elements are separated by comma. For values see
	 * {@link #decodeValue(Object)}. If a {@code name} does not exist in the
	 * target object then it will handled as set by
	 * {@link #setMissingNameAction(ACTION_ON_ERROR)}.
	 *
	 * @param target    The target object for the values
	 * @param keyType   type of key for a map
	 * @param valueType generic type of a map or collection
	 * @return target or new map filled from Json string
	 * @throws IllegalAccessException
	 * @throws ParseException
	 */
	@SuppressWarnings("unchecked")
	private Object _decodeObject(Object target, Type keyType, Type valueType)
		  throws IllegalAccessException, ParseException {
		Map<String, Member> members = null;
		String name = null;
		Token token = null;
		Object value = null;

		// --- New target if none given
		if (target == null) {
			if (valueType == null || valueType == Object.class) {
				return _decodeMap(_tokenizer, null, null, valueType);
			}
			target = _getInstanceFromGeneric(valueType);
		}

		// --- target is a map or a class with fields and getters
		if (Map.class.isAssignableFrom(target.getClass())) {
			return _decodeMap(_tokenizer, (Map<Object, Object>) target,
				  keyType, valueType);
		}
		members = _obtainMembers(target);

		// --- Loop through Json object until "}"
		while (true) {
			name = _obtainNameFromJson();
			if (name == null) {
				break;
			}
			Member member = members.get(name);
			if (member == null) {
				if ((_logLevel4MissingAttribute != Level.OFF)
					  && (!_keysToSkip.contains(name))) {
					String message = _makeErrorInfo("Name '" + name
						  + "' does not exist in " + target.getClass());
					logger.at(_logLevel4MissingAttribute)
						  .log(message);
				}
				_skipValue();
				continue;
			}

			// --- Next token must be the value. Delay target instantiation
			token = _tokenizer.getToken();
			if (token.symbol == Symbol.LEFT_BRACKET
				  || token.symbol == Symbol.LEFT_BRACE) {
				value = _decodeObjectValue(target, member);
			} else {
				value = _decodeValue(null, null, null);
			}
			_setValue(value, target, member);
		}
		return target;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private Object _decodeObjectValue(Object target, Member member)
		  throws IllegalAccessException, ParseException {
		Object newTarget = null;
		Type valueType = null, keyType = null, type = null;

		// --- Determine target type
		if (member instanceof Field) {
			type = ((Field) member).getGenericType();
			((Field) member).setAccessible(true);
			newTarget = ((Field) member).get(target);
		} else {
			type = ((Method) member).getGenericReturnType();
			newTarget = BeanHelper.getValue(target, ((Method) member));
		}

		// --- Generic type
		if (type instanceof ParameterizedType pType) {
			String rawTypeName = pType.getRawType()
				  .getTypeName();
			if (Collection.class.getName()
				  .equals(rawTypeName)
				  || List.class.getName()
				  .equals(rawTypeName)) {
				if (newTarget == null) {
					newTarget = new ArrayList<>();
				}
				valueType = _getActualTypeArgument(pType, 0);
			} else if (Set.class.getName()
				  .equals(rawTypeName)) {
				if (newTarget == null) {
					newTarget = new HashSet<>();
				}
				valueType = _getActualTypeArgument(pType, 0);
			} else if (Map.class.getName()
				  .equals(rawTypeName)) {
				if (newTarget == null) {
					newTarget = new HashMap<>();
				}
				keyType = _getActualTypeArgument(pType, 0);
				valueType = _getActualTypeArgument(pType, 1);
			} else if (EnumMap.class.getName()
				  .contentEquals(rawTypeName)) {
				keyType = _getActualTypeArgument(pType, 0);
				valueType = _getActualTypeArgument(pType, 1);
				if (newTarget == null) {
					newTarget = new EnumMap((Class<?>) keyType);
				}
			}
		}
		if (newTarget == null) {
			valueType = type;
		}
		return _decodeValue(newTarget, keyType, valueType);
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
	 * @param target    target object
	 * @param keyType   type of key in a map ({@code null} if target is no map)
	 * @param valueType type of value
	 * @return value value from decoded Json string
	 * @throws IllegalAccessException
	 * @throws ParseException
	 */
	private Object _decodeValue(Object target, Type keyType, Type valueType)
		  throws IllegalAccessException, ParseException {
		Object retval = null;
		Token token = _tokenizer.getToken();

		if (token.symbol == Symbol.LEFT_BRACKET) { // Json List
			retval = _decodeList((Collection<?>) target, valueType);
		} else if (token.symbol == Symbol.LEFT_BRACE) { // --- Json Object
			retval = _decodeObject(target, keyType, valueType);
		} else if (token.symbol == Symbol.WORD) {
			String name = token.value;
			if (name.equalsIgnoreCase("false")) {
				retval = Boolean.FALSE;
			} else if (name.equalsIgnoreCase("null")) {
				retval = null;
			} else if (name.equalsIgnoreCase("true")) {
				retval = Boolean.TRUE;
			} else {
				String s = _makeErrorInfo("Unknown value: '" + name + "'");
				throw new ParseException(s, _tokenizer.getPosition());
			}
		}

		// --- Value is an integer
		else if (token.symbol == Symbol.VALUE_INTEGER) {
			retval = token.getAsLong();
		}

		// --- Value is hexadecimal
		else if (token.symbol == Symbol.VALUE_HEX) {
			retval = TextHelper.parseHex(token.value);
		}

		// --- Value is a decimal or double
		else if (token.symbol == Symbol.VALUE_DECIMAL) {
			retval = Double.parseDouble(token.value);
		}

		// --- Value is a decimal
		else if (token.symbol == Symbol.VALUE_DOUBLE) {
			retval = token.getAsDouble();
		}

		// --- Value is a string
		else if (token.symbol == Symbol.STRING) {
			retval = token.value;
		} else {
			String s = _makeErrorInfo("Unknown token " + token);
			throw new ParseException(s, _tokenizer.getPosition());
		}
		return retval;
	}

	private Type _getActualTypeArgument(ParameterizedType pType, int index) {
		Type generic = null;
		Type[] actualTypeArguments = pType.getActualTypeArguments();
		if (index < actualTypeArguments.length) {
			String typeArgument = actualTypeArguments[index].getTypeName();
			try {
				generic = Thread.currentThread()
					  .getContextClassLoader()
					  .loadClass(typeArgument);
			} catch (ClassNotFoundException | IllegalArgumentException
			         | SecurityException e) {
				logger.atWarning()
					  .withCause(e)
					  .log(_makeErrorInfo(
							"Cannot obtain generic type '" + typeArgument + "'"));
				return null;
			}
		}
		return generic;
	}

	/**
	 * Gets a new instance from the given generic type.
	 *
	 * @param generic the generic type
	 * @return new instance
	 * @throws IllegalAccessException
	 */
	private Object _getInstanceFromGeneric(Type generic)
		  throws IllegalAccessException {
		Object target = null;
		if (!(generic instanceof Class<?> cls)) {
			return null;
		}
		try {
			// Class<?> clazz = Class.forName( generic.getTypeName() );
			target = cls.getDeclaredConstructor()
				  .newInstance();
		} catch (InstantiationException | IllegalArgumentException
		         | InvocationTargetException | NoSuchMethodException
		         | SecurityException e) {
			IllegalAccessException ex = new IllegalAccessException(
				  _makeErrorInfo("Cannot create generic: " + generic));
			ex.initCause(e);
			throw ex;
		}
		return target;
	}

	/**
	 * Creates a new string starting with the given text. Followed by
	 * information about the position of the error in the Json string and its
	 * first 100 chars.
	 *
	 * @param prefix leading string
	 * @return error info
	 */
	private String _makeErrorInfo(String prefix) {
		String jsonPart = _tokenizer.getText();
		int len = jsonPart.length();
		jsonPart = jsonPart.substring(0, (len > 100 ? 100 : len));
		return prefix + " near " + _tokenizer.getClipping() + " at "
			  + _tokenizer.getPosition() + " in Json " + jsonPart;
	}

	/**
	 * Obtains all fields within the given target which are annotated with
	 * {@link com.djarjo.jetson.Json @Json}.
	 *
	 * @param target The target object
	 * @return Map with Json names and members (fields and getters) as targets
	 * for Json values
	 */
	private Map<String, Member> _obtainMembers(Object target) {
		Map<String, Member> members = new HashMap<>();

		// --- Obtain fields
		for (Field field : target.getClass()
			  .getFields()) {
			Json anno = field.getAnnotation(Json.class);
			if (anno != null) {
				String name = anno.key();
				if (name.equals(Json.defaultName)) {
					name = field.getName();
				}
				members.put(name, field);
			}
		}

		// --- Obtain methods
		for (Method method : target.getClass()
			  .getMethods()) {
			Json anno = method.getAnnotation(Json.class);
			if (anno != null) {
				String name = anno.key();
				if (name.equals(Json.defaultName)) {
					name = BeanHelper
						  .getVarnameFromMethodname(method.getName());
				}
				members.put(name, method);
			}
		}
		return members;
	}

	/**
	 * Parses the Json string within a map or an object. Returns {@code null}
	 * when finding the closing "}". Skips ",". Parses {@code name} (which will
	 * be returned). Skips ":". Parses "value" without evaluating it.
	 *
	 * @return name or {@code null} if Json element is "}"
	 */
	private String _obtainNameFromJson() {
		Token token = _tokenizer.nextToken();

		// --- '}' ends parsing
		if (token.symbol == Symbol.RIGHT_BRACE) {
			return null;
		}

		// --- Skip comma ',' as separator between elements
		if (token.symbol == Symbol.COMMA) {
			token = _tokenizer.nextToken();
		}
		if (token.symbol == Symbol.RIGHT_BRACE) {
			return null; // --- Safely skip , in front of }
		}

		// --- "name" expected
		if ((token.symbol != Symbol.STRING)
			  && (token.symbol != Symbol.WORD)) {
			throw new JsonParseException(
				  _makeErrorInfo("Syntax error. Name or '}' expected"));
		}
		String name = token.value;

		// --- ':' expected
		token = _tokenizer.nextToken();
		if (token.symbol != Symbol.COLON) {
			throw new JsonParseException(
				  _makeErrorInfo("Syntax error. Colon ':' expected"));
		}

		// --- Next token must be the value.
		token = _tokenizer.nextToken();
		return name;
	}

	/**
	 * Sets the given value into the {@code member} of object {@code target}. If
	 * the Json annotation at {@code member} contains
	 * {@code converter=SomeClass} this converter will be used to decode the
	 * Json string into the value for the target.
	 *
	 * @param value  v
	 * @param target t
	 * @param member m
	 * @throws IllegalAccessException ex
	 * @throws ParseException         ex
	 */
	private void _setValue(Object value, Object target, Member member)
		  throws IllegalAccessException, ParseException {

		Json anno = (member instanceof Field)
			  ? ((Field) member).getAnnotation(Json.class)
			  : ((Method) member).getAnnotation(Json.class);

		if (anno.decodable()) {
			// --- Use "converter" if present
			if (anno.converter() != JsonConverter.class) {
				value = _useConverterToDecode(anno, value);
			}

			// --- Set value into field or use setter method
			try {
				if (member instanceof Field) {
					((Field) member).setAccessible(true);
					BeanHelper.set(target, (Field) member, value);
				} else {
					Method setter = BeanHelper.obtainSetter((Method) member);
					if (setter == null) {
						logger.atWarning()
							  .log(_makeErrorInfo(
									"No matching setter for " + member));
					} else {
						BeanHelper.set(target, setter, value);
					}
				}
			} catch (IllegalArgumentException | InvocationTargetException e) {
				IllegalAccessException ex = new IllegalAccessException(
					  _makeErrorInfo("Cannot set member: " + member
							+ " to value \"" + value + "\""));
				ex.initCause(e);
				throw ex;
			}
		}
	}

	private void _skipCollection() {
		_tokenizer.clipUntilSymbol(Symbol.RIGHT_BRACKET);
	}

	private void _skipMap() {
		_tokenizer.clipUntilSymbol(Symbol.RIGHT_BRACE);
	}

	/**
	 * Skips the current Json value without using it
	 */
	private void _skipValue() {
		Token token = _tokenizer.getToken();
		if (token.symbol == Symbol.LEFT_BRACKET) {
			_skipCollection();
		} else if (token.symbol == Symbol.LEFT_BRACE) {
			_skipMap();
		}
	}

	@SuppressWarnings("rawtypes")
	private Object _useConverterToDecode(Json anno, Object value)
		  throws IllegalAccessException {
		if ((anno != null) && (value != null)
			  && (anno.converter() != JsonConverter.class)) {
			Class<? extends JsonConverter> annotatedConverter =
				  anno.converter();
			try {
				JsonConverter converter = annotatedConverter
					  .getDeclaredConstructor()
					  .newInstance();
				value = converter.decodeFromJson((String) value);
			} catch (InstantiationException | IllegalArgumentException
			         | InvocationTargetException | NoSuchMethodException
			         | SecurityException e) {
				IllegalAccessException ex = new IllegalAccessException(
					  _makeErrorInfo("Cannot instantiate converter: "
							+ annotatedConverter));
				ex.initCause(e);
				throw ex;
			}
		}
		return value;
	}
}