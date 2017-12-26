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

package com.github.alturkovic.lock.redis.impl;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.redis.embedded.EmbeddedRedis;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class MultiRedisLockTest implements InitializingBean {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // false IntelliJ warning
  private StringRedisTemplate redisTemplate;

  private Lock lock;

  @Override
  public void afterPropertiesSet() {
    final DefaultRedisScript<Boolean> lockScript = new DefaultRedisScript<>();
    lockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/multilock.lua")));
    lockScript.setResultType(Boolean.class);

    final DefaultRedisScript<Boolean> lockReleaseScript = new DefaultRedisScript<>();
    lockReleaseScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/release-multilock.lua")));
    lockReleaseScript.setResultType(Boolean.class);

    lock = new MultiRedisLock(redisTemplate, lockScript, lockReleaseScript, () -> "abc");
  }

  @Before
  public void cleanRedis() {
    redisTemplate.execute((RedisCallback<?>) connection -> {
      connection.flushDb();
      return null;
    });
  }

  @Test
  public void shouldLockSingleKey() {
    final String token = lock.acquire(Collections.singletonList("1"), "locks", 1000);
    assertThat(token).isEqualTo("abc");
    assertThat(redisTemplate.opsForValue().get("locks:1")).isEqualTo("abc");
    assertThat(redisTemplate.getExpire("locks:1", TimeUnit.MILLISECONDS)).isCloseTo(1000, Offset.offset(100L));
  }

  @Test
  public void shouldLockMultipleKeys() {
    final String token = lock.acquire(Arrays.asList("1", "2"), "locks", 1000);
    assertThat(token).isEqualTo("abc");
    assertThat(redisTemplate.opsForValue().get("locks:1")).isEqualTo("abc");
    assertThat(redisTemplate.opsForValue().get("locks:2")).isEqualTo("abc");
    assertThat(redisTemplate.getExpire("locks:1", TimeUnit.MILLISECONDS)).isCloseTo(1000, Offset.offset(100L));
    assertThat(redisTemplate.getExpire("locks:2", TimeUnit.MILLISECONDS)).isCloseTo(1000, Offset.offset(100L));
  }

  @Test
  public void shouldNotLockWhenWholeLockIsTaken() {
    redisTemplate.opsForValue().set("locks:1", "def");
    redisTemplate.opsForValue().set("locks:2", "ghi");
    final String token = lock.acquire(Arrays.asList("1", "2"), "locks", 1000);
    assertThat(token).isNull();
    assertThat(redisTemplate.opsForValue().get("locks:1")).isEqualTo("def");
    assertThat(redisTemplate.opsForValue().get("locks:2")).isEqualTo("ghi");
  }

  @Test
  public void shouldNotLockWhenLockIsPartiallyTaken() {
    redisTemplate.opsForValue().set("locks:1", "def");
    final String token = lock.acquire(Arrays.asList("1", "2"), "locks", 1000);
    assertThat(token).isNull();
    assertThat(redisTemplate.opsForValue().get("locks:1")).isEqualTo("def");
    assertThat(redisTemplate.opsForValue().get("locks:2")).isNull();
  }

  @Test
  public void shouldReleaseSingleKey() {
    redisTemplate.opsForValue().set("locks:1", "abc");
    lock.release(Collections.singletonList("1"), "abc", "locks");
    assertThat(redisTemplate.opsForValue().get("locks:1")).isNull();
  }

  @Test
  public void shouldReleaseMultipleKeys() {
    redisTemplate.opsForValue().set("locks:1", "abc");
    redisTemplate.opsForValue().set("locks:2", "abc");
    lock.release(Arrays.asList("1", "2"), "abc", "locks");
    assertThat(redisTemplate.opsForValue().get("locks:1")).isNull();
    assertThat(redisTemplate.opsForValue().get("locks:2")).isNull();
  }

  @Test
  public void shouldNotReleaseWhenTokenDoesNotFullyMatch() {
    redisTemplate.opsForValue().set("locks:1", "def");
    redisTemplate.opsForValue().set("locks:2", "ghi");
    lock.release(Arrays.asList("1", "2"), "abc", "locks");
    assertThat(redisTemplate.opsForValue().get("locks:1")).isEqualTo("def");
    assertThat(redisTemplate.opsForValue().get("locks:2")).isEqualTo("ghi");
  }

  @Test
  public void shouldNotReleaseWhenTokenDoesNotPartiallyMatch() {
    redisTemplate.opsForValue().set("locks:1", "def");
    lock.release(Arrays.asList("1", "2"), "abc", "locks");
    assertThat(redisTemplate.opsForValue().get("locks:1")).isEqualTo("def");
    assertThat(redisTemplate.opsForValue().get("locks:2")).isNull();
  }

  @SpringBootApplication(
      exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class},
      scanBasePackageClasses = EmbeddedRedis.class
  )
  static class TestApplication {
  }
}
