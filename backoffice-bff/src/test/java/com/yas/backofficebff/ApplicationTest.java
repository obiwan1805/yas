package com.yas.backofficebff;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationTest {

    @Test
    void writeableHeaders_shouldExposeWritableRequestHeaders() {
        Application application = new Application();
        WebFilter webFilter = application.writeableHeaders();

        MockServerHttpRequest request = MockServerHttpRequest.get("/authentication/user")
            .header("X-Source", "test")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = filteredExchange -> {
            ServerHttpRequest filteredRequest = filteredExchange.getRequest();
            assertEquals("test", filteredRequest.getHeaders().getFirst("X-Source"));
            return Mono.empty();
        };

        webFilter.filter(exchange, chain).block();
    }
}
