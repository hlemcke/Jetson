package com.djarjo.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Produces some hash digest
 */
public class Crypto {

	/**
	 * Crypts the given string with MD5.
	 *
	 * @param input
	 *            Text to be crypted
	 * @return crypted with MD5 with hex encoding
	 */
	public static String getMD5( String input ) {
		MessageDigest md;
		try {
			// Static getInstance method is called with hashing MD5
			md = MessageDigest.getInstance( "MD5" );
			md.update( input.getBytes( StandardCharsets.UTF_8 ) );
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

		// digest() method called to calculate message digest
		// of an input digest() return array of byte
		byte[] messageDigest = md.digest();

		// Convert hash bytes to string in hex format
		String output = toHex( messageDigest );
		return output;
	}

	/**
	 * Hex characters with lowercase letters a-f
	 */
	public final static char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Gets a string representation in hex codes of the byte array.
	 *
	 * @param input
	 *            byte array
	 * @return string representation in hex
	 */
	public static String toHex( byte[] input ) {
		char[] output = new char[2 * input.length];
		int j = 0;
		for ( byte b : input ) {
			output[j++] = HEX[(b & 0xF0) >>> 4];
			output[j++] = HEX[(b & 0x0F)];
		}
		return new String( output );
	}
}