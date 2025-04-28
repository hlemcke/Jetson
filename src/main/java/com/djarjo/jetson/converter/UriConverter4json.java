package com.djarjo.jetson.converter;

import java.net.URI;

/**
 * Converts URI to string and back
 */
public class UriConverter4json implements JsonConverter<URI> {

	/**
	 * Useless public constructor implemented for Javadoc only
	 */
	public UriConverter4json() {
	}

	@Override
	public String encodeToJson( URI uri ) {
		return uri != null ? uri.toString() : null;
	}

	@Override
	public URI decodeFromJson( String uriAsJson ) {
		return (uriAsJson != null && !uriAsJson.isBlank())
				? URI.create( uriAsJson )
				: null;
	}
}