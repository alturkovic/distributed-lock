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

package com.github.alturkovic.lock.embedded;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Component;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;

@Component
public class EmbeddedRedis {

    private RedisServer server;

    @PostConstruct
    public void start() throws IOException {
        // find free port
        final ServerSocket serverSocket = new ServerSocket(0);
        final Integer port = serverSocket.getLocalPort();
        serverSocket.close();

        server = RedisServer.builder().setting("bind 127.0.0.1").port(port).build(); // bind to ignore windows firewall popup each time the server starts
        server.start();

        final JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
        connectionFactory.setPort(port);
        connectionFactory.setHostName("localhost");
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