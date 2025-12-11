package com.djarjo.jetson;

import com.djarjo.jetson.converter.JsonConverter;

import java.lang.annotation.*;

/**
 * Annotate a property or a getter method will encode its value into a Json
 * string or sets the value from decoding a Json string.
 * <p>
 * A Json annotation provides the following optional parameters:
 * <ul>
 * <li>{@link #converter()} - JsonConverter defaults to no converter</li>
 * <li>{@link #decodable()} - boolean defaults to true</li>
 * <li>{@link #encodable()} - boolean defaults to true</li>
 * <li>{@link #key()} - String defaults to property name</li>
 * </ul>
 * Setting both {@link #decodable()} and {@link #encodable()} to false is
 * exactly the same as not annotating the field at all.
 *
 * @author Hajo Lemcke
 * @version 2014-12-06 Initial version
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface Json {

	/**
	 * Just tells the annotation parser that this annotation is default
	 */
	String defaultName = "##default";

	/**
	 * Access type if a class is annotated.
	 *
	 * @return type of access: field or property. Defaults to property
	 */
	JsonAccessType accessType() default JsonAccessType.PROPERTY;

	/**
	 * Converter encodes a given value into a Json string and decodes a given
	 * string into a Java object. Defaults to no converter.
	 *
	 * @return converter class
	 */
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
	 * Controls encoding and decoding of enumeration values.
	 * <p>
	 * This is a class level annotation to be used on enumerations.
	 * </p>
	 * <p>
	 * Example:<br />
	 *
	 * <pre>
	 * &#64;Json( enumAccessor = "getCode" )
	 * enum SomeEnum {
	 *   VAL1 ( 17 ),
	 *   VAL2 ( 31 );
	 *   private final int code;
	 *   SomeEnum( int code ) { this.code = code; }
	 *   public int getCode() { return code; }
	 * </pre>
	 * <p>
	 * will encode {@code SomeEnum.VAL1} to {@code 17} and
	 * decode any one of {@code 17} or {@code VAL1} or {@code SomeEnum.VAL1}
	 * to SomeEnum.VAL1;
	 * <p>
	 * DO NOT PUT slash P here! produces a false javadoc error :-(
	 *
	 * @return name of getter method to obtain the enumeration value for
	 * encoding
	 */
	String enumAccessor() default defaultName;

	/**
	 * Controls behaviour when decoding a JSON string into a collection property of an existing object.
	 * <p>
	 * Default {@code false} will completely replace the collection of the object by a new one from JSON string.
	 * {@code true} will keep existing entries in the collection and merge values from JSON string.
	 * </p>
	 *
	 * @return {@code true} if collection items will be kept
	 */
	boolean mergeCollection() default false;

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
	 * <p>
	 * will write {@code "PassWord":"badPw"} into the Json string during
	 * encoding instead of {@code "userPw":"badPw"}. Decoding also expects the
	 * key "PassWord".
	 *
	 * @return key for Json value
	 */
	String key() default defaultName;
}