/*
 * Copyright (c)  2017 Alen Turković <alturkovic@gmail.com>
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
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
    final List<String> singletonKeyList = Collections.singletonList(storeId + ":" + keys.get(0));

    final String token = tokenSupplier.get();
    final boolean locked = stringRedisTemplate.execute(lockScript, singletonKeyList, token, String.valueOf(expiration));
    log.debug("Tried to acquire lock for key {} with token {} in store {}. Locked: {}", keys.get(0), token, storeId, locked);
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