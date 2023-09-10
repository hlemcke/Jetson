package com.djarjo.text;

/**********************************************************************
 * Exception to indicate a recursion which would result in an endless loop.
 * <p>
 * Extends {@code RuntimeException}
 * </p>
 *
 * @author Hajo Lemcke Mai 2014
 */
public class RecursionException extends RuntimeException {

	private static final long serialVersionUID = -1230447255275021461L;

	/**
	 * Constructor with message
	 *
	 * @param info
	 *            message
	 */
	public RecursionException( String info ) {
		super( info );
	}
}