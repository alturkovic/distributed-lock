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

package com.github.alturkovic.lock.redis.impl;

import com.github.alturkovic.lock.Lock;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class SimpleRedisLockTest implements InitializingBean {

  @Container
  @ServiceConnection
  private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:alpine")).withExposedPorts(6379);

  @Autowired
  private StringRedisTemplate redisTemplate;

  private Lock lock;

  @Override
  public void afterPropertiesSet() {
    lock = new SimpleRedisLock(() -> "abc", redisTemplate);
  }

  @BeforeEach
  public void cleanRedis() {
    redisTemplate.execute((RedisCallback<?>) connection -> {
      connection.flushDb();
      return null;
    });
  }

  @Test
  public void shouldLock() {
    final String token = lock.acquire(Collections.singletonList("1"), "locks", 1000);
    assertThat(token).isEqualTo("abc");
    assertThat(redisTemplate.opsForValue().get("locks:1")).isEqualTo("abc");
    assertThat(redisTemplate.getExpire("locks:1", TimeUnit.MILLISECONDS)).isCloseTo(1000, Offset.offset(100L));
  }

  @Test
  public void shouldNotLock() {
    redisTemplate.opsForValue().set("locks:1", "def");
    final String token = lock.acquire(Collections.singletonList("1"), "locks", 1000);
    assertThat(token).isNull();
    assertThat(redisTemplate.opsForValue().get("locks:1")).isEqualTo("def");
  }

  @Test
  public void shouldRelease() {
    redisTemplate.opsForValue().set("locks:1", "abc");
    lock.release(Collections.singletonList("1"), "locks", "abc");
    assertThat(redisTemplate.opsForValue().get("locks:1")).isNull();
  }

  @Test
  public void shouldNotRelease() {
    redisTemplate.opsForValue().set("locks:1", "def");
    lock.release(Collections.singletonList("1"), "locks", "abc");
    assertThat(redisTemplate.opsForValue().get("locks:1")).isEqualTo("def");
  }

  @Test
  public void shouldRefresh() throws InterruptedException {
    final String token = lock.acquire(Collections.singletonList("1"), "locks", 1000);
    assertThat(redisTemplate.getExpire("locks:1", TimeUnit.MILLISECONDS)).isCloseTo(1000, Offset.offset(100L));
    Thread.sleep(500);
    assertThat(redisTemplate.getExpire("locks:1", TimeUnit.MILLISECONDS)).isCloseTo(500, Offset.offset(100L));
    assertThat(lock.refresh(Collections.singletonList("1"), "locks", token, 1000)).isTrue();
    assertThat(redisTemplate.getExpire("locks:1", TimeUnit.MILLISECONDS)).isCloseTo(1000, Offset.offset(100L));
  }

  @Test
  public void shouldNotRefreshBecauseKeyExpired() {
    assertThat(lock.refresh(Collections.singletonList("1"), "locks", "abc", 1000)).isFalse();
    assertThat(redisTemplate.keys("*")).isNullOrEmpty();
  }

  @Test
  public void shouldNotRefreshBecauseTokenDoesNotMatch() throws InterruptedException {
    lock.acquire(Collections.singletonList("1"), "locks", 1000);
    assertThat(redisTemplate.getExpire("locks:1", TimeUnit.MILLISECONDS)).isCloseTo(1000, Offset.offset(100L));
    Thread.sleep(500);
    assertThat(redisTemplate.getExpire("locks:1", TimeUnit.MILLISECONDS)).isCloseTo(500, Offset.offset(100L));
    assertThat(lock.refresh(Collections.singletonList("1"), "locks", "wrong-token", 1000)).isFalse();
    Thread.sleep(500);
    assertThat(redisTemplate.opsForValue().get("locks:1")).isNull();
  }

  @Test
  public void shouldExpire() throws InterruptedException {
    lock.acquire(Collections.singletonList("1"), "locks", 100);
    Thread.sleep(100);
    assertThat(redisTemplate.opsForValue().get("locks:1")).isNull();
  }

  @SpringBootApplication
  static class TestApplication {}
}
