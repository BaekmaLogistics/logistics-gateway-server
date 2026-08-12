package com.sparta.gateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class SwaggerConfig {

    public SwaggerConfig(RouteDefinitionLocator locator, SwaggerUiConfigParameters swaggerUiConfigParameters) {
        List<RouteDefinition> definitions = locator.getRouteDefinitions().collectList().block();

        if (definitions != null) {
            Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = new HashSet<>();

            definitions.stream()
                    .filter(routeDefinition -> routeDefinition.getId().matches(".*-service"))
                    .forEach(routeDefinition -> {
                        String name = routeDefinition.getId();

                        AbstractSwaggerUiConfigProperties.SwaggerUrl swaggerUrl =
                                new AbstractSwaggerUiConfigProperties.SwaggerUrl();
                        swaggerUrl.setName(name);
                        swaggerUrl.setUrl("/v3/api-docs/" + name);

                        urls.add(swaggerUrl);
                    });

            swaggerUiConfigParameters.setUrls(urls);
        }
    }
}