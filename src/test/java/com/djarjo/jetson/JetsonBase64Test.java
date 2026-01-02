/**
 *
 */
package com.djarjo.jetson;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
class JetsonBase64Test {

	@Test
	void testDecodeBase64ToByteArray() throws ParseException, IllegalAccessException {
		//--- given
		List<String> input = List.of( "{\"bytes\": \"AQIDBA\"}",
				"{\"bytes\":\"AQIDBP8\"}" );
		List<byte[]> expected = new ArrayList<>();
		expected.add( new byte[]{1, 2, 3, 4} );
		expected.add( new byte[]{1, 2, 3, 4, (byte) 255} );

		//--- when / then
		for ( int i = 0; i < input.size(); i++ ) {
			Pojo pojo = (Pojo) Jetson.decodeIntoObject( input.get( i ), new Pojo() );
			assertNotNull( pojo );
			assertArrayEquals( expected.get( i ), pojo.bytes, "[" + i + "]" );
		}
	}

	@Test
	void testEncodeBytesToBase64() throws ParseException, IllegalAccessException {
		//--- given
		List<byte[]> input = new ArrayList<>();
		input.add( new byte[]{1, 2, 3, 4} );
		input.add( new byte[]{1, 2, 3, 4, (byte) 255} );
		List<String> expected = List.of( "\"AQIDBA\"", "\"AQIDBP8\"" );

		//--- when / then
		for ( int i = 0; i < input.size(); i++ ) {
			String json = Jetson.encode( input.get( i ) );
			assertEquals( expected.get( i ), json, "[" + i + "]" );
		}
	}

	private static class Pojo {
		@Json
		public byte[] bytes;

		public static Pojo build() {
			return new Pojo();
		}

		public Pojo setBytes( byte[] bytes ) {
			this.bytes = bytes;
			return this;
		}
	}
}
