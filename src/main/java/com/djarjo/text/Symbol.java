package com.djarjo.text;

/******************************************************************
 * Symbol is part of a {@link com.djarjo.text.Token Token} as returned by the
 * {@link com.djarjo.text.Tokenizer Tokenizer}.
 * <p>
 * Some symbols will be returned by the tokenizer only if specific options are
 * set.
 * </p>
 *
 * <pre>
 * Parentheses are smooth and curved ( ),
 * brackets are square [ ]
 * and braces are curly { }.
 * </pre>
 */
public enum Symbol {

	/** Single character: "&#38;" = &#38;#38; = 0x26 = &#38;amp; */
	AMPERSAND("&"),

	/**
	 * Letter following an at-symbol.
	 * <p>
	 * Only produced if {@link com.djarjo.text.TokenizerOption#ACCEPT_ANNOTATION
	 * ACCEPT_ANNOTATION} is set.
	 */
	ANNOTATION(""),

	/** Single character: "'" = &#38;#39; = 0x27 = &apos; */
	APOSTROPH("'"),

	/** Single character: "*" = &#38;#42; = 0x2a = &ast; */
	ASTERISK("*"),

	/** Single character: "@" = &#38;#40; = 0x28 = &commat; */
	AT("@"),

	/** Single character: "\" = &#38;#92; = 0x5c = &bsol; */
	BACKSLASH("\\"),

	/**
	 * Single character: "\" = &#38;#92; = 0x5c = &bsol; BACKSLASH("\\"),
	 *
	 * /** Tokenizer method {@link com.djarjo.text.Tokenizer#getToken()
	 * getToken()} returns this token to indicate that the parsing has not
	 * started yet.
	 */
	BEGIN(""),

	/** Single character: "^" = &#38;#94; = 0x5e = &#38;hat; */
	CARET("^"),

	/** Single character: ":" = &#38;#58 = 0x3a = &#38;colon; */
	COLON(":"),

	/** Single character: "," */
	COMMA(","),

	/**
	 * Value is text between {@link #LEFT_C_COMMENT} and
	 * {@link #RIGHT_C_COMMENT} or between {@link #LEFT_XML_COMMENT} and
	 * {@link #RIGHT_XML_COMMENT}. This token will be returned unless options
	 * {@link com.djarjo.text.TokenizerOption#EXPLODE_C_COMMENT
	 * EXPLODE_C_COMMENT} or
	 * {@link com.djarjo.text.TokenizerOption#EXPLODE_XML_COMMENT
	 * EXPLODE_XML_COMMENT} are set.
	 */
	COMMENT(""),

	/** Backslash at end of line */
	CONTINUATION(""),

	/** Dollar sign '$' */
	DOLLAR("$"),

	/** Two consecutive characters '==' */
	DOUBLE_EQUAL("=="),

	/** Two consecutive characters '--' */
	DOUBLE_MINUS("--"),

	/** Two consecutive characters '++' */
	DOUBLE_PLUS("++"),

	/** Two consecutive characters '//' */
	DOUBLE_SLASH("//"),

	/** End of file */
	EOF("EOF"),

	/** Single character: "=" = &#38;#61; = 0x3d = &equals; */
	EQUAL("="),

	/** Single character: "!" = &#38;#33; = 0x21 = &excl; */
	EXCLAMATION("!"),

	/** Greater equal '&gt;=' or '=&gt;' */
	GREATER_EQUAL(">="),

	/** Single character: ">" = &#38;#62; = 0x3e = &#38;gt; */
	GREATER_THAN(">"),

	/** Single character: "#" = &#38;#35; = 0x23 = &#38;num; */
	HASH("#"),

	/**
	 * An identifier starts with a letter or underscore followed by letters,
	 * digits or underscores.
	 * <p>
	 * Tokenizer switches from {@link #WORD} to IDENTIFIER when a digit or
	 * underscore occurs.
	 */
	IDENTIFIER(""),

	/** Curly brace open '{' */
	LEFT_BRACE("{"),

	/**
	 * Single character bracket open '{@code [}'
	 */
	LEFT_BRACKET("["),

	/**
	 * Start of a C comment '{@code /*}'.
	 * <p>
	 * Will only be returned if option
	 * {@link com.djarjo.text.TokenizerOption#EXPLODE_C_COMMENT
	 * EXPLODE_C_COMMENT} is set.
	 */
	LEFT_C_COMMENT("/*"),

	/** Parenthesis open '(' */
	LEFT_PARENTHESIS("("),

	/**
	 * Start of an XML comment '{@code <!--}'.
	 * <p>
	 * Will only be returned if option
	 * {@link com.djarjo.text.TokenizerOption#EXPLODE_XML_COMMENT
	 * EXPLODE_XML_COMMENT} is set.
	 */
	LEFT_XML_COMMENT("<!--"),

	/** Two characters: "&lt;=" or "=&lt;" */
	LESS_EQUAL("<="),

	/** Single character: "&lt;" = &#38;#60; = 0x3c = &#38;lt; */
	LESS_THAN("<"),

	/**
	 * Single character: "\n". Any "\r" will be skipped
	 * <ul>
	 * <li>0x0A = {@code '\n'} = LF (LineFeed)</li>
	 * <li>0x0D = {@code '\r'} = CR (CarriageReturn) will be skipped</li>
	 * </ul>
	 */
	LINEBREAK("\n"),

	/** Single character: "-" = &#38;#45; = 0x2d = &minus; */
	MINUS("-"),

	/** Two characters: "!=" */
	NOT_EQUAL("!="),

	/**
	 * Tokenizer will switch from {@link #IDENTIFIER} or {@link #WORD} to PATH
	 * when option {@link com.djarjo.text.TokenizerOption#ACCEPT_PATH
	 * ACCEPT_PATH} is set and any character ":./\-" occurs.
	 */
	PATH(""),

	/** Single character: "%" = &#38;#37; = 0x25 = &percnt; */
	PERCENT("%"),

	/** Single character: "." = &#38;#46; = 0x2e = &period; */
	PERIOD("."),

	/** The pipe character '|' */
	PIPE("|"),

	/** Plus '+' */
	PLUS("+"),

	/** Question mark '?' */
	QUESTION("?"),

	/** Curly brace close '}' */
	RIGHT_BRACE("}"),

	/**
	 * This token will be produced by the single character bracket close
	 * '{@code ]}'
	 */
	RIGHT_BRACKET("]"),

	/**
	 * End of a C comment '*&#x2f;'
	 * <p>
	 * Will only be returned if option
	 * {@link com.djarjo.text.TokenizerOption#EXPLODE_C_COMMENT
	 * EXPLODE_C_COMMENT} is set.
	 */
	RIGHT_C_COMMENT("*/"),

	/** Parenthesis close ')' */
	RIGHT_PARENTHESIS(")"),

	/**
	 * End of an XML comment '{@code -->}'
	 * <p>
	 * Will only be returned if option
	 * {@link com.djarjo.text.TokenizerOption#EXPLODE_XML_COMMENT
	 * EXPLODE_XML_COMMENT} is set.
	 */
	RIGHT_XML_COMMENT("-->"),

	/** Semicolon ';' */
	SEMICOLON(";"),

	/** Slash '/' */
	SLASH("/"),

	/**
	 * Any characters enclosed in single or double quotes. Quotes will be
	 * removed.
	 */
	STRING(""),

	/** Single character: "~" */
	TILDE("~"),

	/**
	 * Single character: "_" = &#38;#95; = 0x5f = &#38;lowbar;
	 * <p>
	 * Tokenizer will return [IDENTIFIER] if directly followed by a letter, a
	 * digit or another underscore.
	 */
	UNDERSCORE("_"),

	/** What is this? */
	UNKNOWN(""),

	/**
	 * A decimal like "-468.91".
	 * <p>
	 * Tokenizer switches from {@link VALUE_INTEGER} to VALUE_DECIMAL when a dot
	 * '.' occurs.
	 *
	 * <pre>
	 * decimal  = integer '.' digits
	 * </pre>
	 */
	VALUE_DECIMAL(""),

	/**
	 * A double like "-468.91e-2".
	 * <p>
	 * Tokenizer switches from {@link VALUE_INTEGER} or {@link VALUE_DECIMAL} to
	 * VALUE_DOUBLE when an 'e' or 'E' occurs.
	 *
	 * <pre>
	 * double     = decimal exponent
	 *            | integer exponent
	 * exponent = | sign 'e' digits
	 *            | sign 'E' digits
	 * </pre>
	 */
	VALUE_DOUBLE(""),

	/**
	 * An integer in byte format like "0x3a4b7f"
	 *
	 * <pre>
	 * hexValue = "0x" digits
	 * digits   = digit
	 *          | digit digits
	 * digit    = '0' .. '9'
	 *          | 'a' .. 'f'
	 *          | 'A' .. 'F'
	 * </pre>
	 */
	VALUE_HEX("0x"),

	/**
	 * An INTEGER like "-468"
	 *
	 * <pre>
	 * integer  = sign digits
	 * sign     = ""
	 *          | '+'
	 *          | '-'
	 * digits   = digit
	 *          | digit digits
	 * digit    = '0' .. '9'
	 * </pre>
	 */
	VALUE_INTEGER(""),

	/**
	 * Any number of consecutive whitespace characters:
	 * <ul>
	 * <li>0x09 = '\t' = TAB (HorizontalTab)</li>
	 * <li>0x0b = '\v' = VT (VerticalTab)</li>
	 * <li>0x0c = '\f' = FF (FormFeed)</li>
	 * <li>0x20 = {@code ' '} = SPC (Space)</li>
	 * </ul>
	 */
	WHITESPACE(" "),

	/**
	 * A single word. Starts with a letter and contains only letters. If a digit
	 * is found then the token will be changed to {@link #STRING}.
	 */
	WORD("");

	private String _text;

	Symbol( String text ) {
		this._text = text;
	}

	String getText() {
		return this._text;
	}
}