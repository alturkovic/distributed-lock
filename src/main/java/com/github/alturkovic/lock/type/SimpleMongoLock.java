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
import com.github.alturkovic.lock.mongo.LockDocument;
import com.mongodb.WriteResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.Assert;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Data
@Slf4j
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SimpleMongoLock extends AbstractLock {

    private final MongoTemplate mongoTemplate;
    private final Supplier<String> tokenSupplier;

    public SimpleMongoLock(final MongoTemplate mongoTemplate) {
        this(mongoTemplate, () -> UUID.randomUUID().toString());
    }

    @Override
    public String acquireLock(final List<String> keys, final String storeId, final TimeUnit expirationUnit, final long expiration) {
        Assert.isTrue(keys.size() == 1, "Cannot acquire lock for multiple keys with this lock: " + keys);

        final String key = keys.get(0);
        final String token = tokenSupplier.get();

        final Query query = Query.query(Criteria.where("_id").is(key));
        final Update update = new Update()
                .setOnInsert("_id", key)
                .setOnInsert("expireAt", DateTime.now().plus(expirationUnit.toMillis(expiration)))
                .setOnInsert("token", token);
        final FindAndModifyOptions options = new FindAndModifyOptions().upsert(true).returnNew(true);

        final LockDocument doc = mongoTemplate.findAndModify(query, update, options, LockDocument.class, storeId);

        final boolean locked = doc.getToken().equals(token);
        log.debug("Tried to acquire lock for key '{}' in store '{}' with safety token '{}'. Locked: {}", key, storeId, token, locked);
        return locked ? token : null;
    }

    @Override
    public void release(final List<String> keys, final String token, final String storeId) {
        Assert.isTrue(keys.size() == 1, "Cannot release lock for multiple keys with this lock: " + keys);

        final String key = keys.get(0);

        final WriteResult result = mongoTemplate.remove(Query.query(Criteria.where("_id").is(key).and("token").is(token)), storeId);
        if (result.getN() == 1) {
            log.debug("Released key '{}' with token '{}' in store '{}', result: {}", key, token, storeId, result);
        } else {
            log.error("Couldn't release lock for key '{}' with token '{}' in store '{}', result: {}", key, token, storeId, result);
        }
    }
}
