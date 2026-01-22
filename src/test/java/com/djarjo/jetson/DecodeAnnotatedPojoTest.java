package com.djarjo.jetson;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DecodeAnnotatedPojoTest {
	@Test
	void decodeIntoAnnotatedPojoFields() throws ParseException, IllegalAccessException {
		//--- given
		LocalDate date = LocalDate.of( 2018, 9, 21 );
		String json = """
				{"date": "2018-09-21", "name": "another", "value": 567}""";

		//--- when
		PojoWithFields pojo = (PojoWithFields) Jetson.decodeIntoObject( json, new PojoWithFields() );

		//--- then
		assertNotNull( pojo );
		assertEquals( date, pojo.date );
		assertEquals( "another", pojo.name );
		assertEquals( 123, pojo.value );
	}

	@Json(accessType = JsonAccessType.FIELD)
	static class PojoWithFields {
		LocalDate date = LocalDate.of( 2001, 12, 31 );
		String name = "who am I";
		@JsonTransient
		int value = 123;
	}
}
