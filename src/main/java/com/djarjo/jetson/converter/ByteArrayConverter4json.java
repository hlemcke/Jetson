package com.djarjo.jetson.converter;

import com.djarjo.common.Base64;

/**
 * Converts a byte array to a Json string in web safe Base64 encoding and back.
 */
@SuppressWarnings("javadoc")
public class ByteArrayConverter4json implements JsonConverter<byte[]> {

	/**
	 * Useless public constructor implemented for Javadoc only
	 */
	public ByteArrayConverter4json() {
	}

	@Override
	public String encodeToJson( byte[] byteArray ) {
		return (byteArray == null) ? null
				: Base64.encoder().encode( byteArray );
	}

	@Override
	public byte[] decodeFromJson( String jsonValue ) {
		return (jsonValue == null) ? null
				: Base64.decoder().decode( jsonValue );
	}
}