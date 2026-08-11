package com.sparta.gateway.filter.custom;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.sparta.gateway.code.ErrorResponseCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Value("${jwt.secret.key}")
    private String secretKeyString;

    @Autowired
    private WireMockServer wireMockServer;

    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    // 테스트용 JWT 토큰 생성
    private String createTestToken(String userId, String role, String username, long expirationMillis) {
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secretKey)
                .compact();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 BEARER_NOT_FOUND 에러를 반환한다")
    void missingAuthorizationHeader_returns401() {
        webTestClient.get()
                .uri("/api/v1/orders/1")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo(ErrorResponseCode.BEARER_NOT_FOUND.getErrorCode())
                .jsonPath("$.message").isEqualTo(ErrorResponseCode.BEARER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer로 시작하지 않으면 BAD_JWT_FORMAT 에러를 반환한다")
    void invalidBearerFormat_returns401() {
        webTestClient.get()
                .uri("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Basic invalid_token_format")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo(ErrorResponseCode.BAD_JWT_FORMAT.getErrorCode())
                .jsonPath("$.message").isEqualTo(ErrorResponseCode.BAD_JWT_FORMAT.getMessage());
    }

    @Test
    @DisplayName("만료된 JWT 토큰으로 요청하면 INVALID_TOKEN 에러를 반환한다")
    void expiredJwtToken_returns401() {
        // 이미 만료된 토큰 생성 (-1000ms)
        String expiredToken = createTestToken("100", "MASTER", "sparta_user", -1000L);

        webTestClient.get()
                .uri("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo(ErrorResponseCode.INVALID_TOKEN.getErrorCode())
                .jsonPath("$.message").isEqualTo(ErrorResponseCode.INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 인증이 성공하면 401(Unauthorized)을 통과하여 다음 라우팅 단계로 진입한다")
    void validJwtToken_passesAuthentication() {
        // 유효기간 10분짜리 정상 토큰 생성
        String validToken = createTestToken("100", "MASTER", "sparta_user", 600000L);

        webTestClient.get()
                .uri("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                // 해커가 보낸 조작된 헤더 (HeaderSanitizationGlobalFilter 및 JwtAuthenticationFilter 동작 검증)
                .header("X-User-Id", "HACKER_999")
                .header("X-User-Role", "SUPER_ADMIN_HACK")
                .exchange()
                .expectStatus().value(status -> {
                    // Gateway 필터 단에서 401로 차단되지 않고 라우팅 단계(통과 후 503/404 등)까지 진입했는지 확인
                    assert status != HttpStatus.UNAUTHORIZED.value();
                });
    }

    @Test
    @DisplayName("인증 필터가 적용되지 않은 /api/v1/auth/** 경로는 헤더 없이도 인증을 통과한다")
    void publicAuthRoute_bypassesJwtFilter() {
        webTestClient.get()
                .uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().value(status -> {
                    // 인증 필터를 안 타므로 401이 발생하지 않음
                    assert status != HttpStatus.UNAUTHORIZED.value();
                });
    }

    @Test
    @DisplayName("필터 통과 후 Downstream으로 전달되는 요청에서 Authorization은 제거되고 X-User-* 헤더가 주입된다")
    void validJwtToken_mutatesHeadersCorrectlyForDownstream() {
        // WireMock가 받을 가짜 Downstream 응답 설정
        stubFor(WireMock.get(urlEqualTo("/api/v1/orders/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"SUCCESS\"}")));

        String validToken = createTestToken("100", "MASTER", "sparta_user", 600000L);

        // Gateway로 요청 전송 (해커가 임의로 주입한 외부 헤더 포함)
        webTestClient.get()
                .uri("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .header("X-User-Id", "HACKER_999") // 스푸핑 시도 헤더
                .exchange()
                .expectStatus().isOk();

        // WireMock(가짜 Downstream)이 전달받은 Request Header 검사
        WireMock.verify(getRequestedFor(urlEqualTo("/api/v1/orders/1"))
                // 1. JWT Authorization 헤더가 완전히 지워졌는지 검증
                .withoutHeader(HttpHeaders.AUTHORIZATION)
                // 2. 외부 위조 헤더(HACKER_999)가 지워지고, 토큰의 Claim(100) 값으로 교체되었는지 검증
                .withHeader("X-User-Id", equalTo("100"))
                .withHeader("X-User-Role", equalTo("MASTER"))
                .withHeader("X-User-Username", equalTo("sparta_user"))
        );
    }
}