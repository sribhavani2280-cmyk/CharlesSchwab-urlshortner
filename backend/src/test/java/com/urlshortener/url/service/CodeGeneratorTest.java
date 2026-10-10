package com.urlshortener.url.service;

import com.urlshortener.common.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeGeneratorTest {

    private final CodeGenerator generator = new CodeGenerator();

    @Test
    void generatesRequestedLength() {
        assertThat(generator.generate(7)).hasSize(7);
        assertThat(generator.generate(10)).hasSize(10);
    }

    @Test
    void generatesBase62Only() {
        String code = generator.generate(64);
        assertThat(code).matches("[A-Za-z0-9]+");
    }

    @Test
    void acceptsValidAlias() {
        assertThat(generator.validateAlias("my-link_01")).isEqualTo("my-link_01");
    }

    @Test
    void rejectsTooShortAlias() {
        assertThatThrownBy(() -> generator.validateAlias("ab"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsIllegalCharacters() {
        assertThatThrownBy(() -> generator.validateAlias("bad/alias"))
                .isInstanceOf(ApiException.class);
    }
}
