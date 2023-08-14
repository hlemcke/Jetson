package com.djarjo.jetson.converter;

import java.net.URI;

public class UriConverter4json implements JsonConverter<URI> {

	@Override
	public String encodeToJson( URI uri ) {
		return uri != null ? uri.toString() : null;
	}

	@Override
	public URI decodeFromJson( String uriAsJson ) {
		return (uriAsJson != null && ! uriAsJson.isBlank())
				? URI.create( uriAsJson )
				: null;
	}
}