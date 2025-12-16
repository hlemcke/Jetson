package com.djarjo.jetson;

import com.djarjo.jetson.converter.JsonConverter;
import com.djarjo.text.TextHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JsonDecodePojoTest {

	@Test
	@DisplayName("decode POJO annotated for Field access")
	void decodePojoAnnotatedForFieldAccess() {
		//--- given
		PojoAnnotatedField pojo = new PojoAnnotatedField();

		//--- when
		String json = Jetson.encode( pojo );

		//--- then
		assertNotNull( json );
		assertEquals( "{\"ival\":42}", json );
	}

	@Test
	@DisplayName("decode POJO annotated for Property access")
	void decodePojoAnnotatedForPropertyAccess() {
		//--- given
		PojoAnnotatedProperty pojo = new PojoAnnotatedProperty();

		//--- when
		String json = Jetson.encode( pojo );

		//--- then
		assertNotNull( json );
		assertEquals( "{\"some\":42}", json );
	}

	@Test
	@DisplayName("decode POJO with List")
	void testPojoWithArray() throws ParseException,
			IllegalAccessException {
		//--- given
		String json = "{\"pojoArray\":[{\"ival\":71},{\"ival\":123}]}";

		//--- when
		PojoWithCollection pojo = (PojoWithCollection)
				Jetson.decodeIntoObject( json, new PojoWithCollection() );

		//--- then
		assertNotNull( pojo );
		assertEquals( 2, pojo.pojoArray.length );
		assertEquals( 71, pojo.pojoArray[0].ival );
		assertEquals( 123, pojo.pojoArray[1].ival );
	}

	@Test
	@DisplayName("decode POJO with List")
	void testPojoWithList() throws ParseException,
			IllegalAccessException {
		//--- given
		String json = "{\"pojoList\":[{\"ival\":71},{\"ival\":123}]}";

		//--- when
		PojoWithCollection pojo = (PojoWithCollection)
				Jetson.decodeIntoObject( json, new PojoWithCollection() );

		//--- then
		assertNotNull( pojo );
		assertEquals( 2, pojo.pojoList.size() );
		assertEquals( 71, pojo.pojoList.get( 0 ).ival );
		assertEquals( 123, pojo.pojoList.get( 1 ).ival );
	}

	@Test
	@DisplayName("decode POJO with Converter")
	void testDecodePojoWithConverter() throws ParseException, IllegalAccessException {
		//--- given
		String json = "\"enc:93\"";

		//--- when
		Furi pojo = (Furi) Jetson.decodeIntoObject( json, new Furi() );

		//--- then
		assertNotNull( pojo );
		assertEquals( 93, pojo.ival );
	}

	@Test
	void testDecodePojoWithFromJson() throws ParseException, IllegalAccessException {
		//--- given
		String json = "\"enc:93\"";

		//--- when
		PojoWithFromJson pojo = (PojoWithFromJson)
				Jetson.decodeIntoObject( json, new PojoWithFromJson() );

		//--- then
		assertNotNull( pojo );
		assertEquals( 93, pojo.ival );
	}

	@Test
	@DisplayName("POJO with array, list, set and furies")
	void testDecodePojoWithConverterCollection() throws ParseException,
			IllegalAccessException {
		//--- given
//		String json = """
//				{"pojoArray":[{"ival":42,"str":"öäü$"},{"ival":17,"str":"?µM"}],
//				"pojoList":[{"ival":42,"str":"öäü$"},{"ival":27,"str":"pojo in list"}],
//				"pojoSet":[{"ival":37,"str":"pojo in Set"},{"ival":42,"str":"öäü$"}],
//				"furies":["enc=4711","enc=69"]}""";
		String json = """
				{
				"pojoList":[{"ival":42,"str":"öäü$"},{"ival":27,"str":"pojo in list"}],
				"pojoSet":[{"ival":37,"str":"pojo in Set"},{"ival":42,"str":"öäü$"}],
				"furies":["enc=4711","enc=69"]}""";

		//--- when
		PojoWithCollection pojo = (PojoWithCollection) Jetson.decodeIntoObject( json,
				new PojoWithCollection() );

		//--- then
		assertNotNull( pojo );
		assertEquals( 2, pojo.furies.size() );
	}

	@Test
	@DisplayName("POJO with mixed fields and getters")
	void pojoWithMixedFieldsAndGettersMustNotShowPrivateFields() {
		//--- given
		PojoWithMixedFieldsAndGetters pojo = new PojoWithMixedFieldsAndGetters();

		//--- when
		String json = Jetson.encode( pojo );

		//--- then
		assertFalse( json.isEmpty() );
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

	@Json(accessType = JsonAccessType.FIELD)
	private static class PojoAnnotatedField {
		public int ival = 42;

		@JsonTransient
		public String nonono = "but with value";

		public int getSome() {
			return ival;
		}

		@JsonTransient
		public int getNot() {
			return 4711;
		}
	}

	@Json
	private static class PojoAnnotatedProperty {
		public int ival = 42;

		@JsonTransient
		public String nonono = "but with value";

		public int getSome() {
			return ival;
		}

		@JsonTransient
		public int getNot() {
			return 4711;
		}
	}

	@Json(converter = Furi.Converter4json.class)
	private static class Furi {
		public int ival = 42;

		@JsonTransient
		public String nonono = "but with value";

		public Furi() {
		}

		public Furi( int value ) {
			this.ival = value;
		}

		public static Furi decode( String json ) {
			Furi pojo = new Furi();
			if ( json != null && !json.isBlank() ) {
				pojo.ival = TextHelper.parseInteger( json.substring( "enc=".length() ) );
			}
			return pojo;
		}

		public String encode() {
			return String.format( "\"enc=%d\"", ival );
		}

		public int getSome() {
			return ival;
		}

		@JsonTransient
		public int getNot() {
			return 4711;
		}

		public static class Converter4json implements JsonConverter<Furi> {
			@Override
			public Furi decodeFromJson( String jsonValue ) {
				if ( jsonValue == null || jsonValue.isBlank() ) return null;
				return Furi.decode( jsonValue );
			}

			@Override
			public String encodeToJson( Furi pojo ) {
				return (pojo == null) ? null : pojo.encode();
			}
		}
	}

	private static class PojoWithCollection {
		public static final String str2 = "?µM";
		public static final String strList = "pojo in list";
		public static final String strSet = "pojo in Set";

		@Json
		public Pojo[] pojoArray = {new Pojo(), new Pojo( 17, str2 )};

		@Json
		public List<Pojo> pojoList = new ArrayList<>(
				Arrays.asList( new Pojo(), new Pojo( 27, strList ) ) );

		@Json
		public Set<Pojo> pojoSet = new HashSet<>(
				Arrays.asList( new Pojo(), new Pojo( 37, strSet ) ) );

		@Json
		public List<Furi> furies = new ArrayList<>( Arrays.asList( new Furi( 4711 ),
				new Furi( 69 ) ) );
	}

	@Json
	private static class PojoWithFromJson {
		public int ival = 77;
		public String text = "POJO with fromJson()";

		public static PojoWithFromJson fromJson( String json ) {
			PojoWithFromJson pojo = new PojoWithFromJson();
			if ( json != null && !json.isBlank() ) {
				pojo.ival = TextHelper.parseInteger( json.substring( "enc:".length() ) );
			}
			return pojo;
		}
	}

	private static class PojoWithMixedFieldsAndGetters {
		@Json
		private final String mustNotShowUp1 = "invisible #1";
		public String mustNotShowUp2 = "invisible #2";
		@Json
		public String mustShowUp = "visible field";
		private String _hidden = "but with value";

		@Json
		public String getHidden() {
			return _hidden;
		}

		public void setHidden( String hidden ) {
			_hidden = hidden;
		}
	}
}
