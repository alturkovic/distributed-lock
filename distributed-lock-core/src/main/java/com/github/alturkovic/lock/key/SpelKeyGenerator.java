/*
 * MIT License
 *
 * Copyright (c) 2018 Alen Turkovic
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
  private final ConversionService conversionService;
  private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>();
  private final Map<AnnotatedElementKey, Method> targetMethodCache = new ConcurrentHashMap<>();

  @Override
  public List<String> resolveKeys(final String lockKeyPrefix, final String expression, final Object object, final Method method, final Object[] args) {
    final EvaluationContext context = new MethodBasedEvaluationContext(object, method, args, super.getParameterNameDiscoverer());
    context.setVariable("executionPath", object.getClass().getCanonicalName() + "." + method.getName());

    final List<String> keys = convertResultToList(getExpression(this.conditionCache, new AnnotatedElementKey(method, object.getClass()), expression).getValue(context));

    if (keys.stream().anyMatch(Objects::isNull)) {
      throw new EvaluationConvertException("null keys are not supported: " + keys);
    }

    if (StringUtils.isEmpty(lockKeyPrefix)) {
      return keys;
    }

    return keys.stream().map(key -> lockKeyPrefix + key).collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  protected List<String> convertResultToList(final Object expressionValue) {
    final List<String> list;

    if (expressionValue == null) {
      throw new EvaluationConvertException("Expression evaluated in a null");
    }

    if (expressionValue instanceof Iterable) {
      final TypeDescriptor genericCollection = TypeDescriptor.collection(Collection.class, TypeDescriptor.valueOf(Object.class));
      final TypeDescriptor stringList = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class));

      list = (List<String>) conversionService.convert(expressionValue, genericCollection, stringList);
    } else if (expressionValue.getClass().isArray()) {
      final TypeDescriptor genericArray = TypeDescriptor.array(TypeDescriptor.valueOf(Object.class));
      final TypeDescriptor stringList = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class));

      list = (List<String>) conversionService.convert(expressionValue, genericArray, stringList);
    } else {
      list = Collections.singletonList(expressionValue.toString());
    }

    if (CollectionUtils.isEmpty(list)) {
      throw new EvaluationConvertException("Expression evaluated in an empty list");
    }

    return list;
  }
}
