package com.urlshortener.url.web;

import com.urlshortener.common.ApiException;
import com.urlshortener.url.domain.ShortLink;
import com.urlshortener.url.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Optional;

/** Public redirect endpoint. No API key required; this is the shortener's primary surface. */
@RestController
public class RedirectController {

    private final ShortLinkService shortLinkService;

    public RedirectController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    @GetMapping("/{code:[A-Za-z0-9_-]{3,64}}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        Optional<ShortLink> resolved = shortLinkService.resolveForRedirect(
                code,
                request.getHeader("Referer"),
                request.getHeader("User-Agent"),
                clientIp(request));
        ShortLink link = resolved.orElseThrow(
                () -> ApiException.notFound("Short link is unavailable, disabled, or expired"));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.getLongUrl()))
                .build();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
