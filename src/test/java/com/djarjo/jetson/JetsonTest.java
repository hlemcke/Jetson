/**
 *
 */
package com.djarjo.jetson;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 *
 */
class JetsonTest {

	/**
	 * Test method for
	 * {@link com.djarjo.jetson.Jetson#decode(java.lang.String)}.
	 */
	@Test
	void testDecodeToLong() throws ParseException, IllegalAccessException {
		//--- given
		String json = "123456789";
		//--- when
		Object value = Jetson.decode( json );
		//--- then
		assertEquals( 123_456_789L, value );
	}

	@Test
	void testDecodeToList() throws ParseException, IllegalAccessException {
		//--- given
		String json = "[ 123, 456, 789 ]";
		//--- when
		Object value = Jetson.decode( json );
		//--- then
		assertInstanceOf( List.class, value );
		List<?> list = (List<?>) value;
		assertEquals( 3, list.size() );
		assertEquals( 123L, list.get( 0 ) );
	}
}
