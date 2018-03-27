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
import com.github.alturkovic.lock.advice.dummy.CustomLocked;
import com.github.alturkovic.lock.advice.dummy.DummyLock;
import com.github.alturkovic.lock.advice.dummy.DummyLocked;
import com.github.alturkovic.lock.converter.IntervalConverter;
import com.github.alturkovic.lock.exception.DistributedLockException;
import com.github.alturkovic.lock.key.SpelKeyGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    lockMap.put(DummyLock.class, lock);
    final IntervalConverter intervalConverter = new IntervalConverter(new DefaultListableBeanFactory());
    proxyFactory.addAspect(new LockAdvice(intervalConverter, new SpelKeyGenerator(), lockMap));
    locked = proxyFactory.getProxy();
  }

  @Test
  public void shouldAcquireDefaultLockAndCallMethod() {
    when(lock.acquire(anyListOf(String.class), any(), anyLong())).thenReturn("abc");
    when(lock.release(eq(Collections.singletonList("lock:com.github.alturkovic.lock.advice.LockAdviceTest.LockedService.tryWithDefaultLock")), eq("abc"), any()))
        .thenReturn(true);

    locked.tryWithDefaultLock();

    verify(lock, atLeastOnce())
        .release(eq(Collections.singletonList("lock:com.github.alturkovic.lock.advice.LockAdviceTest.LockedService.tryWithDefaultLock")), eq("abc"), any());
  }

  @Test
  public void shouldAcquireNamedLockAndCallMethod() {
    when(lock.acquire(anyListOf(String.class), any(), anyLong())).thenReturn("abc");
    when(lock.release(eq(Collections.singletonList("lock:test")), eq("abc"), any())).thenReturn(true);

    locked.tryWithNamedLock();

    verify(lock, atLeastOnce()).release(eq(Collections.singletonList("lock:test")), eq("abc"), any());
  }

  @Test(expected = DistributedLockException.class)
  public void shouldNotAcquireNamedLockAndThrowException() {
    when(lock.acquire(anyListOf(String.class), any(), anyLong())).thenReturn(null);

    locked.tryWithNamedLock();
  }

  @Test(expected = DistributedLockException.class)
  public void shouldFailWithUnregisteredLockType() {
    locked.tryWithUnregisteredLock();
  }

  @Test
  public void shouldLockWithLockAlias() {
    when(lock.acquire(anyListOf(String.class), any(), anyLong())).thenReturn("abc");
    when(lock.release(eq(Collections.singletonList("lock:aliased")), eq("abc"), any())).thenReturn(true);

    locked.tryWithLockAlias();

    verify(lock, atLeastOnce()).release(eq(Collections.singletonList("lock:aliased")), eq("abc"), any());
  }

  interface LockedInterface {
    void tryWithDefaultLock();

    void tryWithNamedLock();

    void tryWithLockAlias();

    void tryWithUnregisteredLock();
  }

  static class LockedService implements LockedInterface {

    @Locked
    public void tryWithDefaultLock() {
    }

    @Locked(expression = "'test'")
    public void tryWithNamedLock() {
    }

    @Override
    @DummyLocked(expression = "'aliased'")
    @SuppressWarnings("unchecked") // added @Override and @SuppressWarnings to make sure merging works fine with other annotations
    public void tryWithLockAlias() {
    }

    @CustomLocked
    public void tryWithUnregisteredLock() {
    }
  }
}