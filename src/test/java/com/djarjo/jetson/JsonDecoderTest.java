/**
 *
 */
package com.djarjo.jetson;

import com.djarjo.text.Token;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Hajo Lemcke
 * @since 2023-09-21
 */
class JsonDecoderTest {

	@Test
	void test_getInstanceFromGeneric() throws IllegalAccessException,
			InstantiationException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		Token token = (Token) _getInstanceFromGeneric( Token.class );
		assertNotNull( token );
	}

	private Object _getInstanceFromGeneric(
			Type generic ) throws IllegalAccessException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException {
		Object target = null;
		if ( !(generic instanceof Class<?> cls) ) {
			return null;
		}
		// Class<?> clazz = Class.forName( generic.getTypeName() );
		target = cls.getDeclaredConstructor()
				.newInstance();
		return target;
	}
}