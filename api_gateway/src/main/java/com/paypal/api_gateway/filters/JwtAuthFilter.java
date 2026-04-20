package com.paypal.api_gateway.filters;

import com.paypal.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/signup",
            "/auth/login"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getPath().value();
        String normalizedPath = path.replaceAll("/+$", "");

        if (PUBLIC_PATHS.stream().anyMatch(normalizedPath::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing/Invalid Authorization header for path: {}", path);
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        try {
            String token = authHeader.substring(7);

            Claims claims = JwtUtil.validateToken(token);

            String userEmail = claims.getSubject();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-User-Email", userEmail)
                            .build())
                    .build();

            log.info("JWT validated for user: {}", userEmail);

            return chain.filter(mutatedExchange);

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            return unauthorized(exchange, "Token expired");

        } catch (JwtException e) {
            log.warn("JWT invalid: {}", e.getMessage());
            return unauthorized(exchange, "Invalid token");

        } catch (Exception e) {
            log.error("Unexpected error: ", e);
            return unauthorized(exchange, "Authentication failed");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        var buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}