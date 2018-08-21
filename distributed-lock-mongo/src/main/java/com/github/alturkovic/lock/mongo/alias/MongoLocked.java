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

package com.github.alturkovic.lock.mongo.alias;

import com.github.alturkovic.lock.Interval;
import com.github.alturkovic.lock.Locked;
import com.github.alturkovic.lock.mongo.impl.SimpleMongoLock;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import org.springframework.core.annotation.AliasFor;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Locked(type = SimpleMongoLock.class)
public @interface MongoLocked {

  @AliasFor(annotation = Locked.class)
  boolean manuallyReleased() default false;

  @AliasFor(annotation = Locked.class)
  String storeId() default "lock";

  @AliasFor(annotation = Locked.class)
  String prefix() default "";

  @AliasFor(annotation = Locked.class)
  String expression() default "#executionPath";

  @AliasFor(annotation = Locked.class)
  Interval expiration() default @Interval(value = "10", unit = TimeUnit.SECONDS);

  @AliasFor(annotation = Locked.class)
  Interval timeout() default @Interval(value = "1", unit = TimeUnit.SECONDS);

  @AliasFor(annotation = Locked.class)
  Interval retry() default @Interval(value = "50");
}
