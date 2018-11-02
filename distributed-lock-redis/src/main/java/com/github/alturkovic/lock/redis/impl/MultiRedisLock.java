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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.StringUtils;

@Data
@Slf4j
@AllArgsConstructor
public class MultiRedisLock implements Lock {
  private static final String LOCK_SCRIPT = "local msetnx_keys_with_tokens = {}\n" +
    "for _, key in ipairs(KEYS) do\n" +
    "    msetnx_keys_with_tokens[#msetnx_keys_with_tokens + 1] = key\n" +
    "    msetnx_keys_with_tokens[#msetnx_keys_with_tokens + 1] = ARGV[1]\n" +
    "end\n" +
    "local keys_successfully_set = redis.call('MSETNX', unpack(msetnx_keys_with_tokens))\n" +
    "if (keys_successfully_set == 0) then\n" +
    "    return false\n" +
    "end\n" +
    "local expiration = tonumber(ARGV[2])\n" +
    "for _, key in ipairs(KEYS) do\n" +
    "    redis.call('PEXPIRE', key, expiration)\n" +
    "end\n" +
    "return true\n";

  private static final String LOCK_RELEASE_SCRIPT = "for _, key in pairs(KEYS) do\n" +
    "    if redis.call('GET', key) ~= ARGV[1] then\n" +
    "        return false\n" +
    "    end\n" +
    "end\n" +
    "redis.call('DEL', unpack(KEYS))\n" +
    "return true\n";

  private final RedisScript<Boolean> lockScript = new DefaultRedisScript<>(LOCK_SCRIPT, Boolean.class);
  private final RedisScript<Boolean> lockReleaseScript = new DefaultRedisScript<>(LOCK_RELEASE_SCRIPT, Boolean.class);

  private final StringRedisTemplate stringRedisTemplate;
  private final Supplier<String> tokenSupplier;

  public MultiRedisLock(final StringRedisTemplate stringRedisTemplate) {
    this(stringRedisTemplate, () -> UUID.randomUUID().toString());
  }

  @Override
  public String acquire(final List<String> keys, final String storeId, final long expiration) {
    final List<String> keysWithStoreIdPrefix = keys.stream().map(key -> storeId + ":" + key).collect(Collectors.toList());
    final String token = tokenSupplier.get();

    if (StringUtils.isEmpty(token)) {
      throw new IllegalStateException("Cannot lock with empty token");
    }

    final Boolean locked = stringRedisTemplate.execute(lockScript, keysWithStoreIdPrefix, token, String.valueOf(expiration));
    log.debug("Tried to acquire lock for keys '{}' in store '{}' with safety token '{}'. Locked: {}", keys, storeId, token, locked);
    return locked ? token : null;
  }

  @Override
  public boolean release(final List<String> keys, final String storeId, final String token) {
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
