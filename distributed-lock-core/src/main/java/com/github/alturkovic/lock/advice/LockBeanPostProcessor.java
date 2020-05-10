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

import com.github.alturkovic.lock.Locked;
import com.github.alturkovic.lock.interval.IntervalConverter;
import com.github.alturkovic.lock.key.KeyGenerator;
import com.github.alturkovic.lock.retry.RetriableLockFactory;
import lombok.AllArgsConstructor;
import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.TaskScheduler;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} for beans with {@link Locked} methods.
 */
@AllArgsConstructor
public class LockBeanPostProcessor extends AbstractAdvisingBeanPostProcessor implements InitializingBean {
  private final KeyGenerator keyGenerator;
  private final LockTypeResolver lockTypeResolver;
  private final IntervalConverter intervalConverter;
  private final RetriableLockFactory retriableLockFactory;
  private final TaskScheduler taskScheduler;

  @Override
  public void afterPropertiesSet() {
    final var pointcut = new AnnotationMatchingPointcut(null, Locked.class, true);
    final var interceptor = new LockMethodInterceptor(keyGenerator, lockTypeResolver, intervalConverter, retriableLockFactory, taskScheduler);

    this.advisor = new DefaultPointcutAdvisor(pointcut, interceptor);
  }
}
