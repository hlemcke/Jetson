package com.djarjo.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextHelperTest {

	@Test
	void testParseHex() {
		assertEquals( 14, TextHelper.parseHex( "0xE" ) );
		assertEquals( 302, TextHelper.parseHex( "0x12E" ) );
		assertEquals( 255, TextHelper.parseHex( "0xFf" ) );
		assertEquals( 7455987, TextHelper.parseHex( "0x71c4f3" ) );
	}
}