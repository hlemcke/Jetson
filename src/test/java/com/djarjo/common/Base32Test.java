package com.djarjo.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Base32Test {

	/**
	 * Tests encoding using RFC 4648 test vectors.
	 * The input varies in length to trigger different padding requirements.
	 */
	@ParameterizedTest
	@CsvSource({
			"'',            ''",
			"'f',           'MY'",
			"'fo',          'MZXQ'",
			"'foo',         'MZXW6'",
			"'foob',        'MZXW6YQ'",
			"'fooba',       'MZXW6YTB'",
			"'foobar',      'MZXW6YTBOI'"
	})
	void testEncode(String input, String expected) {
		String actual = Base32.encoder().encode(input);
		assertEquals(expected, actual, "Encoding failed for input: " + input);
	}


	@ParameterizedTest
	@CsvSource({
			"'',            ''",
			"'f',           'MY======'",
			"'fo',          'MZXQ===='",
			"'foo',         'MZXW6==='",
			"'foob',        'MZXW6YQ='",
			"'fooba',       'MZXW6YTB'",
			"'foobar',      'MZXW6YTBOI======'"
	})
	void testEncodeWithPadding(String input, String expected) {
		String actual = Base32.encoder().withPadding( true ).encode(input);
		assertEquals(expected, actual, "Encoding failed for input: " + input);
	}

	/**
	 * Tests decoding using RFC 4648 test vectors.
	 * Also tests case-insensitivity which is a standard feature of Base32.
	 */
	@ParameterizedTest
	@CsvSource({
			"'',            ''",
			"'MY',    'f'",
			"'MZXQ',    'fo'",
			"'MZXW6',    'foo'",
			"'MZXW6YQ',    'foob'",
			"'MZXW6YTB',    'fooba'",
			"'mzxw6ytboi', 'foobar'" // Testing lowercase input
	})
	void testDecode(String input, String expected) {
		byte[] actualBytes = Base32.decoder().decode(input);
		String actual = new String(actualBytes, StandardCharsets.UTF_8);
		assertEquals(expected, actual, "Decoding failed for input: " + input);
	}

	/**
	 * Tests decoding using RFC 4648 test vectors.
	 * Also tests case-insensitivity which is a standard feature of Base32.
	 */
	@ParameterizedTest
	@CsvSource({
			"'',            ''",
			"'MY======',    'f'",
			"'MZXQ====',    'fo'",
			"'MZXW6===',    'foo'",
			"'MZXW6YQ=',    'foob'",
			"'MZXW6YTB',    'fooba'",
			"'mzxw6ytboi======', 'foobar'" // Testing lowercase input
	})
	void testDecodeWithPadding(String input, String expected) {
		byte[] actualBytes = Base32.decoder().decode(input);
		String actual = new String(actualBytes, StandardCharsets.UTF_8);
		assertEquals(expected, actual, "Decoding failed for input: " + input);
	}
}