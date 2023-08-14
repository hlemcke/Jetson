/**
 * @author Hajo Lemcke
 *
 */

package com.djarjo.text;

/**********************************************************************
 * This exception can be raised by parsers.
 * <p>
 * It extends java.lang.RuntimeException by providing the index into the parsed
 * text.
 * </p>
 */
public class ParserException extends RuntimeException {

	/** Version UUID for serialization */
	private static final long serialVersionUID = -5053333365577856741L;

	/** Offset into parsed text */
	private int offset = 0;

	/******************************************************************
	 * Constructor with error message and offset into parsed text.
	 *
	 * @param message
	 *            message of parsing error
	 * @param offset
	 *            index into parsed text
	 */
	public ParserException( String message, int offset ) {
		super( message );
		this.offset = offset;
	}

	/**
	 * Gets the offset into the parsed text.
	 * 
	 * @return index
	 */
	public int getOffset() {
		return offset;
	}
}
