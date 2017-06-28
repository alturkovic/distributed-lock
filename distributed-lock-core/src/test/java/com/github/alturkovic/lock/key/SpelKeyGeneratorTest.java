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
import lombok.Getter;
import org.aspectj.lang.JoinPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpelKeyGeneratorTest {

    @SuppressWarnings("unused") // used in test expression, false IntelliJ warning
    public static final String CSV_DUMMY = "a,b,c";

    @Getter
    private final String stringDummy = "contextTest";

    private final SpelKeyGenerator generator = new SpelKeyGenerator();

    @Mock
    private JoinPoint joinPoint;

    // dummy method used in expression to generate a set
    public static Set<String> generateKeySet() {
        return new HashSet<>(Arrays.asList("a", "b", "c"));
    }

    // dummy method used in expression to generate a list from parameters
    public static List<String> generateKeys(final String csv, final String postfix) {
        return Stream.of(csv.split(",")).map(s -> s + postfix).collect(Collectors.toList());
    }

    @Test
    public void shouldGenerateSingleKeyFromJoinPointContextAndVariables() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{1});
        when(joinPoint.getTarget()).thenReturn(this);

        final String exp = "getStringDummy() + 'Example' + #p0";
        final List<String> keys = generator.resolveKeys("lock_", exp, "p", joinPoint);
        assertThat(keys).containsExactly("lock_contextTestExample1");
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
    public void shouldConvertStringList() {
        assertThat(new SpelKeyGenerator().convertResultToList(Arrays.asList("a", "b", "c"))).containsExactly("a","b","c");
    }

    @Test
    public void shouldConvertIntegerSetToStringList() {
        assertThat(new SpelKeyGenerator().convertResultToList(new HashSet<>(Arrays.asList(1, 2, 3)))).containsExactly("1","2","3");
    }

    @Test
    public void shouldConvertBooleanToStringList() {
        assertThat(new SpelKeyGenerator().convertResultToList(true)).containsExactly("true");
    }
}