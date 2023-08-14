package com.djarjo.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextHelperTest {

	@Test
	void testParseHex() {
		assertEquals( 14, TextHelper.parseHex( "0xE" ) );
		assertEquals( 302, TextHelper.parseHex( "0x12E" ) );
		assertEquals( 255, TextHelper.parseHex( "0xFf" ) );
	}
}