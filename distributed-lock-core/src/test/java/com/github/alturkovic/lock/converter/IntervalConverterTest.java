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

package com.github.alturkovic.lock.converter;

import com.github.alturkovic.lock.Interval;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "locked.interval=10")
public class IntervalConverterTest {

  @Autowired
  private IntervalConverter intervalConverter;

  @Test
  @Interval(value = "10", unit = TimeUnit.MILLISECONDS)
  public void shouldResolveStatic() throws NoSuchMethodException {
    final long millis = intervalConverter.toMillis(this.getClass().getMethod("shouldResolveStatic").getAnnotation(Interval.class));
    assertThat(millis).isEqualTo(10);
  }

  @Test
  @Interval(value = "${locked.interval}", unit = TimeUnit.MILLISECONDS)
  public void shouldResolveProperty() throws NoSuchMethodException {
    final long millis = intervalConverter.toMillis(this.getClass().getMethod("shouldResolveProperty").getAnnotation(Interval.class));
    assertThat(millis).isEqualTo(10);
  }

  @SpringBootApplication
  @ComponentScan("com.github.alturkovic.lock.converter")
  static class TestApp {}

  @Bean
  StandardBeanExpressionResolver resolver() {
    return new StandardBeanExpressionResolver();
  }
}