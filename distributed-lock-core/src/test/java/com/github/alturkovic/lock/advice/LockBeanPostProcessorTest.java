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

import com.github.alturkovic.lock.Locked;
import com.github.alturkovic.lock.converter.BeanFactoryAwareIntervalConverter;
import com.github.alturkovic.lock.key.SpelKeyGenerator;
import com.github.alturkovic.lock.advice.support.SimpleLock;
import com.github.alturkovic.lock.advice.support.SimpleLocked;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class LockBeanPostProcessorTest {
  private LockedInterface lockedInterface;
  private SimpleLock lock;

  @Before
  public void setUp() {
    final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    lock = new SimpleLock();

    final LockTypeResolver lockTypeResolver = Mockito.mock(LockTypeResolver.class);
    when(lockTypeResolver.get(SimpleLock.class)).thenReturn(lock);

    final SpelKeyGenerator keyGenerator = new SpelKeyGenerator(new DefaultConversionService());
    final LockBeanPostProcessor processor = new LockBeanPostProcessor(new BeanFactoryAwareIntervalConverter(beanFactory), lockTypeResolver, keyGenerator);
    processor.afterPropertiesSet();

    beanFactory.addBeanPostProcessor(processor);
    beanFactory.registerBeanDefinition("lockedService", new RootBeanDefinition(LockedInterface.class, LockedInterfaceImpl::new));
    lockedInterface = beanFactory.getBean(LockedInterface.class);
    beanFactory.preInstantiateSingletons();
  }

  @Test
  public void shouldLockInheritedFromInterface() {
    lockedInterface.doLocked(1, "hello");
    assertThat(lock.getLockMap()).containsExactly(new SimpleEntry<>("lock", Collections.singletonList("lock:hello")));
  }

  @Test
  public void shouldLockInheritedFromInterfaceWithAlias() {
    lockedInterface.doLockedWithAlias(1, "hello");
    assertThat(lock.getLockMap()).containsExactly(new SimpleEntry<>("lock", Collections.singletonList("hello"))); // @SimpleLock does not add prefix
  }

  @Test
  public void shouldLockOverridenFromInterface() {
    lockedInterface.doLockedOverriden(1, "hello");
    assertThat(lock.getLockMap()).containsExactly(new SimpleEntry<>("lock", Collections.singletonList("lock:1")));
  }

  @Test
  public void shouldLockOverridenFromInterfaceWithAlias() {
    lockedInterface.doLockedOverridenWithAlias(1, "hello");
    assertThat(lock.getLockMap()).containsExactly(new SimpleEntry<>("lock", Collections.singletonList("1"))); // @SimpleLock does not add prefix
  }

  @Test
  public void shouldLockFromImplementation() {
    lockedInterface.doLockedFromImplementation(1, "hello");
    assertThat(lock.getLockMap()).containsExactly(new SimpleEntry<>("lock", Collections.singletonList("lock:hello")));
  }

  @Test
  public void shouldLockFromImplementationWithAlias() {
    lockedInterface.doLockedFromImplementationWithAlias(1, "hello");
    assertThat(lock.getLockMap()).containsExactly(new SimpleEntry<>("lock", Collections.singletonList("hello"))); // @SimpleLock does not add prefix
  }

  @Test
  public void shouldLockWithImplementationDetail() {
    lockedInterface.doLockedWithImplementationDetail(1, "hello");
    assertThat(lock.getLockMap()).containsExactly(new SimpleEntry<>("lock", Collections.singletonList("lock:4")));
  }

  @Test
  public void shouldLockWithExecutionPath() {
    lockedInterface.doLockedWithExecutionPath();
    assertThat(lock.getLockMap()).containsExactly(new SimpleEntry<>("lock", Collections.singletonList("com.github.alturkovic.lock.advice.LockBeanPostProcessorTest.LockedInterfaceImpl.doLockedWithExecutionPath")));
  }

  @Test
  public void shouldLockFromImplementationWithImplementationDetail() {
    lockedInterface.doLockedFromImplementationWithImplementationDetail(1, "hello");
    assertThat(lock.getLockMap()).containsExactly(new SimpleEntry<>("lock", Collections.singletonList("lock:4")));
  }

  private interface LockedInterface {

    @Locked(prefix = "lock:", expression = "#s", type = SimpleLock.class)
    void doLocked(int num, String s);

    @SimpleLocked(expression = "#s")
    void doLockedWithAlias(int num, String s);

    @Locked(prefix = "lock:", expression = "#s", type = SimpleLock.class)
    void doLockedOverriden(int num, String s);

    @SimpleLocked(expression = "#s")
    void doLockedOverridenWithAlias(int num, String s);

    void doLockedFromImplementation(int num, String s);

    void doLockedFromImplementationWithAlias(int num, String s);

    @Locked(prefix = "lock:", expression = "getStaticValue()", type = SimpleLock.class)
    void doLockedWithImplementationDetail(int num, String s);

    void doLockedFromImplementationWithImplementationDetail(int num, String s);

    @SimpleLocked
    void doLockedWithExecutionPath();
  }

  private class LockedInterfaceImpl implements LockedInterface {

    @Override
    public void doLocked(final int num, final String s) {
    }

    @Override
    public void doLockedWithAlias(final int num, final String s) {
    }

    @Override
    @Locked(prefix = "lock:", expression = "#num", type = SimpleLock.class)
    public void doLockedOverriden(final int num, final String s) {
    }

    @Override
    @SimpleLocked(expression = "#num")
    public void doLockedOverridenWithAlias(final int num, final String s) {
    }

    @Override
    @Locked(prefix = "lock:", expression = "#s", type = SimpleLock.class)
    public void doLockedFromImplementation(final int num, final String s) {
    }

    @Override
    @SimpleLocked(expression = "#s")
    public void doLockedFromImplementationWithAlias(final int num, final String s) {
    }

    @Override
    public void doLockedWithImplementationDetail(final int num, final String s) {
    }

    @Override
    @Locked(prefix = "lock:", expression = "getStaticValue()", type = SimpleLock.class)
    public void doLockedFromImplementationWithImplementationDetail(final int num, final String s) {
    }

    @Override
    public void doLockedWithExecutionPath() {
    }

    public int getStaticValue() {
      return 4;
    }
  }
}