package com.sparta.gateway.code;

import org.springframework.http.HttpStatus;

public interface ApiResponseCode {
    HttpStatus getStatus();
    String getMessage();
}
