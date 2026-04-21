package com.paypal.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {

            // 1. Get userId from header
            String userId = exchange.getRequest()
                                    .getHeaders()
                                    .getFirst("X-User-Id");

            // 2. If userId exists → use it
            if (userId != null && !userId.isEmpty()) {
                return Mono.just("USER_" + userId);
            }

            // 3. Fallback to IP
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();

            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                String ip = remoteAddress.getAddress().getHostAddress();
                return Mono.just("IP_" + ip);
            }

            // 4. Final fallback
            return Mono.just("UNKNOWN");
        };
    }
}