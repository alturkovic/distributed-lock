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

package com.github.alturkovic.lock.redis.configuration;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.redis.impl.MultiRedisLock;
import com.github.alturkovic.lock.redis.impl.SimpleRedisLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisDistributedLockConfiguration {

  @Bean
  public Lock simpleRedisLock(final StringRedisTemplate stringRedisTemplate,
                              final RedisScript<Boolean> lockScript,
                              final RedisScript<Boolean> lockReleaseScript) {
    return new SimpleRedisLock(stringRedisTemplate, lockScript, lockReleaseScript);
  }

  @Bean
  public Lock multiRedisLock(final StringRedisTemplate stringRedisTemplate,
                             final RedisScript<Boolean> multiLockScript,
                             final RedisScript<Long> multiLockReleaseScript) {
    return new MultiRedisLock(stringRedisTemplate, multiLockScript, multiLockReleaseScript);
  }

  @Bean
  public RedisScript<Boolean> lockScript() {
    final DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/lock.lua")));
    redisScript.setResultType(Boolean.class);
    return redisScript;
  }

  @Bean
  public RedisScript<Boolean> multiLockScript() {
    final DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/multilock.lua")));
    redisScript.setResultType(Boolean.class);
    return redisScript;
  }

  @Bean
  public RedisScript<Boolean> lockReleaseScript() {
    final DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/release-lock.lua")));
    redisScript.setResultType(Boolean.class);
    return redisScript;
  }

  @Bean
  public RedisScript<Long> multiLockReleaseScript() {
    final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/release-multilock.lua")));
    redisScript.setResultType(Long.class);
    return redisScript;
  }
}
