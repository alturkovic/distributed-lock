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

package com.github.alturkovic.lock.type;

import com.github.alturkovic.lock.AbstractLock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Data
@Slf4j
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MultiRedisLock extends AbstractLock {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Boolean> lockScript;
    private final RedisScript<Long> lockReleaseScript;
    private final Supplier<String> tokenSupplier;

    public MultiRedisLock(final StringRedisTemplate stringRedisTemplate, final RedisScript<Boolean> lockScript, final RedisScript<Long> lockReleaseScript) {
        this(stringRedisTemplate, lockScript, lockReleaseScript, () -> UUID.randomUUID().toString());
    }

    @Override
    public String acquireLock(final List<String> keys, final String storeId, final TimeUnit expirationUnit, final long expiration) {
        final List<String> keysWithStoreIdPrefix = keys.stream().map(key -> storeId + ":" + key).collect(Collectors.toList());
        final String token = tokenSupplier.get();

        final Boolean locked = stringRedisTemplate.execute(lockScript, keysWithStoreIdPrefix, token, String.valueOf(expirationUnit.toSeconds(expiration)));
        log.debug("Tried to acquire lock for keys '{}' in store '{}' with safety token '{}'. Locked: {}", keys, storeId, token, locked);
        return locked ? token : null;
    }

    @Override
    public void release(final List<String> keys, final String token, final String storeId) {
        final List<String> keysWithStoreIdPrefix = keys.stream().map(key -> storeId + ":" + key).collect(Collectors.toList());
        final long released = stringRedisTemplate.execute(lockReleaseScript, keysWithStoreIdPrefix, token);
        if (released == keys.size()) {
            log.debug("Released keys '{}' with token '{}' in store '{}'", keys, token, storeId);
        } else {
            log.error("Couldn't release all locks for keys '{}' with token '{}' in store '{}', released: {}", keys, token, storeId, released);
        }
    }
}
