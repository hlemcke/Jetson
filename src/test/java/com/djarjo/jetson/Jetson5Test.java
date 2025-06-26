/**
 *
 */
package com.djarjo.jetson;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Jetson5Test {
	static String commentJson = """
			// Start comment
			{ name: 'comment test',
			// comment in Json object
			array: [ 'abc', // comment in array line,
			'def',
			// comment in array value,
			'ghi' ],
			intVal: 42
			}
			""";
	static String singleEntityJson = """
			// Start comment
			{ name: 'TestEntity',
				"decimalVal": 123.456,
				boolVal: true,
				doubleVal: 12.34e56,
				"intVal": 42,
				timestamptz: "2023-09-21T13:14:15Z",
				stringVal: "String from test file",
				uuidVal: '7cc1ffa3-faf2-423a-96c4-292785e7aa36',
				}""";
	static String json5text = """
						{
			  // comments
			  unquoted: 'and you can quote me on that',
			  singleQuotes: 'I can use "double quotes" here',
			  lineBreaks: "Look, Mom! \
			No \\n's!",
			  hexadecimal: 0xdecaf,
			  leadingDecimalPoint: 0.8675309, andTrailing: 8675309.,
			  positiveSign: +1,
			  trailingComma: 'in objects', andIn: ['arrays',],
			  "backwardsCompatible": "with JSON",
			}
			""";
	TestData.PojoBasics entity = null;

	public static void main( String[] args ) throws IOException {
		Jetson5Test cmd = new Jetson5Test();
		// cmd.testJson5Comment();
		// cmd.testDecodeSingleEntity();
		// cmd.testJson5Decode();
		cmd.testJson5Encode();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	@Order(1)
	@Test
	void testJson5Comment() {
		try {
			Object object = Jetson.decode( commentJson );
			assertEquals( HashMap.class, object.getClass() );
			Map<?, ?> map = (Map<?, ?>) object;
			assertEquals( ArrayList.class, map.get( "array" )
					.getClass() );
			ArrayList<?> array = (ArrayList<?>) map.get( "array" );
			assertEquals( 3, array.size() );
			assertEquals( "abc", array.get( 0 ) );
			assertEquals( "def", array.get( 1 ) );
			assertEquals( "ghi", array.get( 2 ) );
		} catch ( IllegalAccessException | ParseException e ) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 */
	@Order(2)
	@Test
	void testDecodeSingleEntityIntoMap() throws IOException {
		try {
			Object object = Jetson.decode( singleEntityJson );
			assertEquals( HashMap.class, object.getClass() );
			Map<?, ?> map = (Map<?, ?>) object;
			assertEquals( 8, map.size() );
			assertTrue( (boolean) map.get( "boolVal" ) );
		} catch ( IllegalAccessException | ParseException e ) {
			e.printStackTrace();
		}
	}

	@Order(4)
	@Test
	void testJson5Decode() {
		try {
			System.out.println( json5text );
			Object object = Jetson.decode( json5text );
			assertEquals( HashMap.class, object.getClass() );
			Map<?, ?> map = (Map<?, ?>) object;
			assertEquals( 10, map.size() );
			assertEquals( "and you can quote me on that", map.get( "unquoted" ) );
			assertEquals( "I can use \"double quotes\" here", map.get( "singleQuotes" ) );
			// System.out.println( map.get( "lineBreaks" ) );
			assertEquals( "Look, Mom! No n's!", map.get( "lineBreaks" ) );
			// TODO correct assertEquals
			// assertEquals( 0xdecaf, map.get( "hexadecimal" ) );
			assertEquals( 0.8675309, map.get( "leadingDecimalPoint" ) );
			assertEquals( 8675309.0, map.get( "andTrailing" ) );
			assertEquals( 1L, map.get( "positiveSign" ) );
			assertEquals( "in objects", map.get( "trailingComma" ) );
			assertEquals( List.of( "arrays" ), map.get( "andIn" ) );
			@SuppressWarnings("unchecked") ArrayList<String> arrays =
					(ArrayList<String>) map.get( "andIn" );
			assertEquals( "arrays", arrays.get( 0 ) );
			assertEquals( "with JSON", map.get( "backwardsCompatible" ) );
		} catch ( IllegalAccessException | ParseException e ) {
			e.printStackTrace();
		}
	}

	@Order(10)
	@Test
	void testJson5Encode() {
		String expected =
				"{\n  unquoted: 'and you can quote me on that',\n  singleQuotes: " + "'I can " +
						"use" + " " + "\"double quotes\" here',\n  trailingComma: 'in " + "objects" +
						"'\n}";
		String encoded = Jetson.toJson5()
				.encode( new Json5Entity() );
		assertEquals( expected, encoded );
	}

	private static class Json5Entity {
		@Json
		public String unquoted = "and you can quote me on that";
		@Json
		public String singleQuotes = "I can use \"double quotes\" here";
		@Json
		public String lineBreaks, trailingComma = "in objects";
	}
}
