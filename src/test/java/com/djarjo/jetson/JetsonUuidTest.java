package com.djarjo.jetson;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JetsonUuidTest {

	private final static UUID uuid1 = UUID.randomUUID();
	private final static UUID uuid2 = UUID.randomUUID();
	private final static UUID uuid3 = UUID.randomUUID();


	@Test
	void encodePojo() {
		//--- given
		String encodedId = String.format( "\"id\":\"%s\"", uuid1 );
		String encodedList = String.format( "[\"%s\",\"%s\",\"%s\"]", uuid1, uuid2, uuid3 );
		Pojo pojo = new Pojo();

		//--- when
		String json = Jetson.encode( pojo );

		//--- then
		assertNotNull( json );
		assertTrue( json.startsWith( "{" ) );
		assertTrue( json.endsWith( "}" ) );
		assertTrue( json.contains( encodedId ) );
		assertTrue( json.contains( encodedList ) );
	}

	@Test
	void decodeIntoPojo() throws ParseException, IllegalAccessException {
		//--- given
		UUID uuid4 = UUID.randomUUID();
		UUID uuid5 = UUID.randomUUID();
		String encoded = String.format(
				"""
						{ "references" : [ "%s", "%s" ],
						  "id" : "%s"
						}""", uuid4, uuid5, uuid4 );

		//--- when
		Pojo pojo = (Pojo) Jetson.decodeIntoObject( encoded, new Pojo() );

		//--- then
		assertNotNull( pojo );
		assertEquals( uuid4, pojo.id );
		assertEquals( 2, pojo.references.size() );
		assertEquals( uuid4, pojo.references.get( 0 ) );
		assertEquals( uuid5, pojo.references.get( 1 ) );
	}

	private static class Pojo {
		@Json
		public List<UUID> references = new ArrayList<>(
				Arrays.asList( uuid1, uuid2, uuid3 ) );

		@Json
		public UUID id = uuid1;
	}
}
