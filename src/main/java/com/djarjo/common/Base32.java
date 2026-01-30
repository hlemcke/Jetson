package com.djarjo.common;

import java.nio.charset.StandardCharsets;

/**
 * Encodes a binary stream into a Base64 string and decodes such a string back to a binary
 * stream according to RFC 4648.
 * <p>
 * Base64 encoding takes 3 bytes (3*8 bit = 24bit) from the input stream and encodes them
 * into 4 ASCII characters (4*6bit = 24bit) from the following alphabet: {@code A-Z},
 * {@code a-z}, {@code 0-9}, {@code -}, {@code _} and the padding char {@code =}. This
 * encoding results in filesystem and URL safe strings.
 * </p>
 * <p>
 * The decoders also accepts {@code +} for char 62 and {@code /} for char 63. These
 * characters are not safe to use in URLs and file systems.
 * </p>
 *
 * @author Hajo Lemcke
 * @since 2026-01
 */
public class Base32 {

	/** The padding char to fill up the encoding to 5*n bytes */
	private static final int paddingChar = '=';

	/** The characters used for Base32 A-Z and 2-7 */
	private static final byte[] BASE32_BYTES = {'A', 'B', 'C', 'D',
			'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q',
			'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '2', '3', '4', '5',
			'6', '7', };

	/** Exists only to comply Javadoc */
	public Base32() {
	}

	/**
	 * Instantiates a new decoder (fluent API).
	 *
	 * @return new instance of Decoder
	 */
	public static Decoder decoder() {
		return new Decoder();
	}

	/**
	 * Instantiates a new encoder (fluent API).
	 *
	 * @return new instance of Encoder
	 */
	public static Encoder encoder() {
		return new Encoder();
	}

	/** Base 32 decoder */
	public static class Decoder {

		/**
		 * Decodes given string which must be base32 encoded.
		 *
		 * @param base32Text string in base32 format
		 * @return byte array of decoded text
		 */
		public byte[] decode( String base32Text ) {
			if ( base32Text==null || base32Text.isBlank() ) return new byte[0];

			//--- Cleanup input: Base32 is case-insensitive
			String cleanInput = base32Text.toUpperCase().replace( " ", "" );
			byte[] data = cleanInput.getBytes( StandardCharsets.US_ASCII );
			int inputLength = data.length;

			//--- Adjust output length based on padding
			int padding = 0;
			if ( cleanInput.endsWith( "======" ) ) padding = 4;
			else if ( cleanInput.endsWith( "====" ) ) padding = 3;
			else if ( cleanInput.endsWith( "===" ) ) padding = 2;
			else if ( cleanInput.endsWith( "=" ) ) padding = 1;

			int decodedLength = (inputLength * 5 / 8) - padding;
			byte[] out = new byte[decodedLength];

			// --- Loop through encoded Base32
			int bitsLeft = 0;
			int buffer = 0;
			int index = 0;

			for ( byte b : data ) {
				int val = indexOf( b );
				if ( val==-2 ) break; // Padding reached
				if ( val==-1 ) continue; // Skip invalid chars

				buffer = (buffer << 5) | val;
				bitsLeft += 5;

				if ( bitsLeft >= 8 ) {
					out[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
					bitsLeft -= 8;
				}
			}
			return out;
		}

		/**
		 * Gets the index of the given character into the Base32 encoding.
		 *
		 * @param c The base32 character
		 * @return the index [0..31] for any valid char, -2 for the padding char '=', -1 if
		 * not a valid Base32 char
		 */
		private int indexOf( byte c ) {
			if ( c==paddingChar ) return -2;
			if ( 'A' <= c && c <= 'Z' ) return c - 'A';
			if ( '2' <= c && c <= '7' ) return c - '2' + 26;
			return -1;
		}
	}

	/**
	 * Encodes given text into base 64. Defaults to web safe character set without line
	 * breaks.
	 */
	public static class Encoder {
		private boolean _lineBreaks = false;
		private boolean _withPadding = false;

		/**
		 * Encodes given text into base 64. Defaults to web safe character set without line
		 * breaks.
		 */
		public Encoder() {
		}

		/**
		 * Encodes the given string into Base32 5bit characters. The string is interpreted as
		 * being encoded in US-ASCII.
		 *
		 * @param textData The string to be encoded
		 * @return text in Base32 format
		 */
		public String encode( String textData ) {
			return encode( textData.getBytes( StandardCharsets.US_ASCII ) );
		}

		/**
		 * Encodes the given binary data into Base32 5bit characters. If {@code lineBreaks = true} then every 76 characters on output a line-feed will be inserted.
		 *
		 * @param binaryInput The input to be encoded into Base64
		 * @return Base64 encoded string
		 */
		public String encode( byte[] binaryInput ) {

			//--- Output length is always a multiple of 8
			int len = binaryInput.length;
			StringBuilder sb = new StringBuilder( ((len + 4) / 5) * 8 );
			int buffer = 0;
			int bitsLeft = 0;

			for ( byte b : binaryInput ) {
				buffer = (buffer << 8) | (b & 0xFF);
				bitsLeft += 8;
				while ( bitsLeft >= 5 ) {
					int index = (buffer >> (bitsLeft - 5)) & 0x1F;
					sb.append( (char) BASE32_BYTES[index] );
					bitsLeft -= 5;
				}

				//--- Handle optional line breaks. Multiple of 8
				if (_lineBreaks && (sb.length() % 72 == 0)) {
					sb.append('\n');
				}
			}

			//--- Handle remaining bits (Partial block)
			if ( bitsLeft > 0 ) {
				int index = (buffer << (5 - bitsLeft)) & 0x1F;
				sb.append( (char) BASE32_BYTES[index] );
			}

			// Add padding to reach multiple of 8
			while ( _withPadding && sb.length() % 8!=0 ) {
				sb.append( (char) paddingChar );
			}

			return sb.toString();
		}

		/**
		 * Sets creation of line breaks every 64 chars. Default = {@code false}.
		 *
		 * @param lineBreaks {@code true} will produce line breaks every 64 chars
		 * @return encoder for fluent api
		 */
		public Encoder withLineBreaks( boolean lineBreaks ) {
			this._lineBreaks = lineBreaks;
			return this;
		}

		public Encoder withPadding( boolean padding ) {
			this._withPadding = padding;
			return this;
		}
	}
}
