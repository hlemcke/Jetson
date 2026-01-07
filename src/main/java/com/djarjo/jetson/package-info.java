/**
 * (C)opyright 2023 by djarjo.com
 */

/**
 * The <b>Jetson</b> package implements a subset of {@code javax.json} interfaces which is
 * fully sufficient to work with JSON. It also provides annotation {@code @Json} and
 * methods to encode and decode JSON values.
 * <p>
 * But most of all it can decode a JSON text into an instantiated object! This allows
 * setting a subset of attributes in that object without overwriting others.
 *
 * <h2>Usage</h2>
 * <p>
 * Annotate getter method like:
 *
 * <pre>
 * &#64;Json( converter = ReturnTypeConverter4son.class )
 * ReturnType getter() {...}
 * </pre>
 * <p>
 * Supplying a converter is optional.
 *
 * @author hlemcke
 * @since 2018-07
 */
package com.djarjo.jetson;