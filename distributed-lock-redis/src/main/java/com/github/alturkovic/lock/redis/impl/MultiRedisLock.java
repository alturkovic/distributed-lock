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
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@Data
@Slf4j
@AllArgsConstructor
public class MultiRedisLock implements Lock {

  private final StringRedisTemplate stringRedisTemplate;
  private final RedisScript<Boolean> lockScript;
  private final RedisScript<Boolean> lockReleaseScript;
  private final Supplier<String> tokenSupplier;

  public MultiRedisLock(final StringRedisTemplate stringRedisTemplate, final RedisScript<Boolean> lockScript, final RedisScript<Boolean> lockReleaseScript) {
    this(stringRedisTemplate, lockScript, lockReleaseScript, () -> UUID.randomUUID().toString());
  }

  @Override
  public String acquire(final List<String> keys, final String storeId, final long expiration) {
    final List<String> keysWithStoreIdPrefix = keys.stream().map(key -> storeId + ":" + key).collect(Collectors.toList());
    final String token = tokenSupplier.get();

    final Boolean locked = stringRedisTemplate.execute(lockScript, keysWithStoreIdPrefix, token, String.valueOf(expiration));
    log.debug("Tried to acquire lock for keys '{}' in store '{}' with safety token '{}'. Locked: {}", keys, storeId, token, locked);
    return locked ? token : null;
  }

  @Override
  public boolean release(final List<String> keys, final String token, final String storeId) {
    final List<String> keysWithStoreIdPrefix = keys.stream().map(key -> storeId + ":" + key).collect(Collectors.toList());

    final boolean released = stringRedisTemplate.execute(lockReleaseScript, keysWithStoreIdPrefix, token);
    if (released) {
      log.debug("Release script deleted the record for keys {} with token {} in store {}", keys, token, storeId);
    } else {
      log.error("Release script failed for keys {} with token {} in store {}", keys, token, storeId);
    }
    return released;
  }
}
