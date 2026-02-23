package com.djarjo.text;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateTimeParserTest {
    @Test
    void testParseTime() {
        //--- given
        DateTimeParser parser = new DateTimeParser();
        List<String> inputs = List.of("01.02.03", "174326", "17432612345",
                "13.14.15.16");
        List<LocalTime> expected = List.of(LocalTime.of(1, 2, 3),
                LocalTime.of(17, 43, 26),
                LocalTime.of(17, 43, 26, 123_450_000),
                LocalTime.of(13, 14, 15, 160_000_000));

        //--- when / then
        for (int i = 0; i < inputs.size(); i++) {
            LocalTime lt = parser.parseTime(inputs.get(i));
            assertEquals(expected.get(i), lt);
        }
    }
}
