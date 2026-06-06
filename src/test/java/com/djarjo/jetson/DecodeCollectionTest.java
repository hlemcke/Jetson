package com.djarjo.jetson;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DecodeCollectionTest {

  @Test
  void decodeIntoPojoWithSet() throws ParseException, IllegalAccessException {
    //--- given
    UUID uuid0 = UUID.randomUUID(), uuid1 = UUID.randomUUID();
    String json = String.format( "{\"set\":[\"%s\",\"%s\" ]}", uuid0, uuid1 );

    //--- when
    PojoWithSet pojo = new PojoWithSet();
    Jetson.decodeIntoObject( json, pojo );

    //--- then
    assertNotNull( pojo.set );
    assertTrue( pojo.set.contains( uuid0 ) );
    assertTrue( pojo.set.contains( uuid1 ) );
  }

  @Test
  void decodeIntoPojoWithSetOfClass() throws ParseException, IllegalAccessException {
    //--- given
    UUID uuid0 = UUID.randomUUID(), uuid1 = UUID.randomUUID(), uuid2 = UUID.randomUUID();
    String json = String.format( "{\"nestedSet\":[{\"set\":[\"%s\",\"%s\"]},{\"set\":[\"%s\"]}]}", uuid0, uuid1, uuid2 );

    //--- when
    PojoWithSetOfClass pojo = new PojoWithSetOfClass();
    Jetson.decodeIntoObject( json, pojo );

    //--- then
    assertNotNull( pojo.nestedSet );
    assertEquals( 2, pojo.nestedSet.size() );
    Iterator<PojoWithSet> iterator = pojo.nestedSet.iterator();
    PojoWithSet pws = iterator.next();
    if ( pws.set.size() == 1 ) {
      assertTrue( pws.set.contains( uuid2 ) );
    } else {
      assertTrue( pws.set.contains( uuid0 ) );
      assertTrue( pws.set.contains( uuid1 ) );
    }
  }

  static class PojoWithSet {
    @Json
    Set<UUID> set;
  }

  static class PojoWithSetOfClass {
    @Json
    Set<PojoWithSet> nestedSet;
  }
}
