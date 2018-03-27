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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.aspectj.lang.JoinPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpelKeyGeneratorTest {

  @SuppressWarnings("unused") // used in test expression, false IntelliJ warning
  public static final String CSV_DUMMY = "a,b,c";

  @Getter
  private final String stringDummy = "contextTest";

  @Getter
  private final int integerDummy = 15;

  private final SpelKeyGenerator generator = new SpelKeyGenerator();

  @Mock
  private JoinPoint joinPoint;

  @Test
  public void shouldGenerateSingleKeyFromJoinPointContextAndVariables() {
    when(joinPoint.getArgs()).thenReturn(new Object[]{1});
    when(joinPoint.getTarget()).thenReturn(this);

    final String exp = "getStringDummy() + 'Example' + #p0";
    final List<String> keys = generator.resolveKeys("lock_", exp, "p", joinPoint);
    assertThat(keys).containsExactly("lock_contextTestExample1");
  }

  @Test
  public void shouldGenerateWithDefaultConverter() {
    when(joinPoint.getArgs()).thenReturn(new Object[]{1});
    when(joinPoint.getTarget()).thenReturn(this);

    final String exp = "getIntegerDummy()";
    final List<String> keys = generator.resolveKeys("lock_", exp, "p", joinPoint);
    assertThat(keys).containsExactly("lock_15");
  }

  @Test
  public void shouldGenerateMultipleKeysFromJoinPointContextAndVariables() {
    when(joinPoint.getArgs()).thenReturn(new Object[]{"_after"});
    when(joinPoint.getTarget()).thenReturn(this);

    final String exp = "T(com.github.alturkovic.lock.key.SpelKeyGeneratorTest).generateKeys(CSV_DUMMY, #v0)";
    final List<String> keys = generator.resolveKeys("pre_", exp, "v", joinPoint);
    assertThat(keys).containsExactly("pre_a_after", "pre_b_after", "pre_c_after");
  }

  @Test
  public void shouldGenerateMultipleKeysFromSet() {
    when(joinPoint.getArgs()).thenReturn(new Object[]{});
    when(joinPoint.getTarget()).thenReturn(this);

    final String exp = "T(com.github.alturkovic.lock.key.SpelKeyGeneratorTest).generateKeySet()";
    final List<String> keys = generator.resolveKeys("", exp, "", joinPoint);
    assertThat(keys).containsExactly("a", "b", "c");
  }

  @Test(expected = EvaluationConvertException.class)
  public void shouldFailWithExpressionThatEvaluatesInNull() {
    when(joinPoint.getArgs()).thenReturn(new Object[]{});
    when(joinPoint.getTarget()).thenReturn(this);

    final String exp = "null";
    generator.resolveKeys("", exp, "", joinPoint);
  }

  @Test(expected = EvaluationConvertException.class)
  public void shouldFailWithExpressionThatEvaluatesInEmptyList() {
    when(joinPoint.getArgs()).thenReturn(new Object[]{});
    when(joinPoint.getTarget()).thenReturn(this);

    final String exp = "T(java.util.Collections).emptyList()";
    generator.resolveKeys("", exp, "", joinPoint);
  }

  @Test
  public void shouldConvertUsingRegisteredConverters() {
    final SpelKeyGenerator spelKeyGenerator = new SpelKeyGenerator();
    spelKeyGenerator.registerConverter(Date.class, date -> String.valueOf(date.getTime()));

    assertThat(spelKeyGenerator.convertResultToList(new Date(123))).containsExactly("123");
    assertThat(spelKeyGenerator.convertResultToList(true)).containsExactly("true");
    assertThat(spelKeyGenerator.convertResultToList(Arrays.asList("a", "b", "c"))).containsExactly("a", "b", "c");
    assertThat(spelKeyGenerator.convertResultToList(new HashSet<>(Arrays.asList(1, 2, 3)))).containsExactly("1", "2", "3");
  }

  @Test(expected = EvaluationConvertException.class)
  public void shouldNotConvertUnregisteredClasses() {
    new SpelKeyGenerator().convertResultToList(new HashMap<>());
  }

  // dummy method used in expression to generate a set
  public static Set<String> generateKeySet() {
    return new HashSet<>(Arrays.asList("a", "b", "c"));
  }

  // dummy method used in expression to generate a list from parameters
  public static List<String> generateKeys(final String csv, final String postfix) {
    return Stream.of(csv.split(",")).map(s -> s + postfix).collect(Collectors.toList());
  }
}