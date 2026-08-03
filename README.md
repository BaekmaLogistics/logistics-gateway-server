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
└── GatewayApplication.java  # Gateway 메인 실행 클래스
```

---

## ⚙️ 주요 서버 설정 가이드

본 서버는 API Gateway 역할을 수행하며, 외부 요청을 수신하여 Eureka에 등록된 마이크로서비스로 라우팅합니다.

### `application.yml`
- `spring.application.name`: `gateway`
- `server.port`: `18080`
- `spring.main.web-application-type`: `reactive`
- `eureka.client.service-url.defaultZone`: `${EUREKA_SERVER_URL:http://localhost:8761/eureka/}`
- `spring.cloud.gateway.server.webflux.routes`: 서비스 라우팅 규칙 설정 (필요 시 연동 마이크로서비스 라우팅 추가 예정)

---

## 🚀 빌드 및 실행

```bash
./gradlew bootRun
```

---

## 👤 담당자 (Maintainer)
- **담당자**: kth ([pni2396@gmail.com](mailto:pni2396@gmail.com))
