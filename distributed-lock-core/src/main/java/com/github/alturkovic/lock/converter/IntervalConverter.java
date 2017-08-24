/*
 * Copyright (c)  2017 Alen Turković <alturkovic@gmail.com>
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.github.alturkovic.lock.converter;

import com.github.alturkovic.lock.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class IntervalConverter {

    private final ConfigurableListableBeanFactory beanFactory;

    @Autowired
    public IntervalConverter(final ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public long toMillis(final Interval interval) {
        final String value = beanFactory.resolveEmbeddedValue(interval.value());
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Cannot convert interval " + interval + " to milliseconds");
        }
        return interval.unit().toMillis(Long.valueOf(value));
    }
}
