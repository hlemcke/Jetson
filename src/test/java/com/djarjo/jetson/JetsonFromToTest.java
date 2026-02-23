package com.djarjo.jetson;

import com.djarjo.common.BaseConverter;
import com.djarjo.text.TextHelper;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class JetsonFromToTest {

    @Test
    void testToJsonWithEmptyList() {
        //--- given
        Pojo pojo = new Pojo();

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertNotNull(json);
        assertEquals("""
                {"fruit":"Apple"}""", json);
    }

    @Test
    void testToJsonWithOneFuri() {
        //--- given
        Pojo pojo = new Pojo();
        pojo.furies = new ArrayList<>();
        pojo.furies.add(new Furi());

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertNotNull(json);
        assertEquals(1, pojo.furies.size());
        assertEquals("""
                {"fruit":"Apple","furies":["tel:12345678"]}""", json);
    }

    @Test
    void testToJsonWithTwoFuries() {
        //--- given
        Pojo pojo = new Pojo();
        pojo.furies = new ArrayList<>();
        pojo.furies.add(new Furi());
        Furi furi2 = new Furi();
        furi2.type = "mailto";
        furi2.value = "someone@email.com";
        pojo.furies.add(furi2);

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertNotNull(json);
        assertEquals("""
                        {"fruit":"Apple","furies":["tel:12345678","mailto:someone@email.com"]}""",
                json);
    }

    @Test
    void testToJsonFromRecord() {
        //--- given
        Pojo2 pojo = new Pojo2();
        pojo.weight = new SIValue(17.23, SIUnit.MASS);

        //--- when
        String json = Jetson.encode(pojo);

        //--- then
        assertNotNull(json);
        assertEquals("""
                {"weight":"17.23 kg"}""", json);
    }

    @Test
    void testFromJsonIntoRecord() throws ParseException, IllegalAccessException {
        //--- given
        String json = """
                {"weight":"17.23 kg"}""";

        //--- when
        Pojo2 pojo = (Pojo2) Jetson.decodeIntoObject(json, new Pojo2());

        //--- then
        assertNotNull(pojo);
        assertEquals(pojo.weight.value(), 17.23);
    }

    @Test
    void testFromJsonWithEmptyList() throws ParseException, IllegalAccessException {
        //--- given
        String input = """
                {"fruit":"Apple"}""";
        Pojo pojo = new Pojo();

        //--- when
        Pojo decoded = (Pojo) Jetson.decodeIntoObject(input, pojo);

        //--- then
        assertEquals("Apple", decoded.fruit);
        assertNull(decoded.furies);
    }

    @Test
    void testFromJsonWithOneFuri() throws ParseException, IllegalAccessException {
        //--- given
        String input = """
                {"fruit":"Banana","furies":["im:me@chatter"]}""";
        Pojo pojo = new Pojo();

        //--- when
        Pojo decoded = (Pojo) Jetson.decodeIntoObject(input, pojo);

        //--- then
        assertEquals("Banana", decoded.fruit);
        assertNotNull(decoded.furies);
        assertEquals(1, decoded.furies.size());
        Furi furi = decoded.furies.getFirst();
        assertNotNull(furi);
        assertEquals("im", furi.type);
        assertEquals("me@chatter", furi.value);
    }

    @Test
    void testFromJsonWithTwoFuries() throws ParseException, IllegalAccessException {
        //--- given
        String input = """
                {"fruit":"Banana", "furies":["im:me@chatter", "url:djarjo.com"]}""";
        Pojo pojo = new Pojo();

        //--- when
        Pojo decoded = (Pojo) Jetson.decodeIntoObject(input, pojo);

        //--- then
        assertEquals("Banana", decoded.fruit);
        assertNotNull(decoded.furies);
        assertEquals(2, decoded.furies.size());
        Furi furi = decoded.furies.getFirst();
        assertNotNull(furi);
        assertEquals("im", furi.type);
        assertEquals("me@chatter", furi.value);
        furi = decoded.furies.get(1);
        assertEquals("url", furi.type);
        assertEquals("djarjo.com", furi.value);
    }

    public static enum SIUnit {
        CURRENT("A"),
        LENGTH("m"),
        MASS("kg"),
        TIME("s");

        public final String code;

        SIUnit(String code) {
            this.code = code;
        }

        public static SIUnit find(String text) {
            return TextHelper.findEnum(text, SIUnit.class, null, "code");
        }
    }

    @Json
    static class Furi {
        String type = "tel";
        String value = "12345678";

        public static Furi fromJson(String json) {
            Furi furi = new Furi();
            String[] parts = json.split(":");
            furi.type = parts[0];
            furi.value = parts[1];
            return furi;
        }

        public String toJson() {
            return String.format("%s:%s", type, value);
        }
    }

    @Json(accessType = Json.AccessType.FIELD)
    static class Pojo {
        public String fruit = "Apple";
        public List<Furi> furies;
    }

    @Json
    public record SIValue(double value, SIUnit unit) {
        private static final DecimalFormat FORMATTER;

        static {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            FORMATTER = new DecimalFormat("0.#######", symbols);
        }

        public static SIValue fromJson(String json) {
            if (json == null || json.isBlank()) return null;
            String input = json.trim().replaceAll(" {2}", " ");
            String[] parts = input.split(" ");
            double value = parts[0].isBlank() ? 0 : BaseConverter.toDouble(parts[0]);
            SIUnit unit = parts[1].isBlank() ? null : SIUnit.find(parts[1]);
            return new SIValue(value, unit);
        }

        public String toJson() {
            String formattedValue = FORMATTER.format(value);
            String unitCode = (unit == null) ? "" : unit.code;
            return (formattedValue + " " + unitCode).trim();
        }
    }

    @Json(accessType = Json.AccessType.FIELD)
    public class Pojo2 {
        public SIValue weight;
    }
}
