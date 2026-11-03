package com.talent360.security.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LoginRateLimiterService {

    @Value("${app.security.rate-limit.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.rate-limit.window-seconds:900}")
    private long windowSeconds; // 15 minutes default

    private static class AttemptRecord {
        int attempts;
        Instant firstAttemptTime;

        AttemptRecord(int attempts, Instant firstAttemptTime) {
            this.attempts = attempts;
            this.firstAttemptTime = firstAttemptTime;
        }
    }

    private final ConcurrentHashMap<String, AttemptRecord> attemptsCache = new ConcurrentHashMap<>();

    public void checkRateLimit(String key) {
        if (key == null) return;
        AttemptRecord record = attemptsCache.get(key);
        if (record == null) return;

        Instant now = Instant.now();
        long elapsedSeconds = now.getEpochSecond() - record.firstAttemptTime.getEpochSecond();

        if (elapsedSeconds > windowSeconds) {
            attemptsCache.remove(key);
            return;
        }

        if (record.attempts >= maxAttempts) {
            long remainingSeconds = windowSeconds - elapsedSeconds;
            log.warn("Rate limit exceeded for key '{}'. Blocked for {} remaining seconds.", key, remainingSeconds);
            throw new RateLimitExceededException(
                    "Too many failed login attempts. Please try again in " + (remainingSeconds / 60 + 1) + " minutes.",
                    remainingSeconds
            );
        }
    }

    public void recordFailedAttempt(String key) {
        if (key == null) return;
        Instant now = Instant.now();
        attemptsCache.compute(key, (k, existing) -> {
            if (existing == null) {
                return new AttemptRecord(1, now);
            }
            long elapsedSeconds = now.getEpochSecond() - existing.firstAttemptTime.getEpochSecond();
            if (elapsedSeconds > windowSeconds) {
                return new AttemptRecord(1, now);
            }
            existing.attempts++;
            return existing;
        });
    }

    public void resetAttempts(String key) {
        if (key != null) {
            attemptsCache.remove(key);
        }
    }
}
