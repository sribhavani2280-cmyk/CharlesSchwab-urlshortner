package com.urlshortener.url.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "short_link", indexes = {
        @Index(name = "ux_short_link_code", columnList = "code", unique = true)
})
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "custom_alias", nullable = false)
    private boolean customAlias = false;

    @Column(name = "click_count", nullable = false)
    private long clickCount = 0;

    @Version
    private long version;

    protected ShortLink() {
    }

    public ShortLink(String code, String longUrl, Instant createdAt, Instant expiresAt, boolean customAlias) {
        this.code = code;
        this.longUrl = longUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.customAlias = customAlias;
        this.active = true;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public boolean isResolvable(Instant now) {
        return active && !isExpired(now);
    }

    public void registerClick() {
        this.clickCount++;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCustomAlias() {
        return customAlias;
    }

    public long getClickCount() {
        return clickCount;
    }

    public long getVersion() {
        return version;
    }
}
