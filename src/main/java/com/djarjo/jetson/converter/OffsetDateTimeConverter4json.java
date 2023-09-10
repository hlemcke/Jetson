package com.djarjo.jetson.converter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Converts an OffsetDateTime into its string representation and back
 */
public class OffsetDateTimeConverter4json
		implements JsonConverter<OffsetDateTime> {

	@Override
	public String encodeToJson( OffsetDateTime iso8601dateTime ) {
		return iso8601dateTime.toString();
	}

	@Override
	public OffsetDateTime decodeFromJson( String iso8601dateTimeString ) {
		OffsetDateTime offsetDateTime;
		try {
			offsetDateTime = OffsetDateTime.parse( iso8601dateTimeString,
					DateTimeFormatter.ISO_DATE_TIME );
		} catch (DateTimeParseException exception) {
			// May not be a DateTime with offset information
			LocalDateTime localDateTime =
					LocalDateTime.parse( iso8601dateTimeString,
							DateTimeFormatter.ISO_LOCAL_DATE_TIME );
			offsetDateTime = localDateTime.atOffset( ZoneOffset.UTC );
		}
		return offsetDateTime;
	}
}
