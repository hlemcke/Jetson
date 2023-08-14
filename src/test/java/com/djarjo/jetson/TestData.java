package com.djarjo.jetson;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.djarjo.jetson.converter.JsonConverter;
import com.djarjo.text.TextHelper;

/**
 * Contains public inner classes used by test methods
 *
 * @author Hajo Lemcke
 * @since 2020-08-16
 */
public class TestData {

	/** -------------- Pojo with basic values --------------- */
	public static class PojoBasics {
		public final BigDecimal decimalConst = new BigDecimal( "1234.56" );
		public final Double doubleConst = Double.valueOf( "456.789e17" );
		public final int intConst = 4711;
		public final long longConst = 4711;
		public final Locale langConst = Locale.GERMAN;
		public final String nameConst = "UTF-8 text with äöüß";
		public final OffsetDateTime now = OffsetDateTime.now();
		public final UUID uuidConst = UUID.randomUUID();

		@Json
		public Boolean boolVar = null;
		@Json
		public BigDecimal decimalVar = null;
		@Json
		public Double doubleVar = null;
		@Json(key = "PI")
		public Double doublePi = null;
		@Json
		public int intVar = 0;
		@Json
		public Locale language = null;
		@Json
		public String name = null;
		@Json
		public OffsetDateTime timestamptz = null;
		@Json
		public UUID uuidVar = null;

		public void initialize() {
			boolVar = false;
			decimalVar = decimalConst;
			doubleVar = doubleConst;
			intVar = intConst;
			language = langConst;
			name = nameConst;
			doublePi = Math.PI;
			timestamptz = now;
			uuidVar = uuidConst;
		}
	}

	/** -------------- Pojo with collections --------------- */
	public static class PojoCollections {
		@Json
		public String[] nameArray = { "Bad", "syntax", "design" };
		private Set<Float> floatSet = new HashSet<>();
		@Json
		public List<String> nameList =
				Arrays.asList( "remove", "arrays", "from", "Java!" );
		@Json
		public Map<String, Object> map = new HashMap<>();

		@Json
		public Set<Float> getFloatSet() {
			return floatSet;
		}

		public void setFloatSet( Set<Float> val ) {
			floatSet = val;
		}

		public void initialize() {
			floatSet.add( Float.valueOf( "1234.567" ) );
			floatSet.add( Float.valueOf( "2.99e8" ) );
			map.put( "mapKey_1", "mapValue_1" );
			map.put( "mapInt", 4711 );
		}
	}

	/** -------------- Root class for testing --------------- */
	public static class PojoRoot {

		@Json(converter = EnumWithCodeConverter4json.class)
		public EnumWithCode enumVal = EnumWithCode.ENUM_123;

		@Json(key = "anotherKey")
		public String key = "value for key";

		private Boolean boolValue = null;

		@Json
		public Boolean getBoolValue() {
			return boolValue;
		}

		public void setBoolValue( Boolean newVal ) {
			boolValue = newVal;
		}
	}

	/** -------------- Test enum --------------------------- */
	public static enum PlainEnum {
		PLAIN_A,
		PLAIN_B;
	}

	/** -------------- Test enum with integer code --------- */
	public static enum EnumWithCode {
		ENUM_123(123),
		ENUM_456(456);

		int code;

		EnumWithCode( int code ) {
			this.code = code;
		}

		int getCode() {
			return code;
		}

		static EnumWithCode findByCode( Integer code ) {
			if ( code != null ) {
				for ( EnumWithCode eval : EnumWithCode.values() ) {
					if ( eval.code == code ) {
						return eval;
					}
				}
			}
			return null;
		}
	}

	/** -------------- Converter for EnumWithCode --------- */
	public static class EnumWithCodeConverter4json
			implements JsonConverter<EnumWithCode> {

		@Override
		public String encodeToJson( EnumWithCode attribute ) {
			return (attribute == null) ? null : "" + attribute.getCode();
		}

		@Override
		public EnumWithCode decodeFromJson( String jsonValue ) {
			Integer intVal = TextHelper.parseInteger( jsonValue );
			for ( EnumWithCode eval : EnumWithCode.values() ) {
				if ( eval.code == intVal ) {
					return eval;
				}
			}
			return null;
		}
	}
}