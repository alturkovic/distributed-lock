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

package com.github.alturkovic.lock.redis.script;

import com.github.alturkovic.lock.redis.embedded.EmbeddedRedis;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RedisReleaseMultiLockScriptTest implements InitializingBean {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // false IntelliJ warning
  private StringRedisTemplate redisTemplate;

  private RedisScript<Long> releaseLockScript;

  @Override
  public void afterPropertiesSet() throws Exception {
    final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/release-multilock.lua")));
    redisScript.setResultType(Long.class);

    releaseLockScript = redisScript;
  }

  @Before
  public void cleanRedis() throws IOException {
    redisTemplate.execute((RedisCallback<?>) connection -> {
      connection.flushDb();
      return null;
    });
  }

  @Test
  public void shouldReleaseSingleKey() {
    redisTemplate.opsForValue().set("lock:test", "token");
    final Long released = redisTemplate.execute(releaseLockScript, Collections.singletonList("lock:test"), "token");
    assertThat(released).isEqualTo(1);
    assertThat(redisTemplate.opsForValue().get("lock:test")).isNull();
  }

  @Test
  public void shouldReleaseMultipleKeys() {
    redisTemplate.opsForValue().set("lock:test", "token");
    redisTemplate.opsForValue().set("lock:another", "token");
    final Long released = redisTemplate.execute(releaseLockScript, Arrays.asList("lock:test", "lock:another"), "token");
    assertThat(released).isEqualTo(2);
    assertThat(redisTemplate.opsForValue().get("lock:test")).isNull();
    assertThat(redisTemplate.opsForValue().get("lock:another")).isNull();
  }

  @Test
  public void shouldNotReleaseWithDifferentToken() {
    redisTemplate.opsForValue().set("lock:test", "exists");
    final Long released = redisTemplate.execute(releaseLockScript, Collections.singletonList("lock:test"), "token");
    assertThat(released).isZero();
    assertThat(redisTemplate.opsForValue().get("lock:test")).isEqualTo("exists");
  }

  @Test
  public void shouldNotReleaseNotExistingKey() {
    final Long released = redisTemplate.execute(releaseLockScript, Collections.singletonList("lock:test"), "token");
    assertThat(released).isZero();
  }

  @SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class},
      scanBasePackageClasses = EmbeddedRedis.class)
  static class TestApplication {
  }
}
