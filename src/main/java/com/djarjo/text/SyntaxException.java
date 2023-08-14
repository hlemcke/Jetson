package com.djarjo.text;

/**********************************************************************
 * Exception to indicate a syntax error in some statement.
 * <p>
 * Extends {@code RuntimeException}
 * </p>
 *
 * @author Hajo Lemcke Oct. 2014
 */
public class SyntaxException extends RuntimeException {

	private static final long serialVersionUID = 8137933967788926879L;

	public SyntaxException( String info ) {
		super( info );
	}
}