package com.djarjo.text;

public enum SomeEnum {
	ENUM_VALUE_1( "code1", 101 ),
	ENUM_VALUE_2( "code2", 102 ),
	ENUM_VALUE_3( "code3", 103 );

	public final String code;
	private final int id;

	SomeEnum( String code, int id ) {
		this.code = code;
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
