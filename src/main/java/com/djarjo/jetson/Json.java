package com.djarjo.jetson;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.djarjo.jetson.converter.JsonConverter;

/**
 * Annotate a property or a getter method will encode its value into a Json
 * string or sets the value from decoding a Json string.
 * <p>
 * A Json annotation provides the following optional parameters:
 * <ul>
 * <li>{@link converter()} - JsonConverter defaults to no converter</li>
 * <li>{@link decodable()} - boolean defaults to true</li>
 * <li>{@link encodable()} - boolean defaults to true</li>
 * <li>{@link key()} - String defaults to property name</li>
 * </ul>
 * Setting both {@link decodable()} and {@link encodable()} to false is exactly
 * the same as not annotating the field at all.
 * </p>
 *
 * @author Hajo Lemcke
 * @version 2023-08-11 added encodable and decodable
 * @version 2020-04-29 Added defaultName
 * @version 2014-12-06 Initial version
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface Json {

	static String defaultName = "##default";

	/**
	 * Converter encodes a given value into a Json string and decodes a given
	 * string into a Java object. Defaults to no converter.
	 *
	 * @return converter class
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends JsonConverter> converter() default JsonConverter.class;

	/**
	 * Controls decoding of a Json string. {@code false} will not write the
	 * decoded value into the field. Default is {@code true}.
	 *
	 * @return {@code false} if the field must not be set during decoding
	 */
	boolean decodable() default true;

	/**
	 * Controls encoding to a Json string. {@code false} will not write the
	 * encoded value. Default is {@code true}.
	 *
	 * @return {@code false} if the field will not be encoded
	 */
	boolean encodable() default true;

	/**
	 * The key used for the value in the Json string. Defaults to the name of
	 * the field or the name of the getter method without "get".
	 *
	 * <p>
	 * Example for an annotated getter:
	 *
	 * <pre>
	 * &#64;Json( key="PassWord" )
	 * public String getUserPw() { return "badPw; }
	 * </pre>
	 *
	 * will write {@code "PassWord":"badPw"} into the Json string during
	 * encoding instead of {@code "UserPw":"badPw"}. Decoding also expects the
	 * key "PassWord".
	 * </p>
	 *
	 * @return key for Json value
	 */
	String key() default defaultName;
}