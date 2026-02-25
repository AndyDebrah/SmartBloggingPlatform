package com.smartblog.auth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SecurityEventMetricsService {

    private static final long BRUTE_FORCE_THRESHOLD = 5;

    private final AtomicLong loginAttempts = new AtomicLong(0);
    private final AtomicLong loginSuccesses = new AtomicLong(0);
    private final AtomicLong loginFailures = new AtomicLong(0);
    private final AtomicLong oauth2Successes = new AtomicLong(0);
    private final AtomicLong refreshSuccesses = new AtomicLong(0);
    private final AtomicLong refreshFailures = new AtomicLong(0);
    private final AtomicLong tokenValidationSuccesses = new AtomicLong(0);
    private final AtomicLong tokenValidationFailures = new AtomicLong(0);
    private final AtomicLong tokenRevocations = new AtomicLong(0);
    private final AtomicLong unauthorizedEvents = new AtomicLong(0);
    private final AtomicLong accessDeniedEvents = new AtomicLong(0);

    private final ConcurrentHashMap<String, AtomicLong> loginFailuresByPrincipal = new ConcurrentHashMap<>();

    public void recordLoginAttempt(String principal, String remoteIp) {
        loginAttempts.incrementAndGet();
        log.info("security_event=login_attempt principal={} ip={}", normalize(principal), normalize(remoteIp));
    }

    public void recordLoginSuccess(String principal, String remoteIp) {
        loginSuccesses.incrementAndGet();
        log.info("security_event=login_success principal={} ip={}", normalize(principal), normalize(remoteIp));
    }

    public void recordLoginFailure(String principal, String remoteIp, String reason) {
        loginFailures.incrementAndGet();
        loginFailuresByPrincipal.computeIfAbsent(normalize(principal), k -> new AtomicLong(0)).incrementAndGet();
        log.warn("security_event=login_failure principal={} ip={} reason={}",
                normalize(principal), normalize(remoteIp), normalize(reason));
    }

    public void recordOauth2Success(String principal, String remoteIp) {
        oauth2Successes.incrementAndGet();
        log.info("security_event=oauth2_success principal={} ip={}", normalize(principal), normalize(remoteIp));
    }

    public void recordRefreshSuccess(String principal) {
        refreshSuccesses.incrementAndGet();
        log.info("security_event=refresh_success principal={}", normalize(principal));
    }

    public void recordRefreshFailure(String reason) {
        refreshFailures.incrementAndGet();
        log.warn("security_event=refresh_failure reason={}", normalize(reason));
    }

    public void recordTokenValidationSuccess(String principal, String path) {
        tokenValidationSuccesses.incrementAndGet();
        log.debug("security_event=token_validation_success principal={} path={}", normalize(principal), normalize(path));
    }

    public void recordTokenValidationFailure(String path, String reason) {
        tokenValidationFailures.incrementAndGet();
        log.warn("security_event=token_validation_failure path={} reason={}", normalize(path), normalize(reason));
    }

    public void recordTokenRevocation(String principal, String reason) {
        tokenRevocations.incrementAndGet();
        log.info("security_event=token_revocation principal={} reason={}", normalize(principal), normalize(reason));
    }

    public void recordUnauthorized(String path, String reason) {
        unauthorizedEvents.incrementAndGet();
        log.warn("security_event=unauthorized path={} reason={}", normalize(path), normalize(reason));
    }

    public void recordAccessDenied(String path, String principal) {
        accessDeniedEvents.incrementAndGet();
        log.warn("security_event=access_denied path={} principal={}", normalize(path), normalize(principal));
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("loginAttempts", loginAttempts.get());
        report.put("loginSuccesses", loginSuccesses.get());
        report.put("loginFailures", loginFailures.get());
        report.put("oauth2Successes", oauth2Successes.get());
        report.put("refreshSuccesses", refreshSuccesses.get());
        report.put("refreshFailures", refreshFailures.get());
        report.put("tokenValidationSuccesses", tokenValidationSuccesses.get());
        report.put("tokenValidationFailures", tokenValidationFailures.get());
        report.put("tokenRevocations", tokenRevocations.get());
        report.put("unauthorizedEvents", unauthorizedEvents.get());
        report.put("accessDeniedEvents", accessDeniedEvents.get());

        Map<String, Long> failuresByPrincipal = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : loginFailuresByPrincipal.entrySet()) {
            failuresByPrincipal.put(entry.getKey(), entry.getValue().get());
        }
        report.put("loginFailuresByPrincipal", failuresByPrincipal);

        List<String> suspiciousPrincipals = new ArrayList<>();
        for (Map.Entry<String, Long> entry : failuresByPrincipal.entrySet()) {
            if (entry.getValue() >= BRUTE_FORCE_THRESHOLD) {
                suspiciousPrincipals.add(entry.getKey());
            }
        }
        report.put("suspiciousPrincipals", suspiciousPrincipals);
        report.put("bruteForceThreshold", BRUTE_FORCE_THRESHOLD);
        return report;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
