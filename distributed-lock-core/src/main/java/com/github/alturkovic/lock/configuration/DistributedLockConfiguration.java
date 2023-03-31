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
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

@Configuration
public class DistributedLockConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public static LockBeanPostProcessor lockBeanPostProcessor(@Lazy final KeyGenerator keyGenerator,
                                                            @Lazy final LockTypeResolver lockTypeResolver,
                                                            @Lazy final IntervalConverter intervalConverter,
                                                            @Lazy final RetriableLockFactory retriableLockFactory,
                                                            @Lazy @Autowired(required = false) final TaskScheduler taskScheduler) {
    final LockBeanPostProcessor processor = new LockBeanPostProcessor(keyGenerator, lockTypeResolver, intervalConverter, retriableLockFactory, taskScheduler);
    processor.setBeforeExistingAdvisors(true);
    return processor;
  }

  @Bean
  @ConditionalOnMissingBean
  public IntervalConverter intervalConverter(@Lazy final ConfigurableBeanFactory configurableBeanFactory) {
    return new BeanFactoryAwareIntervalConverter(configurableBeanFactory);
  }

  @Bean
  @ConditionalOnMissingBean
  public RetriableLockFactory retriableLockFactory(@Lazy final IntervalConverter intervalConverter) {
    return new DefaultRetriableLockFactory(new DefaultRetryTemplateConverter(intervalConverter));
  }

  @Bean
  @ConditionalOnMissingBean
  public KeyGenerator spelKeyGenerator(@Lazy @Autowired(required = false) final ConversionService conversionService) {
    if (conversionService == null) {
      return new SpelKeyGenerator(DefaultConversionService.getSharedInstance());
    }
    return new SpelKeyGenerator(conversionService);
  }

  @Bean
  @ConditionalOnMissingBean
  public LockTypeResolver lockTypeResolver(@Lazy final ConfigurableBeanFactory configurableBeanFactory) {
    return configurableBeanFactory::getBean;
  }

  @Bean
  @ConditionalOnMissingBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
  @ConditionalOnProperty(prefix = "com.github.alturkovic.lock.task-scheduler.default", name = "enabled", havingValue = "true", matchIfMissing = true)
  public TaskScheduler distributedLockTaskScheduler() {
    return new ThreadPoolTaskScheduler();
  }
}
