/**
 *
 */
package com.djarjo.jetson;

import com.djarjo.text.Token;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hajo Lemcke
 * @since 2025-06-26 added test of orphan removal in collections
 * @since 2023-09-21
 */
class JsonDecoderTest {

	@Test
	void testEncodingPojoWithList() {
		//--- given
		PojoWithList bean = new PojoWithList();

		//--- when
		String json = Jetson.encode( bean );

		//--- then
		assertEquals( "{\"id\":", json.substring( 0, 6 ) );
		assertTrue( json.contains( "\"fruits\":[\"Apple\",\"Kiwi\"]" ), "actual=" + json );
		assertTrue( json.contains( "\"pojos\":[{\"id\":" ), "actual=" + json );
	}

	@Test
	void testDecodeJsonIntoPojoWithList() throws ParseException, IllegalAccessException {
		//--- given
		OffsetDateTime now = OffsetDateTime.now();
		PojoWithList beanFromDb = new PojoWithList();
		beanFromDb.whenInserted = now;
		PojoWithList beanFromUi = new PojoWithList();
		beanFromUi.fruits = new ArrayList<>( List.of( "Banana", "Cherry", "Peach" ) );
		beanFromUi.pojos.remove( 0 );
		String json = Jetson.encode( beanFromUi );

		//--- when
		PojoWithList decodedBean = (PojoWithList) Jetson.decodeIntoObject( json, beanFromDb );

		//--- then
		assertEquals( 3, decodedBean.fruits.size() );
		assertEquals( "Banana", decodedBean.fruits.get( 0 ) );
		assertEquals( "Cherry", decodedBean.fruits.get( 1 ) );
		assertEquals( "Peach", decodedBean.fruits.get( 2 ) );
		assertEquals( 1, decodedBean.pojos.size() );
		assertEquals( "second", decodedBean.pojos.get( 0 ).title );
		assertEquals( now, decodedBean.whenInserted, "Must be kept from DB entity" );
	}

	@Test
	void testDecodeJsonIntoPojoWithMergeDecoding() throws ParseException, IllegalAccessException {
		//--- given
		OffsetDateTime now = OffsetDateTime.now();
		PojoWithList beanFromDb = new PojoWithList();
		beanFromDb.whenInserted = now;
		PojoWithList beanFromUi = new PojoWithList();
		beanFromUi.id = beanFromDb.id;
		beanFromUi.fruits = new ArrayList<>( List.of( "Banana", "Cherry", "Peach" ) );
		beanFromUi.pojos.remove( 0 );
		beanFromUi.pojos.get( 0 ).id = beanFromDb.pojos.get( 1 ).id;
		beanFromUi.pojos.get( 0 ).title = "second changed";
		String json = Jetson.encode( beanFromUi );

		//--- when
		PojoWithList decodedBean = (PojoWithList) Jetson.mergeCollection().decodeIntoObject( json, beanFromDb );

		//--- then
		assertEquals( 5, decodedBean.fruits.size() );
		assertEquals( "Apple", decodedBean.fruits.get( 0 ) );
		assertEquals( "Kiwi", decodedBean.fruits.get( 1 ) );
		assertEquals( "Banana", decodedBean.fruits.get( 2 ) );
		assertEquals( "Cherry", decodedBean.fruits.get( 3 ) );
		assertEquals( "Peach", decodedBean.fruits.get( 4 ) );
		assertEquals( 2, decodedBean.pojos.size() );
		assertEquals( "first", decodedBean.pojos.get( 0 ).title );
		assertEquals( "second changed", decodedBean.pojos.get( 1 ).title );
		assertEquals( now, decodedBean.whenInserted, "Must be kept from DB entity" );
	}

	/**
	 * Should use annotation @Json(mergeCollection = true)
	 */
	@Test
	@Disabled
	void testDecodeJsonIntoPojoWithMergeAnnotation() throws ParseException, IllegalAccessException {
		//--- given
		OffsetDateTime now = OffsetDateTime.now();
		PojoWithMergedCollections beanFromDb = new PojoWithMergedCollections();
		beanFromDb.whenInserted = now;
		PojoWithMergedCollections beanFromUi = new PojoWithMergedCollections();
		beanFromUi.fruits = new ArrayList<>( List.of( "Banana", "Cherry", "Peach" ) );
		beanFromUi.pojos.remove( 0 );
		String json = Jetson.encode( beanFromUi );

		//--- when
		PojoWithMergedCollections decodedBean = (PojoWithMergedCollections) Jetson.decodeIntoObject( json, beanFromDb );

		//--- then
		assertEquals( 5, decodedBean.fruits.size() );
		assertEquals( "Banana", decodedBean.fruits.get( 0 ) );
		assertEquals( "Cherry", decodedBean.fruits.get( 1 ) );
		assertEquals( "Peach", decodedBean.fruits.get( 2 ) );
		assertEquals( 1, decodedBean.pojos.size() );
		assertEquals( "second", decodedBean.pojos.get( 0 ).title );
		assertEquals( now, decodedBean.whenInserted, "Must be kept from DB entity" );
	}

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

	/**
	 *
	 */
	private static class SimplePojo {
		@Json
		public UUID id = UUID.randomUUID();

		private String title = null;

		SimplePojo() {
		}

		SimplePojo( String title ) {
			this.title = title;
		}

		@Json
		public String getTitle() {
			return title;
		}

		public void setTitle( String title ) {
			this.title = title;
		}

		@Override
		public boolean equals( Object other ) {
			return this==other || (other!=null)
					&& (getClass()==other.getClass())
					&& (id.equals( ((SimplePojo) other).id ));
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public String toString() {
			return String.format( "%s:%s %s", SimplePojo.class.getSimpleName(), id, title );
		}
	}

	/**
	 *
	 */
	private static class PojoWithList {
		@Json
		public UUID id = UUID.randomUUID();

		@Json
		public List<String> fruits = new ArrayList<>( List.of( "Apple", "Kiwi" ) );

		@Json
		public List<SimplePojo> pojos = new ArrayList<>( List.of(
				new SimplePojo( "first" ),
				new SimplePojo( "second" ) ) );

		//--- value must be kept across encoding and decoding
		public OffsetDateTime whenInserted;
	}

	/**
	 *
	 */
	private static class PojoWithMergedCollections {
		@Json
		public UUID id = UUID.randomUUID();

		@Json(mergeCollection = true)
		public List<String> fruits = new ArrayList<>( List.of( "Apple", "Kiwi" ) );

		@Json(mergeCollection = true)
		public List<SimplePojo> pojos = new ArrayList<>( List.of(
				new SimplePojo( "first" ),
				new SimplePojo( "second" ) ) );

		//--- value must be kept across encoding and decoding
		public OffsetDateTime whenInserted;
	}
}