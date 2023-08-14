/**
 * (C)opyright 2021 by djarjo.com
 */

/**
 * The <b>Jetson</b> package is an implementation of the {@code javax.json}
 * interfaces. It also includes the {@code @Json} annotation and the
 * {@code codec} to encode and decode Json values.
 * 
 * <h2>Usage</h2>
 * <p>
 * Annotate getter method like:
 * 
 * <pre>
 * &#64;Json( converter = ReturnTypeConverter4son.class )
 * ReturnType getter() {...}
 * </pre>
 * 
 * Supplying a converter is optional.
 * </p>
 * 
 * @author hlemcke
 * @since 2018-07
 */
package com.djarjo.jetson;