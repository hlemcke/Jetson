package com.djarjo.jetson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

///
///
@DisplayName("Test decoding and encoding of string values")
public class JetsonStringTest {
  @Test
  void decodeAllControlCharacters() throws ParseException, IllegalAccessException {
    // Build JSON with escaped control chars
    StringBuilder json = new StringBuilder();
    json.append( "{\"text\":\"" );
    for ( int i = 0; i <= 0x1F; i++ ) {
      json.append( String.format( "\\u%04x", i ) );
    }
    json.append( "\"}" );

    Pojo pojo = new Pojo();
    Jetson.decodeIntoObject( json.toString(), pojo );

    //--- then
    assertNotNull( pojo.text );
    assertEquals( 0x20, pojo.text.length() );
    for ( int i = 0; i <= 0x1F; i++ ) {
      assertEquals( (char) i, pojo.text.charAt( i ),
          "Mismatch at index " + i );
    }
  }

  @Test
  @DisplayName("Decode solidus and double-quote")
  void decodeSolidusAndQuote() throws ParseException, IllegalAccessException {
    //--- given
    String given = """
        {"text":"\\\\\\""}""";

    //--- when
    Pojo pojo = new Pojo();
    Jetson.decodeIntoObject( given, pojo );

    //--- then
    assertEquals( 2, pojo.text.length() );
    assertEquals( "\\\"", pojo.text );
  }

  @Test
  @DisplayName("Encode all characters according to RFC 8259 chapter 7")
  void encodeCharacterTest() {
    StringBuilder raw = new StringBuilder();
    for ( int i = 0; i <= 0x1F; i++ ) {
      raw.append( (char) i );
    }

    Pojo pojo = new Pojo();
    pojo.text = raw.toString();

    String json = Jetson.encode( pojo );

    Map<Integer, String> special = Map.of(
        0x08, "\\b",
        0x09, "\\t",
        0x0A, "\\n",
        0x0C, "\\f",
        0x0D, "\\r"
    );

    for ( int i = 0; i <= 0x1F; i++ ) {
      String expected = special.containsKey( i ) ?
          special.get( i ) : String.format( "\\u%04x", i );
      assertTrue( json.contains( expected ),
          "Missing escape for char 0x" + Integer.toHexString( i ) );
    }
  }

  @Test
  @DisplayName("Encode solidus and double-quote")
  void encodeSolidusAndQuote() {
    //--- given
    String given = "\"\\";
    Pojo pojo = new Pojo();
    pojo.text = given;
    String expected = """
        {"text":"\\\"\\\\"}""";

    //--- when
    String json = Jetson.encode( pojo );

    //--- then
    assertEquals( expected, json );
  }

  @Json(accessType = Json.AccessType.FIELD)
  private static class Pojo {
    String text;
  }
}

