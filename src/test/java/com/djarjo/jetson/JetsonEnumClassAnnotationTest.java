package com.djarjo.jetson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Enum class annotated with @Json without converter")
public class JetsonEnumClassAnnotationTest {

  @Test
  void testEncodeEnumWithToJson() {
    //--- given
    PojoToJson pojo = new PojoToJson();
    pojo.enumList.add( ToJsonEnum.SECOND_VALUE );
    String expected = """
        {"enumList":["second-value"],"value":"first-value"}""";

    //--- when
    String json = Jetson.encode( pojo );

    //--- then
    assertEquals( expected, json );
  }

  @Test
  void testEncodeEnumWithClassEnumAccessor() {
    //--- given
    PojoAccessor pojo = new PojoAccessor();
    pojo.enumList.add( AccessorEnum.LESS_THAN_1_5_KMH );
    String expected = """
        {"enumList":["lessThan1p5Kmh"],"value":"notKnown"}""";

    //--- when
    String json = Jetson.encode( pojo );

    //--- then
    assertEquals( expected, json );
  }

  @Json
  public enum ToJsonEnum {
    FIRST_VALUE,
    SECOND_VALUE;

    public String toJson() {
      return name().toLowerCase().replace( '_', '-' );
    }
  }

  @Json(enumAccessor = "json")
  public enum AccessorEnum {
    LESS_THAN_1_5_KMH( "lessThan1p5Kmh" ),
    NOT_KNOWN( "notKnown" );

    public final String json;

    AccessorEnum( String json ) {
      this.json = json;
    }
  }

  @Json(accessType = Json.AccessType.FIELD)
  static class PojoToJson {
    public ToJsonEnum value = ToJsonEnum.FIRST_VALUE;
    public List<ToJsonEnum> enumList = new ArrayList<>();
  }

  @Json(accessType = Json.AccessType.FIELD)
  static class PojoAccessor {
    public AccessorEnum value = AccessorEnum.NOT_KNOWN;
    public List<AccessorEnum> enumList = new ArrayList<>();
  }
}
