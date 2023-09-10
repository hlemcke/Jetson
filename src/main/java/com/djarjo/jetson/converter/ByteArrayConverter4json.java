package com.djarjo.jetson.converter;

import java.util.Optional;

import com.djarjo.common.Base64;
import com.djarjo.common.Base64.CODING;

/**
 * Converts a byte array to a Json string in web safe Base64 encoding and back.
 */
public class ByteArrayConverter4json implements JsonConverter<byte[]> {

	@Override
	public String encodeToJson( byte[] byteArray ) {
		return Optional
				.ofNullable( byteArray ).map( bytes -> Base64.encoder()
						.withEncoding( CODING.WEB_SAFE ).encode( bytes ) )
				.orElse( null );
	}

	@Override
	public byte[] decodeFromJson( String jsonValue ) {
		return Optional.ofNullable( jsonValue )
				.map( base64String -> Base64.decoder().decode( base64String ) )
				.orElse( null );
	}
}