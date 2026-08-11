package com.sparta.gateway.filter.global;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-100) // 가장 먼저 실행
public class HeaderSanitizationGlobalFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(httpHeaders -> {
                    // 외부에서 들어온 위조 가능성 있는 X-User-* 헤더 일괄 제거
                    httpHeaders.remove("X-User-Id");
                    httpHeaders.remove("X-User-Role");
                    httpHeaders.remove("X-User-Username");
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }
}