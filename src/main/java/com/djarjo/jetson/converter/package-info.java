/**
 * (C)opyright 2023 by djarjo GmbH
 * <p>
 * Provided under Apache License 2.0
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
 *
 * @since 2021-07-21 removed LocaleConverter4json. Now directly integrated into
 *        the encoder and decoder. Also included: {@code Currency}
 */
package com.djarjo.jetson.converter;