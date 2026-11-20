package de.swiftbyte.gmc.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public final class SessionCookieUtil {

    public static final String SESSION_COOKIE_NAME = "gmc_session";

    @Value("${gmc.cors.set-samesite-none:false}")
    private boolean sameSiteNone;

    @Value("${gmc.cors.secure-cookies:true}")
    private boolean secureCookies;

    @Value("${gmc.session.cookie-domain:}")
    private String cookieDomain;

    private SessionCookieUtil() {}

    public String readSessionId(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void setSessionCookie(HttpServletResponse response, String sessionId, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(SESSION_COOKIE_NAME, sessionId)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSiteNone ? "None" : "Lax")
                .maxAge(Duration.ofSeconds(Math.max(0, maxAgeSeconds)))
                .path("/");
        if (cookieDomain != null && !cookieDomain.isBlank()) builder.domain(cookieDomain);
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public void clearSessionCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(SESSION_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSiteNone ? "None" : "Lax")
                .maxAge(Duration.ZERO)
                .path("/");
        if (cookieDomain != null && !cookieDomain.isBlank()) builder.domain(cookieDomain);
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
