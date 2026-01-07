package com.djarjo.jetson.converter;

import com.djarjo.jetson.JsonDecoder;
import com.djarjo.jetson.JsonEncoder;
import com.google.common.flogger.FluentLogger;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;

/**
 * Converter for JSON to convert a collection (map or list) into a JSON string and vice
 * versa.
 * <p>
 * Recursively invokes just another JSON converter.
 */
@SuppressWarnings("javadoc")
public class CollectionConverter4json implements JsonConverter<Collection<?>> {
	private final static FluentLogger logger = FluentLogger.forEnclosingClass();

	/**
	 * Useless public constructor implemented for Javadoc only
	 */
	public CollectionConverter4json() {
	}

	@Override
	public String encodeToJson( Collection<?> jsonObject ) {
		return (jsonObject == null) ? null
				: JsonEncoder.encoder().encode( jsonObject );
	}

	@Override
	public Collection<?> decodeFromJson( String jsonValue ) {
		Collection<?> collection = null;
		try {
			if ( jsonValue != null ) {
				collection = (Collection<?>) JsonDecoder.decoder()
						.decode( jsonValue );
				collection = Collections.unmodifiableCollection( collection );
			}
		} catch ( IllegalAccessException | ParseException e ) {
			logger.atSevere()
					.log( "Exception %s while decoding '%s'", e.getMessage(),
							jsonValue );
		}
		return collection;
	}
}