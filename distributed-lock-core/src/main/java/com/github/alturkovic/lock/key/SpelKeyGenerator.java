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

package com.github.alturkovic.lock.key;

import com.github.alturkovic.lock.exception.EvaluationConvertException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Data
@EqualsAndHashCode(callSuper = false)
public class SpelKeyGenerator extends CachedExpressionEvaluator implements KeyGenerator {
  private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>();
  private final ConversionService conversionService;

  @Override
  public List<String> resolveKeys(final String lockKeyPrefix, final String expression, final Object object, final Method method, final Object[] args) {
    final Object expressionValue = evaluateExpression(expression, object, method, args);
    final List<String> keys = convertResultToList(expressionValue);

    if (keys.stream().anyMatch(Objects::isNull)) {
      throw new EvaluationConvertException("null keys are not supported: " + keys);
    }

    if (!StringUtils.hasText(lockKeyPrefix)) {
      return keys;
    }

    return keys.stream().map(key -> lockKeyPrefix + key).collect(Collectors.toList());
  }

  protected List<String> convertResultToList(final Object expressionValue) {
    final List<String> list;
    if (expressionValue instanceof Iterable) {
      list = iterableToList(expressionValue);
    } else if (expressionValue.getClass().isArray()) {
      list = arrayToList(expressionValue);
    } else {
      list = Collections.singletonList(expressionValue.toString());
    }

    if (CollectionUtils.isEmpty(list)) {
      throw new EvaluationConvertException("Expression evaluated in an empty list");
    }

    return list;
  }

  private Object evaluateExpression(final String expression, final Object object, final Method method, final Object[] args) {
    final EvaluationContext context = new MethodBasedEvaluationContext(object, method, args, super.getParameterNameDiscoverer());
    context.setVariable("executionPath", object.getClass().getCanonicalName() + "." + method.getName());

    final Expression evaluatedExpression = getExpression(this.conditionCache, new AnnotatedElementKey(method, object.getClass()), expression);
    final Object expressionValue = evaluatedExpression.getValue(context);
    if (expressionValue == null) {
      throw new EvaluationConvertException("Expression evaluated in a null");
    }

    return expressionValue;
  }

  private List<String> iterableToList(final Object expressionValue) {
    final TypeDescriptor genericCollection = TypeDescriptor.collection(Collection.class, TypeDescriptor.valueOf(Object.class));
    return toList(expressionValue, genericCollection);
  }

  private List<String> arrayToList(final Object expressionValue) {
    final TypeDescriptor genericArray = TypeDescriptor.array(TypeDescriptor.valueOf(Object.class));
    return toList(expressionValue, genericArray);
  }

  private List<String> toList(final Object expressionValue, final TypeDescriptor from) {
    final TypeDescriptor listTypeDescriptor = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class));
    //noinspection unchecked
    return (List<String>) conversionService.convert(expressionValue, from, listTypeDescriptor);
  }
}
