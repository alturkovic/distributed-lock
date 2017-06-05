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
import com.github.alturkovic.lock.annotation.alias.MongoLocked;
import com.github.alturkovic.lock.annotation.alias.RedisLocked;
import com.github.alturkovic.lock.exception.DistributedLockException;
import com.github.alturkovic.lock.key.SpelKeyGenerator;
import com.github.alturkovic.lock.type.SimpleMongoLock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LockAdviceTest {

    private LockedInterface locked;

    @Mock
    private Lock lock;

    @Before
    public void init() {
        final AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new LockedService());
        final Map<Class<? extends Lock>, Lock> lockMap = new HashMap<>();
        lockMap.put(Lock.class, lock);
        lockMap.put(SimpleMongoLock.class, lock);
        proxyFactory.addAspect(new LockAdvice(new SpelKeyGenerator(), lockMap));
        locked = proxyFactory.getProxy();
    }

    @Test
    public void shouldAcquireLockAndCallMethod() {
        when(lock.acquire(anyListOf(String.class), any(), any(), anyLong(), anyLong(), any(), anyLong())).thenReturn("abc");
        doNothing().when(lock).release(eq(Collections.singletonList("lock:test")), eq("abc"), any());

        locked.tryWithLock();

        verify(lock, atLeastOnce()).release(eq(Collections.singletonList("lock:test")), eq("abc"), any());
    }

    @Test(expected = DistributedLockException.class)
    public void shouldNotAcquireLockAndThrowException() {
        when(lock.acquire(anyListOf(String.class), any(), any(), anyLong(), anyLong(), any(), anyLong())).thenReturn(null);

        locked.tryWithLock();
    }

    @Test(expected = DistributedLockException.class)
    public void shouldFailWithUnregisteredLockType() {
        locked.tryWithUnregisteredLock();
    }

    @Test
    public void shouldLockWithLockAlias() {
        when(lock.acquire(anyListOf(String.class), any(), any(), anyLong(), anyLong(), any(), anyLong())).thenReturn("abc");
        doNothing().when(lock).release(eq(Collections.singletonList("lock:aliased")), eq("abc"), any());

        locked.tryWithLockAlias();

        verify(lock, atLeastOnce()).release(eq(Collections.singletonList("lock:aliased")), eq("abc"), any());
    }

    interface LockedInterface {
        void tryWithLock();

        void tryWithLockAlias();

        void tryWithUnregisteredLock();
    }

    static class LockedService implements LockedInterface {

        @Locked(expression = "'test'", type = Lock.class)
        public void tryWithLock() {
        }

        @Override
        @MongoLocked(expression = "'aliased'")
        @SuppressWarnings("unchecked") // added @Override and this to make sure merging works fine with other annotations
        public void tryWithLockAlias() {
        }

        @RedisLocked
        public void tryWithUnregisteredLock() {
        }
    }
}