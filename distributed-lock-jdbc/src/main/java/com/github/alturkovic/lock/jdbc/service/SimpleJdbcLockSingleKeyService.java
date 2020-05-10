/*
 * MIT License
 *
 * Copyright (c) 2020 Alen Turkovic
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

package com.github.alturkovic.lock.jdbc.service;

import java.util.Date;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Data
@Slf4j
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class SimpleJdbcLockSingleKeyService implements JdbcLockSingleKeyService {

  public static final String ACQUIRE_FORMATTED_QUERY = "INSERT INTO %s (lock_key, token, expireAt) VALUES (?, ?, ?);";
  public static final String RELEASE_FORMATTED_QUERY = "DELETE FROM %s WHERE lock_key = ? AND token = ?;";
  public static final String DELETE_EXPIRED_FORMATTED_QUERY = "DELETE FROM %s WHERE expireAt < ?;";
  public static final String REFRESH_FORMATTED_QUERY = "UPDATE %s SET expireAt = ? WHERE lock_key = ? AND token = ?;";

  private final JdbcTemplate jdbcTemplate;

  @Override
  public String acquire(final String key, final String storeId, final String token, final long expiration) {
    final var now = new Date();
    final var expired = jdbcTemplate.update(String.format(DELETE_EXPIRED_FORMATTED_QUERY, storeId), now);
    log.debug("Expired {} locks", expired);

    try {
      final var expireAt = new Date(now.getTime() + expiration);
      final var created = jdbcTemplate.update(String.format(ACQUIRE_FORMATTED_QUERY, storeId), key, token, expireAt);
      return created == 1 ? token : null;
    } catch (final DuplicateKeyException e) {
      return null;
    }
  }

  @Override
  public boolean release(final String key, final String storeId, final String token) {
    final var deleted = jdbcTemplate.update(String.format(RELEASE_FORMATTED_QUERY, storeId), key, token);

    final var released = deleted == 1;
    if (released) {
      log.debug("Release query successfully affected 1 record for key {} with token {} in store {}", key, token, storeId);
    } else if (deleted > 0) {
      log.error("Unexpected result from release for key {} with token {} in store {}, released {}", key, token, storeId, deleted);
    } else {
      log.error("Release query did not affect any records for key {} with token {} in store {}", key, token, storeId);
    }

    return released;
  }

  @Override
  public boolean refresh(final String key, final String storeId, final String token, final long expiration) {
    final var now = new Date();
    final var expireAt = new Date(now.getTime() + expiration);

    final var updated = jdbcTemplate.update(String.format(REFRESH_FORMATTED_QUERY, storeId), expireAt, key, token);
    final var refreshed = updated == 1;
    if (refreshed) {
      log.debug("Refresh query successfully affected 1 record for key {} with token {} in store {}", key, token, storeId);
    } else if (updated > 0) {
      log.error("Unexpected result from refresh for key {} with token {} in store {}, refreshed {}", key, token, storeId, updated);
    } else {
      log.error("Refresh query did not affect any records for key {} with token {} in store {}", key, token, storeId);
    }

    return refreshed;
  }
}
