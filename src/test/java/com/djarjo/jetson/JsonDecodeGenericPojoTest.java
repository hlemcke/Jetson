package com.djarjo.jetson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JsonDecodeGenericPojoTest {

    @Test
    @DisplayName("Decode json into ValueDTO<String>")
    void testValueDTO_String() throws ParseException, IllegalAccessException {
        //--- given
        String code = "A4X3";
        String value = "some Text";
        String json = """
                {"value": "some Text", "code": "A4X3" }""";

        //--- when
        @SuppressWarnings("unchecked")
        ValueDTO<String> dto = (ValueDTO<String>)
                Jetson.decodeIntoObject(json, new ValueDTO<String>());

        //--- then
        assertNotNull(dto);
        assertEquals(code, dto.code);
        assertEquals(value, dto.value);
    }

    @Test
    @DisplayName("Decode json into ValueDTO<Pojo>")
    void testValueDTO_Pojo() throws ParseException, IllegalAccessException {
        //--- given
        String code = "A4X3";
        String str = "Magnolia";
        String json = """
                {"value": { "str": "Magnolia" }, "code": "A4X3" }""";

        //--- when
        ValueDTO<Pojo> target = new ValueDTO<>();
        @SuppressWarnings("unchecked")
        ValueDTO<Pojo> dto = (ValueDTO<Pojo>)
                new JsonDecoder().decodeIntoObjectWithGeneric(json, target, Pojo.class);

        //--- then
        assertNotNull(dto);
        assertEquals(code, dto.code);
        assertInstanceOf(Pojo.class, dto.value);
        Pojo pojo = dto.value;
        assertNotNull(pojo);
        assertEquals(str, pojo.str);
    }

    @Test
    @DisplayName("Decode json into ValueListDTO<String>")
    void testValueListDTO_String() throws ParseException, IllegalAccessException {
        //--- given
        String code = "A4X3";
        String item1 = "value#1", value2 = "value#2";
        String json = """
                {"code": "A4X3", "items": ["value#1", "value#2"] }""";

        //--- when
        @SuppressWarnings("unchecked")
        ValueListDTO<String> dto = (ValueListDTO<String>)
                Jetson.decodeIntoObject(json, new ValueListDTO<String>());

        //--- then
        assertNotNull(dto);
        assertEquals(code, dto.code);
        assertEquals(2, dto.items.size());
        assertEquals(item1, dto.items.get(0));
        assertEquals(value2, dto.items.get(1));
    }

    @Test
    @DisplayName("Decode json into ValueListDTO<Pojo>")
    void testValueListDTO_Pojo() throws ParseException, IllegalAccessException {
        //--- given
        String code = "A4X3";
        String item1 = "value#1", item2 = "value#2";
        String json = """
                {"code": "A4X3", "items": [ { "str": "value#1" },
                { "str": "value#2"} ] }""";

        //--- when
        ValueListDTO<Pojo> target = new ValueListDTO<>();
        @SuppressWarnings("unchecked")
        ValueListDTO<Pojo> dto = (ValueListDTO<Pojo>)
                new JsonDecoder().decodeIntoObjectWithGeneric(json, target, Pojo.class);

        //--- then
        assertNotNull(dto);
        assertEquals(code, dto.code);
        assertEquals(2, dto.items.size());
        List<Pojo> list = dto.items;
        assertEquals(item1, list.get(0).str);
        assertEquals(item2, list.get(1).str);
    }

    private static class Pojo {
        @Json
        public String str = "abc";
    }

    private static class ValueDTO<T> {
        @Json
        T value;
        @Json
        String code;
    }

    private static class ValueListDTO<T> {
        @Json
        List<T> items = null;
        @Json
        String code;
    }
}
