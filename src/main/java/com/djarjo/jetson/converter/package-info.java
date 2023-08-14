/**
 * (C)opyright 2021 by djarjo GmbH
 */

/**
 * Contains converters for <b>Jetson</b>.
 * 
 * <p>
 * Usage:
 * 
 * <pre>
 * &#64;Json(converter=xyz.class)
 * int someMethod() {...}
 * </pre>
 * </p>
 * 
 * @since 2021-07-21 removed LocaleConverter4json. Now directly integrated into
 *        the encoder and decoder. Also included {@code Currency}
 */
package com.djarjo.jetson.converter;