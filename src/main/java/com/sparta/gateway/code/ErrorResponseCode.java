package com.sparta.gateway.code;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum ErrorResponseCode implements ApiResponseCode {
    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_0001", "알 수 없는 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_0002", "유효하지 않은 요청입니다."),
    FEIGN_CLIENT_ERROR(HttpStatus.BAD_GATEWAY, "COMMON_0003", "Feign 통신 중 오류가 발생했습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_1001", "인증되지 않은 요청입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_1002", "유효하지 않거나 만료된 토큰입니다."),
    BAD_JWT_FORMAT(HttpStatus.UNAUTHORIZED, "AUTH_1003", "올바르지 않은 Authorization 헤더 포맷입니다."),
    BEARER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_1004", "Authorization 헤더가 누락되었습니다.");

    private final HttpStatus status;
    private final String errorCode;
    private final String message;
}