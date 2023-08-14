/**
 * 
 */
package com.djarjo.text;

import com.google.common.flogger.FluentLogger;

/**
 * Provides methods to parse some standard objects. Current token of tokenizer
 * must be the initial symbol of the object to parse.
 * 
 * @author Hajo Lemcke
 * @since 2021-12-08 Initial version
 */
public class Parser {
	private final static FluentLogger logger = FluentLogger.forEnclosingClass();

	protected Tokenizer tokenizer;
	private boolean _throwExceptionOnError;

	/**
	 * Sole constructor requires an initialized tokenizer
	 * 
	 * @param tokenizer
	 */
	public Parser( Tokenizer tokenizer, boolean throwExceptionOnError ) {
		this.tokenizer = tokenizer;
		this._throwExceptionOnError = throwExceptionOnError;
	}

	/**
	 * Assert symbol for current token.
	 * 
	 * @param symbol
	 *            expected symbol
	 * @param message
	 *            error message if not
	 * @return true if symbol is same as current token
	 * @throws ParserException
	 *             if throwExceptionOnError is true
	 */
	public boolean assertSymbol( Symbol symbol, String message ) {
		if ( symbol == tokenizer.getToken().symbol ) {
			return true;
		}
		String errorMessage = String.format( "Expected %s but found %s at %s",
				message, tokenizer.getToken(), tokenizer.getLocation() );
		logger.atFine().log( errorMessage );
		if ( _throwExceptionOnError ) {
			throw new ParserException( errorMessage, tokenizer.getPosition() );
		}
		return false;
	}

	/**
	 * Parses an email address like "{@code John Doe <John.Doe@example.org>}".
	 * 
	 * <pre>
	 * email       = name ws "<" email-adr ">"
	 *             | email-adr
	 * name        = word
	 *             | word ws name
	 * email-adr   = local-part "@" domain-part
	 * local-part  = 
	 * domain-part = domain-name
	 *              | ip-address
	 * ip-address  = "[" ipv4 "]"
	 *             | "[" ipv6 "]"
	 * </pre>
	 * 
	 * @param tokenizer
	 * @return Email in string format or {@code null} if not an email
	 */
	String parseEmail() {
		return null;
	}

	/**
	 * Parses an URI like "{@code mailto:John.Doe@example.org}".
	 * 
	 * <pre>
	 * uri =
	 * </pre>
	 * 
	 * @param tokenizer
	 * @return
	 */
	String parseUri() {
		Token token = tokenizer.getToken();
		if ( token.symbol != Symbol.WORD ) {

		}
		return null;
	}
}
