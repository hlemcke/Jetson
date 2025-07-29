package com.djarjo.common;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BaseConverterTest {
	@Test
	void convertToTypeBoolean() {
		//--- given
		List<Object> inputs = List.of( "false", "true", "bla", 0, 1, 42, 123.456 );
		List<Boolean> expected = List.of( false, true, false, false, true, true, true );

		//--- when / then
		for ( int i = 0; i < inputs.size(); i++ ) {
			Boolean result = BaseConverter.toBoolean( inputs.get( i ) );
			assertEquals( expected.get( i ), result );
		}
	}

	@Test
	void convertToTypeLong() {
		//--- given
		List<Object> inputs = List.of( "0", "42", 0, 1, 42, "123456" );
		List<Long> expected = List.of( 0L, 42L, 0L, 1L, 42L, 123456L );

		//--- when / then
		for ( int i = 0; i < inputs.size(); i++ ) {
			Long result = BaseConverter.toLong( inputs.get( i ) );
			assertEquals( expected.get( i ), result );
		}
	}

	@Test
	void toTimeZoneFromMinutes() {
		//--- given
		List<Long> inputs = List.of( 0L, 60L, 150L, -210L );
		List<TimeZone> expected = List.of( TimeZone.getTimeZone( "GMT" ),
				TimeZone.getTimeZone( "CET" ),
				TimeZone.getTimeZone( "GMT+02:30" ),
				TimeZone.getTimeZone( "GMT-03:30" ) );

		//--- when / then
		for ( int i = 0; i < inputs.size(); i++ ) {
			TimeZone tz = BaseConverter.toTimeZoneFromMinutes( inputs.get( i ) );
			assertEquals( expected.get( i ).getRawOffset(), tz.getRawOffset(),
					String.format( "inputs[%d] = %s", i, inputs.get( i ) ) );
		}
	}

	@Test
	void toTimeZone() {
		//--- given
		List<Object> inputs = List.of( "0", "-90", "PT4H", "-PT4H30M", "Europe/Berlin" );
		List<TimeZone> expected = List.of( TimeZone.getTimeZone( "GMT" ),
				TimeZone.getTimeZone( "GMT-01:30" ),
				TimeZone.getTimeZone( "GMT+04:00" ),
				TimeZone.getTimeZone( "GMT-04:30" ),
				TimeZone.getTimeZone( "GMT+01:00" ) );
	}
}
