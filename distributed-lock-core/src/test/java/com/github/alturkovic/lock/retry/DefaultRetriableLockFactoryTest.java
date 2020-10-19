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

package com.github.alturkovic.lock.retry;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.Locked;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultRetriableLockFactoryTest {

  @Mock
  private RetryTemplateConverter retryTemplateConverter;

  @Mock
  private Lock lock;

  @Mock
  private Locked locked;

  @Mock
  private RetryTemplate retryTemplate;

  @Test
  public void shouldGenerateRetriableLock() {
    when(retryTemplateConverter.construct(eq(locked))).thenReturn(retryTemplate);

    final RetriableLockFactory factory = new DefaultRetriableLockFactory(retryTemplateConverter);
    final RetriableLock retriableLock = factory.generate(lock, locked);

    assertThat(retriableLock.getLock()).isEqualTo(lock);
    assertThat(retriableLock.getRetryTemplate()).isEqualTo(retryTemplate);
  }
}