package com.djarjo.jetson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonEncodePojoTest {

	@Test
	@DisplayName("Pojo with Array, List and Set")
	void pojoWithCollectionsShouldEncodeCorrectly() {
//--- given
		PojoWithCollections pojoWithCollections = new PojoWithCollections();

		//--- when
		String json = Jetson.encode( pojoWithCollections );

		//--- then
		System.out.println( json );
		assertNotNull( json );
		assertTrue( json.contains( "[{\"ival\":42," ) );
		assertTrue( json.contains( "{\"ival\":17," ) );
		assertTrue( json.contains( "{\"ival\":27," ) );
		assertTrue( json.contains( "{\"ival\":37," ) );
	}

	private static class Pojo {
		@Json
		public int ival = 42;
		@Json
		public String str = "öäü$";

		Pojo() {
		}

		Pojo( int i, String s ) {
			ival = i;
			str = s;
		}
	}

	private static class PojoWithCollections {
		public static final String str2 = "?µM";
		public static final String strList = "pojo in list";
		public static final String strSet = "pojo in Set";

		@Json
		public Pojo[] pojoArray = {new Pojo(), new Pojo( 17, str2 )};

		@Json
		public List<Pojo> pojoList = List.of( new Pojo(), new Pojo( 27, strList ) );

		@Json
		public Set<Pojo> pojoSet = Set.of( new Pojo(), new Pojo( 37, strSet ) );
	}
}
