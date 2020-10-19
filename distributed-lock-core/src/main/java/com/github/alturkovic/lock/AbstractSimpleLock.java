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

package com.github.alturkovic.lock;

import java.util.List;
import java.util.function.Supplier;
import lombok.Data;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An abstract lock used as a base for all locks that operate with only 1 key instead of multiple keys.
 */
@Data
public abstract class AbstractSimpleLock implements Lock {
  private final Supplier<String> tokenSupplier;

  @Override
  public String acquire(final List<String> keys, final String storeId, final long expiration) {
    Assert.isTrue(keys.size() == 1, "Cannot acquire lock for multiple keys with this lock");

    final String token = tokenSupplier.get();
    if (StringUtils.isEmpty(token)) {
      throw new IllegalStateException("Cannot lock with empty token");
    }

    return acquire(keys.get(0), storeId, token, expiration);
  }

  @Override
  public boolean release(final List<String> keys, final String storeId, final String token) {
    Assert.isTrue(keys.size() == 1, "Cannot release lock for multiple keys with this lock");
    return release(keys.get(0), storeId, token);
  }

  @Override
  public boolean refresh(final List<String> keys, final String storeId, final String token, final long expiration) {
    Assert.isTrue(keys.size() == 1, "Cannot refresh lock for multiple keys with this lock");
    return refresh(keys.get(0), storeId, token, expiration);
  }

  protected abstract String acquire(String key, String storeId, String token, long expiration);
  protected abstract boolean release(String key, String storeId, String token);
  protected abstract boolean refresh(String key, String storeId, String token, long expiration);
}
