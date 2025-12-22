package com.djarjo.jetson;

import com.djarjo.text.SomeEnum;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonEnumTest {
	@Test
	void testJsonDecodeEnumWithAccessor() throws ParseException, IllegalAccessException {
		//--- given
		List<String> inputs = List.of( "{\"someEnum\":\"code2\"}",
				"{\"someEnum\":\"ENUM_VALUE_3\"}" );
		List<SomeEnum> expected = List.of( SomeEnum.ENUM_VALUE_2, SomeEnum.ENUM_VALUE_3 );

		//--- when
		for ( int i = 0; i < inputs.size(); i++ ) {
			PojoCode pojo = (PojoCode) Jetson.decodeIntoObject( inputs.get( i ),
					new PojoCode() );
			assertEquals( expected.get( i ), pojo.someEnum );
		}
	}

	@Test
	void testDecodeEnumWithAccessor() throws ParseException, IllegalAccessException {
		//--- given
		String input = "{\"someEnum\":\"code2\"}";

		//--- when
		PojoCode pojo = (PojoCode) Jetson.decodeIntoObject( input, new PojoCode() );

		//--- then
		assertEquals( SomeEnum.ENUM_VALUE_2, pojo.someEnum );
	}

	@Test
	void testDecodeEnumWithOrdinal() throws ParseException, IllegalAccessException {
		//--- given
		String input = "{\"someEnum\":1}";

		//--- when
		PojoOrdinal pojo = (PojoOrdinal) Jetson.decodeIntoObject( input, new PojoOrdinal() );

		//--- then
		assertEquals( SomeEnum.ENUM_VALUE_2, pojo.someEnum );
	}

	@Test
	void testEncodeEnumWithOrdinal() throws ParseException, IllegalAccessException {
		//--- given
		PojoOrdinal pojo = new PojoOrdinal();

		//--- when
		String json = Jetson.encode( pojo );

		//--- then
		assertEquals( "{\"someEnum\":0}", json );
	}

	private static class PojoCode {
		@Json(enumAccessor = "code")
		public SomeEnum someEnum = SomeEnum.ENUM_VALUE_1;
	}

	private static class PojoOrdinal {
		@Json(enumAccessor = "ordinal")
		public SomeEnum someEnum = SomeEnum.ENUM_VALUE_1;
	}

}
