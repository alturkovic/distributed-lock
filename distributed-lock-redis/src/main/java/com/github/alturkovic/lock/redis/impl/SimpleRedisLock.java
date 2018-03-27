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

package com.github.alturkovic.lock.redis.impl;

import com.github.alturkovic.lock.Lock;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

/**
 * Works the same way as {@link MultiRedisLock} but is optimized better to work with a single key.
 */
@Data
@Slf4j
@AllArgsConstructor
public class SimpleRedisLock implements Lock {

  private final StringRedisTemplate stringRedisTemplate;
  private final RedisScript<Boolean> lockScript;
  private final RedisScript<Boolean> lockReleaseScript;
  private final Supplier<String> tokenSupplier;

  public SimpleRedisLock(final StringRedisTemplate stringRedisTemplate, final RedisScript<Boolean> lockScript, final RedisScript<Boolean> lockReleaseScript) {
    this(stringRedisTemplate, lockScript, lockReleaseScript, () -> UUID.randomUUID().toString());
  }

  @Override
  public String acquire(final List<String> keys, final String storeId, final long expiration) {
    Assert.isTrue(keys.size() == 1, "Cannot acquire lock for multiple keys with this lock: " + keys);

    final String key = keys.get(0);
    final List<String> singletonKeyList = Collections.singletonList(storeId + ":" + key);
    final String token = tokenSupplier.get();

    final boolean locked = stringRedisTemplate.execute(lockScript, singletonKeyList, token, String.valueOf(expiration));
    log.debug("Tried to acquire lock for key {} with token {} in store {}. Locked: {}", key, token, storeId, locked);
    return locked ? token : null;
  }

  @Override
  public boolean release(final List<String> keys, final String token, final String storeId) {
    Assert.isTrue(keys.size() == 1, "Cannot release lock for multiple keys with this lock: " + keys);
    final String key = keys.get(0);

    final List<String> singletonKeyList = Collections.singletonList(storeId + ":" + key);

    final boolean released = stringRedisTemplate.execute(lockReleaseScript, singletonKeyList, token);
    if (released) {
      log.debug("Release script deleted the record for key {} with token {} in store {}", key, token, storeId);
    } else {
      log.error("Release script failed for key {} with token {} in store {}", key, token, storeId);
    }
    return released;
  }
}