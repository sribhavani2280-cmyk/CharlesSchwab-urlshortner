package com.urlshortener.url.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Privacy-conscious click record. Stores a salted hash of the client IP rather than the raw address.
 */
@Entity
@Table(name = "click_event", indexes = {
        @Index(name = "ix_click_event_code_time", columnList = "code, occurred_at")
})
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(length = 256)
    private String referer;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    protected ClickEvent() {
    }

    public ClickEvent(String code, Instant occurredAt, String referer, String userAgent, String ipHash) {
        this.code = code;
        this.occurredAt = occurredAt;
        this.referer = referer;
        this.userAgent = userAgent;
        this.ipHash = ipHash;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getReferer() {
        return referer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpHash() {
        return ipHash;
    }
}
