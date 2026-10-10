package com.urlshortener.url.web.dto;

import com.urlshortener.url.domain.ShortLink;

import java.time.Instant;

public record ShortLinkResponse(
        String code,
        String shortUrl,
        String longUrl,
        boolean active,
        boolean customAlias,
        long clickCount,
        Instant createdAt,
        Instant expiresAt) {

    public static ShortLinkResponse from(ShortLink link, String baseUrl) {
        return new ShortLinkResponse(
                link.getCode(),
                baseUrl + "/" + link.getCode(),
                link.getLongUrl(),
                link.isActive(),
                link.isCustomAlias(),
                link.getClickCount(),
                link.getCreatedAt(),
                link.getExpiresAt());
    }
}
