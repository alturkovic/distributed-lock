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

package com.github.alturkovic.lock.advice;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.annotation.Locked;
import com.github.alturkovic.lock.exception.DistributedLockException;
import com.github.alturkovic.lock.exception.EvaluationConvertException;
import com.github.alturkovic.lock.key.KeyGenerator;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Aspect
@AllArgsConstructor
public final class LockAdvice {

    private final KeyGenerator keyGenerator;
    private final Map<Class<? extends Lock>, Lock> lockMap;

    @Around("execution(@(@com.github.alturkovic.lock.annotation.Locked *) * *(..))")
    public Object lockAlias(final ProceedingJoinPoint joinPoint) throws Throwable {
        final Method method = ClassUtils.getMostSpecificMethod(((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getTarget().getClass());

        final Locked locked = AnnotatedElementUtils.getMergedAnnotation(method, Locked.class);
        if (locked != null) {
            return lock(joinPoint, locked);
        }

        throw new DistributedLockException(String.format("Couldn't find the nested @Locked on the method %s", joinPoint.getSignature().toLongString()));
    }

    @Around("@annotation(locked)")
    public Object lock(final ProceedingJoinPoint joinPoint, final Locked locked) throws Throwable {
        final Lock lock = lockMap.get(locked.type());

        if (lock == null) {
            throw new DistributedLockException(String.format("Lock type '%s' not configured", locked.type()));
        }

        final List<String> keys;
        try {
            keys = keyGenerator.resolveKeys(locked.prefix(), locked.expression(), locked.parameter(), joinPoint);
        } catch (final RuntimeException e) {
            throw new DistributedLockException("Cannot resolve keys to lock from expression: " + locked.expression(), e);
        }

        String token = null;
        try {
            token = lock.acquire(keys, locked.typeSpecificStoreId(), locked.expirationTimeUnit(), locked.expire(), locked.retryMillis(), locked.timeoutTimeUnit(), locked.timeout());

            if (token != null) {
                return joinPoint.proceed();
            } else {
                throw new DistributedLockException("Unable to acquire lock with expression: " + locked.expression());
            }
        } finally {
            if (token != null && !locked.manuallyReleased()) {
                lock.release(keys, token, locked.typeSpecificStoreId());
            }
        }
    }
}