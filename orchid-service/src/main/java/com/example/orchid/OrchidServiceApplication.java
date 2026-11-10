package com.example.orchid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
@RestController
@RequestMapping("/api/orchids")
public class OrchidServiceApplication {

    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void main(String[] args) {
        SpringApplication.run(OrchidServiceApplication.class, args);
        System.out.println("==========================================================");
        System.out.println(">>> ORCHID-SERVICE (BACKEND) STARTED ON PORT 8082 <<<");
        System.out.println("==========================================================");
    }

    @GetMapping
    public Map<String, Object> getAllOrchids() {
        int count = requestCounter.incrementAndGet();
        String now = LocalTime.now().format(TIME_FMT);
        System.out.printf("[%s] [ORCHID-SERVICE] Da xu ly Request #%d thanh cong!%n", now, count);

        return Map.of(
            "service", "Orchid-Service (Port 8082)",
            "backendProcessedCount", count,
            "timestamp", now,
            "status", "200 OK - Request da vuot qua Gateway thanh cong!",
            "data", List.of(
                Map.of("id", 1, "name", "Lan Ho Diep Trang (Phalaenopsis)", "price", 450000),
                Map.of("id", 2, "name", "Lan Phi Diep Tim (Dendrobium)", "price", 1200000),
                Map.of("id", 3, "name", "Lan Cattleya Hoang Hau", "price", 850000)
            )
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOrchidById(@PathVariable int id) {
        int count = requestCounter.incrementAndGet();
        String now = LocalTime.now().format(TIME_FMT);
        System.out.printf("[%s] [ORCHID-SERVICE] Da xu ly Request #%d (Orchid ID=%d) thanh cong!%n", now, count, id);

        return Map.of(
            "service", "Orchid-Service (Port 8082)",
            "backendProcessedCount", count,
            "timestamp", now,
            "orchid", Map.of(
                "id", id,
                "name", "Lan Ho Diep Dot Bien #" + id,
                "price", 500000,
                "stock", 25
            )
        );
    }

    @PostMapping("/reset")
    public Map<String, Object> resetCounter() {
        requestCounter.set(0);
        System.out.println("[ORCHID-SERVICE] ===> Da reset bo dem request ve 0!");
        return Map.of("message", "Reset counter to 0", "backendProcessedCount", 0);
    }
}
