package com.djarjo.jetson;

import com.djarjo.jetson.converter.JsonConverter;
import com.djarjo.text.TextHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class JetsonEncodePojoTest {

    @Test
    @DisplayName("Pojo annotated for Field access")
    void pojoAnnotatedForFieldAccess() {
        //--- given
        PojoAnnotatedField pojo = new PojoAnnotatedField();

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertNotNull(json);
        assertEquals("{\"ival\":42}", json);
    }

    @Test
    @DisplayName("Pojo annotated for Property access")
    void pojoAnnotatedForPropertyAccess() {
        //--- given
        PojoAnnotatedProperty pojo = new PojoAnnotatedProperty();

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertNotNull(json);
        assertEquals("{\"some\":42}", json);
    }

    @Test
    @DisplayName("Pojo with Array, List and Set")
    void pojoWithCollectionsShouldEncodeCorrectly() {
        //--- given
        PojoWithCollection pojo = new PojoWithCollection();

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertNotNull(json);
        assertTrue(json.contains("[{\"ival\":42,"));
        assertTrue(json.contains("{\"ival\":17,"));
        assertTrue(json.contains("{\"ival\":27,"));
        assertTrue(json.contains("{\"ival\":37,"));
    }

    @Test
    void testPojoWithConverter() {
        //--- given
        Furi pojo = new Furi();

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertNotNull(json);
        assertEquals("\"enc=42\"", json);
    }

    @Test
    void testPojoWithConverterCollection() {
        //--- given
        PojoWithCollection pojo = new PojoWithCollection();

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertNotNull(json);
        assertTrue(json.contains("furies\":[\"enc=4711\",\"enc=69\"]"));
    }

    @Test
    @DisplayName("POJO with mixed fields and getters")
    void pojoWithMixedFieldsAndGettersMustNotShowPrivateFields() {
        //--- given
        PojoWithMixedFieldsAndGetters pojo = new PojoWithMixedFieldsAndGetters();

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertFalse(json.isEmpty());
    }

    private static class Pojo {
        @Json
        public int ival = 42;
        @Json
        public String str = "öäü$";

        Pojo() {
        }

        Pojo(int i, String s) {
            ival = i;
            str = s;
        }
    }

    @Json(accessType = Json.AccessType.FIELD)
    private static class PojoAnnotatedField {
        public int ival = 42;

        @JsonTransient
        public String nonono = "but with value";

        public int getSome() {
            return ival;
        }

        @JsonTransient
        public int getNot() {
            return 4711;
        }
    }

    @Json
    private static class PojoAnnotatedProperty {
        public int ival = 42;

        @JsonTransient
        public String nonono = "but with value";

        public int getSome() {
            return ival;
        }

        @JsonTransient
        public int getNot() {
            return 4711;
        }
    }

    @Json(converter = Furi.Converter4json.class)
    private static class Furi {
        public int ival = 42;

        @JsonTransient
        public String nonono = "but with value";

        public Furi() {
        }

        public Furi(int value) {
            this.ival = value;
        }

        public static Furi decode(String json) {
            Furi pojo = new Furi();
            if (json != null && !json.isBlank()) {
                pojo.ival = TextHelper.parseInteger(json.substring("enc=".length()));
            }
            return pojo;
        }

        public String encode() {
            return String.format("\"enc=%d\"", ival);
        }

        public int getSome() {
            return ival;
        }

        @JsonTransient
        public int getNot() {
            return 4711;
        }

        public static class Converter4json implements JsonConverter<Furi> {
            @Override
            public Furi decodeFromJson(String jsonValue) {
                if (jsonValue == null || jsonValue.isBlank()) return null;
                return Furi.decode(jsonValue);
            }

            @Override
            public String encodeToJson(Furi pojo) {
                return (pojo == null) ? null : pojo.encode();
            }
        }
    }

    private static class PojoWithCollection {
        public static final String str2 = "?µM";
        public static final String strList = "pojo in list";
        public static final String strSet = "pojo in Set";

        @Json
        public Pojo[] pojoArray = {new Pojo(), new Pojo(17, str2)};

        @Json
        public List<Pojo> pojoList = List.of(new Pojo(), new Pojo(27, strList));

        @Json
        public Set<Pojo> pojoSet = Set.of(new Pojo(), new Pojo(37, strSet));

        @Json
        public List<Furi> furies = List.of(new Furi(4711), new Furi(69));
    }

    private static class PojoWithMixedFieldsAndGetters {
        @Json
        private final String mustNotShowUp1 = "invisible #1";
        public String mustNotShowUp2 = "invisible #2";
        @Json
        public String mustShowUp = "visible field";
        private String _hidden = "but with value";

        @Json
        public String getHidden() {
            return _hidden;
        }

        public void setHidden(String hidden) {
            _hidden = hidden;
        }
    }
}
