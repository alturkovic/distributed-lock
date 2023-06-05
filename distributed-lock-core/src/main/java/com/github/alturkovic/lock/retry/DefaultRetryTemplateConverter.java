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

import com.github.alturkovic.lock.Locked;
import com.github.alturkovic.lock.exception.LockNotAvailableException;
import com.github.alturkovic.lock.interval.IntervalConverter;
import java.util.Collections;
import lombok.Data;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Data
public class DefaultRetryTemplateConverter implements RetryTemplateConverter {
  private final IntervalConverter intervalConverter;

  @Override
  public RetryTemplate construct(final Locked locked) {
    final RetryTemplate retryTemplate = new RetryTemplate();
    RetryPolicy retryPolicy = resolveLockRetryPolicy(locked);
    if (retryPolicy == null) {
      return null;
    }

    BackOffPolicy backOffPolicy = resolveBackOffPolicy(locked);
    if (backOffPolicy == null) {
      return null;
    }

    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    return retryTemplate;
  }

  private RetryPolicy resolveLockRetryPolicy(final Locked locked) {
    final CompositeRetryPolicy compositeRetryPolicy = new CompositeRetryPolicy();

    final RetryPolicy timeoutRetryPolicy = resolveTimeoutRetryPolicy(locked);
    if (timeoutRetryPolicy == null) {
      return null;
    }

    final RetryPolicy exceptionTypeRetryPolicy = resolveExceptionTypeRetryPolicy();
    compositeRetryPolicy.setPolicies(new RetryPolicy[]{timeoutRetryPolicy, exceptionTypeRetryPolicy});
    return compositeRetryPolicy;
  }

  private RetryPolicy resolveTimeoutRetryPolicy(final Locked locked) {
    long timeout = intervalConverter.toMillis(locked.timeout());
    if (timeout <= 0) {
      return null;
    }

    final TimeoutRetryPolicy timeoutRetryPolicy = new TimeoutRetryPolicy();
    timeoutRetryPolicy.setTimeout(timeout);
    return timeoutRetryPolicy;
  }

  private RetryPolicy resolveExceptionTypeRetryPolicy() {
    return new SimpleRetryPolicy(Integer.MAX_VALUE, Collections.singletonMap(LockNotAvailableException.class, true));
  }

  private BackOffPolicy resolveBackOffPolicy(final Locked locked) {
    long retry = intervalConverter.toMillis(locked.retry());
    if (retry <= 0) {
      return null;
    }

    final FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(retry);
    return fixedBackOffPolicy;
  }
}
