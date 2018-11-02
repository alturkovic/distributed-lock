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

package com.github.alturkovic.lock.advice.support;

import com.github.alturkovic.lock.Lock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class SimpleLock implements Lock {
  private final Map<String, List<String>> lockMap = new HashMap<>();

  @Override
  public String acquire(final List<String> keys, final String storeId, final long expiration) {
    final List<String> locksForStore = lockMap.get(storeId);
    if (locksForStore != null && keys.stream().anyMatch(locksForStore::contains)) {
      return null;
    }

    lockMap.computeIfAbsent(storeId, s -> new ArrayList<>()).addAll(keys);

    return UUID.randomUUID().toString();
  }

  @Override
  public boolean release(final List<String> keys, final String storeId, final String token) {
    // we do not release keys from here for easier testing, keys should be released with reset()
    return true;
  }

  public void reset() {
    lockMap.clear();
  }
}
