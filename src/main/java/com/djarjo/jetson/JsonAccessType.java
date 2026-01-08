package com.djarjo.jetson;

/**
 * Specifies access to fields or getters (property) if the class itself is annotated with
 * {@literal @Json}.
 * <p>
 * Defaults to {#PROPERTY} if not specified
 * </p>
 */
public enum JsonAccessType {
	/** Uses only fields for encoding to JSON */
	FIELD,

	/** Uses only properties (getters) for encoding to JSON (default) */
	PROPERTY
}
