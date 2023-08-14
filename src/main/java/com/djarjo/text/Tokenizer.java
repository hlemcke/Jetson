package com.djarjo.text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.flogger.FluentLogger;

/**********************************************************************
 * The tokenizer separates a text into tokens. It is a lexical analyzer. Tokens
 * are specified by {@link com.djarjo.text.Symbol Token}. The tokenization
 * process can be modified by setting options with {@link #setOptions(int)}.
 * 
 * <p>
 * Usage:
 * <ul>
 * <li>Instantiate it with the text to be tokenized</li>
 * <li>Get all tokens in a loop with {@link #nextToken()}</li>
 * <li>Get the value of a token with {@link #getValue()}</li>
 * </ul>
 * The tokenizer provides a look-ahead mechanism (see {@link #lookAhead()}).
 * </p>
 * <p>
 * To recover from syntax errors, the method {@link #advanceTo(Symbol...)} can
 * be used.
 * </p>
 * <p>
 * A value might be numeric with optional fraction.
 *
 * @author Hajo Lemcke
 */

public class Tokenizer {

	private final static FluentLogger logger = FluentLogger.forEnclosingClass();

	private int _colNo = 1, _curPos = 0, _lineNo = 1, _textLength = 0;
	private String _text = null;

	/** Tokens from input not fully consumed */
	private int _lookAheadIndex = 0;
	private List<Token> _lookAheadList = new ArrayList<>();

	/** Current options to be used for next token */
	private Set<TokenizerOption> _options = new HashSet<>();

	/** Skipping can be temporarily disabled by {@code clipUntil...} */
	private boolean _skipEnabled = true;

	/** Current token */
	private Token _token = new Token( Symbol.BEGIN );

	/******************************************************************
	 * Constructor sets text with options.
	 * 
	 * @param text
	 *            The text to be tokenized
	 * @param options
	 *            Options which control the tokenization process. See
	 *            {@link #setOptions(int)}
	 */
	public Tokenizer( String text, TokenizerOption... options ) {
		_text = text;
		_textLength = _text.length();
		_options.addAll( List.of( options ) );
	}

	/**
	 * Advances to the token which matches the current one. The current one must
	 * be one of: '(', '{', '[', '<', '/*', '<!--'. The returned token contains
	 * the whole text between current token and its matching counterpart.
	 * 
	 * @return Matching token with text or {@link com.djarjo.text.Symbol#UNKNOWN
	 *         UNKNOWN}.
	 */
	public Token clipUntilMatching() {
		Symbol currentSymbol = _token.symbol;
		Symbol matchingSymbol = _getMatchingSymbol();
		_token = new Token( Symbol.UNKNOWN );
		if ( matchingSymbol == Symbol.UNKNOWN ) {
			return _token;
		}
		int startPos = _curPos, depth = 1;
		while ( true ) {
			_token = nextToken();
			if ( _token.symbol == currentSymbol ) {
				depth++;
			} else if ( _token.symbol == matchingSymbol ) {
				depth--;
				if ( depth <= 0 ) {
					_token.value = _text.substring( startPos,
							_curPos - _token.symbol.getText().length() );
					_token.value = _token.value.replaceAll( "\r", "" );
					break;
				}
			}
		}
		return _token;
	}

	/**
	 * Advances tokenizer to any one of the given symbols. Returns symbol found
	 * with value text from current symbol until found one.
	 * 
	 * @param symbols
	 *            Any of these symbol stops reading
	 * @return token with symbol found
	 */
	public Token clipUntilSymbol( Symbol... symbols ) {
		_skipEnabled = false;
		Set<Symbol> symbolSet = new HashSet<>( List.of( symbols ) );
		symbolSet.add( Symbol.EOF );
		int startPos = _curPos;
		while ( true ) {
			_token = nextToken();
			if ( symbolSet.contains( _token.symbol ) ) {
				_token.value = _text
						.substring( startPos,
								_curPos - _token.symbol.getText().length() )
						.replaceAll( "\r", "" );
				break;
			}
		}
		_skipEnabled = true;
		return _token;
	}

	/**
	 * Gets a clipping of the text which is 50 characters before the current
	 * position and 20 characters behind. The current position is marked with
	 * "&lt;&lt;&lt;"
	 * 
	 * @return a clipping of the text around the current position
	 */
	public String getClipping() {
		int i = _curPos - 50;
		if ( i < 0 ) {
			i = 0;
		}
		int j = _curPos + 20;
		if ( j >= _textLength ) {
			j = _textLength;
		}
		String clip = "..." + _text.substring( i, _curPos ) + "<<<<"
				+ _text.substring( _curPos, j ) + "...";
		return clip;
	}

	/**
	 * Gets the current column number. This is the number of characters since
	 * the last line feed.
	 * 
	 * @return index into current line
	 */
	public int getColno() {
		return _colNo;
	}

	/**
	 * Gets the current line number (starts at 1). This is the number of line
	 * feeds from start to current line plus 1.
	 * 
	 * @return number of line feeds + 1
	 */
	public int getLineno() {
		return _lineNo;
	}

	/**
	 * Gets the current location into the parsed text. If still in first line
	 * then the string "pos #" will be returned. After the first linefeed, the
	 * string "line # col #" will be returned
	 * 
	 * @return current location
	 */
	public String getLocation() {
		String str = null;
		if ( _curPos >= _textLength ) {
			str = "end of text";
		} else if ( _lineNo <= 1 ) {
			str = "pos " + _colNo;
		} else {
			str = "line " + _lineNo + " col " + _colNo;
		}
		return str;
	}

	/**
	 * Gets the position behind the end of the current token.
	 * 
	 * @return Returns index into text behind current token or -1 at end of text
	 */
	public int getNextPosition() {
		return _curPos;
	}

	/**
	 * Gets the current options. Modifying this set will change how the
	 * tokenizer parses the next token.
	 * 
	 * @return current setting of options
	 */
	public Set<TokenizerOption> getOptions() {
		return _options;
	}

	/**
	 * Gets current position into text. -1 indicates end of text.
	 * 
	 * @return index into text or -1 at end of text
	 */
	public int getPosition() {
		if ( _curPos >= _textLength ) {
			return -1;
		}
		return _curPos;
	}

	/**
	 * Gets the complete text which is tokenized.
	 * 
	 * @return Returns the complete text
	 */
	public String getText() {
		return _text;
	}

	/**
	 * Gets current token from the stream
	 * 
	 * @return token or {@code EOF} on end of stream
	 */
	public Token getToken() {
		return _token;
	}

	/******************************************************************
	 * Performs a look ahead to obtain the next token without <i>consuming</i>
	 * it. Multiple calls to this method will return consecutive tokens. Any
	 * call to {@link #nextToken()} clears the look ahead queue and returns the
	 * token from the very first call to lookAheadfollowed by others.
	 * 
	 * @return the next token
	 */
	public Token lookAhead() {
		if ( _lookAheadIndex < _lookAheadList.size() ) {
			_token = _lookAheadList.get( _lookAheadIndex );
		} else {
			_parse();
			_lookAheadList.add( _token );
		}
		_lookAheadIndex++;
		return _token;
	}

	/******************************************************************
	 * Obtains next token and sets value and position.
	 *
	 * @return token or {@code EOF} at end of stream
	 */
	public Token nextToken() {
		if ( _lookAheadList.isEmpty() ) {
			_parse();
		} else {
			_token = _lookAheadList.remove( 0 );
			_lookAheadIndex = 0;
		}
		return _token;
	}

	/**
	 * Gets the current char from the input stream without advancing to the next
	 * one. This is the char from the latest call to {@link #_nextChar()}.
	 * 
	 * @return next character or 0 at end of stream
	 */
	private char _getChar() {
		if ( _curPos >= _textLength ) {
			return 0;
		}
		return _text.charAt( _curPos );
	}

	private Symbol _getMatchingSymbol() {
		Symbol result = Symbol.UNKNOWN;
		if ( _token.symbol == Symbol.LEFT_BRACE ) {
			result = Symbol.RIGHT_BRACE;
		} else if ( _token.symbol == Symbol.LEFT_BRACKET ) {
			result = Symbol.RIGHT_BRACKET;
		} else if ( _token.symbol == Symbol.LEFT_C_COMMENT ) {
			result = Symbol.RIGHT_C_COMMENT;
		} else if ( _token.symbol == Symbol.LEFT_PARENTHESIS ) {
			result = Symbol.RIGHT_PARENTHESIS;
		} else if ( _token.symbol == Symbol.LEFT_XML_COMMENT ) {
			result = Symbol.RIGHT_XML_COMMENT;
		}
		return result;
	}

	/**
	 * Gets the current character from the input stream and advances to the next
	 * one. Increments column counter. If the character is line feed '\n' then
	 * line number will be incremented and the column counter will be set to 1.
	 * Useless Mickisoft char '\r' will be skipped.
	 * 
	 * @return character or 0 at end of stream
	 */
	private char _nextChar() {
		if ( _curPos >= _textLength ) {
			return 0;
		}
		// --- Skip useless Mickisoft returns
		while ( (_curPos < _textLength) && (_text.charAt( _curPos ) == '\r') ) {
			_curPos++;
		}
		char c = _text.charAt( _curPos++ );
		_colNo++;
		if ( c == '\n' ) {
			_lineNo++;
			_colNo = 1;
		}
		// --- Skip useless Mickisoft returns
		while ( (_curPos < _textLength) && (_text.charAt( _curPos ) == '\r') ) {
			_curPos++;
		}
		return c;
	}

	/******************************************************************
	 * Parses the input stream to obtain the next token. Sets value and
	 * position. Uses options as set by {@link #setOptions(int)}.
	 * 
	 * @return token or {@code null} at end of stream
	 */
	private void _parse() {
		_token = new Token( Symbol.EOF );
		if ( _curPos >= _textLength ) {
			return;
		}
		char chr = _nextChar();
		// --- Check for skipping LF and / or whitespaces
		while ( chr != 0 ) {
			if ( chr == '\n' ) {
				if ( _skipEnabled && _options
						.contains( TokenizerOption.SKIP_LINEBREAK ) ) {
					chr = _nextChar();
				} else {
					_token.symbol = Symbol.LINEBREAK;
					return;
				}
			} else if ( Character.isWhitespace( chr ) ) {
				if ( _skipEnabled && _options
						.contains( TokenizerOption.SKIP_WHITESPACE ) ) {
					chr = _nextChar();
				} else {
					_token.symbol = Symbol.WHITESPACE;
					return;
				}
			} else if ( chr == '\r' ) { // Skip useless CR
				chr = _nextChar();
			} else {
				break;
			}
		}
		if ( chr == 0 ) {
			return;
		}
		_token.symbol = Symbol.UNKNOWN; // Default
		_token.value = "" + chr;

		if ( Character.isDigit( chr ) ) {
			_parseNumber();
		} else if ( Character.isLetter( chr ) ) {
			_parseWord();
		} else if ( chr == '\"' || chr == '\'' ) {
			_parseString( chr );
		} else if ( chr == '&' ) {
			_token.symbol = Symbol.AMPERSAND;
		} else if ( chr == ':' ) {
			_token.symbol = Symbol.COLON;
		} else if ( chr == ',' ) {
			_token.symbol = Symbol.COMMA;
		} else if ( chr == '$' ) {
			_token.symbol = Symbol.DOLLAR;
		} else if ( chr == '#' ) {
			_token.symbol = Symbol.HASH;
		} else if ( chr == '{' ) {
			_token.symbol = Symbol.LEFT_BRACE;
		} else if ( chr == '[' ) {
			_token.symbol = Symbol.LEFT_BRACKET;
		} else if ( chr == '(' ) {
			_token.symbol = Symbol.LEFT_PARENTHESIS;
		} else if ( chr == '%' ) {
			_token.symbol = Symbol.PERCENT;
		} else if ( chr == '.' ) {
			_token.symbol = Symbol.PERIOD;
		} else if ( chr == '|' ) {
			_token.symbol = Symbol.PIPE;
		} else if ( chr == '?' ) {
			_token.symbol = Symbol.QUESTION;
		} else if ( chr == '}' ) {
			_token.symbol = Symbol.RIGHT_BRACE;
		} else if ( chr == ']' ) {
			_token.symbol = Symbol.RIGHT_BRACKET;
		} else if ( chr == ')' ) {
			_token.symbol = Symbol.RIGHT_PARENTHESIS;
		} else if ( chr == ';' ) {
			_token.symbol = Symbol.SEMICOLON;
		} else if ( chr == '~' ) {
			_token.symbol = Symbol.TILDE;
		}
		// --- Token starting with "@"
		else if ( chr == '@' ) {
			_token.symbol = Symbol.AT;
			_token.value = "";
			if ( Character.isLetter( _getChar() ) ) {
				_token.symbol = Symbol.ANNOTATION;
				while ( Character.isLetterOrDigit( _getChar() ) ) {
					_token.value += _getChar();
					_nextChar();
				}
			}
		}
		// --- Token starting with "*"
		else if ( chr == '*' ) {
			_token.symbol = Symbol.ASTERISK;
			if ( _getChar() == '/' ) {
				_token.symbol = Symbol.RIGHT_C_COMMENT;
				_token.value = "*/";
				_nextChar();
			}
		}
		// --- Token starting with "\"
		else if ( chr == '\\' ) {
			_token.symbol = Symbol.BACKSLASH;
			if ( _getChar() == '\n' ) {
				_nextChar();
				_token.symbol = Symbol.CONTINUATION;
			}
		}
		// --- Token starting with "="
		else if ( chr == '=' ) {
			_token.symbol = Symbol.EQUAL;
			if ( _getChar() == '=' ) {
				_token.symbol = Symbol.DOUBLE_EQUAL;
				_token.value = "==";
				_nextChar();
			} else if ( _getChar() == '<' ) {
				_token.symbol = Symbol.LESS_EQUAL;
				_token.value = "<=";
				_nextChar();
			} else if ( _getChar() == '>' ) {
				_token.symbol = Symbol.GREATER_EQUAL;
				_token.value = "=>";
				_nextChar();
			}
		}
		// ----- Token starting with '!'
		else if ( chr == '!' ) {
			_token.symbol = Symbol.EXCLAMATION;
			if ( _getChar() == '=' ) {
				_token.symbol = Symbol.NOT_EQUAL;
				_token.value = "!=";
				_nextChar();
			}
		}
		// ----- Token starting with '>'
		else if ( chr == '>' ) {
			_token.symbol = Symbol.GREATER_THAN;
			if ( _getChar() == '=' ) {
				_token.symbol = Symbol.GREATER_EQUAL;
				_token.value = ">=";
				_nextChar();
			}
		}
		// --- Token starting with '<'
		else if ( chr == '<' ) {
			_token.symbol = Symbol.LESS_THAN;
			if ( _getChar() == '=' ) {
				_token.symbol = Symbol.LESS_EQUAL;
				_token.value = "<=";
				_nextChar();
			} else if ( (_curPos + 3 < _textLength) && (_text
					.substring( _curPos, _curPos + 3 ).equals( "!--" )) ) {
				_token.symbol = Symbol.LEFT_XML_COMMENT;
				_token.value = "<!--";
				_nextChar();
				_nextChar();
				_nextChar();
				if ( !_options
						.contains( TokenizerOption.EXPLODE_XML_COMMENT ) ) {
					clipUntilMatching();
					_token.symbol = Symbol.COMMENT;
					return;
				}
			}
		}
		// --- Token starting with "-"
		else if ( chr == '-' ) {
			_token.symbol = Symbol.MINUS;
			if ( Character.isDigit( _getChar() ) ) {
				_parseNumber();
			} else if ( (_curPos + 2 < _textLength) && (_text
					.substring( _curPos, _curPos + 2 ).equals( "->" )) ) {
				_token.symbol = Symbol.RIGHT_XML_COMMENT;
				_token.value = "-->";
				_nextChar();
				_nextChar();
			} else if ( _getChar() == '-' ) {
				_token.symbol = Symbol.DOUBLE_MINUS;
				_token.value = "--";
				_nextChar();
			}
		}
		// --- Token starting with "+"
		else if ( chr == '+' ) {
			_token.symbol = Symbol.PLUS;
			if ( Character.isDigit( _getChar() ) ) {
				_parseNumber();
			} else if ( _getChar() == '+' ) {
				_token.symbol = Symbol.DOUBLE_PLUS;
				_token.value = "++";
				_nextChar();
			}
		}
		// --- Token starting with '/'
		else if ( chr == '/' ) {
			_token.symbol = Symbol.SLASH;
			if ( _getChar() == '/' ) {
				_token.symbol = Symbol.DOUBLE_SLASH;
				_token.value = "//";
				_nextChar();
				if ( _options.contains(
						TokenizerOption.SKIP_DOUBLESLASH_UNTIL_EOL ) ) {
					clipUntilSymbol( Symbol.LINEBREAK );
					_parse();
					return;
				}
			} else if ( _getChar() == '*' ) {
				_token.symbol = Symbol.LEFT_C_COMMENT;
				_token.value = "/*";
				_nextChar();
				if ( !_options.contains( TokenizerOption.EXPLODE_C_COMMENT ) ) {
					clipUntilMatching();
					_token.symbol = Symbol.COMMENT;
					return;
				}
			}
		}
		// --- Token starting with "_"
		else if ( chr == '_' ) {
			_token.symbol = Symbol.UNDERSCORE;
			if ( Character.isDigit( _getChar() )
					|| Character.isLetter( _getChar() ) ) {
				_token.symbol = Symbol.IDENTIFIER;
				_parseWord();
			}
		}

		// --- Return type of token
		logger.atFinest().log( "token: %s", _token );
	}

	/**
	 * Parses text starting with a digit. The digit may be prefixed with a minus
	 * or plus character. Accepts hexadecimal values starting with "0x" like
	 * "0xab7f".
	 * 
	 * @return {@code VALUE_INTEGER}, {@code VALUE_DECIMAL},
	 *         {@code VALUE_DOUBLE} or {@code VALUE_HEX}
	 */
	private void _parseNumber() {
		final String numberChars = "0123456789abcdefABCDEF";
		_token.symbol = Symbol.VALUE_INTEGER;
		boolean isHex = false;
		int idx = 0;
		if ( "0".equals( _token.value )
				&& ((_getChar() == 'x') || (_getChar() == 'X')) ) {
			isHex = true;
			_token.symbol = Symbol.VALUE_HEX;
			_token.value = "0x";
			_nextChar();
		}
		while ( _getChar() != 0 ) {
			idx = numberChars.indexOf( _getChar() );
			if ( idx < 0 ) {
				if ( _getChar() == '_' ) {
					if ( !_options.contains( TokenizerOption.JAVA_INT ) ) {
						break;
					}
					_nextChar(); // just skip
				} else if ( _getChar() == '.' ) {
					if ( !_options.contains( TokenizerOption.INT_ONLY ) ) {
						_token.symbol = Symbol.VALUE_DECIMAL;
						_token.value += _getChar();
					}
					_nextChar();
				} else {
					break;
				}
			} else if ( idx < 10 ) {
				_token.value += _nextChar(); // digit
			} else if ( isHex && idx > 0 ) {
				_token.value += _nextChar();
			} else if ( _getChar() == 'e' || _getChar() == 'E' ) {
				_token.symbol = Symbol.VALUE_DOUBLE;
				_token.value += _nextChar();
				if ( _getChar() == '-' ) {
					_token.value += _nextChar();
				} else if ( _getChar() == '+' ) {
					_nextChar();
				}
			} else {
				break;
			}
		}
	}

	private void _parseString( char matchingChar ) {
		char nextCharacter;
		_token.symbol = Symbol.STRING;
		_token.value = ""; // value is without quotes
		do {
			nextCharacter = _getChar();
			if ( nextCharacter == 0 ) {
				break;
			} else if ( nextCharacter == '\\'
					&& _text.charAt( _curPos + 1 ) == '\"' ) {
				_curPos++;
				_nextChar();
				_token.value += "\"";
			} else if ( nextCharacter == '\\'
					&& _text.charAt( _curPos + 1 ) == '\\' ) {
				_curPos++;
				_nextChar();
				_token.value += "\\";
			} else if ( nextCharacter == '\\'
					&& _text.charAt( _curPos + 1 ) != '\"' ) {
				_nextChar();
			} else if ( nextCharacter == matchingChar ) {
				break;
			} else {
				_token.value += nextCharacter;
				_nextChar();
			}
		} while ( true );
		_nextChar();
	}

	/**
	 * Parses a text which started with a letter.
	 * <p>
	 * Switches to {@link com.djarjo.text.Symbol#IDENTIFIER IDENTIFIER} if a
	 * digit or '_' occurs.
	 * <p>
	 * Switches to {@link com.djarjo.text.Symbol#PATH PATH} if any ":./\-"
	 * occurs and option {@link com.djarjo.text.TokenizerOption#ACCEPT_PATH
	 * ACCEPT_PATH} is set.
	 */
	private void _parseWord() {
		char c = 0;
		_token.symbol = Symbol.WORD;
		while ( (c = _getChar()) != 0 ) {
			if ( Character.isLetter( c ) ) {
				_token.value += _nextChar();
			} else if ( Character.isDigit( c ) || c == '_' ) {
				_token.symbol = Symbol.IDENTIFIER;
				_token.value += _nextChar();
			} else if ( (c == '/' || c == '\\' || c == ':' || c == '.'
					|| c == '-')
					&& (_options.contains( TokenizerOption.ACCEPT_PATH )) ) {
				_token.symbol = Symbol.PATH;
				_token.value += _nextChar();
			} else {
				break;
			}
		}
		if ( _options.contains( TokenizerOption.TO_LOWER ) ) {
			_token.value = _token.value.toLowerCase();
		} else if ( _options.contains( TokenizerOption.TO_UPPER ) ) {
			_token.value = _token.value.toUpperCase();
		}
	}

	@Override
	public String toString() {
		return _curPos + ": " + _token;
	}
}