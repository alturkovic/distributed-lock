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

package com.github.alturkovic.lock.redis.embedded;

import java.io.IOException;
import java.net.ServerSocket;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;
import redis.embedded.RedisServer;

@Component
public class EmbeddedRedis {

  private RedisServer server;

  @PostConstruct
  public void start() throws IOException {
    // find free port
    final ServerSocket serverSocket = new ServerSocket(0);
    final int port = serverSocket.getLocalPort();
    serverSocket.close();

    server = RedisServer.builder().setting("bind 127.0.0.1").port(port).build(); // bind to ignore windows firewall popup each time the server starts
    server.start();

    final LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", port));
    connectionFactory.setDatabase(0);
    connectionFactory.afterPropertiesSet();

    // set the property so that RedisAutoConfiguration picks up the right port
    System.setProperty("spring.redis.port", String.valueOf(port));
  }

  @PreDestroy
  public void stop() {
    server.stop();
    System.clearProperty("spring.redis.port");
  }
}