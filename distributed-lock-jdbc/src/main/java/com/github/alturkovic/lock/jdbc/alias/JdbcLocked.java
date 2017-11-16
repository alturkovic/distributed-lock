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

package com.github.alturkovic.lock.jdbc.alias;

import com.github.alturkovic.lock.Interval;
import com.github.alturkovic.lock.Locked;
import com.github.alturkovic.lock.jdbc.impl.SimpleJdbcLock;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Locked(type = SimpleJdbcLock.class)
public @interface JdbcLocked {

    @AliasFor(annotation = Locked.class)
    boolean manuallyReleased() default false;

    @AliasFor(annotation = Locked.class)
    String storeId() default "lock";

    @AliasFor(annotation = Locked.class)
    String prefix() default "lock:";

    @AliasFor(annotation = Locked.class)
    String expression() default "#executionPath";

    @AliasFor(annotation = Locked.class)
    String parameter() default "p";

    @AliasFor(annotation = Locked.class)
    Interval expiration() default @Interval("10");

    @AliasFor(annotation = Locked.class)
    Interval timeout() default @Interval("1");

    @AliasFor(annotation = Locked.class)
    Interval retry() default @Interval(value = "50", unit = TimeUnit.MILLISECONDS);
}
