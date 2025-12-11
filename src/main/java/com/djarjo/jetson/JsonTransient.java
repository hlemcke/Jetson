package com.djarjo.jetson;

import java.lang.annotation.*;

/**
 * Annotate a property or a getter method will skip both encoding and decoding its value
 * <p>
 * This annotation is only analyzed if the class itself is annotated with {@literal @Json}
 * and:
 *   <ul><li>that class annotation has no converter specified</li>
 *   <li>the class has no methods named {@code toJson()} and {@code fromJson}</li></ul>
 *   This annotation behaves exactly like {@literal @Json(decodable = false, encodable = false)}
 * </p>
 *
 * @author Hajo Lemcke
 * @version 2025-12-09 Initial version
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface JsonTransient {
}