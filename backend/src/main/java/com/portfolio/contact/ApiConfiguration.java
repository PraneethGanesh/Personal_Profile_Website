package com.portfolio.contact;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.web.cors.*;
import org.springframework.web.filter.CorsFilter;

@Configuration
@Profile("api")
public class ApiConfiguration {
    @Bean
    FilterRegistrationBean<CorsFilter> contactCors(@Value("${contact.allowed-origins}") String origins) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::strip).toList());
        cors.setAllowedMethods(java.util.List.of("POST", "OPTIONS"));
        cors.setAllowedHeaders(java.util.List.of("Content-Type"));
        cors.setExposedHeaders(java.util.List.of("Retry-After"));
        cors.setMaxAge(600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/contact", cors);
        var registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
