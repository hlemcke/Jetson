package com.djarjo.jetson;

import com.djarjo.jetson.converter.UriConverter4json;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JetsonCacheTest {
    @Test
    public void testBeanWithFields() throws Exception {
        //--- given

        //--- when
        List<JsonAccessor> accessors = JsonCache.obtainAllJsonAccessors(
                BeanWithFields.class);

        //--- then
        assertEquals(2, accessors.size());
        JsonAccessor acc1 = accessors.getFirst();
        assertEquals(UriConverter4json.class, acc1.getConverter().getClass());
        JsonAccessor acc2 = accessors.get(1);
        assertEquals("other", acc2.getJsonName());
    }

    @Test
    public void testAnnotatedBeanNeverDecodes() throws Exception {
        //--- when
        List<JsonAccessor> accessors = JsonCache.obtainAllJsonAccessors(
                AnnotatedBeanNeverDecodes.class);

        //--- then
        assertEquals(2, accessors.size());
        JsonAccessor acc1 = accessors.getFirst();
        assertEquals("field1", acc1.getJsonName());
        JsonAccessor acc2 = accessors.get(1);
        assertEquals("field2", acc2.getJsonName());
    }

    static class BeanWithFields {
        @JsonTransient
        protected BigDecimal field3;
        @Json(converter = UriConverter4json.class)
        private String field1;
        @Json(name = "other")
        private Double field2;
    }

    @Json(decode = Json.DecodeMode.NEVER)
    static class AnnotatedBeanNeverDecodes {
        @Json
        private String field1;
        @JsonTransient
        private Double field2;

        public Double getField2() {
            return field2;
        }
    }
}
