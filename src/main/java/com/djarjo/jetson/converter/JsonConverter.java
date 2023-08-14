package com.djarjo.jetson.converter;

/**
 * Each JSON converter must implement this interface.
 * <p>
 * Any class implementing this interface (like an enum) will be encoded and
 * decoded with this methods.
 * </p>
 *
 *
 * @author Hajo Lemcke
 * @since 2021-07-16
 * @param <S>
 *            Type of value to be de-/encoded
 */
public interface JsonConverter<S> {

	/**
	 * Encodes given attribute of type {@code S} to a Json encoded string.
	 *
	 * @param attribute
	 *            of type {@code S}
	 * @return Json encoded attribute
	 */
	String encodeToJson( S attribute );

	/**
	 * Decodes given Json string to a value of type {@code S}.
	 *
	 * @param jsonValue
	 *            Json encoded value
	 * @return value of type {@code S}
	 */
	S decodeFromJson( String jsonValue );
}