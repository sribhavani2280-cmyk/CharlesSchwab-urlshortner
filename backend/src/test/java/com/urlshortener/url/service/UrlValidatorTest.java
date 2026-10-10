package com.urlshortener.url.service;

import com.urlshortener.common.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlValidatorTest {

    @Test
    void acceptsHttpAndHttps() {
        assertThat(UrlValidator.normalizeAndValidate("https://example.com/path"))
                .isEqualTo("https://example.com/path");
        assertThat(UrlValidator.normalizeAndValidate("  http://example.com  "))
                .isEqualTo("http://example.com");
    }

    @Test
    void rejectsJavascriptScheme() {
        assertThatThrownBy(() -> UrlValidator.normalizeAndValidate("javascript:alert(1)"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsMissingHost() {
        assertThatThrownBy(() -> UrlValidator.normalizeAndValidate("https://"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> UrlValidator.normalizeAndValidate("  "))
                .isInstanceOf(ApiException.class);
    }
}
