package com.djarjo.jetson.converter;

import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import com.djarjo.jetson.Jetson;
import com.djarjo.jetson.JsonDecoder;

/**
 * Converts list of UUIDs
 */
public class UuidListConverter4json implements JsonConverter<List<UUID>> {

	/**
	 * Useless public constructor implemented for Javadoc only
	 */
	public UuidListConverter4json() {
	}

	@Override
	public String encodeToJson( List<UUID> uuids ) {
		return uuids != null ? Jetson.encode( uuids ) : null;
	}

	@Override
	public List<UUID> decodeFromJson( String uuidsAsJson ) {
		List<UUID> uuids = null;
		try {
			uuids = (uuidsAsJson != null && !uuidsAsJson.isBlank())
					? (List<UUID>) JsonDecoder.decoder()
							.decodeList( uuidsAsJson, UUID.class )
					: null;
		} catch (IllegalAccessException | ParseException exception) {
		}
		return uuids;
	}
}
