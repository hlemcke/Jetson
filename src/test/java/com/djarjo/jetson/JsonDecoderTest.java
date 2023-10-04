/**
 *
 */
package com.djarjo.jetson;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import org.junit.jupiter.api.Test;

import com.djarjo.text.Token;

/**
 * @author Hajo Lemcke
 * @since 2023-09-21
 */
class JsonDecoderTest {
	static boolean isVerbose = false;

	public static void main( String[] args ) {
		isVerbose = true;
		JsonDecoderTest test = new JsonDecoderTest();
		System.out.println( "nothing to do" );
	}

	@Test
	void test_getInstanceFromGeneric()
			throws IllegalAccessException, InstantiationException,
			IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		Token token = (Token) _getInstanceFromGeneric( Token.class );
		assertNotNull( token );
	}

	private Object _getInstanceFromGeneric( Type generic )
			throws IllegalAccessException, InstantiationException,
			IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		Object target = null;
		if ( generic instanceof Class == false ) {
			return null;
		}
		Class<?> cls = (Class<?>) generic;
		// Class<?> clazz = Class.forName( generic.getTypeName() );
		target = cls.getDeclaredConstructor().newInstance();
		return target;
	}
}