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

package com.github.alturkovic.lock.jdbc.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Data
@Slf4j
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class SimpleJdbcLockSingleKeyService implements JdbcLockSingleKeyService {

  private static final String ACQUIRE_FORMATTED_QUERY = "INSERT INTO %s (key, token, expireAt) VALUES (?, ?, ?)";
  private static final String RELEASE_FORMATTED_QUERY = "DELETE FROM %s WHERE key = ? AND token = ?";
  private static final String DELETE_EXPIRED_FORMATTED_QUERY = "DELETE FROM %s WHERE expireAt < ?";

  private final JdbcTemplate jdbcTemplate;

  @Override
  public String acquire(final String key, final String token, final String storeId, final long expiration) {
    final Date now = new Date();
    final Date expireAt = new Date(now.getTime() + expiration);

    final int expired = jdbcTemplate.update(String.format(DELETE_EXPIRED_FORMATTED_QUERY, storeId), now);
    log.debug("Expired {} locks", expired);

    try {
      final int created = jdbcTemplate.update(String.format(ACQUIRE_FORMATTED_QUERY, storeId), key, token, expireAt);
      return created == 1 ? token : null;
    }
    catch (final DuplicateKeyException e) {
      return null;
    }
  }

  @Override
  public boolean release(final String key, final String token, final String storeId) {
    final int deleted = jdbcTemplate.update(String.format(RELEASE_FORMATTED_QUERY, storeId), key, token);

    final boolean released = deleted == 1;
    if (released) {
      log.debug("Release query successfully affected 1 record for key {} with token {} in store {}", key, token, storeId);
    } else if (deleted > 0) {
      log.error("Unexpected result from release for key {} with token {} in store {}, released {}", key, token, storeId, deleted);
    } else {
      log.error("Release query did not affect any records for key {} with token {} in store {}", key, token, storeId);
    }

    return released;
  }
}
