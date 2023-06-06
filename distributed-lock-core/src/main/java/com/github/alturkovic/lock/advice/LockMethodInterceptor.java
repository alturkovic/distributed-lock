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

package com.github.alturkovic.lock.advice;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.Locked;
import com.github.alturkovic.lock.exception.DistributedLockException;
import com.github.alturkovic.lock.interval.IntervalConverter;
import com.github.alturkovic.lock.key.KeyGenerator;
import com.github.alturkovic.lock.retry.RetriableLockFactory;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.StringUtils;

@Slf4j
@AllArgsConstructor
public class LockMethodInterceptor implements MethodInterceptor {
  private final KeyGenerator keyGenerator;
  private final LockTypeResolver lockTypeResolver;
  private final IntervalConverter intervalConverter;
  private final RetriableLockFactory retriableLockFactory;
  private final TaskScheduler taskScheduler;

  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    final LockContext context = new LockContext(invocation);
    try {
      return executeLockedMethod(invocation, context);
    } finally {
      cleanAfterExecution(context);
    }
  }

  private Object executeLockedMethod(final MethodInvocation invocation, final LockContext context) throws Throwable {
    final long expiration = intervalConverter.toMillis(context.getLocked().expiration());
    try {
      Lock lock = retriableLockFactory.generate(context.getLock(), context.getLocked());
      context.setToken(lock.acquire(context.getKeys(), context.getLocked().storeId(), expiration));
    } catch (final Exception e) {
      throw new DistributedLockException(String.format("Unable to acquire lock with expression: %s", context.getLocked().expression()), e);
    }

    log.debug("Acquired lock for keys {} with token {} in store {}", context.getKeys(), context.getToken(), context.getLocked().storeId());

    scheduleLockRefresh(context, expiration);
    return invocation.proceed();
  }

  private void scheduleLockRefresh(final LockContext context, final long expiration) {
    final long refresh = intervalConverter.toMillis(context.getLocked().refresh());
    if (refresh > 0) {
      context.setScheduledFuture(taskScheduler.scheduleAtFixedRate(constructRefreshRunnable(context, expiration), new Date(System.currentTimeMillis() + refresh), refresh));
    }
  }

  private Runnable constructRefreshRunnable(final LockContext context, final long expiration) {
    return () -> context.getLock().refresh(context.getKeys(), context.getLocked().storeId(), context.getToken(), expiration);
  }

  private void cleanAfterExecution(final LockContext context) {
    final ScheduledFuture<?> scheduledFuture = context.getScheduledFuture();
    if (scheduledFuture != null && !scheduledFuture.isCancelled() && !scheduledFuture.isDone()) {
      scheduledFuture.cancel(true);
    }

    if (StringUtils.hasText(context.getToken()) && !context.getLocked().manuallyReleased()) {
      final boolean released = context.getLock().release(context.getKeys(), context.getLocked().storeId(), context.getToken());
      if (released) {
        log.debug("Released lock for keys {} with token {} in store {}", context.getKeys(), context.getToken(), context.getLocked().storeId());
      } else {
        // this could indicate that locks are released before method execution is finished and that locks expire too soon
        // this could also indicate a problem with the store where locks are held, connectivity issues or query problems
        log.error("Couldn't release lock for keys {} with token {} in store {}", context.getKeys(), context.getToken(), context.getLocked().storeId());
      }
    }
  }

  @Data
  private class LockContext {
    private final Method method;
    private final Locked locked;
    private final Lock lock;
    private final List<String> keys;

    private String token;
    private ScheduledFuture<?> scheduledFuture;

    public LockContext(final MethodInvocation invocation) {
      method = AopUtils.getMostSpecificMethod(invocation.getMethod(), invocation.getThis().getClass());
      locked = AnnotatedElementUtils.findMergedAnnotation(method, Locked.class);
      lock = lockTypeResolver.get(locked.type());
      keys = resolveKeys(invocation, method, locked);

      validateConstructedContext();
    }

    private List<String> resolveKeys(final MethodInvocation invocation, final Method method, final Locked locked) {
      try {
        return keyGenerator.resolveKeys(locked.prefix(), locked.expression(), invocation.getThis(), method, invocation.getArguments());
      } catch (final RuntimeException e) {
        throw new DistributedLockException(String.format("Cannot resolve keys to lock: %s on method %s", locked, method), e);
      }
    }

    private void validateConstructedContext() {
      if (StringUtils.isEmpty(locked.expression())) {
        throw new DistributedLockException(String.format("Missing expression: %s on method %s", locked, method));
      }

      if (lock == null) {
        throw new DistributedLockException(String.format("Lock type %s not configured", locked.type().getName()));
      }
    }
  }
}
