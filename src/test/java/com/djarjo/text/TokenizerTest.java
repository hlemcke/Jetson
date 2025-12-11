package com.djarjo.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TokenizerTest {
	@Test
	void testCutOutObjectFromJson() {
		//--- given
		String json = "{\"intVal\":42,\"entity\":{\"boolVal\":true}}";
		Tokenizer tokenizer = new Tokenizer( json );
		tokenizer.nextToken(); // skip first brace

		//--- when

		tokenizer.clipUntilSymbol( Symbol.LEFT_BRACE );
		Token token = tokenizer.clipUntilMatching();

		//--- then
		assertNotNull( token );
		assertEquals( "\"boolVal\":true", token.value );
	}
}
