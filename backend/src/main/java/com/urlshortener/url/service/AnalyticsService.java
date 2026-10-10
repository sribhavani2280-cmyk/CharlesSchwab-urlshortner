package com.urlshortener.url.service;

import com.urlshortener.url.domain.ClickEvent;
import com.urlshortener.url.repo.ClickEventRepository;
import com.urlshortener.url.web.dto.AnalyticsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ClickEventRepository clickEventRepository;

    public AnalyticsService(ClickEventRepository clickEventRepository) {
        this.clickEventRepository = clickEventRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse forCode(String code, int windowDays) {
        Instant from = Instant.now().minus(Duration.ofDays(windowDays));
        long total = clickEventRepository.countByCode(code);
        List<ClickEvent> events = clickEventRepository.findSince(code, from);

        Map<String, Long> clicksByDay = new LinkedHashMap<>();
        for (ClickEvent event : events) {
            LocalDate day = event.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate();
            clicksByDay.merge(day.toString(), 1L, Long::sum);
        }

        List<String> topReferers = events.stream()
                .map(ClickEvent::getReferer)
                .filter(r -> r != null && !r.isBlank())
                .collect(Collectors.groupingBy(r -> r, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        return new AnalyticsResponse(code, total, from, clicksByDay, topReferers);
    }
}
