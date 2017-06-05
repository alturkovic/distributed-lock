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

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.mongo.LockDocument;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SimpleMongoLockTest implements InitializingBean {

    @Autowired
    private MongoTemplate mongoTemplate;

    private Lock lock;

    @Override
    public void afterPropertiesSet() throws Exception {
        // instead of writing a custom test configuration, we can just initialize it after autowiring mongoTemplate with a custom tokenSupplier
        lock = new SimpleMongoLock(mongoTemplate, () -> "abc");
    }

    @Before
    public void cleanMongoCollection() {
        mongoTemplate.dropCollection("locks");
    }

    @Test
    public void shouldLock() {
        final String token = lock.acquire(Collections.singletonList("1"), "locks", TimeUnit.MINUTES, 1, 0, TimeUnit.SECONDS, 0);
        assertThat(token).isEqualTo("abc");
        assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getToken()).isEqualTo("abc");
    }

    @Test
    public void shouldNotLock() {
        mongoTemplate.insert(new LockDocument("1", DateTime.now().plusMinutes(1), "def"), "locks");
        final String token = lock.acquire(Collections.singletonList("1"), "locks", TimeUnit.MINUTES, 1, 0, TimeUnit.SECONDS, 0);
        assertThat(token).isNull();
        assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getToken()).isEqualTo("def");
    }

    @Test
    public void shouldRelease() {
        mongoTemplate.insert(new LockDocument("1", DateTime.now().plusMinutes(1), "abc"), "locks");
        lock.release(Collections.singletonList("1"), "abc", "locks");
        assertThat(mongoTemplate.findById("1", LockDocument.class, "locks")).isNull();
    }

    @Test
    public void shouldNotRelease() {
        mongoTemplate.insert(new LockDocument("1", DateTime.now().plusMinutes(1), "def"), "locks");
        lock.release(Collections.singletonList("1"), "abc", "locks");
        assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getToken()).isEqualTo("def");
    }

    @SpringBootApplication
    static class TestApplication {
    }
}