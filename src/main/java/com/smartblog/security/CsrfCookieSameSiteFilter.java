package com.smartblog.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.springframework.web.filter.OncePerRequestFilter;

public class CsrfCookieSameSiteFilter extends OncePerRequestFilter {

    private final String cookieName;
    private final String sameSiteValue;

    public CsrfCookieSameSiteFilter() {
        this("XSRF-TOKEN", "Lax");
    }

    public CsrfCookieSameSiteFilter(String cookieName, String sameSiteValue) {
        this.cookieName = cookieName;
        this.sameSiteValue = sameSiteValue;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        CookieCaptureWrapper wrapper = new CookieCaptureWrapper(response);
        filterChain.doFilter(request, wrapper);

        List<String> cookies = wrapper.getCapturedSetCookieHeaders();
        for (String cookie : cookies) {
            String out = cookie;
            if (cookie.contains(cookieName) && !cookie.toLowerCase().contains("samesite")) {
                out = cookie + "; SameSite=" + sameSiteValue;
            }
            response.addHeader("Set-Cookie", out);
        }
    }

    private static class CookieCaptureWrapper extends HttpServletResponseWrapper {
        private final List<String> setCookieHeaders = new ArrayList<>();

        public CookieCaptureWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addHeader(String name, String value) {
            if ("Set-Cookie".equalsIgnoreCase(name)) {
                setCookieHeaders.add(value);
            } else {
                super.addHeader(name, value);
            }
        }

        @Override
        public void setHeader(String name, String value) {
            if ("Set-Cookie".equalsIgnoreCase(name)) {
                setCookieHeaders.clear();
                setCookieHeaders.add(value);
            } else {
                super.setHeader(name, value);
            }
        }

        public List<String> getCapturedSetCookieHeaders() {
            return setCookieHeaders;
        }
    }
}
