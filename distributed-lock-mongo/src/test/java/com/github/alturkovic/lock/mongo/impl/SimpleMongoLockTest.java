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
import java.util.Collections;
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
  public void afterPropertiesSet() {
    // instead of writing a custom test configuration, we can just initialize it after autowiring mongoTemplate with a custom tokenSupplier
    lock = new SimpleMongoLock(mongoTemplate, () -> "abc");
  }

  @Before
  public void cleanMongoCollection() {
    mongoTemplate.dropCollection("locks");
  }

  @Test
  public void shouldLock() {
    final String token = lock.acquire(Collections.singletonList("1"), "locks", 1000);
    assertThat(token).isEqualTo("abc");
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getToken()).isEqualTo("abc");
  }

  @Test
  public void shouldNotLock() {
    mongoTemplate.insert(new LockDocument("1", DateTime.now().plusMinutes(1), "def"), "locks");
    final String token = lock.acquire(Collections.singletonList("1"), "locks", 1000);
    assertThat(token).isNull();
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getToken()).isEqualTo("def");
  }

  @Test
  public void shouldRelease() {
    mongoTemplate.insert(new LockDocument("1", DateTime.now().plusMinutes(1), "abc"), "locks");
    final boolean released = lock.release(Collections.singletonList("1"), "abc", "locks");
    assertThat(released).isTrue();
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks")).isNull();
  }

  @Test
  public void shouldNotRelease() {
    mongoTemplate.insert(new LockDocument("1", DateTime.now().plusMinutes(1), "def"), "locks");
    final boolean released = lock.release(Collections.singletonList("1"), "abc", "locks");
    assertThat(released).isFalse();
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getToken()).isEqualTo("def");
  }

  @SpringBootApplication
  static class TestApplication {
  }
}