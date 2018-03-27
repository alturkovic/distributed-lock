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

package com.github.alturkovic.lock.configuration;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.advice.LockAdvice;
import com.github.alturkovic.lock.converter.IntervalConverter;
import com.github.alturkovic.lock.key.KeyGenerator;
import com.github.alturkovic.lock.key.SpelKeyGenerator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan("com.github.alturkovic.lock.converter")
public class DistributedLockConfiguration {

  @Bean
  public LockAdvice lockAdvice(final IntervalConverter intervalConverter, final KeyGenerator spelKeyGenerator, final List<Lock> locks) {
    return new LockAdvice(intervalConverter, spelKeyGenerator, locks.stream().collect(Collectors.toMap(Lock::getClass, Function.identity())));
  }

  @Bean
  public KeyGenerator spelKeyGenerator() {
    return new SpelKeyGenerator();
  }
}
