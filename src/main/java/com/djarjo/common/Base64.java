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
 * @since 2015-10
 */
public class Base64 {

	/** The padding char to fill up the encoding to 3*n bytes */
	private static final int paddingChar = '=';
	/** The characters used for Base64 */
	private static final byte[] BASE64_BYTES_DEFAULT = {'A', 'B', 'C', 'D',
			'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q',
			'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
			'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
			'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
			'4', '5', '6', '7', '8', '9', '+', '/'};
	/** The characters used for Base64 */
	private static final byte[] BASE64_BYTES_WEB_SAFE = {'A', 'B', 'C', 'D',
			'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q',
			'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
			'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
			'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
			'4', '5', '6', '7', '8', '9', '-', '_'};

	/** Exists only to comply Javadoc */
	public Base64() {
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

	/**
	 * Coding type for encoding.
	 */
	public enum CODING {
		/**
		 * Standard encoding uses characters "+" and "-" producing problems in URLs
		 */
		DEFAULT,

		/**
		 * Replaces default coding character {@code +} with {@code -} and character {@code /}
		 * with {@code _}
		 */
		WEB_SAFE
	}

	/**
	 * Base 64 decoder. Accepts both character sets (standard and web-safe).
	 */
	public static class Decoder {

		/**
		 * Base 64 decoder. Accepts both character sets (standard and web-safe).
		 */
		public Decoder() {
		}

		/**
		 * Decodes given string which must be base64 encoded.
		 *
		 * @param base64Text string in base64 format
		 * @return byte array of decoded text
		 */
		public byte[] decode( String base64Text ) {
			if ( base64Text == null || base64Text.isBlank() ) return new byte[0];
			byte[] base64Data = base64Text.getBytes();
			int inputLength = base64Data.length;

			// --- Prepare output
			int decodedLength = inputLength * 6 / 8;
			if ( base64Text.endsWith( "==" ) ) decodedLength -= 2;
			else if ( base64Text.endsWith( "=" ) ) decodedLength -= 1;
			byte[] decodedBytes = new byte[decodedLength];

			// --- Loop through encoded Base64
			int i = 0, j = 0;
			while ( i < inputLength && j < decodedLength ) {
				// 1. Read 4 Base64 chars (24 bits total)
				int b0 = Math.max( 0, indexOf( base64Data[i++] ) );
				int b1 = (i < inputLength) ? Math.max( 0, indexOf( base64Data[i++] ) ) : 0;
				int b2 = (i < inputLength) ? Math.max( 0, indexOf( base64Data[i++] ) ) : 0;
				int b3 = (i < inputLength) ? Math.max( 0, indexOf( base64Data[i++] ) ) : 0;

				// Combine into a single 24-bit integer block
				int combined = (b0 << 18) | (b1 << 12) | (b2 << 6) | b3;

				// 2. Extract 3 bytes from the block, checking bounds for each
				decodedBytes[j++] = (byte) ((combined >> 16) & 0xFF);
				if ( j < decodedLength ) {
					decodedBytes[j++] = (byte) ((combined >> 8) & 0xFF);
				}
				if ( j < decodedLength ) {
					decodedBytes[j++] = (byte) (combined & 0xFF);
				}
			}
			return decodedBytes;
		}

		/**
		 * Gets the index of the given character into the Base64 encoding.
		 *
		 * @param c The base64 character
		 * @return the index [0..63] for any valid char, -2 for the padding char '=', -1 if
		 * not a valid Base64 char
		 */
		private int indexOf( byte c ) {
			if ( c == paddingChar )
				return -2;
			if ( (c == '+') || (c == '-') ) {
				return 62;
			}
			if ( (c == '/') || (c == '_') ) {
				return 63;
			}
			if ( 'A' <= c && c <= 'Z' ) {
				return c - 'A';
			}
			if ( 'a' <= c && c <= 'z' ) {
				return c - 'a' + 26;
			}
			if ( '0' <= c && c <= '9' ) {
				return c - '0' + 52;
			}
			return -1;
		}
	}

	/**
	 * Encodes given text into base 64. Defaults to web safe character set without line
	 * breaks.
	 */
	public static class Encoder {
		private CODING _coding = CODING.WEB_SAFE;
		private byte[] _codeTable = BASE64_BYTES_WEB_SAFE;
		private boolean _lineBreaks = false;

		/**
		 * Encodes given text into base 64. Defaults to web safe character set without line
		 * breaks.
		 */
		public Encoder() {
		}

		/**
		 * Encodes the given string into Base64 7bit characters. The string is interpreted as
		 * being encoded in UTF-8.
		 *
		 * @param textData The string to be encoded
		 * @return text in Base64 format
		 */
		public String encode( String textData ) {
			byte[] binaryInput = null;
			binaryInput = textData.getBytes( StandardCharsets.UTF_8 );
			return encode( binaryInput );
		}

		/**
		 * Encodes the given binary data into Base64 7bit characters. If
		 * {@code lineBreaks = true} then every 76 characters on output (which is 57 bytes on
		 * input) a line-feed will be inserted.
		 *
		 * @param binaryInput The input to be encoded into Base64
		 * @return Base64 encoded string
		 */
		public String encode( byte[] binaryInput ) {

			int len = binaryInput.length;

			// --- Things needed for output
			int outLength = (((len + 2) / 3) * 4);
			if ( _lineBreaks ) {
				outLength += len / 57;
			}
			byte[] binaryOutput = new byte[outLength];
			int val = 0;
			int j = 0;

			// --- Loop through binary input
			int i = 0;
			while ( true ) {
				// --- First char
				if ( i >= len )
					break;
				val = (binaryInput[i] >>> 2) & 0x003F;
				binaryOutput[j++] = _codeTable[val];

				// --- Second char
				val = ((binaryInput[i++] & 0x0003) << 4);
				if ( i >= len ) {
					binaryOutput[j++] = _codeTable[val];
					binaryOutput[j++] = paddingChar;
					binaryOutput[j++] = paddingChar;
					break;
				}
				val = val | ((binaryInput[i] >>> 4) & 0x000F);
				binaryOutput[j++] = _codeTable[val];

				// --- Third char
				val = ((binaryInput[i++] & 0x000F) << 2);
				if ( i >= len ) {
					binaryOutput[j++] = _codeTable[val];
					binaryOutput[j++] = paddingChar;
					break;
				}
				val = val | ((binaryInput[i] >>> 6) & 0x0003);
				binaryOutput[j++] = _codeTable[val];

				// --- Fourth char
				val = binaryInput[i++] & 0x003F;
				binaryOutput[j++] = _codeTable[val];

				// --- Insert line break
				if ( _lineBreaks && ((i % 57) == 0) ) {
					binaryOutput[j++] = '\n';
				}
			}
			return new String( binaryOutput );
		}

		/**
		 * Gets coding: URL-safe or standard
		 *
		 * @return coding
		 */
		public CODING getCoding() {
			return _coding;
		}

		/**
		 * Gets line breaks setting.
		 *
		 * @return {@code true} if line breaks included during encoding
		 */
		public boolean hasLineBreaks() {
			return _lineBreaks;
		}

		/**
		 * Set coding to web-safe or standard. Defaults to web-safe.
		 *
		 * @param coding standard or web-safe
		 * @return encoder for fluent api
		 */
		public Encoder withEncoding( CODING coding ) {
			this._coding = coding;
			_codeTable = (coding == CODING.DEFAULT) ? BASE64_BYTES_DEFAULT
					: BASE64_BYTES_WEB_SAFE;
			return this;
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
	}
}
