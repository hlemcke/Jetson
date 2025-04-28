package com.djarjo.common;

/**********************************************************************
 * Helper class to manage hex-codes.
 * 
 * @author Hajo Lemcke
 */
public class Hex {

	private static final String hexChars = "0123456789abcdef";

	/**
	 * Useless public constructor implemented for Javadoc only
	 */
	public Hex() {
	}

	/**
	 * Encodes byte to hey string
	 *
	 * @param value
	 *            byte
	 * @return hex string
	 */
	public static String encode( byte value ) {
		String str = "" + hexChars.charAt( (value >>> 4) & 0x000F )
				+ hexChars.charAt( value & 0x000F );
		return str;
	}

	/**
	 * Encodes the array of bytes into a string of hex-digits.
	 *
	 * @param value
	 *            byte array
	 * @return hex string
	 */
	public static String encode( byte[] value ) {
		StringBuilder encoded = new StringBuilder();
		for ( byte b : value ) {
			encoded.append( hexChars.charAt( (b >>> 4) & 0x000F ) )
					.append( hexChars.charAt( b & 0x000F ) );
		}
		return encoded.toString();
	}

	/**
	 * Encodes the array of bytes into a string of hex-digits separated by the
	 * given separator.
	 *
	 * @param value
	 *            byte array
	 * @param separator
	 *            separates the hex bytes
	 * @return hex or empty string
	 */
	public static String encode( byte[] value, char separator ) {
		if ( value == null || value.length == 0 ) {
			return "";
		}
		StringBuilder encoded = new StringBuilder();
		for ( byte b : value ) {
			encoded.append( separator )
					.append( hexChars.charAt( (b >>> 4) & 0x000F ) )
					.append( hexChars.charAt( b & 0x000F ) );
		}
		return encoded.substring( 1 ).toString();
	}

	/**
	 * Encodes a short value into a hex string
	 *
	 * @param value
	 *            2 bytes
	 * @return short encoded to hex
	 */
	public static String encode( short value ) {
		String str = "" + hexChars.charAt( value >>> 12 )
				+ hexChars.charAt( (value >>> 8) & 0x000F )
				+ hexChars.charAt( (value >>> 4) & 0x000F )
				+ hexChars.charAt( value & 0x000F );
		return str;
	}

	/**
	 * Encodes an integer value into a hex string
	 *
	 * @param value
	 *            4 bytes
	 * @return int encoded to hex
	 */
	public static String encode( int value ) {
		String str = "" + hexChars.charAt( value >>> 28 )
				+ hexChars.charAt( (value >>> 24) & 0x000F )
				+ hexChars.charAt( (value >>> 20) & 0x000F )
				+ hexChars.charAt( (value >>> 16) & 0x000F )
				+ hexChars.charAt( (value >>> 12) & 0x000F )
				+ hexChars.charAt( (value >>> 8) & 0x000F )
				+ hexChars.charAt( (value >>> 4) & 0x000F )
				+ hexChars.charAt( value & 0x000F );
		return str;
	}

	/**
	 * Encodes a long value into a hex string
	 *
	 * @param value
	 *            8 bytes
	 * @return long encoded to hex
	 */
	public static String encode( long value ) {
		String str = "" + hexChars.charAt( (int) (value >>> 60) )
				+ hexChars.charAt( (int) ((value >>> 56) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 52) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 48) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 44) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 40) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 36) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 32) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 28) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 24) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 20) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 16) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 12) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 8) & 0x000F) )
				+ hexChars.charAt( (int) ((value >>> 4) & 0x000F) )
				+ hexChars.charAt( (int) (value & 0x000F) );
		return str;
	}
}