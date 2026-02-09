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

package com.github.alturkovic.lock.mongo.impl;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.mongo.model.LockDocument;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class SimpleMongoLockTest implements InitializingBean {

  @Container
  @ServiceConnection
  private static final MongoDBContainer mongo = new MongoDBContainer("mongo:latest").withExposedPorts(27017);

  @Autowired
  private MongoTemplate mongoTemplate;

  private Lock lock;

  @Override
  public void afterPropertiesSet() {
    // instead of writing a custom test configuration, we can just initialize it after autowiring mongoTemplate with a custom tokenSupplier
    lock = new SimpleMongoLock(() -> "abc", mongoTemplate);
  }

  @BeforeEach
  public void cleanMongoCollection() {
    mongoTemplate.dropCollection("locks");
  }

  @Test
  public void shouldLock() {
    final LocalDateTime expectedExpiration = LocalDateTime.now().plus(1000, ChronoUnit.MILLIS);

    final String token = lock.acquire(Collections.singletonList("1"), "locks", 1000);
    assertThat(token).isEqualTo("abc");

    final LockDocument document = mongoTemplate.findById("1", LockDocument.class, "locks");
    assertThat(document.getToken()).isEqualTo("abc");
    assertThat(document.getExpireAt()).isCloseTo(expectedExpiration, new TemporalUnitWithinOffset(100, ChronoUnit.MILLIS));
  }

  @Test
  public void shouldNotLock() {
    mongoTemplate.insert(new LockDocument("1", LocalDateTime.now().plusMinutes(1), "def"), "locks");

    final String token = lock.acquire(Collections.singletonList("1"), "locks", 1000);
    assertThat(token).isNull();
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getToken()).isEqualTo("def");
  }

  @Test
  public void shouldRelease() {
    mongoTemplate.insert(new LockDocument("1", LocalDateTime.now().plusMinutes(1), "abc"), "locks");

    final boolean released = lock.release(Collections.singletonList("1"), "locks", "abc");
    assertThat(released).isTrue();
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks")).isNull();
  }

  @Test
  public void shouldNotRelease() {
    mongoTemplate.insert(new LockDocument("1", LocalDateTime.now().plusMinutes(1), "def"), "locks");

    final boolean released = lock.release(Collections.singletonList("1"), "locks", "abc");
    assertThat(released).isFalse();
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getToken()).isEqualTo("def");
  }

  @Test
  public void shouldRefresh() throws InterruptedException {
    LocalDateTime expectedExpiration = LocalDateTime.now().plus(1000, ChronoUnit.MILLIS);

    final String token = lock.acquire(Collections.singletonList("1"), "locks", 1000);
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getExpireAt()).isCloseTo(expectedExpiration, new TemporalUnitWithinOffset(100, ChronoUnit.MILLIS));
    Thread.sleep(500);
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getExpireAt()).isCloseTo(expectedExpiration, new TemporalUnitWithinOffset(100, ChronoUnit.MILLIS));
    expectedExpiration = LocalDateTime.now().plus(1000, ChronoUnit.MILLIS);
    assertThat(lock.refresh(Collections.singletonList("1"), "locks", token, 1000)).isTrue();
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getExpireAt()).isCloseTo(expectedExpiration, new TemporalUnitWithinOffset(100, ChronoUnit.MILLIS));
  }

  @Test
  public void shouldNotRefreshBecauseTokenDoesNotMatch() {
    final LocalDateTime expectedExpiration = LocalDateTime.now().plus(1000, ChronoUnit.MILLIS);

    lock.acquire(Collections.singletonList("1"), "locks", 1000);
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getExpireAt()).isCloseTo(expectedExpiration, new TemporalUnitWithinOffset(100, ChronoUnit.MILLIS));
    assertThat(lock.refresh(Collections.singletonList("1"), "locks", "wrong-token", 1000)).isFalse();
    assertThat(mongoTemplate.findById("1", LockDocument.class, "locks").getExpireAt()).isCloseTo(expectedExpiration, new TemporalUnitWithinOffset(100, ChronoUnit.MILLIS));
  }

  @Test
  public void shouldNotRefreshBecauseKeyExpired() {
    assertThat(lock.refresh(Collections.singletonList("1"), "locks", "abc", 1000)).isFalse();
    assertThat(mongoTemplate.findAll(LockDocument.class)).isNullOrEmpty();
  }

  @SpringBootApplication
  static class TestApplication {}
}
