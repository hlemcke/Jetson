package com.djarjo.jetson;


import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecodeToNestedClassesTest {
  @Test
  void testGetterAndSetterInSeparateClasses() throws ParseException, IllegalAccessException {
    //--- given
    Instant now = Instant.now();
    String json = String.format( """
        {"whenSynced": "%s"}""", now );

    //--- when
    SubClass pojo = new SubClass();
    assertEquals( Instant.EPOCH, pojo.getWhenSynced() );
    Jetson.decodeIntoObject( json, pojo );

    //--- then
    assertEquals( now, pojo.getWhenSynced() );
  }


  class BaseClass {
    Instant whenSynced = Instant.EPOCH;

    @Json
    public Instant getWhenSynced() {
      return whenSynced;
    }
  }

  class SubClass extends BaseClass {
    public SubClass setWhenSynced( Instant instant ) {
      whenSynced = instant;
      return this;
    }
  }
}
