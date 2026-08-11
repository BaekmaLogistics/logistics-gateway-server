# Logistics Gateway Service

스파르타 물류 시스템(Sparta Logistics System)의 API Gateway로서 클라이언트 요청의 라우팅, 로드 밸런싱 및 진입점 역할을 담당하는 **Spring Cloud Gateway (WebFlux)** 레포지토리입니다.

---

## 🛠 주요 기술 스택 & 포함된 설정
- **Java**: 17
- **Framework**: Spring Boot 3.5.14
- **Spring Cloud**: Spring Cloud Gateway Server WebFlux (2025.0.0)
- **Service Discovery**: Spring Cloud Netflix Eureka Client
- **Server Port**: 18080

---

## 📁 프로젝트 패키지 구조
```text
src/main/java/com/sparta/gateway
├── GatewayApplication.java      # Gateway 메인 실행 클래스
├── code/                        # 응답 코드 및 에러 코드 정의
├── filter/                      # 커스텀 필터 (인증, 헤더 처리 등)
└── util/                        # JWT 유틸리티 클래스
```

---

## ⚙️ 주요 서버 설정 가이드

본 서버는 API Gateway 역할을 수행하며, 외부 요청을 수신하여 Eureka에 등록된 마이크로서비스로 라우팅합니다.

### `application.yml`
- `spring.application.name`: `gateway`
- `server.port`: `18080`
- `spring.main.web-application-type`: `reactive`
- `eureka.client.service-url.defaultZone`: `${EUREKA_SERVER_URL:http://localhost:8761/eureka/}`

---

## 🔄 라우팅 및 요청 처리 구조

### 1. 라우팅 규칙
API Gateway는 클라이언트의 요청을 받아 Eureka 서버에 등록된 각 마이크로서비스로 전달합니다. 주요 라우팅 경로는 다음과 같습니다.

| 서비스명 | 경로 (Path) | 필터 적용 여부 |
| :--- | :--- | :--- |
| **auth-service** | `/api/v1/auth/**`, `/api/v1/delivery-managers/**` | - |
| **notification-service** | `/api/v1/slack-messages/**` | JwtAuthenticationFilter |
| **user-service** | `/api/v1/users/**` | JwtAuthenticationFilter |
| **delivery-service** | `/api/v1/deliveries/**` | JwtAuthenticationFilter |
| **company-service** | `/api/v1/companies/**`, `/api/v1/products/**` | JwtAuthenticationFilter |
| **order-service** | `/api/v1/orders/**` | JwtAuthenticationFilter |
| **hubs-service** | `/api/v1/hubs/**`, `/api/v1/hub-routes/**`, `/api/v1/hub-inventories/**` | JwtAuthenticationFilter |

### 2. 내부 요청 헤더 구조 (Header Mutation)
인증이 필요한 요청의 경우, `JwtAuthenticationFilter`를 통해 JWT 토큰을 검증합니다. 검증이 완료되면 게이트웨이는 원본 `Authorization` 헤더를 제거하고, 하위 마이크로서비스에서 사용자 정보를 쉽게 사용할 수 있도록 다음과 같은 헤더를 주입하여 전달합니다.

- **X-User-Id**: 토큰에서 추출한 사용자 식별자 (Subject)
- **X-User-Role**: 토큰에서 추출한 사용자 권한 정보
- **X-User-Name**: 토큰에서 추출한 사용자 이름

이 구조를 통해 하위 서비스들은 별도의 토큰 검증 로직 없이도 사용자 정보를 헤더를 통해 안전하게 획득할 수 있습니다.

---

## 🚀 빌드 및 실행

```bash
./gradlew bootRun
```

---

## 👤 담당자 (Maintainer)
- **담당자**: kth ([pni2396@gmail.com](mailto:pni2396@gmail.com))
