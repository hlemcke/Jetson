package com.djarjo.jetson;

import com.djarjo.jetson.converter.JsonConverter;

import java.lang.annotation.*;

/**
 * Annotate a property or a getter method will encode its value into a JSON string or sets
 * the value from decoding a JSON string.
 * <p>
 * A JSON annotation provides the following optional parameters:
 * <ul>
 * <li>{@link #converter()} - JsonConverter defaults to no converter</li>
 * <li>{@link #decode()} - specifies when to encode</li>
 * <li>{@link #encode()} - specifies when to decode</li>
 * <li>{@link #name()} - String defaults to property name</li>
 * </ul>
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
	 * Converter encodes a given value into a JSON string and decodes a given string into a
	 * Java object. Defaults to no converter.
	 *
	 * @return converter class
	 */
	Class<? extends JsonConverter> converter() default JsonConverter.class;

	/**
	 * Controls decoding a JSON string. Defaults to {@code DecodeMode.ALWAYS}
	 *
	 * @return mode
	 */
	DecodeMode decode() default DecodeMode.ALWAYS;

	/**
	 * Controls encoding a JSON string. Defaults to {@code EncodeMode.ONLY_IF_NOT_EMPTY}
	 *
	 * @return mode
	 */
	EncodeMode encode() default EncodeMode.ONLY_IF_NOT_EMPTY;

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
	 * will encode {@code SomeEnum.VAL1} to {@code 17} and decode any one of {@code 17 } or
	 * {@code VAL1} or {@code SomeEnum.VAL1} to SomeEnum.VAL1;
	 *
	 * @return name of getter method to obtain the enumeration value for encoding
	 */
	String enumAccessor() default defaultName;

	/**
	 * Controls behaviour when decoding a JSON string into a collection property of an
	 * existing object.
	 * <p>
	 * Default {@code false} will completely replace the collection of the object by a new
	 * one from JSON string. {@code true} will keep existing entries in the collection and
	 * merge values from JSON string.
	 * </p>
	 *
	 * @return {@code true} if collection items will be kept
	 */
	boolean mergeCollection() default false;

	/**
	 * The key used for the value in the JSON string. Defaults to the name of the field or
	 * the name of the getter method without "get".
	 *
	 * <p>
	 * Example for an annotated getter:
	 *
	 * <pre>
	 * &#64;Json( name="PassWord" )
	 * public String getUserPw() { return "badPw"; }
	 * </pre>
	 * <p>
	 * will write {@code "PassWord":"badPw"} into the JSON string during encoding
	 * instead of
	 * {@code "userPw":"badPw"}. Decoding also expects the name "PassWord".
	 *
	 * @return name for JSON value
	 */
	String name() default defaultName;

	enum DecodeMode {
		ALWAYS,
		NEVER,
		ONLY_IF_EMPTY
	}

	enum EncodeMode {
		ALWAYS,
		NEVER,
		ONLY_IF_NOT_EMPTY
	}
}