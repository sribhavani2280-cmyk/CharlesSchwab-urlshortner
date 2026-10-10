package com.urlshortener.url.service;

import com.urlshortener.common.ApiException;
import com.urlshortener.config.AppProperties;
import com.urlshortener.url.domain.ShortLink;
import com.urlshortener.url.repo.ClickEventRepository;
import com.urlshortener.url.repo.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortLinkServiceTest {

    private ShortLinkRepository shortLinkRepository;
    private ClickEventRepository clickEventRepository;
    private ShortLinkService service;

    @BeforeEach
    void setUp() {
        shortLinkRepository = mock(ShortLinkRepository.class);
        clickEventRepository = mock(ClickEventRepository.class);
        AppProperties properties = new AppProperties();
        service = new ShortLinkService(shortLinkRepository, clickEventRepository,
                new CodeGenerator(), properties);
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void rejectsTakenAlias() {
        when(shortLinkRepository.existsByCode("promo")).thenReturn(true);
        assertThatThrownBy(() -> service.create("https://example.com", "promo", null, "k"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void retriesOnCodeCollision() {
        // First generated code collides, second is free.
        when(shortLinkRepository.existsByCode(anyString())).thenReturn(true, false);
        ShortLink link = service.create("https://example.com", null, null, "k");
        assertThat(link.getCode()).isNotBlank();
        verify(shortLinkRepository).save(any(ShortLink.class));
    }

    @Test
    void rejectsPastExpiry() {
        assertThatThrownBy(() -> service.create(
                "https://example.com", null, Instant.now().minus(1, ChronoUnit.HOURS), "k"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("future");
    }

    @Test
    void doesNotRedirectExpiredLink() {
        ShortLink expired = new ShortLink("abc1234", "https://example.com",
                Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS), false);
        when(shortLinkRepository.findByCode("abc1234")).thenReturn(Optional.of(expired));

        Optional<ShortLink> result = service.resolveForRedirect("abc1234", null, null, "1.2.3.4");
        assertThat(result).isEmpty();
    }

    @Test
    void redirectsActiveLinkAndRecordsClick() {
        ShortLink active = new ShortLink("abc1234", "https://example.com", Instant.now(), null, false);
        when(shortLinkRepository.findByCode("abc1234")).thenReturn(Optional.of(active));

        Optional<ShortLink> result = service.resolveForRedirect("abc1234", "ref", "ua", "1.2.3.4");

        assertThat(result).isPresent();
        assertThat(active.getClickCount()).isEqualTo(1);
        verify(clickEventRepository).save(any());
    }
}
