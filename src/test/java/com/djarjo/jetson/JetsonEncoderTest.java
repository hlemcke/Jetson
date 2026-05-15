/**
 *
 */
package com.djarjo.jetson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hajo Lemcke
 */
class JetsonEncoderTest {

  /**
   * Test method for {@link com.djarjo.jetson.JsonEncoder#isEmpty(java.lang.Object)}.
   */
  @Test
  void testIsEmpty() {
    assertTrue( JsonEncoder.encoder()
        .isEmpty( "null" ) );
  }

  /**
   * Test method for {@link com.djarjo.jetson.JsonEncoder#getIndent()}.
   */
  @Test
  void testGetIndent() {
    String indent = " ";
    JsonEncoder encoder = JsonEncoder.encoder()
        .prettyPrint( indent );
    assertEquals( indent, encoder.getIndent() );
    encoder.prettyPrint( "-+" );
    assertEquals( "  ", encoder.getIndent() );
  }

  /**
   * Test method for {@link com.djarjo.jetson.JsonEncoder#encoder()}.
   */
  @Test
  void testEncoder_Basics() {
    Locale loc = Locale.US;
    String jsonText = JsonEncoder.encoder()
        .encode( loc );
    assertEquals( "\"en_US\"", jsonText );
  }

  @Test
  void testEncodeNull() {
    //--- given
    Map<String, Object> map = new HashMap<>();
    map.put( "key", "value" );
    map.put( "empty", null );

    //--- when
    String json = JsonEncoder.encoder().encode( map );

    //--- then
    assertEquals( "{\"key\":\"value\",\"empty\":null}", json );
  }

  @Test
  void testEncodeSkipNull() {
    //--- given
    Map<String, Object> map = new HashMap<>();
    map.put( "key", "value" );
    map.put( "empty", null );

    //--- when
    String json = JsonEncoder.encoder().skipNull().encode( map );

    //--- then
    assertEquals( "{\"key\":\"value\"}", json );
  }

  /**
   * Test method for {@link com.djarjo.jetson.JsonEncoder#encoder()}.
   */
  @Test
  void testEncoder_Arrays() {
    Integer[] ints = {4711, null, 69};
    String jsonText = JsonEncoder.encoder()
        .encode( ints );
    assertEquals( "[4711,null,69]", jsonText );
  }

  @Test
  @DisplayName("List must be encoded as: '[1,2,3]'")
  void testEncoder_List() {
    List<String> list = List.of( "first", "second", "first" );
    String json = JsonEncoder.encoder()
        .encode( list );
    assertEquals( "[\"first\",\"second\",\"first\"]", json );
  }

  @Test
  @DisplayName("List must be encoded as: '[1,2,3]'")
  void testEncoder_Set() {
    Set<String> set = Set.of( "first", "second" );
    String json = JsonEncoder.encoder()
        .encode( set );
    assertTrue( json.startsWith( "[\"" ) );
    assertTrue( json.contains( "\"first\"" ) );
    assertTrue( json.contains( "\"second\"" ) );
  }

  /**
   * Test method for {@link com.djarjo.jetson.JsonEncoder#encoder()}.
   */
  @Test
  void testEncoder_Map() {
    //--- given
    OffsetDateTime now = OffsetDateTime.now();
    Map<String, Object> map = new HashMap<>();
    map.put( "key4711", 4711 );
    map.put( "keyEmpty", List.of() );
    map.put( "keyNull", null );
    map.put( "keyNow", now );

    //--- when -> no empty and no null values
    String jsonText = JsonEncoder.encoder().skipEmpty().skipNull().encode( map );

    //--- then
    assertTrue( jsonText.contains( "\"key4711\":4711" ) );
    assertTrue( jsonText.contains( "\"keyNow\":\"" + now + "\"" ) );
    assertFalse( jsonText.contains( "keyEmpty" ) );
    assertFalse( jsonText.contains( "keyNull" ) );

    // --- now with empty and null values included
    jsonText = JsonEncoder.encoder().encode( map );
    assertTrue( jsonText.contains( "\"key4711\":4711" ) );
    assertTrue( jsonText.contains( "\"keyNow\":\"" + now + "\"" ) );
    assertTrue( jsonText.contains( "\"keyEmpty\":[]" ) );
    assertTrue( jsonText.contains( "\"keyNull\":null" ) );
  }

  @Test
  void testEncoder_PojoBasics() throws IllegalAccessException, ParseException {
    // --- Test with empty object
    TestData.PojoBasics basics = new TestData.PojoBasics();
    String jsonText = JsonEncoder.encoder().skipNull().encode( basics );
    assertEquals( "{\"intVar\":0}", jsonText );

    // --- Test with initialized object
    basics.initialize();
    jsonText = JsonEncoder.encoder().encode( basics );
    @SuppressWarnings("unchecked") Map<String, Object> map =
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
  void testDecoder_PojoBasics() throws IllegalAccessException, ParseException {
    //--- given
    TestData.PojoBasics basics = new TestData.PojoBasics();
    basics.initialize();
    String jsonText = JsonEncoder.encoder().encode( basics );

    //--- when
    TestData.PojoBasics decoded = new TestData.PojoBasics();
    JsonDecoder.decoder().decodeIntoObject( jsonText, decoded );

    //--- then
    assertEquals( basics.decimalConst, decoded.decimalVar );
    assertFalse( decoded.boolVar );
    assertEquals( basics.longConst, decoded.intVar );
    assertEquals( basics.nameConst, decoded.name );
    assertEquals( Math.PI, decoded.doublePi );
    assertEquals( basics.langConst, decoded.language );
    assertEquals( basics.now.truncatedTo( ChronoUnit.MILLIS ),
        decoded.timestamptz.truncatedTo( ChronoUnit.MILLIS ) );
    assertEquals( basics.uuidConst, decoded.uuidVar );
  }

  @Test
  void testPrettyPrint() {
    TestData.PojoCollections colls = new TestData.PojoCollections();
    colls.initialize();
    String jsonText = JsonEncoder.encoder()
        .encode( colls );

    // --- assertion samples
    assertTrue( jsonText.contains( "\"floatSet\":[" ) );
    assertTrue( jsonText.contains( "\"mapKey_1\":\"mapValue_1\"" ) );
  }
}