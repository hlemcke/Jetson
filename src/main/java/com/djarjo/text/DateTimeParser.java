package com.djarjo.text;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**********************************************************************
 * Very flexible parser for LocalDate and OffsetDateTime. The parser even
 * accepts input like this:
 * 
 * <dl>
 * <dt>,7</dt>
 * <dd>returns the last day of that month (July) in the current year</dd>
 * </dl>
 */
public class DateTimeParser implements Serializable {

	private static final long serialVersionUID = -358507498570262711L;

	private final static String SEPARATOR = ".";

	/**
	 * When parsing a date string with a 2 digit year, the year will be expanded
	 * into a 4 digit year. If its smaller than the current year - 2000 +
	 * {@value} then add 2000 else add 1900.
	 * <p>
	 * The default value is 20.
	 * </p>
	 * 
	 * @see #expandYear(int)
	 */
	public static int YearsIntoFuture = 20;

	public DateTimeParser() {
	}

	/******************************************************************
	 * Expands a given year with 2 digits into a full four digit year. If the
	 * given value is smaller than the current year - 2000 +
	 * <em>getYearsIntoFuture()</em> then add 2000. If it is smaller than 100
	 * then add 1900. If it is smaller than 1000 then add 1900. Else just return
	 * the given value. 0 or a negative value returns the current year.
	 * 
	 * @param yearWith2Digits
	 *            The two digit year
	 * @return Returns a 4 digit year
	 */
	public static int expandYear( int yearWith2Digits ) {
		int currentYear = OffsetDateTime.now().getYear();
		if ( yearWith2Digits <= 0 ) {
			return currentYear;
		} else if ( yearWith2Digits < (currentYear - 2000 + YearsIntoFuture) ) {
			return 2000 + yearWith2Digits;
		} else if ( yearWith2Digits < 100 ) {
			return 1900 + yearWith2Digits;
		} else if ( yearWith2Digits < 1000 ) {
			return 1000 + yearWith2Digits;
		}
		return yearWith2Digits;
	}

	public static int getYearsIntoFuture() {
		return YearsIntoFuture;
	}

	/******************************************************************
	 * Parses the given string and returns a LocalDate.
	 * <p>
	 * The <em>pattern</em> is an optional string which only states the order of
	 * fields within a date. If given it must contain the letters <b>y</b> for
	 * year, <b>M</b> for month and <b>d</b> for day in month. The order of this
	 * three letters defines the order of values in the parsed string.
	 * </p>
	 * 
	 * @param text
	 *            The text with a date
	 * @param pattern
	 *            The pattern for the date or {@code null}
	 * @return new {@code LocalDate} object
	 */
	public LocalDate parseDate( final String text, final String pattern ) {
		LocalDate current = LocalDate.now();

		// --- Split string into tokens
		List<Token> tokens = parseTokenize( text );

		// --- Check for special entry for "end of month"
		if ( (tokens.size() == 2) && (tokens.get( 0 ).type == TokenType.TEXT)
				&& (tokens.get( 1 ).type == TokenType.NUMBER) ) {
			current = parseUltimo( tokens.get( 1 ).value );
		}

		// --- Check for compacted ISO "date T time" string
		else if ( (tokens.size() > 2)
				&& (tokens.get( 0 ).type == TokenType.NUMBER)
				&& (tokens.get( 1 ).text.equalsIgnoreCase( "t" ))
				&& (tokens.get( 2 ).type == TokenType.NUMBER) ) {
			current = parseDate( current, tokens.get( 0 ), pattern );
		}

		// --- Single number => interpret as a date
		else if ( (tokens.size() == 1)
				&& (tokens.get( 0 ).type == TokenType.NUMBER) ) {
			current = parseDate( current, tokens.get( 0 ), pattern );
		}

		// --- Standard analyzation
		else {
			for ( Token token : tokens ) {
				if ( token.type == TokenType.DATE )
					current = parseDate( current, token, pattern );
				else if ( token.type == TokenType.TEXT )
					current = parseText( token.text ).toLocalDate();
			}
		}
		return current;
	}

	/******************************************************************
	 * Parses the given string and returns a new instance of
	 * {@code OffsetDateTime}.
	 * <p>
	 * The {@code pattern} is an optional string which only states the order of
	 * fields within a date. If given it must contain the letters <b>y</b> for
	 * year, <b>M</b> for month and <b>d</b> for day in month. The order of this
	 * three letters defines the order of values in the parsed string.
	 * </p>
	 * 
	 * @param text
	 *            The text with a date and time
	 * @param pattern
	 *            The pattern for the date or {@code null}
	 * @return a new OffsetDateTime object
	 */
	public OffsetDateTime parseDateTime( final String text,
			final String pattern ) {
		LocalDate date = null;
		OffsetDateTime current = OffsetDateTime.now();

		// --- Split string into tokens
		List<Token> tokens = parseTokenize( text );

		// --- Check for special entry for "end of month"
		if ( (tokens.size() == 2) && (tokens.get( 0 ).type == TokenType.TEXT)
				&& (tokens.get( 1 ).type == TokenType.NUMBER) ) {
			date = parseUltimo( tokens.get( 1 ).value );
			current = withDate( current, date );
		}

		// --- Check for compacted ISO date T time string
		else if ( (tokens.size() > 2)
				&& (tokens.get( 0 ).type == TokenType.DATE)
				&& (tokens.get( 1 ).text.equalsIgnoreCase( "t" ))
				&& (tokens.get( 2 ).type == TokenType.TIME) ) {
			date = parseDate( tokens.get( 0 ).text, pattern );
			LocalTime time = parseTime( tokens.get( 2 ).text );
			current = withDate( current, date ).withHour( time.getHour() )
					.withMinute( time.getMinute() )
					.withSecond( time.getSecond() ).withNano( time.getNano() );
			if ( tokens.size() > 3 ) {
				current = current.withOffsetSameLocal(
						ZoneOffset.ofHours( tokens.get( 3 ).value ) );
			}
		}

		// --- Single number => interpret as a date
		else if ( (tokens.size() == 1)
				&& (tokens.get( 0 ).type == TokenType.NUMBER) ) {
			date = parseDate( tokens.get( 0 ).text, pattern );
			current = withDate( current, date );
		}

		// --- Standard analyzation
		else {
			for ( Token token : tokens ) {
				if ( token.type == TokenType.DATE )
					current = withDate( current,
							parseDate( token.text, pattern ) );
				else if ( token.type == TokenType.TEXT )
					current = parseText( token.text );
				else if ( token.type == TokenType.TIME )
					current = withTime( current, parseTime( token.text ) );
			}
		}
		return current;
	}

	/******************************************************************
	 * Date token contains 1 to 3 numbers. Multiple numbers are separated by
	 * {@link #SEPARATOR}
	 * 
	 * @param token
	 * @param pattern
	 * @return the given date with new values
	 */
	private LocalDate parseDate( LocalDate current, Token token,
			String pattern ) {

		// --- Split text into numbers
		String[] numbers = token.text.split( "\\" + SEPARATOR );
		int[] values = new int[3];
		int count;
		for ( count = 0; count < numbers.length; count++ ) {
			values[count] = Integer.valueOf( numbers[count] );
		}

		// --- Single value
		if ( count == 1 ) {
			// --- 8 digits => 4-2-2
			if ( token.text.length() == 8 ) {
				values[0] =
						TextHelper.parseInteger( token.text.substring( 0, 4 ) );
				values[1] =
						TextHelper.parseInteger( token.text.substring( 4, 6 ) );
				values[2] =
						TextHelper.parseInteger( token.text.substring( 6, 8 ) );
				count = 3;
			} else {
				values[0] =
						TextHelper.parseInteger( token.text.substring( 0, 2 ) );
				if ( token.text.length() >= 4 )
					values[count++] = TextHelper
							.parseInteger( token.text.substring( 2, 4 ) );
				if ( token.text.length() >= 6 )
					values[count++] = TextHelper
							.parseInteger( token.text.substring( 4, 6 ) );
			}
		}

		int year = -1, month = -1, day = -1;

		// --- No pattern => determine best fit
		if ( pattern == null ) {
			if ( count == 1 ) { // --- year or day
				if ( values[0] > 31 )
					year = expandYear( values[0] );
				else
					day = values[0];
			} else if ( count == 2 ) {
				if ( values[0] > 31 ) { // --- y-M
					year = expandYear( values[0] );
					month = values[1];
				} else if ( values[1] > 31 ) { // --- M-y
					month = values[0];
					year = expandYear( values[1] );
				} else { // --- d-M
					day = values[0];
					month = values[1];
				}
			} else { // --- count >= 3
				if ( values[0] > 31 ) { // --- y-M-d
					year = expandYear( values[0] );
					month = values[1];
					day = values[2];
				}

				// --- year is the third value
				else {
					if ( values[1] > 12 ) { // --- M-d-Y
						month = values[0];
						day = values[1];
					} else { // --- d-M-y
						day = values[0];
						month = values[1];
					}
					year = expandYear( values[2] );
				}
			}
		}

		// --- Parse with pattern
		else {
			// --- Determine order of date fields in pattern for "yMd"
			int y = pattern.indexOf( "y" );
			int m = pattern.indexOf( "M" );
			int d = pattern.indexOf( "d" );
			if ( y < m ) {
				if ( d < y ) { // d-y-M
					day = values[0];
					year = expandYear( values[1] );
					month = values[2];
				} else if ( d < m ) { // --- y-d-M
					year = expandYear( values[0] );
					day = values[1];
					month = values[2];
				} else { // --- y-M-d
					year = expandYear( values[0] );
					month = values[1];
					day = values[2];
				}
			} else {
				if ( d < m ) { // --- d-M-y
					day = values[0];
					month = values[1];
					year = expandYear( values[2] );
				} else if ( d < y ) { // --- M-d-y
					month = values[0];
					day = values[1];
					year = expandYear( values[2] );
				} else { // --- M-y-d
					month = values[0];
					year = expandYear( values[1] );
					day = values[2];
				}
			}
		}
		if ( year >= 0 ) {
			current = current.withYear( year );
		}
		if ( month >= 0 ) {
			current = current.withMonth( month );
		}
		if ( day >= 0 ) {
			current = current.withDayOfMonth( day );
		}
		return current;
	}

	private OffsetDateTime parseText( String text ) {
		OffsetDateTime result = OffsetDateTime.now();
		int val = 0;
		if ( text.equalsIgnoreCase( "midnight" ) ) {
			result = result.withHour( 0 ).withMinute( 0 ).withSecond( 0 );
		}

		else if ( text.equals( "noon" ) ) {
			result = result.withHour( 12 ).withMinute( 0 ).withSecond( 0 );
		}

		else if ( text.equals( "now" ) ) {
		}

		else if ( text.equals( "pm" ) ) {
			if ( (val = result.getHour()) <= 12 ) {
				result = result.withHour( val + 12 );
			}
		}

		else if ( text.equals( "today" ) ) {
			result = result.withHour( 12 ).withMinute( 0 ).withSecond( 0 )
					.withNano( 0 );
		}

		else if ( text.equals( "tomorrow" ) ) {
			result = result.plusDays( 1 ).withHour( 12 ).withMinute( 0 )
					.withSecond( 0 ).withNano( 0 );
		}

		else if ( text.equals( "yesterday" ) ) {
			result = result.minusDays( 1 ).withHour( 12 ).withMinute( 0 )
					.withSecond( 0 ).withNano( 0 );
		}
		return result;
	}

	/**
	 * Time token contains 2, 3 or 4 numbers separated by {@link #SEPARATOR} in
	 * the order hours, minutes, seconds, milliseconds
	 * 
	 * @param text
	 *            time string to parse
	 * @return new instance of {@code LocalTime}
	 */
	private LocalTime parseTime( String text ) {
		String[] numbers = text.split( "\\" + SEPARATOR );
		int hour = 0, minute = 0, second = 0, nano = 0;

		// --- Check for ISO compacted time like 174326
		if ( numbers.length == 1 ) {
			hour = TextHelper.parseInteger( text.substring( 0, 2 ) );
			if ( text.length() >= 4 ) {
				minute = TextHelper.parseInteger( text.substring( 2, 4 ) );
			}
			if ( text.length() >= 6 ) {
				second = TextHelper.parseInteger( text.substring( 4, 6 ) );
			}
			if ( text.length() >= 7 ) {
				nano = TextHelper.parseInteger( text.substring( 7 ) );
			}
		}
		// --- Non compact => use separated values
		else {
			hour = TextHelper.parseInteger( numbers[0] );
			if ( numbers.length > 1 )
				minute = TextHelper.parseInteger( numbers[1] );
			if ( numbers.length > 2 )
				second = TextHelper.parseInteger( numbers[2] );
			if ( numbers.length > 3 )
				nano = TextHelper.parseInteger( numbers[3] );
		}
		LocalTime time = LocalTime.of( hour, minute, second, nano );
		return time;
	}

	/******************************************************************
	 * Splits the given string into tokens and returns them as a list.
	 * 
	 * @param text
	 *            The string to be interpreted
	 * @return Returns a list of tokens
	 */
	private List<Token> parseTokenize( String text ) {
		List<Token> tokens = new ArrayList<>();
		Token tokenNone = new Token( TokenType.NONE );
		Token activeToken = tokenNone;

		// --- Scan input string and split into token
		for ( int i = 0; i < text.length(); i++ ) {
			char c = text.charAt( i );

			// --- Skip some characters
			if ( "\t\" []".indexOf( c ) >= 0 ) {
				// --- End of previous token data
				activeToken = tokenNone;
				continue;
			}

			// --- Digit
			else if ( Character.isDigit( c ) ) {
				if ( activeToken.type != TokenType.DATE
						&& activeToken.type != TokenType.NUMBER
						&& activeToken.type != TokenType.TIME
						&& activeToken.type != TokenType.TIMEZONE )
					activeToken = parseTokenizeCheck( tokens, activeToken,
							TokenType.NUMBER );
				activeToken.value = activeToken.value * 10 + c - '0';
				activeToken.text += c;
			}

			// --- Colon ":" => time or timezone => skip it
			else if ( c == ':' ) {
				if ( activeToken.type == TokenType.NUMBER ) {
					activeToken.type = TokenType.TIME;
					activeToken.text += SEPARATOR;
				} else if ( activeToken.type == TokenType.TIME
						|| activeToken.type == TokenType.TIMEZONE ) {
					activeToken.text += SEPARATOR;
				} else {
					activeToken = parseTokenizeCheck( tokens, activeToken,
							TokenType.TEXT );
					activeToken.text += c;
				}
			}

			// --- Check some separators
			else if ( ",./".indexOf( c ) >= 0 ) {
				if ( activeToken.type == TokenType.NUMBER ) {
					activeToken.type = TokenType.DATE;
					activeToken.text += SEPARATOR;
				} else if ( activeToken.type == TokenType.DATE
						|| activeToken.type == TokenType.TIME
						|| activeToken.type == TokenType.TIMEZONE )
					activeToken.text += SEPARATOR;
				else {
					activeToken = parseTokenizeCheck( tokens, activeToken,
							TokenType.TEXT );
					activeToken.text += SEPARATOR;
				}
			}

			// --- Minus "-"
			else if ( c == '-' ) {
				if ( activeToken.type == TokenType.NUMBER ) {
					activeToken.type = TokenType.DATE;
					activeToken.text += SEPARATOR;
				} else if ( activeToken.type == TokenType.DATE ) {
					activeToken.text += SEPARATOR;
				} else if ( activeToken.type == TokenType.TIME ) {
					activeToken = parseTokenizeCheck( tokens, activeToken,
							TokenType.TIMEZONE );
					activeToken.text += c;
				}
			}

			// --- Plus "+"
			else if ( c == '+' ) {
				activeToken = parseTokenizeCheck( tokens, activeToken,
						TokenType.TIMEZONE );
				activeToken.text += c;
			}

			// --- Everything else is some kind of text => convert to lower case
			else {
				activeToken = parseTokenizeCheck( tokens, activeToken,
						TokenType.TEXT );
				activeToken.text += Character.toLowerCase( c );
			}
		}
		return tokens;
	}

	/**********************************************************************
	 * Checks the current token. If the type is different then create a new
	 * token with the given type, add it to the list and return it.
	 * 
	 * @param tokens
	 * @param activeToken
	 * @param newType
	 * @return Returns activeToken if it has the same type or a new Token
	 */
	private Token parseTokenizeCheck( List<Token> tokens, Token activeToken,
			TokenType type ) {
		if ( (activeToken == null) || (activeToken.type != type) ) {
			activeToken = new Token( type );
			tokens.add( activeToken );
		}
		return activeToken;
	}

	/******************************************************************
	 * Parses special user input to quickly enter end of month. Accepted input
	 * are:
	 * <ul>
	 * <li>{@code ,7} will return current year end of July
	 * <li>
	 * <li>{@code 1903} will return 2019 end of March (2019-03-31)</li>
	 * <li>{@code 2002} will return 2020 end of February (2020-02-28)</li>
	 * </ul>
	 * This method only gets the integer value.
	 * 
	 * @param value
	 *            value to parse. See above
	 * @return new instance of {@code LocalDate}
	 */
	private LocalDate parseUltimo( int value ) {
		LocalDate date = LocalDate.now().withDayOfMonth( 1 );
		int month = 0, year = -1;

		// --- Value is only the month
		if ( value <= 12 ) {
			month = value;
		}

		// --- Month and year given
		else {
			month = value / 100;
			year = value % 100;
			if ( year < 100 ) {
				year = expandYear( year );
			}
			date = date.withYear( year );
		}
		date = date.withMonth( month );
		date = date.plusMonths( 1 ).minusDays( 1 );
		return date;
	}

	public static void setYearsIntoFuture( int yearsIntoFuture ) {
		YearsIntoFuture = yearsIntoFuture;
	}

	public static OffsetDateTime withDate( OffsetDateTime current,
			LocalDate date ) {
		current = current.withYear( date.getYear() )
				.withMonth( date.getMonthValue() )
				.withDayOfMonth( date.getDayOfMonth() );
		return current;
	}

	public static OffsetDateTime withTime( OffsetDateTime current,
			LocalTime time ) {
		current = current.withHour( time.getHour() )
				.withMinute( time.getMinute() ).withSecond( time.getSecond() )
				.withNano( time.getNano() );
		return current;
	}

	/******************************************************************
	 * Token as a section of a parsed string.
	 */
	private class Token {
		TokenType type = null;
		int value = 0;
		String text = "";

		/** Instantiates a new token with given type */
		protected Token( TokenType type ) {
			this.type = type;
		}

		// --- for debugging purposes
		@Override
		public String toString() {
			String s = type + "(";
			if ( value > 0 )
				s += value;
			return s + " '" + text + "')";
		}
	}

	/**
	 * Types of a token
	 */
	private enum TokenType {
		DATE,
		NONE,
		NUMBER,
		TEXT,
		TIME,
		TIMEZONE
	};
}