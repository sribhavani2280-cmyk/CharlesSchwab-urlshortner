package com.urlshortener.url.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        String code,
        long totalClicks,
        Instant windowStart,
        Map<String, Long> clicksByDay,
        List<String> topReferers) {
}
