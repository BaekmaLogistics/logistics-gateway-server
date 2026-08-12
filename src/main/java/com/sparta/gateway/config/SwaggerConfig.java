package com.sparta.gateway.config;

import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    public SwaggerConfig(RouteDefinitionLocator locator, SwaggerUiConfigParameters swaggerUiConfigParameters) {
        List<RouteDefinition> definitions = locator.getRouteDefinitions().collectList().block();

        if (definitions != null) {
            definitions.stream()
                    .filter(routeDefinition -> routeDefinition.getId().matches(".*-service")) // Eureka 서비스 ID 패턴 (예: auth-service, user-service 등)
                    .forEach(routeDefinition -> {
                        String name = routeDefinition.getId();
                        // 드롭다운에 표시할 라벨 이름과 해당 MS의 Swagger API Spec 경로 설정
                        swaggerUiConfigParameters.addGroup(name, "/" + name + "/api/api-spec");
                    });
        }
    }
}