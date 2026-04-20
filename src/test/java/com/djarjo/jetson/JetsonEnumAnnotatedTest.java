package com.djarjo.jetson;

import com.djarjo.jetson.converter.JsonConverter;
import com.djarjo.text.TextHelper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
@DisplayName("Enum is annotated and used in Pojo")
public class JetsonEnumAnnotatedTest {

  @Test
  void testEncodePojoWithEnums() {
    //--- given
    Pojo pojo = new Pojo();
    pojo.enumList.add( SomeEnum.ENTRY7 );
    pojo.enumList.add( SomeEnum.ENTRY23 );
    String expected = """
        {"someEnum":-1,"enumList":[7,23]}""";

    //--- when
    String json = Jetson.encode( pojo );

    //--- then
    assertEquals( expected, json );
  }

  @Json(converter = SomeEnum.Converter4json.class)
  public enum SomeEnum {
    ENTRY7( 7 ),
    ENTRY23( 23 ),
    UNKNOWN( -1 );
    public final int code;

    SomeEnum( int code ) {
      this.code = code;
    }

    public static SomeEnum find( String text ) {
      System.out.println( "find( " + text + ")" );
      return TextHelper.findEnum( text, SomeEnum.class, SomeEnum.UNKNOWN, "code" );
    }

    public static class Converter4json implements JsonConverter<SomeEnum> {
      @Override
      public String encodeToJson( SomeEnum attribute ) {
        System.out.println( "encodeToJson( " + attribute + ")" );
        return (attribute == null) ? null : "" + attribute.code;
      }

      @Override
      public SomeEnum decodeFromJson( String jsonValue ) {
        return (jsonValue == null) ? null : SomeEnum.find( jsonValue );
      }
    }
  }

  @Json(accessType = Json.AccessType.FIELD)
  static class Pojo {
    @Json(enumAccessor = "code")
    public SomeEnum someEnum = SomeEnum.UNKNOWN;
    public List<SomeEnum> enumList = new ArrayList<>();
  }
}
