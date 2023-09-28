package com.djarjo.codec;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.junit.Test;

import com.djarjo.common.Base64;
import com.djarjo.jetson.Jetson;

/**
 * <pre>
 * char ascii  utf-8
 *  Ä   c4     c3 84
 * </pre>
 *
 * @author Hajo Lemcke
 */
public class Base64Test {
	private boolean _consoleOutput = false;
	String[] input = { "Ä", "This is plain ASCII", "AZöby7ß", "abcö",
			"Polyfon zwitschernd aßen Mäxchens Vögel Rüben, Joghurt und Quark" };
	String[] expected = { "w4Q=", "VGhpcyBpcyBwbGFpbiBBU0NJSQ==",
			"QVrDtmJ5N8Of", "YWJjw7Y=",
			"UG9seWZvbiB6d2l0c2NoZXJuZCBhw59lbiBNw6R4Y2hlbnMgVsO2Z2VsIFLDvGJlbiwgSm9naHVy\ndCB1bmQgUXVhcms=" };
	String jwt =
			"eyJhbGciOiJSUzI1NiIsImtpZCI6ImRqYXJqb1VzZXIiLCJ0eXAiOiJKV1QifQ."
					+ "eyJhdWQiOiJkamFyam8gdXNlciIsImV4cCI6MTU5NzA5NTc1NywianRpIjoiZGRmYm"
					+ "IxMGEtZGI3Ny00ZjczLWIxNDgtZTdlNmVkY2U2Njc2IiwiaWF0IjoxNTk0NTAzNzU3"
					+ "LCJpc3MiOiJ3d3cuZGphcmpvLmNvbSIsInN1YiI6ImphYmJlckBkamFyam8uY29tIi"
					+ "widXBuIjoiOGJmMGZjMzgtMGM1OC00YjA4LWExN2MtY2M3NjhhMGFkZmUxIiwiZGV2"
					+ "IjoiZjk1MTBhNGItODU5OC00MjljLWI0NjAtZGYzNmQ0ZGZjZjg3IiwiZ3JvdXBzIj"
					+ "pbIlVTRVIiXX0.V4qf8dcSFlyMIptd7C2XEBM1hUyJPUj4cero78JLMpNtQEd_nLqW"
					+ "IKib_iKf5EmJRAjiOzqqjUPGFdMsUVQPwtYWCXgtP3SONrMpVq0FzvJtHizRF0QPr3"
					+ "NjxYsh5sPy_oeEKJKuO817GIfJDh-BNOpnNXe8AJRC_VlSyF-9eJGGegzXxSPCcDeb"
					+ "sv59opEa0ipI9vWAQEa6Zl0tpobmsTTta9vKAEFko2_7IkkaUikhu0RsMd_Gd-CxU-"
					+ "d6Mfr2L2myIbkxgeqRIhzivmPy63D1-iF8D8WvZSwE5QWoqpHU8XSzJTe2Y-xL451K"
					+ "v2ko-jDG1PWhuL8jxUG5e-RcDQ";

	public static void main( String[] args ) {
		Base64Test test = new Base64Test();
		test.checkJwt();
	}

	private void checkJwt() {
		String[] parts = jwt.split( "\\." );
		System.out.println( parts[0] + "\n" + parts[1] + "\n" + parts[2] );
		String header = new String( Base64.decoder().decode( parts[0] ) );
		String payload = new String( Base64.decoder().decode( parts[1] ) );
		String signature = new String( Base64.decoder().decode( parts[2] ) );
		try {
			System.out.println( header );
			System.out.println( Jetson.decode( header ) );
			System.out.println( payload );
			System.out.println( Jetson.decode( payload ) );
			System.out.println( "Signature length = " + signature.length() );
		} catch (IllegalAccessException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testShifting() {
		int i = 64;
		int j = i >> 1;
		assertEquals( 32, j );
		i = -64;
		j = i >> 1;
		assertEquals( -32, j );
		j = i >>> 1;
		assertEquals( 0x7FFFFFE0, j );
	}

	@Test
	public void testDecoding() {
		_consoleOutput = true;
		if ( _consoleOutput )
			System.out.println( "--- Decoding ---" );
		String result = null;
		for ( int i = 0; i < expected.length; i++ ) {
			try {
				result = new String( Base64.decoder().decode( expected[i] ),
						"UTF-8" );
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			if ( _consoleOutput ) {
				System.out.printf( "%d : '%s' from %s\n    '%s'", i, result,
						expected[i], input[i] );
			}
			assertEquals( input[i], result );
		}
	}

	@Test
	public void testEncoding() {
		// _consoleOutput = true;
		if ( _consoleOutput )
			System.out.println( "--- Encoding ---" );
		String result = null;
		for ( int i = 0; i < input.length; i++ ) {
			result = Base64.encoder().withStandard().withLineBreaks()
					.encode( input[i] );
			if ( _consoleOutput )
				System.out.println( expected[i] + "\n" + result );
			assertEquals( expected[i], result );
		}
	}

	@Test
	public void testEncodeByteArray() {
		byte[] _bytes = new byte[80];
		for ( int i = 0; i < _bytes.length; i++ ) {
			_bytes[i] = (byte) i;
		}
		String result = Base64.encoder().encode( _bytes );
		String expected =
				"AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0-P0BBQkNERUZHSElKS0xNTk8=";
		int calculated = ((_bytes.length + 2) / 3) * 4;
		int val = result.charAt( result.length() - 1 );
		if ( _consoleOutput ) {
			System.out.format( "%4d - '%s'\n%4d - '%s'\n%4d // %x",
					expected.length(), expected, result.length(), result,
					calculated, val );
		}
		assertEquals( expected, result );
	}

	@Test
	public void testLong() {

	}
}