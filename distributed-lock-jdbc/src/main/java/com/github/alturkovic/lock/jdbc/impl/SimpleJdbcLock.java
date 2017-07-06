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

package com.github.alturkovic.lock.jdbc.impl;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.jdbc.service.JdbcLockSingleKeyService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Data
@Slf4j
@AllArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class SimpleJdbcLock implements Lock {

    private final JdbcLockSingleKeyService lockSingleKeyService;
    private final Supplier<String> tokenSupplier;

    public SimpleJdbcLock(final JdbcLockSingleKeyService lockSingleKeyService) {
        this(lockSingleKeyService, () -> UUID.randomUUID().toString());
    }

    @Override
    public String acquire(final List<String> keys, final String storeId, final long expiration) {
        Assert.isTrue(keys.size() == 1, "Cannot acquire lock for multiple keys with this lock: " + keys);

        final String key = keys.get(0);
        final String token = tokenSupplier.get();

        return lockSingleKeyService.acquire(key, token, storeId, expiration);
    }

    @Override
    public void release(final List<String> keys, final String token, final String storeId) {
        Assert.isTrue(keys.size() == 1, "Cannot release lock for multiple keys with this lock: " + keys);
        lockSingleKeyService.release(keys.get(0), token, storeId);
    }
}
