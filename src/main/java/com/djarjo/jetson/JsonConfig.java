package com.djarjo.jetson;

import com.djarjo.jetson.converter.JsonConverter;

record JsonConfig(Json.DecodeMode decode,
									Json.EncodeMode encode,
									Class<? extends JsonConverter> converter,
									String name,
									String enumAccessor,
									boolean mergeCollection) {

	public static final JsonConfig DEFAULT = new JsonConfig(
			Json.DecodeMode.ALWAYS, Json.EncodeMode.ONLY_IF_NOT_EMPTY, JsonConverter.class,
			Json.defaultName, Json.defaultName, false );

	public static JsonConfig fromAnnotation( Json jsonAnno ) {
		return new JsonConfig( jsonAnno.decode(), jsonAnno.encode(), jsonAnno.converter(),
				jsonAnno.name(), jsonAnno.enumAccessor(), jsonAnno.mergeCollection() );
	}
}
