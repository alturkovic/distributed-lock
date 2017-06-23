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

package com.github.alturkovic.lock.annotation.alias;

import com.github.alturkovic.lock.annotation.Locked;
import com.github.alturkovic.lock.type.SimpleMongoLock;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Locked(type = SimpleMongoLock.class)
public @interface MongoLocked {

    @AliasFor(annotation = Locked.class)
    boolean manuallyReleased() default false;

    @AliasFor(annotation = Locked.class)
    String typeSpecificStoreId() default "lock";

    @AliasFor(annotation = Locked.class)
    String prefix() default "lock:";

    @AliasFor(annotation = Locked.class)
    String expression() default "";

    @AliasFor(annotation = Locked.class)
    String parameter() default "p";

    @AliasFor(annotation = Locked.class)
    TimeUnit expirationTimeUnit() default TimeUnit.SECONDS;

    @AliasFor(annotation = Locked.class)
    long expire() default 10;

    @AliasFor(annotation = Locked.class)
    TimeUnit timeoutTimeUnit() default TimeUnit.SECONDS;

    @AliasFor(annotation = Locked.class)
    long timeout() default 1000;

    @AliasFor(annotation = Locked.class)
    long retryMillis() default 50;
}
