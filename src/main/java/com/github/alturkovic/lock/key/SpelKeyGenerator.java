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

package com.github.alturkovic.lock.key;

import com.github.alturkovic.lock.exception.EvaluationConvertException;
import lombok.Data;
import org.aspectj.lang.JoinPoint;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
public class SpelKeyGenerator implements KeyGenerator {

    @Override
    public List<String> resolveKeys(final String lockKeyPrefix, final String expression, final String contextVariableName, final JoinPoint joinPoint) {
        final List<String> keys = evaluateExpression(expression, contextVariableName, joinPoint);

        if (StringUtils.isEmpty(lockKeyPrefix)) {
            return keys;
        }

        return keys.stream().map(key -> formatKey(lockKeyPrefix, key)).collect(Collectors.toList());
    }

    private List<String> evaluateExpression(final String expression, final String contextVariableName, final JoinPoint joinPoint) {
        final StandardEvaluationContext context = new StandardEvaluationContext(joinPoint.getTarget());

        final Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            IntStream.range(0, args.length).forEach(idx -> context.setVariable(contextVariableName + idx, args[idx]));
        }

        final SpelExpressionParser parser = new SpelExpressionParser();
        final Expression sExpression = parser.parseExpression(expression);

        final Object expressionValue = sExpression.getValue(context);
        return convertResultToList(expressionValue);
    }

    private String formatKey(final String lockKeyPrefix, final String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }

        if (StringUtils.isEmpty(lockKeyPrefix)) {
            return key;
        }

        return lockKeyPrefix + key;
    }

    @SuppressWarnings("unchecked")
    private List<String> convertResultToList(final Object expressionValue) {
        final List<String> list;

        if (expressionValue == null) {
            list = null;
        } else if (expressionValue instanceof String) {
            list = Collections.singletonList((String) expressionValue);
        } else if (expressionValue instanceof List) {
            list = (List<String>) expressionValue;
        } else if (expressionValue instanceof Set) {
            list = new ArrayList<String>((Set) expressionValue);
        } else {
            throw new EvaluationConvertException(String.format("%s is not configured to convert '%s' to list", this.getClass().getName(), expressionValue));
        }

        if (CollectionUtils.isEmpty(list)) {
            throw new EvaluationConvertException("Expression evaluated in an empty or null list");
        }

        return list;
    }
}
