# DEMO SEMINAR MSS301: BUCKET4J & SPRING CLOUD GATEWAY RATE LIMITER (FULL DOCKER COMPOSE)

> **Môn học:** MSS301 - Microservices Architecture  
> **Chủ đề Seminar:** Minh họa bằng code trong 15 phút - **Bucket4j / Gateway Rate Limiter**  
> **Đặc điểm nổi bật:**
> - **100% Docker Compose:** Chạy 1 lệnh duy nhất dựng toàn bộ 3 containers (`redis`, `orchid-service`, `api-gateway`).
> - **100% `application.properties`:** Toàn bộ cấu hình viết bằng `.properties` chuẩn theo giáo trình MSS301 (không dùng `.yml`).
> - **Giao diện Web Dashboard trực quan + Script Terminal:** Có cả giao diện mô phỏng thùng Token trực tiếp trên trình duyệt (`http://localhost:8080`) lẫn script PowerShell bắn tải tự động (`.\test_burst.ps1`).

---

## 🏗️ 1. Kiến trúc Hệ sinh thái Demo trong Docker Compose

Khi chạy `docker compose up -d --build`, hệ thống khởi tạo 3 containers kết nối trong mạng nội bộ `microservices-net`:

```
                         ┌───────────────────────────────────────────────────┐
                         │               DOCKER COMPOSE NETWORK              │
                         │                                                   │
┌───────────────┐        │  ┌─────────────────────────────────────────────┐  │
│  Web Browser  │        │  │         mss301-api-gateway (:8080)          │  │
│  / PowerShell │ ──────►│  │  • Route 1: /api/bucket4j/orchids (Bucket4j)│  │
│  (20 Req/s)   │  10 OK │  │  • Route 2: /api/redis/orchids    (Redis)   │  │
│               │◄────── │  │  • Route 3: /api/unlimited/orchids (No Limit│  │
└───────────────┘ 10x429 │  └──────────┬───────────────────────┬──────────┘  │
                         │             │ Kiểm tra              │ Chỉ 10 Req  │
                         │             ▼ Token                 ▼ hợp lệ đi qua
                         │  ┌─────────────────────┐ ┌─────────────────────┐  │
                         │  │ mss301-redis (:6379)│ │mss301-orchid-service│  │
                         │  │ Lưu Token Bucket    │ │    Backend (:8082)  │  │
                         │  └─────────────────────┘ └─────────────────────┘  │
                         └───────────────────────────────────────────────────┘
```

1. **`mss301-redis` (Port `6379`)**: Lưu trữ trạng thái Token Bucket phân tán cho `RequestRateLimiter` của Spring Cloud Gateway.
2. **`mss301-orchid-service` (Port `8082`)**: Microservice Backend quản lý danh sách hoa lan, có bộ đếm ghi nhận chính xác số lượng request thực sự đi qua được Gateway.
3. **`mss301-api-gateway` (Port `8080`)**: Cổng bảo vệ trung tâm sử dụng cả **Bucket4j** và **Spring Cloud Gateway Redis RateLimiter**, cấu hình thuần bằng `application.properties`.

---

## 🚀 2. Cách Khởi động Toàn bộ Hệ thống (Chỉ 1 lệnh)

Mở Terminal / PowerShell tại thư mục `demo_ratelimiter` và chạy:

```bash
cd demo_ratelimiter
docker compose up -d --build
```

Kiểm tra 3 containers đã chạy thành công:
```bash
docker compose ps
```
*(Đợi khoảng 10–15 giây để `mss301-api-gateway` và `mss301-orchid-service` khởi động hoàn tất).*

---

## 🎯 3. Kịch bản Trình diễn Live Demo 15 Phút trên lớp (Cực kỳ trực quan!)

Nhóm có thể biểu diễn bằng **2 cách song song** trên máy chiếu để thầy và cả lớp nhìn thấy rõ ràng nhất:

### Cách 1: Demo trên Giao diện Web Mô phỏng Thùng Token (Khuyên dùng trên máy chiếu)
1. Mở trình duyệt truy cập: **`http://localhost:8080`**
2. Trên màn hình sẽ hiển thị **Thùng chứa Token (Token Bucket)** với **10 đồng xu Token vàng (`10 / 10`)**.
3. **Bước 1 (Test đơn lẻ):** Nhấn nút xanh **"🟢 Gửi 1 Request Đơn Lẻ"** $\rightarrow$ Cả lớp thấy 1 đồng xu biến mất (`9 / 10`), bảng log hiện `200 OK`, `Orchid-Service` tăng bộ đếm lên `1`.
4. **Bước 2 (Test tấn công Burst 20 Requests):** Nhấn nút **"🔄 Reset Đầy Thùng Token"**, sau đó nhấn nút đỏ **"🔥 Bắn 20 Requests Liên Tiếp (Burst Attack!)"**:
   - **Requests #1 đến #10:** Lấy hết 10 đồng xu trong thùng $\rightarrow$ Hiện nhãn xanh **`200 OK (Allowed)`**.
   - **Requests #11 đến #20:** Thùng báo đỏ **"HẾT TOKEN (0 / 10)"** $\rightarrow$ Toàn bộ 10 requests sau bị Gateway chặn đứng ngay lập tức với nhãn đỏ **`429 TOO MANY REQ`**!
   - Nhìn vào ô thống kê **"ORCHID-SERVICE XỬ LÝ"**: Con số dừng lại đúng ở **10** (Chứng minh 10 requests quá tải đã bị chặn ở cửa ngõ Gateway, không chạm được vào `Orchid-Service`!).
5. **Bước 3 (So sánh khi KHÔNG có Rate Limiter):** Chuyển sang tab **"Chế độ 3: Không bảo vệ (Unlimited)"** rồi bấm bắn 20 requests $\rightarrow$ Cả 20 requests đều tràn thẳng vào `Orchid-Service`!

---

### Cách 2: Demo bằng Script PowerShell & Xem Docker Logs
Mở PowerShell tại thư mục `demo_ratelimiter` và chạy:

```powershell
# Test chế độ Bucket4j (Mặc định)
.\test_burst.ps1 -Mode bucket4j

# Hoặc test chế độ Spring Cloud Gateway + Redis
.\test_burst.ps1 -Mode redis
```

**Xem bằng chứng trong Docker Logs:**
```bash
# Xem log chặn của API Gateway:
docker logs mss301-api-gateway --tail 25

# Xem log của Orchid-Service (Chỉ nhận đúng 10 requests, không hề bị quá tải!):
docker logs mss301-orchid-service --tail 25
```

---

## ⚙️ 4. Giải thích Cấu hình `application.properties` (Không dùng `.yml`)

Toàn bộ cấu hình trong `api-gateway/src/main/resources/application.properties` được viết tường minh:

```properties
server.port=8080
spring.application.name=api-gateway

# Kết nối Redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
orchid.service.url=${ORCHID_SERVICE_URL:http://localhost:8082}

# Cấu hình Route sử dụng RequestRateLimiter + Redis
spring.cloud.gateway.routes[0].id=redis-ratelimit-route
spring.cloud.gateway.routes[0].uri=${orchid.service.url}
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/redis/orchids/**
spring.cloud.gateway.routes[0].filters[0]=StripPrefix=2
spring.cloud.gateway.routes[0].filters[1]=PrefixPath=/api
spring.cloud.gateway.routes[0].filters[2].name=RequestRateLimiter
spring.cloud.gateway.routes[0].filters[2].args.key-resolver=#{@ipKeyResolver}
spring.cloud.gateway.routes[0].filters[2].args.redis-rate-limiter.replenishRate=1
spring.cloud.gateway.routes[0].filters[2].args.redis-rate-limiter.burstCapacity=10
spring.cloud.gateway.routes[0].filters[2].args.redis-rate-limiter.requestedTokens=1
```

---

## 📂 5. Cấu trúc Thư mục Dự án

```
demo_ratelimiter/
├── docker-compose.yml                  # File Docker Compose dựng cả 3 containers
├── test_burst.ps1                      # Script bắn 20 requests tự động trên PowerShell
├── README.md                           # Tài liệu hướng dẫn chi tiết
├── orchid-service/                     # Microservice Backend (Port 8082)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/orchid/OrchidServiceApplication.java
│       └── resources/application.properties
└── api-gateway/                        # Spring Cloud Gateway + Bucket4j (Port 8080)
    ├── Dockerfile
    ├── pom.xml
    └── src/main/
        ├── java/com/example/gateway/
        │   ├── ApiGatewayApplication.java
        │   ├── config/RateLimiterConfig.java
        │   └── filter/Bucket4jRateLimitFilter.java
        └── resources/
            ├── application.properties  # Cấu hình hoàn toàn bằng .properties
            └── static/index.html       # Giao diện Dashboard mô phỏng thùng Token
```

### Tắt hệ thống sau khi Demo xong:
```bash
docker compose down
```
