package com.djarjo.jetson;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecodeMissingFieldsTest {
  @Test
  void decodeFieldsNotInPojo() throws ParseException, IllegalAccessException {
    //--- given
    LocalDate date = LocalDate.of( 2018, 9, 21 );
    String json = """
        {"date": "2018-09-21", "name": "another", "value": 567, "skipValue": true,
        "skipList": [17, 42, 99], "skipMap":{"k1":"v1"}}""";

    //--- when
    Pojo pojo = new Pojo();
    Jetson.decodeIntoObject( json, pojo );

    //--- then
    assertEquals( date, pojo.date );
    assertEquals( "another", pojo.name );
    assertEquals( 123, pojo.value );
  }

  @Json(accessType = Json.AccessType.FIELD)
  static class Pojo {
    LocalDate date = LocalDate.of( 2001, 12, 31 );
    String name = "who am I";
    @JsonTransient
    int value = 123;
  }
}
