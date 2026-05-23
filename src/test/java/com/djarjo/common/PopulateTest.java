package com.djarjo.common;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PopulateTest {
  final String expStr = "have fun";

  @Test
  void testPopulatePojoBasicFields() throws IllegalAccessException {
    //--- given
    Map<String, Object> map = getPojoMap();
    Pojo pojo = new Pojo();

    //--- when
    BeanHelper.populate( pojo, map );

    //--- then
    assertEquals( true, pojo.b );
    assertEquals( map.get( "id" ), pojo.id );
    assertEquals( expStr, pojo.s );
    assertEquals( 42, pojo.x );
  }

  @Test
  @Disabled
  void testPopulatePojoNestedClass() throws IllegalAccessException {
    //--- given
    LocalDate date = LocalDate.now();
    Map<String, Object> map = getPojoMap();
    map.put( "pojo2", new HashMap<>( Map.of( "y", 666, "date", date ) ) );

    Pojo pojo = new Pojo();

    //--- when
    BeanHelper.populate( pojo, map );

    //--- then
    assertNotNull( pojo.pojo2 );
    assertEquals( "y", pojo.pojo2.y );
    assertEquals( date, pojo.pojo2.today );
  }

  Map<String, Object> getPojoMap() {
    Map<String, Object> map = new HashMap<>();
    map.put( "b", true );
    map.put( "id", UUID.randomUUID() );
    map.put( "s", expStr );
    map.put( "x", 42 );
    return map;
  }

  static class Pojo {
    boolean b = false;
    UUID id = null;
    String s = "initial";
    int x = 0;
    Pojo2 pojo2 = null;
    List<Pojo2> pojo2List = null;
    Map<String, Pojo2> pojo2Map = null;
  }

  static class Pojo2 {
    int y = 0;
    LocalDate today = LocalDate.now();
  }
}
