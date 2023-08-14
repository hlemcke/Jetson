/**
 * 
 */
package com.djarjo.text;

/**
 * Options to manage tokenizer work.
 * 
 * @author Hajo Lemcke
 * @since 2021-12-03 extracted from tokenizer
 */
public enum TokenizerOption {

	/**
	 * Returns token {@link com.djarjo.text.Symbol#ANNOTATION ANNOTATION} for
	 * "{@code ws @identifier ...}.
	 */
	ACCEPT_ANNOTATION,

	/**
	 * Returns token {@link com.djarjo.text.Symbol#PATH PATH} if a
	 * {@link com.djarjo.text.Symbol#WORD WORD} or
	 * {@link com.djarjo.text.Symbol#IDENTIFIER IDENTIFIER} contains ":./\-".
	 */
	ACCEPT_PATH,

	/**
	 * Instead of returning token {@link com.djarjo.text.Symbol#COMMENT COMMENT}
	 * returns {@link com.djarjo.text.Symbol#LEFT_C_COMMENT LEFT_C_COMMENT} and
	 * any token until {@link com.djarjo.text.Symbol#RIGHT_C_COMMENT
	 * RIGHT_C_COMMENT} separately.
	 */
	EXPLODE_C_COMMENT,

	/**
	 * Instead of returning token {@link com.djarjo.text.Symbol#COMMENT COMMENT}
	 * returns {@link com.djarjo.text.Symbol#LEFT_XML_COMMENT LEFT_XML_COMMENT}
	 * and any token until {@link com.djarjo.text.Symbol#RIGHT_XML_COMMENT
	 * RIGHT_XML_COMMENT} separately.
	 */
	EXPLODE_XML_COMMENT,

	/**
	 * Set this option to get three tokens
	 * 
	 * <pre>
	 *  VALUEINT (1234) DOT VALUEINT ( 56 )
	 * </pre>
	 * 
	 * for the text "1234.56" instead of one token
	 * 
	 * <pre>
	 * VALUEDEC( 1234.45 )
	 * </pre>
	 */
	INT_ONLY,

	/**
	 * Set this option to skip underscores '_' within integer values like in
	 * Java. So "1_234_567" will return the token {@code VALUEINT} with value
	 * 1234567.
	 */
	JAVA_INT,

	/**
	 * Skips "//" and further text until end of line.
	 */
	SKIP_DOUBLESLASH_UNTIL_EOL,

	/**
	 * Tokenizer never returns symbol LINEBREAK. Skips all:
	 * <ul>
	 * <li>0x0A = {@code '\n'} = LF (LineFeed)</li>
	 * <li>0x0D = {@code '\r'} = CR (CarriageReturn)</li>
	 * </ul>
	 */
	SKIP_LINEBREAK,

	/**
	 * Tokenizer never returns symbol WHITESPACE. Skips all:
	 * <ul>
	 * <li>0x09 = '\t' = TAB (HorizontalTab)</li>
	 * <li>0x0b = '\v' = VT (VerticalTab)</li>
	 * <li>0x0c = '\f' = FF (FormFeed)</li>
	 * <li>0x20 = {@code ' '} = SPC (Space)</li>
	 * </ul>
	 */
	SKIP_WHITESPACE,

	/**
	 * Tokenizer will set all values for {@code IDENTIFIER} and {@code WORD} to
	 * lower case.
	 */
	TO_LOWER,

	/**
	 * Tokenizer will set all values for {@code IDENTIFIER} and {@code WORD} to
	 * upper case.
	 */
	TO_UPPER,
}
