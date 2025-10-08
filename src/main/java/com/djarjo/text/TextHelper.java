package com.djarjo.text;

import com.djarjo.common.BaseConverter;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Static methods for text analysis, modification and parsing.
 * <p>
 * The <em>parseXxx</em> methods parse text for some basic values without throwing
 * exceptions. Other useful methods are:
 * <ul>
 * <li>{@link #condense(String, int)} condenses a loaded text file</li>
 * <li>{@link #splitIntoLines(String)} splits a loaded text file into separate
 * lines</li>
 * <li>{@link #substituteVariables(String, Properties, boolean)} substitutes
 * variables within text by values from properties</li>
 * </ul>
 *
 * @see com.djarjo.text.Tokenizer
 */
public class TextHelper {

	/**
	 * The maximum Levenshtein distance by which two words are identical. The value
	 * {@value}
	 * is a percentage (1=10%, 2=20%, ...)
	 *
	 * @see #isSameByLevenshtein(String, String)
	 */
	public final static int MAX_LEVENSTHEIN_DISTANCE = 2;

	/**
	 * Pattern to parse or print a OffsetDateTime in ISO compact format.
	 */
	public final static String PATTERN_ISO_COMPACT = "yyyyMMdd_hhmmss";

	/**
	 * Allowed characters for {@link #generateCode(int)}
	 */
	public final static String Chars4Code =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz-_=!";

	/**
	 * Allowed characters for {@link #generatePassword(int)}
	 */
	public final static String Chars4Password =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789*$-+?_&=!%{}()[]/";

	/**
	 * Replaces tabs with space and condenses multiple spaces into one single space.<br />
	 * Value for parameter 'option' in {@link #condense(String, int)}.<br /> Multiple
	 * options can be added.
	 */
	public final static int CONDENSE_BLANKS = 1;

	/**
	 * Removes all tabs, spaces, CRs and LFs.<br /> Value for parameter 'option' in
	 * {@link #condense(String, int)}.<br /> Multiple options can be added.
	 */
	public final static int REMOVE_BLANKS = 2;

	/**
	 * Removes carriage returns (useless Microsoft line break)<br /> Value for parameter
	 * 'option' in {@link #condense(String, int)}.<br /> Multiple options can be added.
	 */
	public final static int REMOVE_CARRIAGE_RETURNS = 4;

	/**
	 * Removes empty lines.<br /> Value for parameter 'option' in
	 * {@link #condense(String, int)}.<br /> Multiple options can be added.
	 */
	public final static int REMOVE_EMPTYLINES = 8;

	/**
	 * Removes end of line comments starting with "//..." until end of line <br /> Value
	 * for
	 * parameter 'option' in {@link #condense(String, int)}.<br /> Multiple options can be
	 * added.
	 */
	public final static int REMOVE_COMMENT_EOL = 16;

	/**
	 * Removes annotation "@Generated(...)"<br /> Value for parameter 'option' in
	 * {@link #condense(String, int)}.<br /> Multiple options can be added.
	 */
	public final static int REMOVE_GENERATED = 32;

	/**
	 * Removes linefeed characters (\n)<br /> Value for parameter 'option' in
	 * {@link #condense(String, int)}.<br /> Multiple options can be added.
	 */
	public final static int REMOVE_LINEFEEDS = 64;

	/**
	 * Removes long comments "&#x2f;&#x2a; ... &#x2a;&#x2f;"<br /> Value for parameter
	 * 'option' in {@link #condense(String, int)}.<br /> Multiple options can be added.
	 */
	public final static int REMOVE_COMMENT_LONG = 128;

	/**
	 * Removes remark lines starting with "# " or "-- "until end of line<br /> Value for
	 * parameter 'option' in {@link #condense(String, int)}.<br /> Multiple options can be
	 * added.
	 */
	public final static int REMOVE_COMMENT_LINES = 256;

	/**
	 * Removes XML comments "&lt;!-- ... --&gt;"<br /> Value for parameter 'option' in
	 * {@link #condense(String, int)}.<br /> Multiple options can be added.
	 */
	public final static int REMOVE_COMMENT_XML = 512;

	/**
	 * Removes tabulator characters<br /> Value for parameter 'option' in
	 * {@link #condense(String, int)}.<br /> Multiple options can be added.
	 */
	public final static int REMOVE_TABS = 1024;

	/**
	 * Removes spaces and tabs at start and end of each line<br /> Value for parameter
	 * 'option' in {@link #condense(String, int)}.<br /> Multiple options can be added.
	 */
	public final static int TRIM_LINES = 2048;

	/**
	 * Collapses the text by reducing all occurrences of white space (tab, blank, cr,
	 * lf) to
	 * a single blank character.<br /> Value for parameter 'option' in
	 * {@link #condense(String, int)}.<br /> Multiple options can be added.
	 */
	public final static int COLLAPSE = 4096;

	/**
	 * Removes all comments<br /> Value for parameter 'option' in
	 * {@link #condense(String, int)}. Values can be added.
	 */
	public final static int REMOVE_COMMENTS = REMOVE_COMMENT_EOL
			+ REMOVE_COMMENT_LONG + REMOVE_COMMENT_LINES + REMOVE_COMMENT_XML;

	/**
	 * Removes all comments, blanks and empty lines.<br /> Value for parameter 'option' in
	 * {@link #condense(String, int)}
	 */
	public final static int REMOVE_ALL = 8191;

	/**
	 * Useless public constructor implemented for Javadoc only
	 */
	public TextHelper() {
	}

	/**
	 * Inserts the given word to the sorted comma separated list.
	 * <p>
	 * The words in the list are sorted. They are separated with commas and the list is
	 * also
	 * enclosed in commas.
	 * </p>
	 * <p>
	 * If list is <em>null</em> then the new word will create a new list.
	 * </p>
	 * <p>
	 * If <em>word</em> is already in the list then the <em>list</em> will be returned
	 * unmodified.
	 * </p>
	 *
	 * @param word The new word to be inserted into the list
	 * @param list The comma enclosed list of comma separated words
	 * @return Returns a new list with the new word
	 */
	public static String addToCommaSeparatedList( String word, String list ) {
		if ( list == null || list.length() < 3 ) {
			return "," + word + ",";
		}
		int res = 0;
		StringBuilder newList = new StringBuilder();
		String[] entries = list.split( "," );
		for ( int i = 1; i < entries.length; i++ ) {
			res = word.compareTo( entries[i] );
			if ( res == 0 ) {
				return list;
			}

			// --- Insert given word
			if ( res < 0 ) {
				newList.append( "," )
						.append( word );
				while ( i < entries.length ) {
					newList.append( "," )
							.append( entries[i++] );
				}
				newList.append( "," );
				return newList.toString();
			}
			newList.append( "," )
					.append( entries[i] );
		}
		newList.append( "," )
				.append( word )
				.append( "," );
		return newList.toString();
	}

	/**
	 * Appends {@code appendix} to {@code source}. This method is <em>null</em> save.
	 *
	 * @param source The source string
	 * @param appendix The string to be appended
	 * @return Returns a new string or <em>null</em> if both {@code source} and
	 * {@code appendix} are <em>null</em>
	 */
	public static String append( String source, String appendix ) {
		if ( source == null ) {
			return appendix;
		}
		return source + appendix;
	}

	/**
	 * Returns a new string with the following changes: replaces CRs, LFs and TABs with a
	 * space and multiple spaces with a single one. Spaces at start and end are removed.
	 *
	 * @param str well ... the string
	 * @return Returns condensed string or {@code null} if the given string is {@code null}
	 */
	public static String cleanupString( String str ) {
		if ( str == null ) {
			return null;
		}
		if ( str.isEmpty() ) {
			return "";
		}
		StringBuilder buf = new StringBuilder( str );
		return (cleanupString( buf ));
	}

	/**
	 * Returns a new string with the following changes: replaces CRs, LFs and TABs with a
	 * space and multiple spaces with a single one. Spaces at start and end are removed.
	 *
	 * @param buf buffer to be cleaned
	 * @return Returns condensed string or {@code null} if the given string is {@code null}
	 */
	public static String cleanupString( StringBuilder buf ) {
		if ( buf == null ) {
			return null;
		}
		int i;

		// ----- Get rid of CR's
		while ( (i = buf.indexOf( "\r" )) >= 0 ) {
			buf.setCharAt( i, ' ' );
		}

		// ----- Get rid of LF's
		while ( (i = buf.indexOf( "\n" )) >= 0 ) {
			buf.setCharAt( i, ' ' );
		}

		// ----- Get rid of tabs
		while ( (i = buf.indexOf( "\t" )) >= 0 ) {
			buf.setCharAt( i, ' ' );
		}

		// ----- Reduce multiple blanks to a single one
		while ( (i = buf.indexOf( "  " )) >= 0 ) {
			buf.deleteCharAt( i );
		}

		// ----- Get rid of leading and trailing blanks
		String str = buf.toString();
		str = trim( str );
		return str;
	}

	/**
	 * Computes the <em>Levensthein</em> distance between the two given words.
	 *
	 * @param word1 First word
	 * @param word2 Second word
	 * @return Returns the number of required changes in word1 to get word2
	 */
	public static int computeLevenshteinDistance( String word1, String word2 ) {
		int len1 = word1.length();
		int len2 = word2.length();
		int[][] distance = new int[len1 + 1][len2 + 1];

		for ( int i = 0; i <= len1; i++ ) {
			distance[i][0] = i;
		}
		for ( int j = 1; j <= len2; j++ ) {
			distance[0][j] = j;
		}

		for ( int i = 1; i <= len1; i++ ) {
			for ( int j = 1; j <= len2; j++ ) {
				distance[i][j] =
						minimum( distance[i - 1][j] + 1, distance[i][j - 1] + 1,
								distance[i - 1][j - 1]
										+ ((word1.charAt( i - 1 ) == word2
										.charAt( j - 1 )) ? 0 : 1) );
			}
		}
		return distance[word1.length()][word2.length()];
	}

	/**
	 * Gets the <em>metaphone</em> code of the given word. This algorithm provides good
	 * results for English names.
	 *
	 * <h4>Metaphone code computation algorithm</h4>
	 * <ol>
	 * <li>Make all letters upper case</li>
	 * <li>Remove all repeating neighboring letters except letter C.</li>
	 * <li>The beginning of the word should be transformed using the following
	 * rules:
	 * <ul>
	 * <li>KN -&gt; N</li>
	 * <li>GN -&gt; N</li>
	 * <li>PN -&gt; N</li>
	 * <li>AE -&gt; E</li>
	 * <li>WR -&gt; R</li>
	 * </ul>
	 * </li>
	 * <li>Remove B at the end if it is behind letter M</li>
	 * <li>Replace C using the following rules:
	 * <ul>
	 * <li>With X: CIA -&gt; XIA, SCH -&gt; SKH, CH -&gt; XH</li>
	 * <li>With S: CI -&gt; SI, CE -&gt; SE, CY -&gt; SY</li>
	 * <li>With K: C -&gt; K</li>
	 * </ul>
	 * </li>
	 * <li>Replace D using the following rules:
	 * <ul>
	 * <li>With J: DGE -&gt; JGE, DGY -&gt; JGY, DGI -&gt; JGY</li>
	 * <li>With T: D -&gt; T</li>
	 * </ul>
	 * </li>
	 * <li>Replace GH -&gt; H, except it is at the end or before a vowel.</li>
	 * <li>Replace GN -&gt; N and GNED -&gt; NED, if they are at the end.</li>
	 * <li>Replace G using the following rules
	 * <ul>
	 * <li>With J: GI -&gt; JI, GE -&gt; JE, GY -&gt; JY</li>
	 * <li>With K: G -&gt; K</li>
	 * </ul>
	 * </li>
	 * <li>Remove all H after a vowel but not before a vowel.</li>
	 * <li>Perform following transformations using the rules below:
	 * <ul>
	 * <li>CK -&gt; K</li>
	 * <li>PH -&gt; F</li>
	 * <li>Q -&gt; K</li>
	 * <li>V -&gt; F</li>
	 * <li>Z -&gt; S</li>
	 * </ul>
	 * </li>
	 * <li>Replace S with X:
	 * <ul>
	 * <li>SH -&gt; XH</li>
	 * <li>SIO -&gt; XIO</li>
	 * <li>SIA -&gt; XIA</li>
	 * </ul>
	 * </li>
	 * <li>Replace T using the following rules
	 * <ul>
	 * <li>With X: TIA -&gt; XIA, TIO -&gt; XIO</li>
	 * <li>With 0: TH -&gt; 0</li>
	 * <li>Remove: TCH -&gt; CH</li>
	 * </ul>
	 * </li>
	 * <li>Transform WH -&gt; W at the beginning. Remove W if there is no vowel
	 * after it.</li>
	 * <li>If X is at the beginning, then replace X -&gt; S, else replace X
	 * -&gt; KS</li>
	 * <li>Remove all Y which are not before a vowel.</li>
	 * <li>Remove all vowels except vowel at the start of the word.</li>
	 * </ol>
	 *
	 * <h4>Examples</h4>
	 * <ul>
	 * <li><b>FXPL</b> -&gt; Fishpool</li>
	 * <li><b>LWRS</b> -&gt; Lowers, Lowerson</li>
	 * </ul>
	 *
	 * @param word The word to be encoded
	 * @return Returns the word as a <em>metaphone</em> string
	 * @see <a href="http://www.sound-ex.de/alternative_zu_soundex.htm">
	 * Alternative zu Soundex</a>
	 */
	public static String computeMetaphone( final String word ) {

		// --- 1. Make all letters uppercase
		char c;
		int i;
		StringBuilder metaphone = new StringBuilder( word.toUpperCase() );

		// --- 2. Remove all doubles except 'CC'
		c = metaphone.charAt( 0 );
		for ( i = 1; i < word.length(); i++ ) {
			if ( c == metaphone.charAt( i ) ) {
				metaphone.deleteCharAt( i );
				i--;
			} else {
				c = metaphone.charAt( i );
			}
		}

		// --- 3. Skip first char in some special cases
		String s = metaphone.substring( 0, 2 );
		if ( s.equals( "AE" ) || s.equals( "GN" ) || s.equals( "KN" )
				|| s.equals( "PN" ) || s.equals( "WR" ) ) {
			metaphone.deleteCharAt( 1 );
		}

		// --- 4. Remove B at the end if it is behind letter M
		if ( (metaphone.charAt( metaphone.length() - 1 ) == 'B')
				&& (metaphone.charAt( metaphone.length() - 2 ) == 'M') ) {
			metaphone.deleteCharAt( metaphone.length() - 1 );
		}

		// --- 5. Replace C using rules
		while ( (i = metaphone.indexOf( "CIA" )) >= 0 ) {
			metaphone.setCharAt( i, 'X' );
		}
		while ( (i = metaphone.indexOf( "SCH" )) >= 0 ) {
			metaphone.setCharAt( i + 1, 'K' );
		}
		while ( (i = metaphone.indexOf( "CH" )) >= 0 ) {
			metaphone.setCharAt( i, 'X' );
		}
		while ( (i = metaphone.indexOf( "CI" )) >= 0 ) {
			metaphone.setCharAt( i, 'S' );
		}
		while ( (i = metaphone.indexOf( "CE" )) >= 0 ) {
			metaphone.setCharAt( i, 'S' );
		}
		while ( (i = metaphone.indexOf( "CY" )) >= 0 ) {
			metaphone.setCharAt( i, 'S' );
		}
		while ( (i = metaphone.indexOf( "C" )) >= 0 ) {
			metaphone.setCharAt( i, 'K' );
		}

		return metaphone.toString();
	}

	/**
	 * Gets the soundex code for the given word.
	 * <p>
	 * This algorithm should only be used for the english name of a person.
	 * </p>
	 *
	 * @param word The word to compute the soundex code for
	 * @return Returns the 4 character soundex string
	 */
	public static String computeSoundex( String word ) {
		// --- Start with first character
		StringBuilder soundex = new StringBuilder(
				"" + Character.toUpperCase( word.charAt( 0 ) ) );
		char c;
		for ( int i = 1; i < word.length(); i++ ) {
			c = Character.toUpperCase( word.charAt( i ) );
			if ( "BFPV".indexOf( c ) >= 0 ) {
				soundex.append( '1' );
			} else if ( "CGJKQSXZ".indexOf( c ) >= 0 ) {
				soundex.append( '2' );
			} else if ( "DT".indexOf( c ) >= 0 ) {
				soundex.append( '3' );
			} else if ( c == 'L' ) {
				soundex.append( '4' );
			} else if ( "MN".indexOf( c ) >= 0 ) {
				soundex.append( '5' );
			} else if ( c == 'R' ) {
				soundex.append( '6' );
			}
			if ( soundex.length() >= 4 ) {
				return soundex.toString();
			}
		}
		// --- Fill with 0 if too short
		while ( soundex.length() < 4 ) {
			soundex.append( '0' );
		}
		return soundex.toString();
	}

	/**
	 * Condenses the given text according to the given options. A line with a backslash '\'
	 * at its end will be continued in the next line. {@code options} is the sum of:
	 * <table>
	 * <caption>Options</caption>
	 * <tr>
	 * <td>{@link #REMOVE_ALL}</td>
	 * <td>combination of all options</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #REMOVE_BLANKS}</td>
	 * <td>removes all blanks like tabs, spaces, CRs and LFs</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #REMOVE_EMPTYLINES}</td>
	 * <td>removes empty lines</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #REMOVE_COMMENT_EOL}</td>
	 * <td>removes "//.." until end of line</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #REMOVE_GENERATED}</td>
	 * <td>removes "@Generated(..)"</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #REMOVE_COMMENT_LONG}</td>
	 * <td>removes long comments "&#x2f;&#x2a; ... &#x2a;&#x2f;"</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #REMOVE_COMMENT_LINES}</td>
	 * <td>removes remark lines starting with "# " or "-- " including end of
	 * line</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #REMOVE_COMMENT_XML}</td>
	 * <td>removes XML comments "&lt;!-- ... --&gt;"</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #TRIM_LINES}</td>
	 * <td>removes spaces and tabs at start and end of each line</td>
	 * </tr>
	 * </table>
	 *
	 * @param text The text to be condensed
	 * @param options The options what is to be removed
	 * @return The condensed text in a new String
	 */
	public static String condense( String text, int options ) {
		// --- Collapses all whitespace chars
		if ( (options & TextHelper.COLLAPSE) > 0 ) {
			text = text.replace( "\r\n", " " );
			text = text.replace( '\n', ' ' );
			text = text.replace( '\t', ' ' );
			text = text.replace( "  ", " " );
		}

		StringBuilder buf = new StringBuilder( text );
		int i = 0, j = 0;

		// --- combine extended lines
		while ( (i = buf.indexOf( "\\\n", i )) > 0 ) {
			buf.delete( i, i + 2 );
		}
		i = 0;
		while ( (i = buf.indexOf( "\\\r\n", i )) > 0 ) {
			buf.delete( i, i + 3 );
		}

		// --- condense blanks
		if ( (options & TextHelper.CONDENSE_BLANKS) > 0 ) {
			i = 0;
			while ( (i = buf.indexOf( "\t", i )) >= 0 ) {
				buf.setCharAt( i, ' ' );
			}
			i = 0;
			while ( (i = buf.indexOf( "  ", i )) >= 0 ) {
				buf.deleteCharAt( i );
			}
		}

		// --- Remove long remarks
		if ( (options & TextHelper.REMOVE_COMMENT_LONG) > 0 ) {
			i = 0;
			while ( (i = buf.indexOf( "/*", i )) >= 0 ) {
				j = buf.indexOf( "*/", i );
				if ( j < 0 ) {
					j = buf.length() - 2;
				}
				buf.delete( i, j + 2 );
			}
		}

		// --- Remove XML comments
		if ( (options & TextHelper.REMOVE_COMMENT_XML) > 0 ) {
			i = 0;
			while ( (i = buf.indexOf( "<!--", i )) >= 0 ) {
				j = buf.indexOf( "-->", i );
				if ( j > 0 ) {
					j += 3;
					buf.delete( i, j );
				}
			}
		}

		// --- Remove comments until end of line
		if ( (options & TextHelper.REMOVE_COMMENT_EOL) > 0 ) {
			i = 0;
			while ( (i = buf.indexOf( "//", i )) >= 0 ) {
				j = buf.indexOf( "\n", i );
				if ( j < 0 ) {
					j = buf.length();
				}
				buf.delete( i, j );
			}
		}

		// --- Remove lines starting with "# " or "-- "
		if ( (options & TextHelper.REMOVE_COMMENT_LINES) > 0 ) {
			while ( buf.charAt( 0 ) == '#'
					|| buf.substring( 0, 3 )
					.equals( "-- " ) ) {
				j = buf.indexOf( "\n" );
				if ( j < 0 ) {
					j = buf.length() - 1;
				}
				buf.delete( 0, j + 1 );
			}
			while ( (i = buf.indexOf( "\n#" )) >= 0 ) {
				j = buf.indexOf( "\n", i + 1 );
				if ( j < 0 ) {
					j = buf.length() - 1;
				}
				buf.delete( i + 1, j + 1 );
			}
			while ( (i = buf.indexOf( "\n-- " )) >= 0 ) {
				j = buf.indexOf( "\n", i + 1 );
				if ( j < 0 ) {
					j = buf.length() - 1;
				}
				buf.delete( i + 1, j + 1 );
			}
		}

		// --- Remove all spaces (this deletes empty lines as well)
		if ( (options & TextHelper.REMOVE_BLANKS) > 0 ) {
			i = 0;
			while ( (i = buf.indexOf( " ", i )) >= 0 ) {
				buf.deleteCharAt( i );
			}
			i = 0;
			while ( (i = buf.indexOf( "\n", i )) >= 0 ) {
				buf.deleteCharAt( i );
			}
			i = 0;
			while ( (i = buf.indexOf( "\r", i )) >= 0 ) {
				buf.deleteCharAt( i );
			}
			i = 0;
			while ( (i = buf.indexOf( "\t", i )) >= 0 ) {
				buf.deleteCharAt( i );
			}
		}

		// --- Remove CRs
		if ( (options & TextHelper.REMOVE_CARRIAGE_RETURNS) > 0 ) {
			i = 0;
			while ( (i = buf.indexOf( "\r", i )) >= 0 ) {
				buf.deleteCharAt( i );
			}
		}

		// --- Remove line feeds
		if ( (options & TextHelper.REMOVE_LINEFEEDS) > 0 ) {
			i = 0;
			while ( (i = buf.indexOf( "\n", i )) >= 0 ) {
				buf.deleteCharAt( i );
			}
		}

		// --- Remove Tabs
		if ( (options & TextHelper.REMOVE_TABS) > 0 ) {
			i = 0;
			while ( (i = buf.indexOf( "\t", i )) >= 0 ) {
				buf.deleteCharAt( i );
			}
		}

		// --- Remove empty lines
		if ( (options & TextHelper.REMOVE_EMPTYLINES) > 0 ) {
			while ( buf.charAt( 0 ) == '\n' ) {
				buf.deleteCharAt( 0 );
			}
			while ( (i = buf.indexOf( "\r\n\r\n" )) >= 0 ) {
				buf.delete( i, i + 2 );
			}
			while ( (i = buf.indexOf( "\n\n" )) > 0 ) {
				buf.deleteCharAt( i );
			}
		}

		// --- Remove @Generated(...)
		if ( (options & TextHelper.REMOVE_GENERATED) > 0 ) {
			while ( (i = buf.indexOf( "@Generated", i )) >= 0 ) {
				j = buf.indexOf( ")", i );
				if ( j > 0 ) {
					buf.delete( i, j + 1 );
				}
			}
		}

		// --- Trim lines => removes spaces and tabs at start and end of line
		if ( (options & TextHelper.TRIM_LINES) > 0 ) {
			while ( (i = buf.indexOf( "\n " )) >= 0 ) {
				buf.deleteCharAt( i + 1 );
			}
			while ( (i = buf.indexOf( "\n\t" )) >= 0 ) {
				buf.deleteCharAt( i + 1 );
			}
			while ( (i = buf.indexOf( " \n" )) >= 0 ) {
				buf.deleteCharAt( i );
			}
			while ( (i = buf.indexOf( " \r" )) >= 0 ) {
				buf.deleteCharAt( i );
			}
			while ( (i = buf.indexOf( "\t\n" )) >= 0 ) {
				buf.deleteCharAt( i );
			}
			while ( (i = buf.indexOf( "\t\r" )) >= 0 ) {
				buf.deleteCharAt( i );
			}
		}

		return buf.toString();
	}

	/**
	 * Returns the offset of the matching char in the string.
	 * <p>
	 * Matching characters are:
	 * <ul>
	 * <li>'(' -&gt; ')'</li>
	 * <li>'[' -&gt; ']'</li>
	 * <li>'{' -&gt; '}'</li>
	 * <li>'&lt;' -&gt; '&gt;'</li>
	 * <li>same character as at position {@code start}</li>
	 * </ul>
	 *
	 * @param text text to be searched for the matching char
	 * @param start index to the opening char
	 * @return offset to the closing char or -1 if not found
	 */
	public static int findMatchingCharacter( String text, int start ) {
		char c, match;
		int depth = 1;
		int i = 0, len = text.length();

		// --- Determine matching char
		c = text.charAt( start );
		if ( c == '(' ) {
			match = ')';
		} else if ( c == '[' ) {
			match = ']';
		} else if ( c == '{' ) {
			match = '}';
		} else if ( c == '<' ) {
			match = '>';
		} else {
			match = c;
			depth = 0;
		}

		// --- Scan string
		for ( i = start + 1; i < len; i++ ) {
			if ( text.charAt( i ) == match ) {
				depth--;
				if ( depth <= 0 ) {
					return i;
				}
			} else if ( text.charAt( i ) == c ) {
				// --- Its just the same char
				if ( depth == 0 ) {
					return i;
				}
				// --- One more opening
				depth++;
			}
		}
		return -1;
	}

	/**
	 * Generates a code of given length from characters {@link #Chars4Code}.
	 *
	 * @param length length of code
	 * @return random code
	 */
	public static String generateCode( int length ) {
		return generateRandomString( Chars4Code, length );
	}

	/**
	 * Generates a code of given length from characters specified by type.
	 *
	 * @param length length of code
	 * @param type 0 = only capital letters<br/> 1 = 0 + digits<br/> 2 = 1 + small
	 * letters<br/> 3 = 2 + "-_=!"<br/> 4 = 2 + "*$-+?_&lt;=!%{}()[]/";
	 * @return random code
	 */
	public static String generateCode( int length, int type ) {
		if ( type < 0 || type > 4 ) {
			type = 1;
		}
		String selectables = (type == 0) ? Chars4Code.substring( 0, 26 )
				: (type == 1) ? Chars4Code.substring( 0, 36 )
				: (type == 2) ? Chars4Code.substring( 0, 62 )
				: (type == 3) ? Chars4Code : Chars4Password;
		return generateRandomString( selectables, length );
	}

	/**
	 * Generates a random password with the given length from characters
	 * {@value #Chars4Password}
	 *
	 * @param length length of password
	 * @return random password
	 */
	public static String generatePassword( int length ) {
		return generateRandomString( Chars4Password, length );
	}

	/**
	 * Generates a string of given length with random characters from
	 * {@code selectableCharacters}.
	 *
	 * @param selectableCharacters chars allowed
	 * @param length length of generated string
	 * @return random string
	 */
	public static String generateRandomString( String selectableCharacters,
			int length ) {
		Random rand = new Random();
		char[] text = new char[length];
		int size = selectableCharacters.length();
		for ( int i = 0; i < length; i++ ) {
			text[i] = selectableCharacters.charAt( rand.nextInt( size ) );
		}
		return new String( text );
	}

	/**
	 * Returns a new string of contiguous characters out of the given string starting at
	 * position <code>offset</code>.
	 * <p>
	 * The sequence of contiguous characters starts with the first non-blank character. It
	 * ends when any character is found which is neither a letter, a digit nor an
	 * underscore.
	 * </p>
	 * <p>
	 * If special characters should be included in the string (like a dot '.' in a package
	 * name) these have to be given in the parameter
	 * <code>includes</code>.
	 * </p>
	 *
	 * @param str The string from which a contiguous sequence of characters should be
	 * extracted
	 * @param offset Reads contiguous characters starting at this offset
	 * @param includes A string of characters which may occur in the contiguous string in
	 * addition to letters and digits or null if only letters and digits.
	 * @return Returns a new string of contiguous characters
	 */
	public static String getContiguous( String str, int offset,
			String includes ) {
		char c;
		int i;

		offset = skipSpace( str, offset );
		for ( i = offset; i < str.length(); i++ ) {
			c = str.charAt( i );
			if ( Character.isLetterOrDigit( c ) ) {
				continue;
			}
			if ( c == '_' ) {
				continue;
			}
			if ( (includes != null) && (includes.indexOf( c ) >= 0) ) {
				continue;
			}
			break;
		}
		return str.substring( offset, i );
	}

	/**
	 * Gets the next word from the given string which starts with a letter. The word may
	 * contain characters which are letters, digits, underscore or point. Any other
	 * character ends building the word.
	 *
	 * @param str The string from which to get the next Java name
	 * @return Returns the Java name or null when none found
	 */
	public static String getJavaName( String str ) {
		String name = null;
		char c = 0;
		int i = 0;
		while ( i < str.length() ) {
			c = str.charAt( i++ );
			// --- Start of Java name found
			if ( Character.isLetter( c ) ) {
				name = "" + c;
				while ( i < str.length() ) {
					c = str.charAt( i++ );
					if ( Character.isLetter( c ) || Character.isDigit( c )
							|| (c == '_') || (c == '.') ) {
						name += c;
					} else {
						break;
					}
				}
				break;
			}
		}
		return name;
	}

	/**
	 * Gets the last word of a string.
	 * <p>
	 * The words in the string can be separated by '/', ' ', '.' or '-'. If none of this
	 * separation characters was found then the text itself will be returned;
	 *
	 * @param text string like "abc.def"
	 * @return last word or text itself
	 */
	public static String getLastWord( String text ) {
		if ( text == null || text.isBlank() ) {
			return text;
		}
		// --- determine separation character
		String separationChar = text.contains( "." ) ? "."
				: text.contains( "/" ) ? "/"
				: text.contains( "-" ) ? "-"
				: text.contains( " " ) ? " " : "";
		String[] parts = text.split( separationChar );
		return parts[parts.length - 1];
	}

	/**
	 * Gets the location for the given index into the text. Returns the string "line l
	 * column c".
	 *
	 * @param text The text
	 * @param offset The offset into the text
	 * @return Returns the location
	 */
	public static String getLocation( String text, int offset ) {
		String str = "line ";
		int line = 1, lineStart = 0;
		int len = text.length();
		for ( int i = 0; i < offset && i < len; i++ ) {
			if ( text.charAt( i ) == '\n' ) {
				line++;
				lineStart = i;
			}
		}
		int column = offset - lineStart;
		str += line + " column " + column;
		return str;
	}

	/**
	 * Gets the normalized phone number from the given text by applying the following
	 * rules:
	 * <ol>
	 * <li>a '+' at start will be replaced by "00"</li>
	 * <li>all separators (blanks and dashes) will be removed</li>
	 * <li>digits will be accepted</li>
	 * <li>any other character ends parsing</li>
	 * </ol>
	 *
	 * @param text The phone number to be normalized
	 * @return the normalized phone number
	 */
	public static String getNormalizedPhone( String text ) {
		if ( isEmpty( text ) ) {
			return null;
		}
		if ( text.startsWith( "+" ) ) {
			text = "00" + text.substring( 1 );
		}
		StringBuilder normalizedNbr = new StringBuilder();
		char ch = 0;
		for ( int i = 0; i < text.length(); i++ ) {
			ch = text.charAt( i );
			if ( Character.isDigit( ch ) ) {
				normalizedNbr.append( ch );
			} else if ( Character.isWhitespace( ch ) || ch == '-'
					|| ch == '/' ) {
			} else {
				break;
			}
		}
		return normalizedNbr.toString();
	}

	/**
	 * Gets the word from the given string starting at the given offset. A word consists of
	 * letters. It finishes when anything comes in the text which is not a letter.
	 *
	 * @param str The text from which to extract the word
	 * @param offset Current offset into the text
	 * @return Returns a new string with the next word from the text
	 */
	public static String getWord( String str, int offset ) {
		int j = offset;
		while ( j < str.length() ) {
			if ( !Character.isLetter( str.charAt( j ) ) ) {
				break;
			}
			j++;
		}
		return str.substring( offset, j );
	}

	/**
	 * Checks if {@code digits} does only contain digits. Must contain at least one
	 * digit to
	 * return {@code true}
	 *
	 * @param digits String to check
	 * @return {@code true} if contains only digits
	 */
	public static boolean isDigitsOnly( String digits ) {
		if ( (digits == null) || digits.isEmpty() ) {
			return false;
		}
		for ( int i = 0; i < digits.length(); i++ ) {
			if ( !Character.isDigit( digits.charAt( i ) ) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if text is either null or empty (length = 0).
	 *
	 * @param text text
	 * @return {@code false} if text contains any characters
	 */
	public static boolean isEmpty( String text ) {
		return (text == null) || text.isEmpty();
	}

	/**
	 * Checks the given string if it means <em>false</em>. This is the case when the string
	 * is "false", "no" or "0". Everything else returns
	 * <em>false</em>. Matching is not case sensitive.
	 *
	 * @param booleanString The string to be analyzed
	 * @return Returns <em>true</em> if the string means "false"
	 */
	public static boolean isFalse( String booleanString ) {
		return (booleanString != null)
				&& (booleanString.equalsIgnoreCase( "false" )
				|| booleanString.equalsIgnoreCase( "no" )
				|| booleanString.equals( "0" ));
	}

	/**
	 * Computes the <em>Levenshtein</em> distance between the two words. If the distance is
	 * smaller than 20% (rounded up) of the length of the first word, then {@code true}
	 * will
	 * be returned.
	 *
	 * @param word1 The first word
	 * @param word2 The second word
	 * @return Returns {@code false} if more than 20% of the characters must be changed to
	 * get the second word
	 */
	public static boolean isSameByLevenshtein( String word1, String word2 ) {
		int distance = computeLevenshteinDistance( word1, word2 );
		int len = word1.length();
		return distance * 10 <= len * MAX_LEVENSTHEIN_DISTANCE;
	}

	/**
	 * Checks the given string if it means true. This is the case when the string is
	 * "true",
	 * "yes" or "1". Everything else returns false. Matching is not case sensitive.
	 *
	 * @param booleanString The string to be analyzed
	 * @return Returns true if the string means true
	 */
	public static boolean isTrue( String booleanString ) {
		return (booleanString != null)
				&& (booleanString.equalsIgnoreCase( "true" )
				|| booleanString.equalsIgnoreCase( "yes" )
				|| booleanString.equals( "1" ));
	}

	/**
	 * Makes a string from the given object. If the object is a collection (array or list)
	 * its values will be returned separated by commas.
	 *
	 * @param object some object
	 * @return String string representation (could be empty)
	 */
	public static String makeString( Object object ) {
		StringBuilder s = new StringBuilder();
		if ( object instanceof Collection ) {
			for ( Object obj : (Collection<?>) object ) {
				s.append( "," )
						.append( obj.toString() );
			}
			if ( s.length() > 1 ) {
				s = new StringBuilder( s.substring( 1 ) );
			}
		}
		return s.toString();
	}

	/**
	 * Gets minimum of three supplied values.
	 *
	 * @param a first value
	 * @param b second value
	 * @param c third value
	 * @return smallest value
	 */
	public static int minimum( int a, int b, int c ) {
		return Math.min( Math.min( a, b ), c );
	}

	/**
	 * Parses the given text as a decimal number.
	 *
	 * @param text to be parsed
	 * @return a new BigDecmial or <em>null</em> if the text is not a number at all
	 */
	public static BigDecimal parseBigDecimal( String text ) {
		if ( text == null || text.isBlank() ) {
			return null;
		}
		text = parseNumber( text );
		if ( text == null ) {
			return null;
		}
		return new BigDecimal( text );
	}

	/**
	 * Parses the string argument as a boolean. The boolean returned represents the value
	 * true if the string argument is not null and is equal, ignoring case, to the string
	 * "true".
	 *
	 * @param text text to parse
	 * @return <em>true</em> if the text is 'true' ignoring case
	 */
	public static Boolean parseBoolean( String text ) {
		if ( isEmpty( text ) ) {
			return null;
		}
		return Boolean.parseBoolean( text );
	}

	/**
	 * Parses the given text as a signed byte value (8 bit). Skips leading blanks. Reads
	 * digits for the value until the first non-digit.
	 *
	 * @param text Text to be parsed.
	 * @return Returns the value or <em>null</em> if the text does not have digits
	 */
	public static Byte parseByte( final String text ) {
		Long lval = parseLong( text );
		if ( lval == null ) {
			return null;
		}
		return lval.byteValue();
	}

	/**
	 * Parses the given input text according to ISO pattern (
	 * {@value #PATTERN_ISO_COMPACT}).
	 *
	 * @param text input
	 * @return new instance of {@code LocalDate} or {@code null}
	 */
	public static LocalDate parseDate( final String text ) {
		return parseDate( text, PATTERN_ISO_COMPACT );
	}

	/**
	 * Parses {@code text} according to {@code pattern}.
	 *
	 * @param text input
	 * @param pattern pattern to use for parsing
	 * @return new instance of {@code LocalDate} or {@code null}
	 */
	public static LocalDate parseDate( final String text,
			final String pattern ) {
		if ( isEmpty( text ) ) return null;
		DateTimeParser parser = new DateTimeParser();
		return parser.parseDate( text, pattern );
	}

	/**
	 * Parses the given input text according to ISO pattern (
	 * {@value #PATTERN_ISO_COMPACT})
	 * with time.
	 *
	 * @param text The text to be parsed
	 * @return a new instance of {@code OffsetDateTime} or {@code null}
	 */
	public static OffsetDateTime parseDateTime( final String text ) {
		return parseDateTime( text, PATTERN_ISO_COMPACT );
	}

	/**
	 * Parses {@code text} to an {@code OffsetDateTime} by using {@code pattern}
	 *
	 * @param text ... to parse
	 * @param pattern used for parsing
	 * @return {@code OffsetDateTime} or {@code null}
	 */
	public static OffsetDateTime parseDateTime( final String text,
			final String pattern ) {
		if ( isEmpty( text ) ) {
			return null;
		}
		DateTimeParser parser = new DateTimeParser();
		return parser.parseDateTime( text, pattern );
	}

	/**
	 * Parses the given text for a double value.
	 *
	 * @param text A double value in text form
	 * @return Returns the double or <em>null</em> if the text is null or no double
	 */
	public static Double parseDouble( final String text ) {
		if ( isEmpty( text ) ) {
			return null;
		}
		Double d = null;
		try {
			d = Double.valueOf( text );
		} catch ( NumberFormatException e ) {
			// --- Just return null
		}
		return d;
	}

	/**
	 * Parses the given text for a float value.
	 *
	 * @param text A float value in text form
	 * @return Returns the float or <em>null</em> if the text is null or no float
	 */
	public static Float parseFloat( final String text ) {
		if ( isEmpty( text ) ) {
			return null;
		}
		Float f = null;
		try {
			f = Float.valueOf( text );
		} catch ( NumberFormatException e ) {
			// --- Just return null
		}
		return f;
	}

	/**
	 * Parses the given text as a hexadecimal string. Skips leading blanks. Reads digits
	 * for
	 * the value until the first non-digit.
	 *
	 * @param text Text to be parsed
	 * @return long value or {@code null} if the text is not hex formatted
	 */
	public static Long parseHex( final String text ) {
		final String hexChars = "0123456789abcdef";
		if ( text == null ) {
			return null;
		}
		String byteString = text.stripLeading()
				.toLowerCase();
		if ( !text.startsWith( "0x" ) || text.length() < 3 ) {
			return null;
		}
		// --- Build value from left to right
		long hexVal = 0L;
		int digitIndex = 0;
		for ( int idx = 2; idx < byteString.length()
				&& digitIndex >= 0; idx++ ) {
			digitIndex = hexChars.indexOf( byteString.charAt( idx ) );
			hexVal = hexVal * 16 + digitIndex;
		}
		return hexVal;
	}

	/**
	 * Parses the given text as a signed integer value (32 bit). Skips leading blanks.
	 * Reads
	 * digits for the value until the first non-digit.
	 *
	 * @param text Text to be parsed.
	 * @return signed integer value or {@code null} if the text does not have digits
	 */
	public static Integer parseInteger( final String text ) {
		Long lval = parseLong( text );
		if ( lval == null ) {
			return null;
		}
		return lval.intValue();
	}

	/**
	 * Parses the given text as a signed long integer value (64 bit). Skips leading blanks.
	 * Reads digits for the value until the first non-digit.
	 *
	 * @param text Text to be parsed.
	 * @return Return the value or <em>null</em> if the text does not have digits
	 */
	public static Long parseLong( final String text ) {
		if ( isEmpty( text ) ) {
			return null;
		}
		int sign = 1;
		long result = 0L;
		int i = skipSpace( text, 0 );
		if ( i >= text.length() ) {
			return null;
		}
		char c = text.charAt( i );
		if ( c == '-' ) {
			sign = -1;
			i++;
			if ( i >= text.length() ) {
				return null;
			}
			c = text.charAt( i );
		}
		if ( !Character.isDigit( c ) ) {
			return null;
		}
		while ( true ) {
			if ( c != '_' ) {
				if ( !Character.isDigit( c ) ) {
					break;
				}
				result = result * 10 + (c - '0');
			}
			i++;
			if ( i >= text.length() ) {
				break;
			}
			c = text.charAt( i );
		}
		return sign * result;
	}

	/******************************************************************
	 * Parses the given text as a number.
	 *
	 * @param text
	 *            The text to be parsed
	 * @return Returns the number as "[-]integer[.fraction]" or <em>null</em> if
	 *         the text is not a number at all
	 */
	public static String parseNumber( final String text ) {
		if ( text == null || text.isEmpty() ) {
			return null;
		}
		int i = skipSpace( text, 0 );
		if ( i >= text.length() ) {
			return null;
		}

		// --- Skip leading '+'
		if ( text.charAt( i ) == '+' ) {
			i++;
		}
		StringBuilder buf = new StringBuilder( text.substring( i ) );

		// --- Prepare analysis of thousands and decimal separator
		final int NONE = 0, COMMA = 1, DOT = 2, BOTH = 3;
		int separators = NONE;
		int lastSepPos = -1;
		int commaCount = 0, dotCount = 0, digitCount = 0;

		// --- Scan to end of number
		char c = 0;
		for ( i = 0; i < buf.length(); i++ ) {
			c = buf.charAt( i );

			// --- Digit => just continue
			if ( Character.isDigit( c ) ) {
				digitCount++;
				continue;
			}

			// --- Its a comma
			else if ( c == ',' ) {
				lastSepPos = i;
				separators |= COMMA;
				commaCount++;
				continue;
			}

			// --- Its a dot
			else if ( c == '.' ) {
				lastSepPos = i;
				separators |= DOT;
				dotCount++;
				continue;
			}

			// --- Leading minus
			else if ( c == '-' ) {
				if ( i == 0 ) {
					continue;
				}
			}

			// --- End of number reached => strip it
			buf.delete( i, buf.length() );
		}

		if ( digitCount == 0 ) {
			return null;
		}

		// --- No separators at all => done
		if ( separators == NONE ) {
		} else if ( separators == BOTH ) {
			parseNumberCleanup( buf, lastSepPos );
		}

		// --- Only one separator exists
		else {
			i = commaCount + dotCount;

			// --- Multiple times => must be a thousands separator
			if ( i > 1 ) {
				parseNumberCleanup( buf, -1 );
			}

			// --- Just one separator
			else {
				int fractionDigits = buf.length() - lastSepPos;

				// --- If 3 fraction digits => interpret as thousands separator
				if ( fractionDigits == 4 ) {
					parseNumberCleanup( buf, -1 );
				} else {
					parseNumberCleanup( buf, lastSepPos );
				}
			}
		}
		return buf.isEmpty() ? null : buf.toString();
	}

	/**
	 * Sets the separator at <em>index</em> to the Java decimal separator DOT and removes
	 * all other separators. If <em>index</em> is zero then just all separators will be
	 * cleaned from the buffer.
	 *
	 * @param buf The buffer with the number in text format
	 * @param index Position of the decimal separator
	 */
	private static void parseNumberCleanup( StringBuilder buf, int index ) {
		if ( index > 0 ) {
			buf.setCharAt( index, '|' );
		}
		while ( (index = buf.indexOf( "." )) >= 0 ) {
			buf.deleteCharAt( index );
		}
		while ( (index = buf.indexOf( "," )) >= 0 ) {
			buf.deleteCharAt( index );
		}
		index = buf.indexOf( "|" );
		if ( index > 0 ) {
			buf.setCharAt( index, '.' );
		}
	}

	/**
	 * Parses the given text as a signed short value (16 bit). Skips leading blanks. Reads
	 * digits for the value until the first non-digit.
	 *
	 * @param text Text to be parsed.
	 * @return Returns the value or <em>null</em> if the text does not have digits
	 */
	public static Short parseShort( final String text ) {
		Long value = parseLong( text );
		return (value == null) ? null : value.shortValue();
	}

	/**
	 * Parses {@code text} into local time.
	 *
	 * @param text input
	 * @return LocalTime or {@code null}
	 */
	public static LocalTime parseTime( final String text ) {
		if ( isEmpty( text ) ) return null;
		DateTimeParser parser = new DateTimeParser();
		return parser.parseTime( text );
	}

	/**
	 * Parses {@code text} into {@code TimeZone}
	 *
	 * @param text ... to parse
	 * @return {@code TimeZone} or {@code null} if {@code text} was null or empty
	 */
	public static TimeZone parseTimeZone( final String text ) {
		if ( isEmpty( text ) ) return null;

		//--- its a Duration
		if ( text.startsWith( "PT" ) || text.startsWith( "-PT" ) ) {
			Duration duration = Duration.parse( text );
			return BaseConverter.toTimeZoneFromMinutes( duration.toMinutes() );
		}

		//--- if its just an integer in minutes then it must be a multiple of 60 or 90
		Integer minutes = parseInteger( text );
		if ( (minutes != null) && ((minutes % 60 == 0) || (minutes % 90 == 0)) ) {
			return BaseConverter.toTimeZoneFromMinutes( minutes );
		}

		//--- seems to be a ZoneId
		try {
			ZoneId zoneId = ZoneId.of( text );
			return TimeZone.getTimeZone( zoneId );
		} catch ( DateTimeException e ) {
			return null;
		}
	}

	/**
	 * Parses {@code text} as a UUID (128bit).
	 * <p>
	 * A UUID in string format is always 36 chars long and conform to pattern:
	 * <pre>xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx</pre>
	 * A Base64 encoded UUID is 22 to 24 chars long (char 23 and 24 would be the padding
	 * char '=').
	 *
	 * @param text uuid in string format or Base64 encoded
	 * @return uuid or {@code null}
	 */
	public static UUID parseUuid( final String text ) {
		if ( text == null || text.isBlank() ) return null;

		// --- UUID in string format
		if ( text.length() == 36 ) return UUID.fromString( text );

		//--- UUID must be Base64 encoded
		byte[] bytes = Base64.getDecoder().decode( text );
		long high = BaseConverter.bytesToLong( bytes, 0 );
		long low = BaseConverter.bytesToLong( bytes, 0 );
		return new UUID( high, low );
	}

	/**
	 * Removes the given word from the comma separated list.
	 *
	 * @param word The word to be removed
	 * @param list The current list
	 * @return new list with the removed word. If the new list is empty then {@code null}
	 * will be returned.
	 */
	public static String removeFromCommaSeparatedList( String word,
			String list ) {
		if ( (list == null) || (list.length() < 2) ) {
			return null;
		}
		int res = 0;
		StringBuilder newList = new StringBuilder();
		String[] words = list.split( "," );
		for ( int i = 1; i < words.length; i++ ) {
			res = word.compareTo( words[i] );
			if ( res != 0 ) {
				newList.append( "," )
						.append( words[i] );
			}
		}
		newList.append( "," );
		return newList.toString();
	}

	/**
	 * Skip spaces in string starting at given position.
	 *
	 * @param str The string
	 * @return Returns new string without leading spaces
	 */
	public static String skipSpace( String str ) {
		int i = 0;
		while ( Character.isWhitespace( str.charAt( i ) ) ) {
			i++;
		}
		if ( i > 0 ) {
			str = str.substring( i );
		}
		return str;
	}

	/**
	 * Skip spaces in string starting at given position.
	 *
	 * @param str The string which may start with some space characters
	 * @param start starting index into string
	 * @return Returns index to first non-space character
	 */
	public static int skipSpace( String str, int start ) {
		while ( start < str.length() ) {
			if ( !Character.isWhitespace( str.charAt( start ) ) ) {
				return start;
			}
			start++;
		}
		return start;
	}

	/**
	 * Splits the given text into separate lines. If a line ends with the backslash
	 * character '\' then the next line will be appended to the current one.
	 *
	 * @param text The text (e.g. from a file) with multiple line breaks
	 * @return lines as a list of strings
	 */
	public static List<String> splitIntoLines( final String text ) {
		int backslashIndex = -1, microsoftDesignErrorIndex = -1;
		int from = 0, to = 0, size = text.length();
		StringBuilder line = new StringBuilder();
		List<String> lines = new ArrayList<>();
		for ( int i = 0; i < size; i++ ) {
			if ( text.charAt( i ) == '\\' ) {
				backslashIndex = i;
			} else if ( text.charAt( i ) == '\r' ) {
				microsoftDesignErrorIndex = i;
			} else if ( text.charAt( i ) == '\n' ) {
				to = i;
				if ( microsoftDesignErrorIndex == to - 1 ) {
					to--;
				}
				if ( backslashIndex == to - 1 ) {
					line.append( text, from, backslashIndex );
				} else {
					line.append( text, from, to );
					lines.add( line.toString() );
					line = new StringBuilder();
				}
				from = i + 1;
			}
		}
		return lines;
	}

	/**
	 * Substitutes variables in the given text by their values. Values are taken from the
	 * given properties. The given text remains unmodified. A new text will be returned.
	 * The
	 * text itself cannot specify any variables.
	 *
	 * @param text The text with variables "$(propname)" or "${propname}
	 * @param props The properties
	 * @param nocb Do not replace variables in curly braces "${propname}"
	 * @return Returns a new text with replaced variables
	 * @throws SyntaxException If a variable is not closed or when it is not found in
	 * properties
	 */
	public static String substituteVariables( String text, Properties props,
			boolean nocb ) throws SyntaxException {
		int i = 0, j = 0;
		StringBuilder buf = new StringBuilder( text );
		String varname = null, varvalue = null;
		while ( (i = buf.lastIndexOf( "$(" )) >= 0 ) {
			j = buf.indexOf( ")", i );
			if ( j <= 0 ) {
				throw new SyntaxException(
						"Variable '" + buf.substring( i, i + 20 )
								+ "...' is not closed with ')' in "
								+ getLocation( text, i ) );
			}
			varname = buf.substring( i + 2, j );
			varvalue = props.getProperty( varname );
			if ( varvalue == null ) {
				throw new SyntaxException( "Variable '" + varname
						+ "' does not exist in " + getLocation( text, i ) );
			}
			buf.replace( i, j + 1, varvalue );
		}
		if ( !nocb ) {
			while ( (i = buf.lastIndexOf( "${" )) >= 0 ) {
				j = buf.indexOf( "}", i );
				if ( j <= 0 ) {
					throw new SyntaxException(
							"Variable '" + buf.substring( i, i + 20 )
									+ "...' is not closed with '}' in "
									+ getLocation( text, i ) );
				}
				varname = buf.substring( i + 2, j );
				varvalue = props.getProperty( varname );
				if ( varvalue == null ) {
					throw new SyntaxException( "Variable '" + varname
							+ "' does not exist in " + getLocation( text, i ) );
				}
				buf.replace( i, j + 1, varvalue );
			}
		}
		return buf.toString();
	}

	/**
	 * Returns a new string from which leading and trailing blanks [ \n\r\t] are removed .
	 * If the given string is {@code null} or empty then {@code null} will be returned
	 *
	 * @param str String to trim
	 * @return new string without leading and trailing blanks
	 */
	public static String trim( String str ) {
		if ( str == null ) {
			return null;
		}
		if ( str.isBlank() ) {
			return "";
		}
		int i = 0, j = 0, len = str.length();

		// ----- Find start of string
		for ( i = 0; i < len; i++ ) {
			if ( str.charAt( i ) != ' ' && str.charAt( i ) != '\t' ) {
				break;
			}
		}

		// ----- Find end of string
		for ( j = len; j > i; j-- ) {
			if ( str.charAt( j - 1 ) != 0 && str.charAt( j - 1 ) != ' '
					&& str.charAt( j - 1 ) != '\n'
					&& str.charAt( j - 1 ) != '\r'
					&& str.charAt( j - 1 ) != '\t' ) {
				break;
			}
		}

		// ----- Return trimmed string
		if ( i > 0 || j < len ) {
			str = str.substring( i, j );
		}
		return str;
	}
}