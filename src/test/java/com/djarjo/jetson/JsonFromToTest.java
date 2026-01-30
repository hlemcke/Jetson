package com.djarjo.jetson;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JsonFromToTest {

	@Test
	void testToJsonWithEmptyList() {
		//--- given
		Pojo pojo = new Pojo();

		//--- when
		String json = Jetson.encode( pojo );

		//--- then
		assertNotNull( json );
		assertEquals( """
				{"fruit":"Apple"}""", json );
	}

	@Test
	void testToJsonWithOneFuri() {
		//--- given
		Pojo pojo = new Pojo();
		pojo.furies = new ArrayList<>();
		pojo.furies.add( new Furi() );

		//--- when
		String json = Jetson.encode( pojo );

		//--- then
		assertNotNull( json );
		assertEquals( 1, pojo.furies.size() );
		assertEquals( """
				{"fruit":"Apple","furies":["tel:12345678"]}""", json );
	}


	@Test
	void testToJsonWithTwoFuries() {
		//--- given
		Pojo pojo = new Pojo();
		pojo.furies = new ArrayList<>();
		pojo.furies.add( new Furi() );
		Furi furi2 = new Furi();
		furi2.type = "mailto";
		furi2.value = "someone@email.com";
		pojo.furies.add( furi2 );

		//--- when
		String json = Jetson.encode( pojo );

		//--- then
		assertNotNull( json );
		assertEquals( """
						{"fruit":"Apple","furies":["tel:12345678","mailto:someone@email.com"]}""",
				json );
	}

	@Test
	void testFromJsonWithEmptyList() throws ParseException, IllegalAccessException {
		//--- given
		String input = """
				{"fruit":"Apple"}""";
		Pojo pojo = new Pojo();

		//--- when
		Pojo decoded = (Pojo) Jetson.decodeIntoObject( input, pojo );

		//--- then
		assertEquals( "Apple", decoded.fruit );
		assertNull( decoded.furies );
	}

	@Test
	void testFromJsonWithOneFuri() throws ParseException, IllegalAccessException {
		//--- given
		String input = """
				{"fruit":"Banana","furies":["im:me@chatter"]}""";
		Pojo pojo = new Pojo();

		//--- when
		Pojo decoded = (Pojo) Jetson.decodeIntoObject( input, pojo );

		//--- then
		assertEquals( "Banana", decoded.fruit );
		assertNotNull( decoded.furies );
		assertEquals( 1, decoded.furies.size() );
		Furi furi = decoded.furies.getFirst();
		assertNotNull( furi );
		assertEquals( "im", furi.type );
		assertEquals( "me@chatter", furi.value );
	}

	@Test
	void testFromJsonWithTwoFuries() throws ParseException, IllegalAccessException {
		//--- given
		String input = """
				{"fruit":"Banana", "furies":["im:me@chatter", "url:djarjo.com"]}""";
		Pojo pojo = new Pojo();

		//--- when
		Pojo decoded = (Pojo) Jetson.decodeIntoObject( input, pojo );

		//--- then
		assertEquals( "Banana", decoded.fruit );
		assertNotNull( decoded.furies );
		assertEquals( 2, decoded.furies.size() );
		Furi furi = decoded.furies.getFirst();
		assertNotNull( furi );
		assertEquals( "im", furi.type );
		assertEquals( "me@chatter", furi.value );
		furi = decoded.furies.get( 1 );
		assertEquals( "url", furi.type );
		assertEquals( "djarjo.com", furi.value );
	}

	@Json
	static class Furi {
		String type = "tel";
		String value = "12345678";

		public static Furi fromJson( String json ) {
			Furi furi = new Furi();
			String[] parts = json.split( ":" );
			furi.type = parts[0];
			furi.value = parts[1];
			return furi;
		}

		public String toJson() {
			return String.format( "%s:%s", type, value );
		}
	}

	@Json(accessType = Json.AccessType.FIELD)
	static class Pojo {
		public String fruit = "Apple";
		public List<Furi> furies;
	}
}
