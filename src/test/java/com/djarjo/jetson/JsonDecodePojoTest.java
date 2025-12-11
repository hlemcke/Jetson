package com.djarjo.jetson;

import com.djarjo.jetson.converter.JsonConverter;
import com.djarjo.text.TextHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
	@DisplayName("decode POJO with Array, List and Set")
	void pojoWithCollectionsShouldDecodeCorrectly() throws ParseException, IllegalAccessException {
		//--- given
		String json = "{\"pojoList\":[{\"ival\":71}]}";

		//--- when
		PojoWithCollection pojo = (PojoWithCollection)
				Jetson.decodeIntoObject( json, new PojoWithCollection() );

		//--- then
		assertNotNull( pojo );
		assertEquals( 1, pojo.pojoList.size() );
		assertEquals( 71, pojo.pojoList.get( 0 ).ival );
	}

	@Test
	@DisplayName("decode POJO with Converter")
	void testDecodePojoWithConverter() throws ParseException, IllegalAccessException {
		//--- given
		String json = "\"enc:93\"";

		//--- when
		PojoWithConverter pojo = (PojoWithConverter)
				Jetson.decodeIntoObject( json, new PojoWithConverter() );

		//--- then
		assertNotNull( pojo );
		assertEquals( 93, pojo.ival );
	}

	@Test
	void decodePojoWithFromJson() {
		//--- given
		String json = "\"enc:93\"";

		//--- when
		PojoWithFromJson pojo = (PojoWithConverter)
				Jetson.decodeIntoObject( json, new PojoWithConverter() );

		//--- then
		assertNotNull( pojo );
		assertEquals( 93, pojo.ival );
	}

	@Test
	void testDecodePojoWithConverterCollection() {
		//--- given
		PojoWithCollection pojo = new PojoWithCollection();

		//--- when
		String json = Jetson.encode( pojo );

		//--- then
		assertNotNull( json );
		assertTrue( json.contains( "furies\":[\"enc=4711\",\"enc=69\"]" ) );
	}

	@Test
	@DisplayName("Pojo with mixed fields and getters")
	void pojoWithMixedFieldsAndGettersMustNotShowPrivateFields() {
		//--- given
		PojoWithMixedFieldsAndGetters pojo = new PojoWithMixedFieldsAndGetters();

		//--- when
		String json = Jetson.encode( pojo );

		//--- then
		System.out.println( json );
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

	@Json(converter = PojoWithConverter.Converter4json.class)
	private static class PojoWithConverter {
		public int ival = 42;

		@JsonTransient
		public String nonono = "but with value";

		public PojoWithConverter() {
		}

		public PojoWithConverter( int value ) {
			this.ival = value;
		}

		public static PojoWithConverter decode( String json ) {
			PojoWithConverter pojo = new PojoWithConverter();
			if ( json!=null && !json.isBlank() ) {
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

		public static class Converter4json implements JsonConverter<JsonDecodePojoTest.PojoWithConverter> {
			@Override
			public JsonDecodePojoTest.PojoWithConverter decodeFromJson( String jsonValue ) {
				if ( jsonValue==null || jsonValue.isBlank() ) return null;
				return JsonDecodePojoTest.PojoWithConverter.decode( jsonValue );
			}

			@Override
			public String encodeToJson( JsonDecodePojoTest.PojoWithConverter pojo ) {
				return (pojo==null) ? null : pojo.encode();
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
		public List<Pojo> pojoList = new ArrayList<>( Arrays.asList( new Pojo(), new Pojo( 27, strList ) ) );

		@Json
		public Set<Pojo> pojoSet = Set.of( new Pojo(), new Pojo( 37, strSet ) );

		@Json
		public List<PojoWithConverter> furies = List.of( new PojoWithConverter( 4711 ), new PojoWithConverter( 69 ) );
	}

	@Json
	private static class PojoWithFromJson {
		public int ival = 73;
		public String text = "POJO with fromJson()";

		public static PojoWithFromJson fromJson( String json ) {
			PojoWithFromJson pojo = new PojoWithFromJson();
			if ( json!=null && !json.isBlank() ) {
				pojo.ival = TextHelper.parseInteger( json.substring( "ival:".length() ) );
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
