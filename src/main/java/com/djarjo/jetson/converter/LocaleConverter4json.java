package com.djarjo.jetson.converter;

import java.util.Locale;

/**********************************************************************
 * Converter for Json to convert a <em>Locale</em> object into a char(2) code
 * and vice versa.
 */
public class LocaleConverter4json implements JsonConverter<Locale> {

	@Override
	public String encodeToJson( Locale locale ) {
		return (locale == null) ? null : locale.getLanguage();
	}

	@Override
	public Locale decodeFromJson( String jsonValue ) {
		if ( jsonValue == null ) {
			return null;
		}
		String[] parts = jsonValue.split( "_" );
		return Locale.forLanguageTag( parts[0] );
	}
}