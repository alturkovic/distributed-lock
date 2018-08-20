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

package com.github.alturkovic.lock.redis.script;

import com.github.alturkovic.lock.redis.embedded.EmbeddedRedis;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class RedisReleaseLockScriptTest implements InitializingBean {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // false IntelliJ warning
  private StringRedisTemplate redisTemplate;

  private RedisScript<Boolean> releaseLockScript;

  @Override
  public void afterPropertiesSet() {
    final DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/release-lock.lua")));
    redisScript.setResultType(Boolean.class);

    releaseLockScript = redisScript;
  }

  @Before
  public void cleanRedis() {
    redisTemplate.execute((RedisCallback<?>) connection -> {
      connection.flushDb();
      return null;
    });
  }

  @Test
  public void shouldRelease() {
    redisTemplate.opsForValue().set("lock:test", "token");
    final Boolean released = redisTemplate.execute(releaseLockScript, Collections.singletonList("lock:test"), "token");
    assertThat(released).isTrue();
    assertThat(redisTemplate.opsForValue().get("lock:test")).isNull();
  }

  @Test
  public void shouldNotReleaseWithDifferentToken() {
    redisTemplate.opsForValue().set("lock:test", "exists");
    final Boolean released = redisTemplate.execute(releaseLockScript, Collections.singletonList("lock:test"), "token");
    assertThat(released).isFalse();
    assertThat(redisTemplate.opsForValue().get("lock:test")).isEqualTo("exists");
  }

  @Test
  public void shouldNotReleaseNotExistingKey() {
    final Boolean released = redisTemplate.execute(releaseLockScript, Collections.singletonList("lock:test"), "token");
    assertThat(released).isFalse();
  }

  @SpringBootApplication(scanBasePackageClasses = EmbeddedRedis.class)
  static class TestApplication {
  }
}
