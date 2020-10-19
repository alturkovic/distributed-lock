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

package com.github.alturkovic.lock.retry;

import com.github.alturkovic.lock.Interval;
import com.github.alturkovic.lock.Locked;
import com.github.alturkovic.lock.interval.BeanFactoryAwareIntervalConverter;
import com.github.alturkovic.lock.interval.IntervalConverter;
import org.junit.Test;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultRetryTemplateConverterTest {

  private final IntervalConverter intervalConverter = new BeanFactoryAwareIntervalConverter(new DefaultListableBeanFactory());

  @Test
  @Locked
  public void shouldConstructDefaultRetryTemplate() {
    final Locked locked = new Object() {}.getClass().getEnclosingMethod().getAnnotation(Locked.class);
    final RetryTemplateConverter converter = new DefaultRetryTemplateConverter(intervalConverter);
    final RetryTemplate retryTemplate = converter.construct(locked);

    assertRetryTemplateConstruction(retryTemplate, 1000L, 50L);
  }

  @Test
  @Locked(retry = @Interval("100"), timeout = @Interval("2000"))
  public void shouldConstructCustomizedRetryTemplate() {
    final Locked locked = new Object() {}.getClass().getEnclosingMethod().getAnnotation(Locked.class);
    final RetryTemplateConverter converter = new DefaultRetryTemplateConverter(intervalConverter);
    final RetryTemplate retryTemplate = converter.construct(locked);

    assertRetryTemplateConstruction(retryTemplate, 2000L, 100L);
  }

  // is there a better way to test the RetryTemplate construction?
  private void assertRetryTemplateConstruction(final RetryTemplate retryTemplate, final long timeout, final long backOff) {
    final ConfigurablePropertyAccessor wrapper = PropertyAccessorFactory.forDirectFieldAccess(retryTemplate);

    assertThat(wrapper.getPropertyValue("retryPolicy")).isInstanceOf(CompositeRetryPolicy.class);
    assertThat((RetryPolicy[]) wrapper.getPropertyValue("retryPolicy.policies"))
      .hasSize(2)
      .anyMatch(policy1 -> {
        if (policy1 instanceof TimeoutRetryPolicy) {
          final TimeoutRetryPolicy timeoutRetryPolicy = (TimeoutRetryPolicy) policy1;
          assertThat(timeoutRetryPolicy.getTimeout()).isEqualTo(timeout);
          return true;
        }
        return false;
      })
      .anyMatch(policy2 -> {
        if (policy2 instanceof SimpleRetryPolicy) {
          final SimpleRetryPolicy simpleRetryPolicy = (SimpleRetryPolicy) policy2;
          assertThat(simpleRetryPolicy.getMaxAttempts()).isEqualTo(Integer.MAX_VALUE);
          return true;
        }
        return false;
      });

    assertThat(wrapper.getPropertyValue("backOffPolicy")).isInstanceOf(FixedBackOffPolicy.class);
    assertThat(((FixedBackOffPolicy) wrapper.getPropertyValue("backOffPolicy")).getBackOffPeriod()).isEqualTo(backOff);
  }
}