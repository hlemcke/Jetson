package com.djarjo.jetson;

import com.djarjo.text.SomeEnum;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JetsonEnumTest {

    @Test
    void testEncodeEnumWithName() throws ParseException, IllegalAccessException {
        //--- given
        String expected = """
                {"someEnum":"ENUM_VALUE_1"}""";
        PojoName pojo = new PojoName();

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertEquals(expected, json);
    }

    @Test
    void testEncodeEnumWithOrdinal() throws ParseException, IllegalAccessException {
        //--- given
        PojoOrdinal pojo = new PojoOrdinal();

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertEquals("{\"someEnum\":0}", json);
    }

    @Test
    void testDecodeEnumWithAccessor() throws ParseException, IllegalAccessException {
        //--- given
        List<String> inputs = List.of("{\"someEnum\":\"code2\"}",
                "{\"someEnum\":\"ENUM_VALUE_3\"}");
        List<SomeEnum> expected = List.of(SomeEnum.ENUM_VALUE_2, SomeEnum.ENUM_VALUE_3);

        //--- when
        for (int i = 0; i < inputs.size(); i++) {
            PojoCode pojo = (PojoCode) Jetson.decodeIntoObject(inputs.get(i),
                    new PojoCode());
            assertEquals(expected.get(i), pojo.someEnum);
        }
    }

    @Test
    void testDecodeEnumWithName() throws ParseException, IllegalAccessException {
        //--- given
        String input = """
                {"someEnum":"ENUM_VALUE_1"}""";

        //--- when
        PojoName pojo = (PojoName) Jetson.decodeIntoObject(input, new PojoName());

        //--- then
        assertEquals(SomeEnum.ENUM_VALUE_1, pojo.someEnum);
    }

    @Test
    void testDecodeEnumWithOrdinal() throws ParseException, IllegalAccessException {
        //--- given
        String input = "{\"someEnum\":1}";

        //--- when
        PojoOrdinal pojo = (PojoOrdinal) Jetson.decodeIntoObject(input, new PojoOrdinal());

        //--- then
        assertEquals(SomeEnum.ENUM_VALUE_2, pojo.someEnum);
    }

    static class PojoCode {
        @Json(enumAccessor = "code")
        public SomeEnum someEnum = SomeEnum.ENUM_VALUE_1;
    }

    static class PojoName {
        @Json
        public SomeEnum someEnum = SomeEnum.ENUM_VALUE_1;
    }

    static class PojoOrdinal {
        @Json(enumAccessor = "ordinal")
        public SomeEnum someEnum = SomeEnum.ENUM_VALUE_1;
    }

}
