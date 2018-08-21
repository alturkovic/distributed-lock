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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SpelKeyGeneratorTest {
  private final KeyGenerator keyGenerator = new SpelKeyGenerator(new DefaultConversionService());
  private final MessageService service = new MessageService();
  private final Method sendMessageMethod;

  public SpelKeyGeneratorTest() throws NoSuchMethodException {
    sendMessageMethod = MessageService.class.getMethod("sendMessage", String.class);
  }

  @Test
  public void shouldGenerateExecutionPath() {
    assertThat(keyGenerator.resolveKeys("lock_", "#executionPath", service, sendMessageMethod, new Object[]{"hello"}))
      .containsExactly("lock_com.github.alturkovic.lock.key.SpelKeyGeneratorTest.MessageService.sendMessage");
  }

  @Test
  public void shouldGenerateSingleKeyFromContextAndVariables() {
    assertThat(keyGenerator.resolveKeys("lock_", "#p0", service, sendMessageMethod, new Object[]{"hello"}))
      .containsExactly("lock_hello");

    assertThat(keyGenerator.resolveKeys("lock_", "#a0", service, sendMessageMethod, new Object[]{"hello"}))
      .containsExactly("lock_hello");

    assertThat(keyGenerator.resolveKeys("lock_", "#message", service, sendMessageMethod, new Object[]{"hello"}))
      .containsExactly("lock_hello");
  }

  @Test
  public void shouldGenerateMultipleKeysFromContextAndVariablesWithList() {
    final String expression = "T(com.github.alturkovic.lock.key.SpelKeyGeneratorTest).generateKeys(#message)";
    assertThat(keyGenerator.resolveKeys("lock_", expression, service, sendMessageMethod, new Object[]{"p_"}))
      .containsExactly("lock_p_first", "lock_p_second");
  }

  @Test
  public void shouldGenerateMultipleKeysFromContextAndVariablesWithArray() {
    final String expression = "T(com.github.alturkovic.lock.key.SpelKeyGeneratorTest).generateArrayKeys(#message)";
    assertThat(keyGenerator.resolveKeys("lock_", expression, service, sendMessageMethod, new Object[]{"p_"}))
      .containsExactly("lock_p_first", "lock_15");
  }

  @Test
  public void shouldGenerateMultipleKeysFromContextAndVariablesWithMixedTypeValues() {
    final String expression = "T(com.github.alturkovic.lock.key.SpelKeyGeneratorTest).generateMixedKeys(#message)";
    assertThat(keyGenerator.resolveKeys("lock_", expression, service, sendMessageMethod, new Object[]{"p_"}))
      .containsExactly("lock_p_first", "lock_15");
  }

  @Test(expected = EvaluationConvertException.class)
  public void shouldFailWithExpressionThatEvaluatesInNull() {
    keyGenerator.resolveKeys("lock_", "null", service, sendMessageMethod, new Object[]{"hello"});
    fail("Expected exception with expression that evaluated in null");
  }

  @Test(expected = EvaluationConvertException.class)
  public void shouldFailWithExpressionThatEvaluatesInEmptyList() {
    keyGenerator.resolveKeys("lock_", "T(java.util.Collections).emptyList()", service, sendMessageMethod, new Object[]{"hello"});
    fail("Expected exception with expression that evaluated in empty list");
  }

  @Test(expected = EvaluationConvertException.class)
  public void shouldFailWithExpressionThatEvaluatesInListWithNullValue() {
    keyGenerator.resolveKeys("lock_", "T(java.util.Collections).singletonList(null)", service, sendMessageMethod, new Object[]{"hello"});
    fail("Expected exception with expression that evaluated in a list with a null value");
  }

  @SuppressWarnings("unused")
  public static List<String> generateKeys(final String prefix) {
    return Arrays.asList(prefix + "first", prefix + "second");
  }

  @SuppressWarnings("unused")
  public static Object[] generateArrayKeys(final String prefix) {
    return new Object[] {prefix + "first", 15};
  }

  @SuppressWarnings("unused")
  public static Set<Object> generateMixedKeys(final String prefix) {
    return new HashSet<>(Arrays.asList(prefix + "first", 15));
  }

  @SuppressWarnings("unused")
  private static class MessageService {
    public void sendMessage(String message) {
    }
  }
}