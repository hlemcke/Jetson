package com.djarjo.jetson.converter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.djarjo.common.Base64;

/**
 * Converts a list of byte arrays to a Json list. Each item in the Json list is
 * a string in Base64 format.
 */
@SuppressWarnings("javadoc")
public class ByteArrayListConverter4json
		implements JsonConverter<List<byte[]>> {

	/**
	 * Useless public constructor implemented for Javadoc only
	 */
	public ByteArrayListConverter4json() {
	}

	@Override
	public String encodeToJson( List<byte[]> listOfByteArrays ) {
		return Optional.ofNullable( listOfByteArrays ).map( byteArray -> {
			return byteArray.stream()
					.map( bytes -> Base64.encoder().encode( bytes ) )
					.collect( Collectors.joining( ",", "", "" ) );
		} ).orElse( null );
	}

	@Override
	public List<byte[]> decodeFromJson( String jsonValue ) {
		return Optional.ofNullable( jsonValue ).map( jsonValueToConvert -> {
			return Arrays.asList( jsonValue.split( "," ) )
					.stream()
					.map( base64String -> Base64.decoder()
							.decode( base64String ) )
					.collect( Collectors.toList() );
		} ).orElse( null );
	}
}