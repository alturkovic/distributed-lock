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
import org.aspectj.lang.JoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.stream.IntStream;

@Component
public class IntervalConverter {

    private final Environment env;

    @Autowired
    public IntervalConverter(final Environment env) {
        this.env = env;
    }

    public long toMillis(final Interval interval, final String contextVariableName, final JoinPoint joinPoint) {
        long amount = interval.value();

        final String property = interval.property();
        final String expression = interval.expression();

        if (!StringUtils.isEmpty(property)) {
            amount = env.getProperty(property, Long.class);
        } else if (!StringUtils.isEmpty(expression)) {
            final StandardEvaluationContext context = new StandardEvaluationContext(joinPoint.getTarget());

            final Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                IntStream.range(0, args.length).forEach(idx -> context.setVariable(contextVariableName + idx, args[idx]));
            }

            amount = new SpelExpressionParser().parseRaw(expression).getValue(context, Long.class);
        }

        return interval.unit().toMillis(amount);
    }
}
