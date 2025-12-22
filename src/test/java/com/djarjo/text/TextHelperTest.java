package com.djarjo.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextHelperTest {

	@Test
	void findEnumByName() {
		//--- given
		List<String> inputs = List.of( "ENUM_VALUE_1", "enum_value_2", "Enum-value-3" );
		List<SomeEnum> expected = List.of( SomeEnum.ENUM_VALUE_1, SomeEnum.ENUM_VALUE_2,
				SomeEnum.ENUM_VALUE_3 );

		//--- when / then
		for ( int i = 0; i < inputs.size(); i++ ) {
			SomeEnum result = TextHelper.findEnum( inputs.get( i ), SomeEnum.class, null,
					null );
			assertEquals( expected.get( i ), result );
		}
	}

	@Test
	void findEnumByCode() {
		//--- given
		List<String> inputs = List.of( "code1", "code2", "wrong" );
		List<SomeEnum> expected = List.of( SomeEnum.ENUM_VALUE_1, SomeEnum.ENUM_VALUE_2,
				SomeEnum.ENUM_VALUE_3 );

		//--- when / then
		for ( int i = 0; i < inputs.size(); i++ ) {
			SomeEnum result = TextHelper.findEnum( inputs.get( i ), SomeEnum.class,
					SomeEnum.ENUM_VALUE_3, "code" );
			assertEquals( expected.get( i ), result );
		}
	}

	@Test
	void findEnumById() {
		//--- given
		List<Integer> inputs = List.of( 101, 102, 999 );
		List<SomeEnum> expected = List.of( SomeEnum.ENUM_VALUE_1, SomeEnum.ENUM_VALUE_2,
				SomeEnum.ENUM_VALUE_3 );

		//--- when / then
		for ( int i = 0; i < inputs.size(); i++ ) {
			SomeEnum result = TextHelper.findEnum( inputs.get( i ), SomeEnum.class,
					SomeEnum.ENUM_VALUE_3, "id" );
			assertEquals( expected.get( i ), result );
		}
	}

	@Test
	void findEnumByOrdinal() {
		//--- given
		List<Integer> inputs = List.of( 0, 1, 999 );
		List<SomeEnum> expected = List.of( SomeEnum.ENUM_VALUE_1, SomeEnum.ENUM_VALUE_2,
				SomeEnum.ENUM_VALUE_3 );

		//--- when / then
		for ( int i = 0; i < inputs.size(); i++ ) {
			SomeEnum result = TextHelper.findEnum( inputs.get( i ), SomeEnum.class,
					SomeEnum.ENUM_VALUE_3, "ordinal" );
			assertEquals( expected.get( i ), result );
		}
	}

	@Test
	void testParseHex() {
		assertEquals( 14, TextHelper.parseHex( "0xE" ) );
		assertEquals( 302, TextHelper.parseHex( "0x12E" ) );
		assertEquals( 255, TextHelper.parseHex( "0xFf" ) );
		assertEquals( 7455987, TextHelper.parseHex( "0x71c4f3" ) );
	}
}