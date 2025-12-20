package com.djarjo.common;

import com.djarjo.jetson.JsonEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeanHelperTest {

	@Test
	@DisplayName("Distinguish all basic types from collections or POJOs")
	void distinguishBasicValues() {
		//--- given
		Map<Object, Boolean> map = new HashMap<>();
		map.put( false, true );
		map.put( true, true );
		map.put( new BigDecimal( 1_000_000_000 ), true );
		map.put( new BigDecimal( "1234.6789" ), true );
		map.put( Currency.getInstance( "EUR" ), true );
		map.put( (byte) 72, true );
		map.put( new JsonEncoder(), false );
		map.put( new ArrayList<Long>(), false );

		//--- when / then
		for ( Map.Entry<Object, Boolean> entry : map.entrySet() ) {
			assertEquals( entry.getValue(), BeanHelper.isBasicType( entry.getKey() ),
					"key=" + entry.getKey() + ", type=" + entry.getKey().getClass()
							.getSimpleName() );
		}
	}

	@Test
	@DisplayName("Ensure that only arrays and lists return true")
	void isArrayOrList() {
		//--- given
		List<Object> inputs = List.of( new MainBean(), new ArrayList<NestedBean>(),
				new MainBean[2] );
		List<Boolean> expected = List.of( false, true, true );
	}

	@Test
	void setValueIntoBeanDirectly() {
		//--- given
		MainBean mainBean = new MainBean();
		String newCode = "another";

		//--- when
		boolean wasSet = BeanHelper.setValue( mainBean, "code", newCode );

		//--- then
		assertTrue( wasSet );
		assertEquals( newCode, mainBean.code );
	}

	@Test
	void setValueIntoNestedBean() {
		//--- given
		MainBean mainBean = new MainBean();
		mainBean.nestedBean = new NestedBean();
		String newKey = "another key";
		assertEquals( NestedBean.keyValue, mainBean.nestedBean.key );

		//--- when
		boolean wasSet = BeanHelper.setValue( mainBean, "nestedBean/key", newKey );

		//--- then
		assertTrue( wasSet );
		assertEquals( newKey, mainBean.nestedBean.key );
	}

	@Test
	void setValueIntoNewListOfNestedBean() {
		//--- given
		MainBean mainBean = new MainBean();
		mainBean.nestedBean = new NestedBean();
		String newKey = "another key";
		assertEquals( NestedBean.keyValue, mainBean.nestedBean.key );

		//--- when
		boolean wasSet = BeanHelper.setValue( mainBean, "nestedBeanListNull/key", newKey );

		//--- then
		assertTrue( wasSet );
		assertEquals( newKey, mainBean.nestedBeanListNull.get( 0 ).key );
	}

	@Test
	void setValueIntoEmptyListOfNestedBean() {
		//--- given
		MainBean mainBean = new MainBean();
		mainBean.nestedBean = new NestedBean();
		String newKey = "another key";
		assertEquals( NestedBean.keyValue, mainBean.nestedBean.key );

		//--- when
		boolean wasSet = BeanHelper.setValue( mainBean, "nestedBeanListEmpty/key", newKey );

		//--- then
		assertTrue( wasSet );
		assertEquals( newKey, mainBean.nestedBeanListEmpty.get( 0 ).key );
	}

	@Test
	void setValueUsingSpecialSetter() {
		//--- given
		MainBean mainBean = new MainBean();
		String newCode = "newCode";

		//--- when
		boolean wasSet = BeanHelper.setValue( mainBean, "codeWithPrefix", newCode );

		//--- then
		assertTrue( wasSet );
		assertEquals( "prefix-newCode", mainBean.code );
	}

	@Test
	void setStringsInList() {
		//--- given
		MainBean mainBean = new MainBean();
		String str1 = "First";
		String str2 = "Second";
		String str3 = "Third";

		//--- when / then #1
		boolean wasSet = BeanHelper.setValue( mainBean, "names", str1 );
		assertTrue( wasSet );
		assertEquals( str1, mainBean.names.get( 0 ) );

		//--- when / then #1
		wasSet = BeanHelper.setValue( mainBean, "names", str2 );
		assertTrue( wasSet );
		assertEquals( str2, mainBean.names.get( 0 ) );

		//--- when / then #1
		wasSet = BeanHelper.setValue( mainBean, "names[+]", str3 );
		assertTrue( wasSet );
		assertEquals( 2, mainBean.names.size() );
		assertEquals( str2, mainBean.names.get( 0 ) );
		assertEquals( str3, mainBean.names.get( 1 ) );
	}


	@Test
	@DisplayName("Invoke method given by its full name to append string")
	void callAnyMethodGivenByItsFullName() {

		//--- given
		MainBean mainBean = new MainBean();
		String value = "Meteorite";

		//--- when
		boolean wasSet = BeanHelper.setValue( mainBean, "addName", value );

		//--- then
		assertTrue( wasSet );
		assertEquals( value, mainBean.names.get( 0 ) );
	}

	/**
	 *
	 */
	private static class MainBean {
		private final String prefix = "prefix-";
		private final List<NestedBean> nestedBeanListEmpty = new ArrayList<>();
		private final List<NestedBean> nestedBeanListNull = null;
		private String code = "mainCode";
		private List<String> names;
		private NestedBean nestedBean = new NestedBean();

		public void addName( String name ) {
			if ( names == null ) {
				names = new ArrayList<>();
			}
			names.add( name );
		}

		public List<String> getNames() {
			return names;
		}

		public void setNames( List<String> list ) {
			names = list;
		}

		public void setCodeWithPrefix( String value ) {
			code = prefix + value;
		}
	}

	public static class NestedBean {
		public static String keyValue = "nestedKey";
		private final String key = keyValue;
	}
}
