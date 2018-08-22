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

package com.github.alturkovic.lock.mongo.impl;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.mongo.model.LockDocument;
import com.mongodb.client.result.DeleteResult;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Data
@Slf4j
@AllArgsConstructor
public class SimpleMongoLock implements Lock {

  private final MongoTemplate mongoTemplate;
  private final Supplier<String> tokenSupplier;

  public SimpleMongoLock(final MongoTemplate mongoTemplate) {
    this(mongoTemplate, () -> UUID.randomUUID().toString());
  }

  @Override
  public String acquire(final List<String> keys, final String storeId, final long expiration) {
    Assert.isTrue(keys.size() == 1, "Cannot acquire lock for multiple keys with this lock: " + keys);

    final String key = keys.get(0);
    final String token = tokenSupplier.get();

    if (StringUtils.isEmpty(token)) {
      throw new IllegalStateException("Cannot lock with empty token");
    }

    final Query query = Query.query(Criteria.where("_id").is(key));
    final Update update = new Update()
        .setOnInsert("_id", key)
        .setOnInsert("expireAt", LocalDateTime.now().plus(expiration, ChronoUnit.MILLIS))
        .setOnInsert("token", token);
    final FindAndModifyOptions options = new FindAndModifyOptions().upsert(true).returnNew(true);

    final LockDocument doc = mongoTemplate.findAndModify(query, update, options, LockDocument.class, storeId);

    final boolean locked = doc.getToken().equals(token);
    log.debug("Tried to acquire lock for key {} with token {} in store {}. Locked: {}", key, token, storeId, locked);
    return locked ? token : null;
  }

  @Override
  public boolean release(final List<String> keys, final String token, final String storeId) {
    Assert.isTrue(keys.size() == 1, "Cannot release lock for multiple keys with this lock: " + keys);

    final String key = keys.get(0);

    final DeleteResult deleted = mongoTemplate.remove(Query.query(Criteria.where("_id").is(key).and("token").is(token)), storeId);

    final boolean released = deleted.getDeletedCount() == 1;
    if (released) {
      log.debug("Remove query successfully affected 1 record for key {} with token {} in store {}", key, token, storeId);
    } else if (deleted.getDeletedCount() > 0) {
      log.error("Unexpected result from release for key {} with token {} in store {}, released {}", key, token, storeId, deleted);
    } else {
      log.error("Remove query did not affect any records for key {} with token {} in store {}", key, token, storeId);
    }

    return released;
  }
}