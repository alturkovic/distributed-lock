/*
 * MIT License
 *
 * Copyright (c) 2020 Alen Turkovic
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.alturkovic.lock.interval;

import com.github.alturkovic.lock.Interval;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.util.StringUtils;

/**
 * {@link IntervalConverter} capable of resolving properties.
 */
@AllArgsConstructor
public class BeanFactoryAwareIntervalConverter implements IntervalConverter {
  private final ConfigurableBeanFactory beanFactory;

  @Override
  public long toMillis(final Interval interval) {
    return convertToMilliseconds(interval, resolveMilliseconds(interval));
  }

  private String resolveMilliseconds(final Interval interval) {
    final var value = beanFactory.resolveEmbeddedValue(interval.value());
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException("Cannot convert interval " + interval + " to milliseconds");
    }
    return value;
  }

  private long convertToMilliseconds(final Interval interval, final String value) {
    try {
      return interval.unit().toMillis(Long.parseLong(value));
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException("Cannot convert interval " + interval + " to milliseconds", e);
    }
  }
}
