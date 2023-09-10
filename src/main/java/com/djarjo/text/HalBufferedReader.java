package com.djarjo.text;

import java.io.IOException;
import java.io.Reader;

/**********************************************************************
 * Reads lines from a character stream reader.
 * <p>
 * Options can be set by calling {@link #withOptions(int)}. Options are the sum
 * of:
 * <ul>
 * <li>(2) SKIP_EMPTYLINES skips lines which are of length zero or only contain
 * blanks</li>
 * <li>(4) SKIP_EOLREMARK skips all characters including "//" or "#" until end
 * of line</li>
 * <li>(8) SKIP_MULTILINEREMARK skips all characters from "/*" until and
 * including "&#42;&#47;"</li>
 * <li>(16) SKIP_XMLREMARK skips all characters from "<!--" until and including
 * "-->"</li>
 * <li>(32) UNFOLDING appends next line if it starts with a space. According to
 * RFC5322</li>
 * </ul>
 *********************************************************************/
public class HalBufferedReader extends Reader {

	// --- Public constants (entries must be: 2^n)
	/** No option at all */
	final public static int NO_OPTIONS = 0;

	/** Skip empty lines */
	final public static int SKIP_EMPTYLINES = 2;

	/** Skip comments "//..." or "#..." until end of line */
	final public static int SKIP_EOLCOMMENTS = 4;

	/** Removes long comments "&#x2f;&#x2a; ... &#x2a;&#x2f;" */
	final public static int SKIP_LONGCOMMENTS = 8;

	/** Removes XML comments "&lt;!-- ... --&gt;" */
	final public static int SKIP_XMLCOMMENTS = 16;

	/**
	 * Appends next line if it starts with a single space or tab while dropping
	 * line break and the space (see RFC5322)
	 */
	final public static int UNFOLDING = 32;

	private int options = 0;

	// --- The reader delivering the characters
	private Reader reader = null;

	// --- Location information
	private int charsRead = 0;
	private int colNumber = 0;
	private int lineNumber = 0;

	// --- Round-robin buffer for reading characters from file
	final private int BUFSIZE = 4096;
	private char[] cbuf = new char[BUFSIZE];

	// --- Pointer into the buffer
	private int offs = BUFSIZE;
	private int end = 0;
	private int endOfBuffer = 0;

	/******************************************************************
	 * Constructs a new HalBufferedReader.
	 *
	 * @param reader
	 *            use this reader
	 */
	public HalBufferedReader( Reader reader ) {
		this.reader = reader;
	}

	/**
	 * Closes the <em>reader</em> which was given to the constructor.
	 */
	@Override
	public void close() throws IOException {
		reader.close();
	}

	/**
	 * Gets the current line number
	 *
	 * @return Returns line number
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * Gets the current location.
	 *
	 * @return Return the current location as a string
	 */
	public String getLocation() {
		String s = "Line " + lineNumber + " pos " + colNumber + " Total="
				+ charsRead;
		return s;
	}

	/******************************************************************
	 * Gets options of this reader.
	 *
	 * @return sum of option combinations
	 */
	public int getOptions() {
		return options;
	}

	@Override
	public int read( char[] cbuf, int off, int len ) throws IOException {
		return reader.read( cbuf, off, len );
	}

	/******************************************************************
	 * Reads the next line from a file according to the current options.
	 *
	 * @return next line as a string or null on end of file.
	 * @throws IOException
	 *             on read error
	 */
	public String readLine() throws IOException {
		// --- Already at end of file
		if ( endOfBuffer < 0 ) {
			return null;
		}

		// --- Copy chars for new line always to start of buffer
		end = 0;
		char c;
		int skipping = 0;

		while ( true ) {
			// --- Load next block from file
			if ( offs >= BUFSIZE ) {
				endOfBuffer = end + reader.read( cbuf, end, BUFSIZE - end );
				offs = 0;
			}

			// --- Reached end of file => return last line
			else if ( offs >= endOfBuffer - 1 ) {
				endOfBuffer = -1;
				break;
			}

			// --- Copy char to start of buffer
			c = cbuf[offs++];

			// --- Count line numbers in any case
			if ( c == '\n' ) {
				lineNumber++;
			}

			// --- Still within a multilineremark
			if ( skipping == SKIP_LONGCOMMENTS ) {
				// --- End reached?
				if ( (c == '*') && (cbuf[offs] == '/') ) {
					offs++;
					skipping = 0;
				}
				continue;
			}

			// --- Still within an XML remark
			else if ( skipping == SKIP_XMLCOMMENTS ) {
				// --- End reached?
				if ( (c == '-') && (cbuf[offs] == '-')
						&& (cbuf[offs + 1] == '>') ) {
					offs += 2;
					skipping = 0;
				}
				continue;
			}

			// --- End of line reached
			if ( c == '\n' ) {
				// --- check for unfolding
				if ( ((options & UNFOLDING) > 0)
						&& (cbuf[offs] == ' ' || cbuf[offs] == '\t') ) {
					offs++;
					continue;
				}
				if ( skipping == SKIP_EOLCOMMENTS )
					skipping = 0;

				// --- Do we have to skip empty lines?
				if ( (end == 0) && ((options & SKIP_EMPTYLINES) > 0) ) {
					continue;
				}

				// --- Return line
				break;
			}

			// --- Inline remark => skip until end of line
			if ( skipping == SKIP_EOLCOMMENTS ) {
				continue;
			}

			// --- Just skip CR
			if ( c == '\r' ) {
				continue;
			}

			// --- Check for start of multiline remark
			if ( (c == '/') && (cbuf[offs] == '*')
					&& ((options & SKIP_LONGCOMMENTS) > 0) ) {
				skipping = SKIP_LONGCOMMENTS;
				offs++;
				continue;
			}

			// --- Check for start of XML remark
			if ( (c == '<') && (cbuf[offs] == '!') && (cbuf[offs + 1] == '-')
					&& (cbuf[offs + 2] == '-')
					&& ((options & SKIP_XMLCOMMENTS) > 0) ) {
				skipping = SKIP_XMLCOMMENTS;
				offs += 3;
				continue;
			}

			// --- Check for start of inline remark
			if ( (c == '/') && (cbuf[offs] == '/')
					&& ((options & SKIP_EOLCOMMENTS) > 0) ) {
				skipping = SKIP_EOLCOMMENTS;
				continue;
			} else if ( (c == '#') && ((options & SKIP_EOLCOMMENTS) > 0) ) {
				skipping |= SKIP_EOLCOMMENTS;
				continue;
			}

			// --- Copy char to start of buffer
			cbuf[end++] = c;
		}

		// --- Return line
		String s = new String( cbuf, 0, end );
		return s;
	}

	/******************************************************************
	 * Sets options for this reader.
	 *
	 * <em>options</em> is the sum of:
	 * <table>
	 * <caption>Options</caption>
	 * <tr>
	 * <td>{@link #SKIP_EMPTYLINES}</td>
	 * <td>skips empty lines</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #SKIP_EOLCOMMENTS}</td>
	 * <td>removes "//.." until end of line</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #SKIP_LONGCOMMENTS}</td>
	 * <td>removes long comments "&#x2f;&#x2a; ... &#x2a;&#x2f;"</td>
	 * </tr>
	 * <tr>
	 * <td>{@link #SKIP_XMLCOMMENTS}</td>
	 * <td>removes XML comments "&lt;!-- ... --&gt;" /td>
	 * </tr>
	 * <tr>
	 * <td>{@link #UNFOLDING}</td>
	 * <td>Appends next line if it starts with a single space or tab while
	 * dropping line break and the space (see RFC5322)</td>
	 * </tr>
	 * </table>
	 * <p>
	 * Javadoc is buggy -> not accepting closing tags at end
	 *
	 * @param options
	 *            additive value of options
	 * @return this for fluent API
	 */
	public HalBufferedReader withOptions( int options ) {
		this.options = options;
		return this;
	}
}