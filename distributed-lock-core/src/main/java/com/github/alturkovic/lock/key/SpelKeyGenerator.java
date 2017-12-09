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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Data;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Data
public class SpelKeyGenerator implements KeyGenerator {

  private final Map<Class<?>, Function<Object, String>> converterMap;

  public SpelKeyGenerator() {
    converterMap = new HashMap<>();

    final Function<Object, String> toStringFunction = Object::toString;
    converterMap.put(Boolean.class, toStringFunction);
    converterMap.put(Byte.class, toStringFunction);
    converterMap.put(Character.class, toStringFunction);
    converterMap.put(Double.class, toStringFunction);
    converterMap.put(Float.class, toStringFunction);
    converterMap.put(Integer.class, toStringFunction);
    converterMap.put(Long.class, toStringFunction);
    converterMap.put(Short.class, toStringFunction);
    converterMap.put(String.class, toStringFunction);
  }

  @Override
  public List<String> resolveKeys(final String lockKeyPrefix, final String expression, final String contextVariableName, final JoinPoint joinPoint) {
    final List<String> keys = evaluateExpression(expression, contextVariableName, joinPoint);

    if (StringUtils.isEmpty(lockKeyPrefix)) {
      return keys;
    }

    return keys.stream().map(key -> formatKey(lockKeyPrefix, key)).collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  public <T> void registerConverter(final Class<T> clazz, final Function<T, String> converter) {
    converterMap.put(clazz, (Function<Object, String>) converter);
  }

  private List<String> evaluateExpression(final String expression, final String contextVariableName, final JoinPoint joinPoint) {
    final StandardEvaluationContext context = new StandardEvaluationContext(joinPoint.getTarget());

    final Object[] args = joinPoint.getArgs();
    if (args != null && args.length > 0) {
      IntStream.range(0, args.length).forEach(idx -> context.setVariable(contextVariableName + idx, args[idx]));
    }

    final Signature signature = joinPoint.getSignature();
    if (signature instanceof MethodSignature) {
      context.setVariable("executionPath", joinPoint.getTarget().getClass().getCanonicalName() + "." + ((MethodSignature) signature).getMethod().getName());
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
  protected List<String> convertResultToList(final Object expressionValue) {
    final List<String> list;

    if (expressionValue == null) {
      throw new EvaluationConvertException("Expression evaluated in a null list");
    }

    final Function<Object, String> converterFunction = converterMap.get(expressionValue.getClass());

    if (converterFunction != null) {
      list = Collections.singletonList(converterFunction.apply(expressionValue));
    } else if (expressionValue instanceof Collection) {
      list = ((Collection<Object>) expressionValue).stream().map(o -> {
        final Function<Object, String> elementConvertFunction = converterMap.get(o.getClass());
        if (elementConvertFunction == null) {
          throw new EvaluationConvertException(String.format("Expression evaluated in a list, but element %s has no registered converter", o));
        }
        return elementConvertFunction.apply(o);
      }).collect(Collectors.toList());
    } else {
      throw new EvaluationConvertException(String.format("Expression evaluated in %s that has no registered converter", expressionValue));
    }

    if (CollectionUtils.isEmpty(list)) {
      throw new EvaluationConvertException("Expression evaluated in an empty list");
    }

    return list;
  }
}
