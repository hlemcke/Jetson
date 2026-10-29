package com.djarjo.jetson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
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

  ///
  /// Decoding into an enum whose class is annotated with an `int` accessor
  /// (like `com.djarjo.common.Priority`). JSON `2` must become `MEDIUM`.
  ///
  /// Found in djarjo_server: sync of `MemoryBO` with `"memPriority":0` failed
  /// with `NoSuchMethodException: Priority.<init>()` because the decoder tried
  /// to instantiate the enum class.
  ///
  @Test
  void testDecodeEnumWithClassIntAccessor() throws Exception {
    //--- given
    String json = """
        {"enumList":[1,8],"value":2}""";

    //--- when
    PojoValueAccessor pojo =
        (PojoValueAccessor) Jetson.decodeIntoObject( json, new PojoValueAccessor() );

    //--- then
    assertAll( "int accessor (field access)",
        () -> assertEquals( ValueEnum.MEDIUM, pojo.value ),
        () -> assertEquals( List.of( ValueEnum.LOW, ValueEnum.CRITICAL ),
            pojo.enumList ) );
  }

  ///
  /// Same as [#testDecodeEnumWithClassIntAccessor()] but through annotated
  /// getter and setter (like `MemoryBO.getMemPriority()`)
  ///
  @Test
  void testDecodeEnumWithClassIntAccessorViaGetter() throws Exception {
    //--- given
    String json = """
        {"priority":4}""";

    //--- when
    BeanValueAccessor bean =
        (BeanValueAccessor) Jetson.decodeIntoObject( json, new BeanValueAccessor() );

    //--- then
    assertEquals( ValueEnum.HIGH, bean.getPriority() );
  }

  ///
  /// `0` is a valid value and must not be treated as "missing"
  ///
  @Test
  void testDecodeEnumWithClassIntAccessorZero() throws Exception {
    //--- given
    String json = """
        {"priority":0}""";
    BeanValueAccessor bean = new BeanValueAccessor();
    bean.setPriority( ValueEnum.HIGH );

    //--- when
    Jetson.decodeIntoObject( json, bean );

    //--- then
    assertEquals( ValueEnum.NONE, bean.getPriority() );
  }

  ///
  /// Encoding of an enum with class `int` accessor writes a JSON number
  ///
  @Test
  void testEncodeEnumWithClassIntAccessor() {
    //--- given
    PojoValueAccessor pojo = new PojoValueAccessor();
    pojo.value = ValueEnum.HIGH;
    pojo.enumList.add( ValueEnum.LOW );
    String expected = """
        {"enumList":[1],"value":4}""";

    //--- when
    String json = Jetson.encode( pojo );

    //--- then
    assertEquals( expected, json );
  }

  ///
  /// Collections and arrays with exactly one single-char element must not
  /// start with a comma (was `[,1]`)
  ///
  @Test
  void testEncodeSingleCharElement() {
    assertAll( "single char element",
        () -> assertEquals( "[1]", Jetson.encode( List.of( 1 ) ) ),
        () -> assertEquals( "[5]", Jetson.encode( new int[] { 5 } ) ),
        () -> assertEquals( "[1,2]", Jetson.encode( List.of( 1, 2 ) ) ) );
  }

  ///
  /// Round trip: encode and decode give the same values
  ///
  @Test
  void testRoundTripEnumWithClassIntAccessor() throws Exception {
    //--- given
    BeanValueAccessor original = new BeanValueAccessor();
    original.setPriority( ValueEnum.CRITICAL );

    //--- when
    String json = Jetson.encode( original );
    BeanValueAccessor decoded =
        (BeanValueAccessor) Jetson.decodeIntoObject( json, new BeanValueAccessor() );

    //--- then
    assertEquals( original.getPriority(), decoded.getPriority(), json );
  }

  ///
  /// Decoding into an enum whose class is annotated with a `String` accessor.
  /// Counterpart of [#testEncodeEnumWithClassEnumAccessor()]
  ///
  @Test
  void testDecodeEnumWithClassStringAccessor() throws Exception {
    //--- given
    String json = """
        {"enumList":["notKnown","lessThan1p5Kmh"],"value":"lessThan1p5Kmh"}""";

    //--- when
    PojoAccessor pojo =
        (PojoAccessor) Jetson.decodeIntoObject( json, new PojoAccessor() );

    //--- then
    assertAll( "String accessor",
        () -> assertEquals( AccessorEnum.LESS_THAN_1_5_KMH, pojo.value ),
        () -> assertEquals(
            List.of( AccessorEnum.NOT_KNOWN, AccessorEnum.LESS_THAN_1_5_KMH ),
            pojo.enumList ) );
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

  /// Like `com.djarjo.common.Priority`
  @Json(enumAccessor = "value")
  public enum ValueEnum {
    NONE( 0 ),
    LOW( 1 ),
    MEDIUM( 2 ),
    HIGH( 4 ),
    CRITICAL( 8 );

    public final int value;

    ValueEnum( int value ) {
      this.value = value;
    }
  }

  @Json(accessType = Json.AccessType.FIELD)
  static class PojoValueAccessor {
    public ValueEnum value = ValueEnum.NONE;
    public List<ValueEnum> enumList = new ArrayList<>();
  }

  /// Bean with annotated getter and setter like `MemoryBO`
  public static class BeanValueAccessor {
    private ValueEnum priority = null;

    @Json
    public ValueEnum getPriority() {
      return priority;
    }

    public BeanValueAccessor setPriority( ValueEnum priority ) {
      this.priority = priority;
      return this;
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
