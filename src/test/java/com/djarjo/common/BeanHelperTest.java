package com.djarjo.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeanHelperTest {

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

		//--- when / then #1
		boolean wasSet = BeanHelper.setValue( mainBean, "names", str1 );
		assertTrue( wasSet );
		assertEquals( str1, mainBean.names.get( 0 ) );

		//--- when / then #1
		wasSet = BeanHelper.setValue( mainBean, "names", str2 );
		assertTrue( wasSet );
		assertEquals( str2, mainBean.names.get( 0 ) );

		//--- when / then #1
		wasSet = BeanHelper.setValue( mainBean, "names[+]", str1 );
		assertTrue( wasSet );
		assertEquals( str1, mainBean.names.get( 0 ) );
	}

	/**
	 *
	 */
	private static class MainBean {
		private String code = "mainCode";
		private List<String> names;
		private String prefix = "prefix-";
		private NestedBean nestedBean = new NestedBean();
		private List<NestedBean> nestedBeanListEmpty = new ArrayList<>();
		private List<NestedBean> nestedBeanListNull = null;

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
		private String key = keyValue;
	}
}
