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

package com.github.alturkovic.lock.key;

import org.aspectj.lang.JoinPoint;

import java.util.List;

/**
 * Used to generate keys to lock
 */
public interface KeyGenerator {
    /**
     * Generate keys by evaluating the given expression
     *
     * @param lockKeyPrefix         prefix to put on resolved keys
     * @param expression            key expression to evaluate
     * @param contextVariableName   parameter identifier in expression
     * @param joinPoint             join point of advised method
     *
     * @return generated or resolved keys
     */
    List<String> resolveKeys(String lockKeyPrefix, String expression, String contextVariableName, JoinPoint joinPoint);
}
