package com.smartinventory.service.ai;

import com.smartinventory.dto.RestockPrediction;
import com.smartinventory.dto.TrendAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for the Python Flask + scikit-learn AI microservice.
 *
 * <p>All methods return {@link Optional} and never throw — if the service is
 * unreachable or disabled, an empty Optional is returned and the caller falls
 * back to {@link AiRuleEngine}. The Flask service returns JSON keyed to match the
 * Java DTOs ({@link TrendAnalysis}, {@link RestockPrediction}) so responses map
 * directly.</p>
 */
@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final boolean enabled;

    public AiServiceClient(RestTemplateBuilder builder,
                           @Value("${ai.service.base-url:http://localhost:5000}") String baseUrl,
                           @Value("${ai.service.enabled:true}") boolean enabled,
                           @Value("${ai.service.timeout-ms:4000}") int timeoutMs) {
        this.baseUrl = baseUrl;
        this.enabled = enabled;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public boolean isAvailable() {
        if (!enabled) return false;
        try {
            ResponseEntity<String> r = restTemplate.getForEntity(baseUrl + "/health", String.class);
            return r.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.debug("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /** ML-based restock prediction. payload: {windowDays, products:[{...daily series...}]} */
    public Optional<List<RestockPrediction>> predictRestock(Map<String, Object> payload) {
        if (!enabled) return Optional.empty();
        try {
            ResponseEntity<List<RestockPrediction>> resp = restTemplate.exchange(
                    baseUrl + "/predict/restock",
                    HttpMethod.POST,
                    new HttpEntity<>(payload),
                    new ParameterizedTypeReference<>() {});
            return Optional.ofNullable(resp.getBody());
        } catch (Exception e) {
            log.warn("AI restock prediction unavailable, using rule-based fallback: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** ML-based sales-trend forecast. payload: {labels:[...], values:[...], forecastDays} */
    public Optional<TrendAnalysis> forecastTrend(Map<String, Object> payload) {
        if (!enabled) return Optional.empty();
        try {
            ResponseEntity<TrendAnalysis> resp = restTemplate.postForEntity(
                    baseUrl + "/forecast/trend", new HttpEntity<>(payload), TrendAnalysis.class);
            return Optional.ofNullable(resp.getBody());
        } catch (Exception e) {
            log.warn("AI trend forecast unavailable, using rule-based fallback: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
