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

package com.github.alturkovic.lock.jdbc.impl;

import com.github.alturkovic.lock.AbstractSimpleLock;
import com.github.alturkovic.lock.jdbc.service.JdbcLockSingleKeyService;
import java.util.function.Supplier;

public class SimpleJdbcLock extends AbstractSimpleLock {
  private final JdbcLockSingleKeyService lockSingleKeyService;

  public SimpleJdbcLock(final Supplier<String> tokenSupplier, final JdbcLockSingleKeyService lockSingleKeyService) {
    super(tokenSupplier);
    this.lockSingleKeyService = lockSingleKeyService;
  }

  @Override
  protected String acquire(final String key, final String storeId, final String token, final long expiration) {
    return lockSingleKeyService.acquire(key, storeId, token, expiration);
  }

  @Override
  protected boolean release(final String key, final String storeId, final String token) {
    return lockSingleKeyService.release(key, storeId, token);
  }

  @Override
  protected boolean refresh(final String key, final String storeId, final String token, final long expiration) {
    return lockSingleKeyService.refresh(key, storeId, token, expiration);
  }
}
