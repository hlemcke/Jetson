/**
 *
 */
package com.djarjo.jetson;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Hajo Lemcke
 *
 */
class JsonEncoderTest {
	static boolean isVerbose = false;

	public static void main( String[] args )
			throws IllegalAccessException, ParseException {
		isVerbose = true;
		JsonEncoderTest test = new JsonEncoderTest();
		test.testPrettyPrint();
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

	@Test
	void testIsBytesToBase64() {
		JsonEncoder encoder = JsonEncoder.encoder().withBytesToBase64( false );
		assertFalse( encoder.isBytesToBase64() );
		encoder = JsonEncoder.encoder().withBytesToBase64( true );
		assertTrue( encoder.isBytesToBase64() );
	}

	/**
	 * Test method for
	 * {@link com.djarjo.jetson.JsonEncoder#isEmpty(java.lang.Object)}.
	 */
	@Test
	void testIsEmpty() {
		assertTrue( JsonEncoder.encoder().isEmpty( "null" ) );
	}

	@Test
	void testIsJson5() {
		JsonEncoder encoder = JsonEncoder.encoder().withJson5( false );
		assertFalse( encoder.isJson5() );
		encoder = JsonEncoder.encoder().withJson5( true );
		assertTrue( encoder.isJson5() );
	}

	/**
	 * Test method for {@link com.djarjo.jetson.JsonEncoder#isWithNulls()}.
	 */
	@Test
	void testIsWithNulls() {
		JsonEncoder encoder = JsonEncoder.encoder().withNulls( false );
		assertFalse( encoder.isWithNulls() );
		encoder = JsonEncoder.encoder().withNulls( true );
		assertTrue( encoder.isWithNulls() );
	}

	/**
	 * Test method for {@link com.djarjo.jetson.JsonEncoder#getIndent()}.
	 */
	@Test
	void testGetIndent() {
		String indent = " ";
		JsonEncoder encoder = JsonEncoder.encoder().withPrettyPrint( indent );
		assertEquals( indent, encoder.getIndent() );
		encoder.withPrettyPrint( "-+" );
		assertEquals( "  ", encoder.getIndent() );
	}

	/**
	 * Test method for {@link com.djarjo.jetson.JsonEncoder#encoder()}.
	 */
	@Test
	void testEncoder_Basics() {
		Locale loc = Locale.US;
		String jsonText = JsonEncoder.encoder().encode( loc );
		assertEquals( "\"en_US\"", jsonText );
	}

	/**
	 * Test method for {@link com.djarjo.jetson.JsonEncoder#encoder()}.
	 */
	@Test
	void testEncoder_Arrays() {
		Integer[] ints = { 4711, null, 69 };
		String jsonText = JsonEncoder.encoder().encode( ints );
		assertEquals( "[4711,null,69]", jsonText );
	}

	/**
	 * Test method for {@link com.djarjo.jetson.JsonEncoder#encoder()}.
	 */
	@Test
	void testEncoder_Map() {
		OffsetDateTime now = OffsetDateTime.now();
		Map<String, Object> map = new HashMap<>();
		map.put( "key4711", 4711 );
		map.put( "keyNull", null );
		map.put( "keyNow", now );
		String jsonText = JsonEncoder.encoder().encode( map );
		System.out.println( isVerbose ? jsonText : "" );
		assertTrue( jsonText.contains( "\"key4711\":4711" ) );
		assertTrue(
				jsonText.contains( "\"keyNow\":\"" + now.toString() + "\"" ) );
		assertFalse( jsonText.contains( "keyNull" ) );
		// --- now with nulls included
		jsonText = JsonEncoder.encoder().withNulls( true ).encode( map );
		System.out.println( isVerbose ? jsonText : "" );
		assertTrue( jsonText.contains( "\"key4711\":4711" ) );
		assertTrue(
				jsonText.contains( "\"keyNow\":\"" + now.toString() + "\"" ) );
		assertTrue( jsonText.contains( "keyNull" ) );
	}

	@Test
	void testEncoder_PojoBasics()
			throws IllegalAccessException, ParseException {
		// --- Test with empty object
		TestData.PojoBasics basics = new TestData.PojoBasics();
		String jsonText = JsonEncoder.encoder().encode( basics );
		System.out.println( isVerbose ? jsonText : "" );
		assertEquals( "{\"intVar\":0}", jsonText );
		// --- Test with initialized object
		basics.initialize();
		jsonText = JsonEncoder.encoder().encode( basics );
		System.out.println( isVerbose ? jsonText : "" );
		@SuppressWarnings("unchecked")
		Map<String, Object> map =
				(Map<String, Object>) JsonDecoder.decoder().decode( jsonText );
		assertEquals( basics.decimalConst.toString(), map.get( "decimalVar" ) );
		assertFalse( (Boolean) map.get( "boolVar" ) );
		assertEquals( basics.longConst, map.get( "intVar" ) );
		assertEquals( basics.nameConst, map.get( "name" ) );
		assertEquals( Math.PI, map.get( "PI" ) );
		assertEquals( basics.langConst.toString(), map.get( "language" ) );
		assertEquals( basics.now.toString(), map.get( "timestamptz" ) );
		assertEquals( basics.uuidConst.toString(), map.get( "uuidVar" ) );
	}

	@Test
	void testDecoder_PojoBasics()
			throws IllegalAccessException, ParseException {
		TestData.PojoBasics basics = new TestData.PojoBasics();
		basics.initialize();
		String jsonText = JsonEncoder.encoder().encode( basics );
		System.out.println( isVerbose ? jsonText : "" );
		TestData.PojoBasics decoded = new TestData.PojoBasics();
		JsonDecoder.decoder().decode( jsonText, decoded );
		assertEquals( basics.decimalConst, decoded.decimalVar );
		assertFalse( decoded.boolVar );
		assertEquals( basics.longConst, decoded.intVar );
		assertEquals( basics.nameConst, decoded.name );
		assertEquals( Math.PI, decoded.doublePi );
		assertEquals( basics.langConst, decoded.language );
		assertEquals( basics.now, decoded.timestamptz );
		assertEquals( basics.uuidConst, decoded.uuidVar );
	}

	@Test
	void testPrettyPrint() {
		TestData.PojoCollections colls = new TestData.PojoCollections();
		colls.initialize();
		String jsonText = JsonEncoder.encoder().encode( colls );
		System.out.println( isVerbose ? jsonText : "" );
		System.out.println( jsonText );
		// --- assertion samples
		assertTrue( jsonText.contains( "\"floatSet\":[" ) );
		assertTrue( jsonText.contains( "\"mapKey_1\":\"mapValue_1\"" ) );
		// --- now pretty printing it
		jsonText =
				JsonEncoder.encoder().withPrettyPrint( "  " ).encode( colls );
		System.out.println( "---------\n" + jsonText );
	}
}