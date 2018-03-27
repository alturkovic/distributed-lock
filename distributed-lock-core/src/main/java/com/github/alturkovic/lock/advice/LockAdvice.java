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

package com.github.alturkovic.lock.advice;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.Locked;
import com.github.alturkovic.lock.converter.IntervalConverter;
import com.github.alturkovic.lock.exception.DistributedLockException;
import com.github.alturkovic.lock.key.KeyGenerator;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Aspect
@AllArgsConstructor
public final class LockAdvice {

  private final IntervalConverter intervalConverter;
  private final KeyGenerator keyGenerator;
  private final Map<Class<? extends Lock>, Lock> lockMap;

  /**
   * This advice will bind to all {@link Locked} alias methods.
   * <p>
   * Method will find the alias and merge it into {@link Locked} annotation and behave as if it was bind with {@link Locked} annotation.
   * </p>
   */
  @Around("execution(@(@com.github.alturkovic.lock.Locked *) * *(..))")
  public Object lockAlias(final ProceedingJoinPoint joinPoint) throws Throwable {
    final Method method = ClassUtils.getMostSpecificMethod(((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getTarget().getClass());

    final Locked locked = AnnotatedElementUtils.getMergedAnnotation(method, Locked.class);
    if (locked != null) {
      return lock(joinPoint, locked);
    }

    throw new DistributedLockException(String.format("Couldn't find the nested @Locked on the method %s", joinPoint.getSignature().toLongString()));
  }

  /**
   * This advice will bind to all {@link Locked} methods.
   */
  @Around("@annotation(locked)")
  public Object lock(final ProceedingJoinPoint joinPoint, final Locked locked) throws Throwable {
    final Lock lock = lockMap.get(locked.type());

    if (lock == null) {
      throw new DistributedLockException(String.format("Lock type %s not configured", locked.type()));
    }

    if (StringUtils.isEmpty(locked.expression())) {
      throw new DistributedLockException("Missing expression: " + locked);
    }

    final List<String> keys;
    try {
      keys = keyGenerator.resolveKeys(locked.prefix(), locked.expression(), locked.parameter(), joinPoint);
    }
    catch (final RuntimeException e) {
      throw new DistributedLockException("Cannot resolve keys to lock from expression: " + locked.expression(), e);
    }

    long timeout = intervalConverter.toMillis(locked.timeout());
    final long retry = intervalConverter.toMillis(locked.retry());

    String token = null;
    try {
      while (token == null && timeout >= 0) {
        try {
          token = lock.acquire(keys, locked.storeId(), intervalConverter.toMillis(locked.expiration()));
        }
        catch (Exception e) {
          throw new DistributedLockException("Unable to acquire lock with expression: " + locked.expression(), e);
        }

        // if token was not acquired, wait and try again until timeout
        if (StringUtils.isEmpty(token)) {
          timeout -= retry;

          try {
            Thread.sleep(retry);
          }
          catch (final InterruptedException e) {
            // Do nothing...
          }
        }
      }

      if (StringUtils.isEmpty(token)) {
        throw new DistributedLockException("Unable to acquire lock with expression: " + locked.expression());
      }

      log.debug("Acquired lock for keys {} with token {} in store {}", keys, token, locked.storeId());
      return joinPoint.proceed();
    }
    finally {
      if (token != null && !locked.manuallyReleased()) {
        final boolean released = lock.release(keys, token, locked.storeId());
        if (released) {
          log.debug("Released lock for keys {} with token {} in store {}", keys, token, locked.storeId());
        } else {
          // this could indicate that locks are released before method execution is finished and that locks expire too soon
          // this could also indicate a problem with the store where locks are held, connectivity issues or query problems
          log.error("Couldn't release lock for keys {} with token {} in store {}", keys, token, locked.storeId());
        }
      }
    }
  }
}