package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
        System.out.println("==================================================================");
        System.out.println(">>> API GATEWAY (PORT 8080) DA KHOI DONG THANH CONG! <<<");
        System.out.println(">>> 1. Giao dien Demo Web UI : http://localhost:8080/index.html");
        System.out.println(">>> 2. Endpoint Bucket4j     : http://localhost:8080/api/bucket4j/orchids");
        System.out.println(">>> 3. Endpoint Redis Gateway: http://localhost:8080/api/redis/orchids");
        System.out.println(">>> 4. Endpoint Unlimited    : http://localhost:8080/api/unlimited/orchids");
        System.out.println("==================================================================");
    }
}
