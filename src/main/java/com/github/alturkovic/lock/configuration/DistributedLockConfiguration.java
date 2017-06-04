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

package com.github.alturkovic.lock.configuration;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.advice.LockAdvice;
import com.github.alturkovic.lock.key.KeyGenerator;
import com.github.alturkovic.lock.key.SpelKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@EnableAspectJAutoProxy
public class DistributedLockConfiguration {

    @Bean
    public LockAdvice lockAdvice(final KeyGenerator spelKeyGenerator, final List<Lock> locks) {
        return new LockAdvice(spelKeyGenerator, locks.stream().collect(Collectors.toMap(Lock::getClass, Function.identity())));
    }

    @Bean
    public KeyGenerator spelKeyGenerator() {
        return new SpelKeyGenerator();
    }
}
