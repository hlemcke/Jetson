package com.djarjo.jetson;

/**********************************************************************
 * Exception to indicate an error while parsing some Json string.
 * <p>
 * Extends {@code RuntimeException}
 * </p>
 *
 * @author Hajo Lemcke June 2019
 */
public class JsonParseException extends RuntimeException {

	private static final long serialVersionUID = -7974219286605739660L;

	public JsonParseException() {
		super();
	}

	public JsonParseException( String info ) {
		super( info );
	}
}