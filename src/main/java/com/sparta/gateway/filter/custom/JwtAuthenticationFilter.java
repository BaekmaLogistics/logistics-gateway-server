package com.sparta.gateway.filter.custom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.gateway.code.ErrorResponseCode;
import com.sparta.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Authorization 헤더 존재 여부 확인
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Authorization 헤더가 존재하지 않습니다. Path: {}", request.getPath());
                return onError(exchange, ErrorResponseCode.BEARER_NOT_FOUND);
            }

            // Bearer 토큰 추출
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("올바르지 않은 Authorization 헤더 포맷입니다. Header: {}", authHeader);
                return onError(exchange, ErrorResponseCode.BAD_JWT_FORMAT);
            }

            String token = authHeader.substring(7);

            // JWT 유효성 검증
            if (!jwtUtil.validateToken(token)) {
                log.warn("유효하지 않거나 만료된 JWT 토큰입니다.");
                return onError(exchange, ErrorResponseCode.INVALID_TOKEN);
            }

            // Claims 파싱 후 Downstream 서비스로 전달할 Custom Header 주입
            Claims claims = jwtUtil.extractClaims(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            String username = claims.get("username", String.class);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .headers(httpHeaders -> {
                        // 내부로 전달하는 요청에 JWT 제거
                        httpHeaders.remove(HttpHeaders.AUTHORIZATION);
                    })
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Role", role != null ? role : "")
                    .header("X-User-Username", username != null ? username : "")
                    .build();

            // 변경된 Header를 가진 Request로 필터 체인 진행
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    // 에러 발생 시 JSON 응답 반환
    private Mono<Void> onError(ServerWebExchange exchange, ErrorResponseCode errorResponseCode) {
        HttpStatus status = errorResponseCode.getStatus();
        String message = errorResponseCode.getMessage();
        String errorCode = errorResponseCode.getErrorCode();

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("errorCode", errorCode);
        errorBody.put("message", message);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorBody);
        } catch (JsonProcessingException e) {
            bytes = "{\"errorCode\":\"UNAUTHORIZED\",\"message\":\"인증 실패\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Getter
    @Setter
    public static class Config {
        // 필요 시 yml을 통해 넘어올 설정을 기재 (현재는 사용하지 않음)
    }
}