package com.urlshortener.url.service;

import com.urlshortener.common.ApiException;
import com.urlshortener.common.FixedWindowRateLimiter;
import com.urlshortener.config.AppProperties;
import com.urlshortener.url.domain.ClickEvent;
import com.urlshortener.url.domain.ShortLink;
import com.urlshortener.url.repo.ClickEventRepository;
import com.urlshortener.url.repo.ShortLinkRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class ShortLinkService {

    private final ShortLinkRepository shortLinkRepository;
    private final ClickEventRepository clickEventRepository;
    private final CodeGenerator codeGenerator;
    private final AppProperties properties;
    private final FixedWindowRateLimiter createLimiter;

    public ShortLinkService(ShortLinkRepository shortLinkRepository,
                            ClickEventRepository clickEventRepository,
                            CodeGenerator codeGenerator,
                            AppProperties properties) {
        this.shortLinkRepository = shortLinkRepository;
        this.clickEventRepository = clickEventRepository;
        this.codeGenerator = codeGenerator;
        this.properties = properties;
        this.createLimiter = new FixedWindowRateLimiter(60, Duration.ofMinutes(1));
    }

    @Transactional
    public ShortLink create(String rawUrl, String requestedAlias, Instant expiresAt, String clientKey) {
        if (!createLimiter.tryAcquire(clientKey == null ? "anonymous" : clientKey)) {
            throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMITED", "Too many create requests; try again shortly");
        }
        String url = UrlValidator.normalizeAndValidate(rawUrl);
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            throw ApiException.badRequest("expiresAt must be in the future");
        }

        if (requestedAlias != null && !requestedAlias.isBlank()) {
            String alias = codeGenerator.validateAlias(requestedAlias);
            if (shortLinkRepository.existsByCode(alias)) {
                throw ApiException.conflict("Alias '" + alias + "' is already taken");
            }
            return persist(new ShortLink(alias, url, Instant.now(), expiresAt, true));
        }
        return persistGenerated(url, expiresAt);
    }

    private ShortLink persistGenerated(String url, Instant expiresAt) {
        int maxRetries = properties.getShortcode().getMaxCollisionRetries();
        int length = properties.getShortcode().getLength();
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            String code = codeGenerator.generate(length);
            if (shortLinkRepository.existsByCode(code)) {
                continue;
            }
            try {
                return persist(new ShortLink(code, url, Instant.now(), expiresAt, false));
            } catch (DataIntegrityViolationException ex) {
                // Lost a concurrent race on the unique constraint; retry with a new code.
            }
        }
        throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "CODE_GENERATION_FAILED", "Unable to allocate a unique short code; try again");
    }

    private ShortLink persist(ShortLink link) {
        return shortLinkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public ShortLink get(String code) {
        return shortLinkRepository.findByCode(code)
                .orElseThrow(() -> ApiException.notFound("No short link for code '" + code + "'"));
    }

    @Transactional(readOnly = true)
    public Page<ShortLink> list(Pageable pageable) {
        return shortLinkRepository.findAll(pageable);
    }

    @Transactional
    public ShortLink setActive(String code, boolean active) {
        ShortLink link = get(code);
        link.setActive(active);
        return shortLinkRepository.save(link);
    }

    @Transactional
    public void delete(String code) {
        ShortLink link = get(code);
        shortLinkRepository.delete(link);
    }

    /**
     * Resolves a code for redirection, recording a click. Returns empty when the link
     * does not exist, is disabled, or has expired so the caller can choose the HTTP status.
     */
    @Transactional
    public Optional<ShortLink> resolveForRedirect(String code, String referer, String userAgent, String clientIp) {
        Optional<ShortLink> maybe = shortLinkRepository.findByCode(code);
        if (maybe.isEmpty()) {
            return Optional.empty();
        }
        ShortLink link = maybe.get();
        if (!link.isResolvable(Instant.now())) {
            return Optional.empty();
        }
        link.registerClick();
        clickEventRepository.save(new ClickEvent(
                code, Instant.now(), truncate(referer, 256), truncate(userAgent, 512), hashIp(clientIp)));
        return Optional.of(link);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String hashIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
