package com.example.resilience.core;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRequestUriTest {

    @Test
    void relativeQueryValuesAreEncodedAndNotSplit() {
        URI uri = ApiRequest.builder("/orders/{orderId}")
                .uriVariable("orderId", "a b")
                .queryParam("q", "a&b=c")
                .build()
                .toUri(UriComponentsBuilder.newInstance().scheme("https").host("api.example.test"));

        assertThat(uri.getRawPath()).isEqualTo("/orders/a%20b");
        assertThat(uri.getRawQuery()).isEqualTo("q=a%26b%3Dc");
        assertThat(uri.getQuery()).isEqualTo("q=a&b=c");
    }

    @Test
    void absoluteTemplateExpandsWithoutDroppingExtraQueryParams() {
        URI uri = ApiRequest.builder("https://api.example.test/orders/{orderId}")
                .uriVariable("orderId", "A/B")
                .queryParam("include", "customer")
                .build()
                .toUri(UriComponentsBuilder.fromUriString("https://ignored.example.test/base"));

        assertThat(uri.getScheme()).isEqualTo("https");
        assertThat(uri.getHost()).isEqualTo("api.example.test");
        assertThat(uri.getRawPath()).isEqualTo("/orders/A%2FB");
        assertThat(uri.getRawQuery()).isEqualTo("include=customer");
    }

    @Test
    void logUriRedactsSecretQueryValuesButKeepsDiagnosticParams() {
        String logged = ApiRequest.builder("/orders/{orderId}")
                .queryParam("include", "customer")
                .queryParam("apiKey", "super-secret")
                .queryParam("access_token", "tok")
                .build()
                .logUri();

        assertThat(logged).contains("include=customer");
        assertThat(logged).contains("apiKey=***");
        assertThat(logged).contains("access_token=***");
        assertThat(logged).doesNotContain("super-secret");
        assertThat(logged).doesNotContain("=tok");
        assertThat(List.of(logged.split("&"))).noneMatch(part -> part.contains("super-secret"));
    }
}
