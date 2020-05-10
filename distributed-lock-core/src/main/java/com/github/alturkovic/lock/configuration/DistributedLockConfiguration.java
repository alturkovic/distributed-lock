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

package com.github.alturkovic.lock.configuration;

import com.github.alturkovic.lock.advice.LockBeanPostProcessor;
import com.github.alturkovic.lock.advice.LockTypeResolver;
import com.github.alturkovic.lock.interval.BeanFactoryAwareIntervalConverter;
import com.github.alturkovic.lock.interval.IntervalConverter;
import com.github.alturkovic.lock.key.KeyGenerator;
import com.github.alturkovic.lock.key.SpelKeyGenerator;
import com.github.alturkovic.lock.retry.DefaultRetriableLockFactory;
import com.github.alturkovic.lock.retry.DefaultRetryTemplateConverter;
import com.github.alturkovic.lock.retry.RetriableLockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class DistributedLockConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public LockBeanPostProcessor lockBeanPostProcessor(final KeyGenerator keyGenerator,
                                                     final ConfigurableBeanFactory configurableBeanFactory,
                                                     final IntervalConverter intervalConverter,
                                                     final RetriableLockFactory retriableLockFactory,
                                                     @Autowired(required = false) final TaskScheduler taskScheduler) {
    final var processor = new LockBeanPostProcessor(keyGenerator, configurableBeanFactory::getBean, intervalConverter, retriableLockFactory, taskScheduler);
    processor.setBeforeExistingAdvisors(true);
    return processor;
  }

  @Bean
  @ConditionalOnMissingBean
  public IntervalConverter intervalConverter(final ConfigurableBeanFactory configurableBeanFactory) {
    return new BeanFactoryAwareIntervalConverter(configurableBeanFactory);
  }

  @Bean
  @ConditionalOnMissingBean
  public RetriableLockFactory retriableLockFactory(final IntervalConverter intervalConverter) {
    return new DefaultRetriableLockFactory(new DefaultRetryTemplateConverter(intervalConverter));
  }

  @Bean
  @ConditionalOnMissingBean
  public KeyGenerator spelKeyGenerator(final ConversionService conversionService) {
    return new SpelKeyGenerator(conversionService);
  }

  @Bean
  @ConditionalOnMissingBean
  public LockTypeResolver lockTypeResolver(final ConfigurableBeanFactory configurableBeanFactory) {
    return configurableBeanFactory::getBean;
  }

  @Bean
  @ConditionalOnMissingBean
  public ConversionService conversionService() {
    return new DefaultConversionService();
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(prefix = "com.github.alturkovic.lock.task-scheduler.default", name = "enabled", havingValue = "true", matchIfMissing = true)
  public TaskScheduler taskScheduler() {
    return new ThreadPoolTaskScheduler();
  }
}
