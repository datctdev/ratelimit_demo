package com.example.gateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GlobalFilter minh hoa truc tiep thuat toan Token Bucket cua thu vien Bucket4j:
 * - Capacity (Dung luong thung) = 10 tokens
 * - Refill (Toc do nap)         = Nap 1 token moi sau moi 2 giay
 */
@Component
public class Bucket4jRateLimitFilter implements GlobalFilter, Ordered {

    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    private static final int CAPACITY = 10;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Bucket createNewBucket() {
        // Nap 1 token sau moi 2 giay, dung luong thung toi da 10 tokens
        Refill refill = Refill.intervally(1, Duration.ofSeconds(2));
        Bandwidth limit = Bandwidth.classic(CAPACITY, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Endpoint ho tro Reset lai day thung 10 tokens khi Demo tren lop
        if (path.equals("/api/bucket4j/reset")) {
            bucketCache.clear();
            System.out.println("[GATEWAY - BUCKET4J] ===> Da nap day lai 10/10 tokens trong thung!");
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"message\":\"Da reset Bucket4j ve 10/10 tokens!\",\"availableTokens\":10}";
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        // Chi ap dung Bucket4j Filter cho route /api/bucket4j/**
        if (!path.startsWith("/api/bucket4j/")) {
            return chain.filter(exchange);
        }

        String clientIp = "127.0.0.1";
        if (exchange.getRequest().getRemoteAddress() != null) {
            clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        Bucket bucket = bucketCache.computeIfAbsent(clientIp, k -> createNewBucket());

        // Thu lay 1 token va kiem tra trang thai thung
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        String now = LocalTime.now().format(TIME_FMT);

        if (probe.isConsumed()) {
            long remaining = probe.getRemainingTokens();
            System.out.printf("[%s] [GATEWAY - BUCKET4J] ALLOWED (200) | Client: %s | Token con lai: %d/%d%n",
                    now, clientIp, remaining, CAPACITY);

            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
            exchange.getResponse().getHeaders().add("X-RateLimit-Burst-Capacity", String.valueOf(CAPACITY));
            return chain.filter(exchange);
        } else {
            long waitForRefillSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
            System.out.printf("[%s] [GATEWAY - BUCKET4J] BLOCKED (429) | Client: %s | HET TOKEN! Cho %ds%n",
                    now, clientIp, waitForRefillSeconds);

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            exchange.getResponse().getHeaders().add("X-RateLimit-Burst-Capacity", String.valueOf(CAPACITY));
            exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(waitForRefillSeconds));

            String errorJson = String.format(
                "{\"status\":429,\"error\":\"Too Many Requests (Blocked by Bucket4j at API Gateway)\"," +
                "\"message\":\"Thung Token da can (0/%d)! Request bi chan ngay tai Gateway, Orchid-Service duoc bao ve an toan.\"," +
                "\"clientIp\":\"%s\",\"availableTokens\":0,\"retryAfterSeconds\":%d,\"timestamp\":\"%s\"}",
                CAPACITY, clientIp, waitForRefillSeconds, now
            );

            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(errorJson.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }

    @Override
    public int getOrder() {
        return -1; // Chay truoc khi forward xuong Orchid-Service
    }
}
