package com.example.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class RateLimiterConfig {

    /**
     * Phuc vu trang giao dien Dashboard truc quan tai http://localhost:8080/
     */
    @Bean
    public RouterFunction<ServerResponse> indexRouter() {
        return RouterFunctions.route(GET("/"), request ->
            ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .bodyValue(new ClassPathResource("static/index.html"))
        );
    }

    /**
     * Chien luoc 1 (Mac dinh): Gioi han toc do theo dia chi IP cua Client
     * Duoc tham chieu trong application.properties qua #{ipKeyResolver}
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = "127.0.0.1";
            if (exchange.getRequest().getRemoteAddress() != null) {
                ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just(ip);
        };
    }

    /**
     * Chien luoc 2: Gioi han theo User ID trong Header X-User-Id (hoac JWT Claims)
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just(userId != null ? userId : "anonymous_user");
        };
    }

    /**
     * Chien luoc 3: Gioi han theo API Key trong Header X-API-KEY (Doi tac B2B)
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");
            return Mono.just(apiKey != null ? apiKey : "free_tier_key");
        };
    }
}
