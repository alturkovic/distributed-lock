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
import com.github.alturkovic.lock.exception.LockNotAvailableException;
import java.util.List;
import lombok.Data;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;

/**
 * A {@link Lock} wrapper for retrying {@link #acquire} method calls. This wrapper will retry the acquire method
 * only as specified by the provided {@link RetryTemplate}.
 */
@Data
public class RetriableLock implements Lock {
  private final Lock lock;
  private final RetryTemplate retryTemplate;

  @Override
  public String acquire(final List<String> keys, final String storeId, final long expiration) {
    try {
      return retryTemplate.execute(ctx -> {
        final String token = lock.acquire(keys, storeId, expiration);

        if (!StringUtils.hasText(token)) {
          throw new LockNotAvailableException(String.format("Lock not available for keys: %s in store %s", keys, storeId));
        }

        return token;
      });
    } catch (LockNotAvailableException e) {
      return null;
    }
  }

  @Override
  public boolean release(final List<String> keys, final String storeId, final String token) {
    return lock.release(keys, storeId, token);
  }

  @Override
  public boolean refresh(final List<String> keys, final String storeId, final String token, final long expiration) {
    return lock.refresh(keys, storeId, token, expiration);
  }
}
