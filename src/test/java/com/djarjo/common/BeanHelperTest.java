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

	/**
	 *
	 */
	private static class MainBean {
		private String code = "mainCode";
		private NestedBean nestedBean = new NestedBean();
		private List<NestedBean> nestedBeanListEmpty = new ArrayList<>();
		private List<NestedBean> nestedBeanListNull = null;
	}

	public static class NestedBean {
		public static String keyValue = "nestedKey";
		private String key = keyValue;
	}
}
