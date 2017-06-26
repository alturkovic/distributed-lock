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

import com.github.alturkovic.lock.AbstractLock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Works the same way as {@link MultiRedisLock} but is optimized better to work with a single key.
 */
@Data
@Slf4j
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SimpleRedisLock extends AbstractLock {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Boolean> lockScript;
    private final RedisScript<Boolean> lockReleaseScript;
    private final Supplier<String> tokenSupplier;

    public SimpleRedisLock(final StringRedisTemplate stringRedisTemplate, final RedisScript<Boolean> lockScript, final RedisScript<Boolean> lockReleaseScript) {
        this(stringRedisTemplate, lockScript, lockReleaseScript, () -> UUID.randomUUID().toString());
    }

    @Override
    public String acquireLock(final List<String> keys, final String storeId, final long expiration) {
        Assert.isTrue(keys.size() == 1, "Cannot acquire lock for multiple keys with this lock: " + keys);
        final List<String> singletonKeyList = Collections.singletonList(storeId + ":" + keys.get(0));

        final String token = tokenSupplier.get();
        final Boolean locked = stringRedisTemplate.execute(lockScript, singletonKeyList, token, String.valueOf(expiration));
        log.debug("Tried to acquire lock for key '{}' in store '{}' with safety token '{}'. Locked: {}", keys.get(0), storeId, token, locked);
        return locked ? token : null;
    }

    @Override
    public void release(final List<String> keys, final String token, final String storeId) {
        Assert.isTrue(keys.size() == 1, "Cannot release lock for multiple keys with this lock: " + keys);
        final List<String> singletonKeyList = Collections.singletonList(storeId + ":" + keys.get(0));

        final Boolean released = stringRedisTemplate.execute(lockReleaseScript, singletonKeyList, token);
        if (released) {
            log.debug("Released key '{}' with token '{}' in store '{}'", keys.get(0), token, storeId);
        } else {
            log.error("Couldn't release lock for key '{}' with token '{}' in store '{}'", keys.get(0), token, storeId);
        }
    }
}