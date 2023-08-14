package com.djarjo.text;

/**
 * A {@code Token} consists of a symbol. Its value is the string which defined
 * the symbol.
 * 
 * @author Hajo Lemcke
 * @since 2021-12-03 extracted from Tokenizer to include both Symbol and value
 */
public class Token {
	public int position = -1;
	public Symbol symbol = Symbol.UNKNOWN;
	public String value = "";

	public Token() {
	}

	@Override
	public Token clone() {
		Token clone = new Token();
		clone.position = position;
		clone.symbol = symbol;
		clone.value = value;
		return clone;
	}

	public Token( Symbol symbol ) {
		this.symbol = symbol;
	}

	public Double getAsDouble() {
		Double d = null;
		try {
			d = Double.valueOf( value );
		} catch (NumberFormatException ex) {
			// --- Intentionally left empty
		}
		return d;
	}

	/**
	 * Gets value as a Java integer
	 * 
	 * @return Java integer or {@code null} if no integer
	 */
	public Integer getAsInt() {
		Integer i = null;
		try {
			i = Integer.parseInt( value );
		} catch (NumberFormatException ex) {
			// --- Intentionally left empty
		}
		return i;
	}

	/**
	 * Gets value as a Java long
	 * 
	 * @return Java long or {@code null} if no long
	 */
	public Long getAsLong() {
		Long longValue = null;
		try {
			longValue = Long.parseLong( value );
		} catch (NumberFormatException ex) {
			// --- Intentionally left empty
		}
		return longValue;
	}

	/**
	 * Gets value as an lower case string
	 * 
	 * @return String with all characters lower case. Can be empty but not
	 *         {@code null}.
	 */
	public String getAsLowerCase() {
		return value.toLowerCase();
	}

	/**
	 * Gets value as an upper case string
	 * 
	 * @return String with all characters upper case. Can be empty but not
	 *         {@code null}.
	 */
	public String getAsUpperCase() {
		return value.toUpperCase();
	}

	@Override
	public String toString() {
		return String.format( "%s %s %s", symbol.name(), value,
				(position < 0) ? "" : " @" + position );
	}
}
