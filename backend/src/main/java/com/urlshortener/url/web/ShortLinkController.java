package com.urlshortener.url.web;

import com.urlshortener.config.AppProperties;
import com.urlshortener.url.domain.ShortLink;
import com.urlshortener.url.service.AnalyticsService;
import com.urlshortener.url.service.ShortLinkService;
import com.urlshortener.url.web.dto.AnalyticsResponse;
import com.urlshortener.url.web.dto.CreateShortLinkRequest;
import com.urlshortener.url.web.dto.ShortLinkResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/links")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;
    private final AnalyticsService analyticsService;
    private final AppProperties properties;

    public ShortLinkController(ShortLinkService shortLinkService,
                               AnalyticsService analyticsService,
                               AppProperties properties) {
        this.shortLinkService = shortLinkService;
        this.analyticsService = analyticsService;
        this.properties = properties;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShortLinkResponse create(@Valid @RequestBody CreateShortLinkRequest request) {
        ShortLink link = shortLinkService.create(request.url(), request.alias(), request.expiresAt(), null);
        return ShortLinkResponse.from(link, properties.getBaseUrl());
    }

    @GetMapping("/{code}")
    public ShortLinkResponse get(@PathVariable String code) {
        return ShortLinkResponse.from(shortLinkService.get(code), properties.getBaseUrl());
    }

    @GetMapping
    public List<ShortLinkResponse> list(Pageable pageable) {
        Page<ShortLink> page = shortLinkService.list(pageable);
        return page.map(l -> ShortLinkResponse.from(l, properties.getBaseUrl())).getContent();
    }

    @PostMapping("/{code}/disable")
    public ShortLinkResponse disable(@PathVariable String code) {
        return ShortLinkResponse.from(shortLinkService.setActive(code, false), properties.getBaseUrl());
    }

    @PostMapping("/{code}/enable")
    public ShortLinkResponse enable(@PathVariable String code) {
        return ShortLinkResponse.from(shortLinkService.setActive(code, true), properties.getBaseUrl());
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) {
        shortLinkService.delete(code);
    }

    @GetMapping("/{code}/analytics")
    public AnalyticsResponse analytics(@PathVariable String code,
                                       @RequestParam(defaultValue = "30") int windowDays) {
        shortLinkService.get(code); // 404 if unknown
        return analyticsService.forCode(code, windowDays);
    }
}
