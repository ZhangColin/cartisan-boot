package com.cartisan.openapi.provider;

import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * 远程 API Key 查询实现，带 Caffeine 缓存。
 */
public class RemoteApiKeyProvider implements ApiKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RemoteApiKeyProvider.class);

    private final Cache<String, ApiKeyInfo> cache;
    private final String apikeyServiceUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RemoteApiKeyProvider(CartisanOpenapiProperties properties, ObjectMapper objectMapper) {
        this.apikeyServiceUrl = properties.getApikeyServiceUrl();
        this.objectMapper = objectMapper;
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(properties.getCache().getExpireAfterAccess())
                .maximumSize(properties.getCache().getMaximumSize())
                .build();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public ApiKeyInfo getByAppId(String appId) {
        return cache.get(appId, this::fetchFromRemote);
    }

    private ApiKeyInfo fetchFromRemote(String appId) {
        if (apikeyServiceUrl == null || apikeyServiceUrl.isBlank()) {
            log.warn("apikey-service-url not configured, cannot fetch ApiKey for: {}", appId);
            return null;
        }

        try {
            String url = apikeyServiceUrl + "?appId=" + appId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Failed to fetch ApiKey for {}: HTTP {}", appId, response.statusCode());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.has("data") ? root.get("data") : root;

            Set<String> permissions = new HashSet<>();
            JsonNode permsNode = data.get("permissions");
            if (permsNode != null && permsNode.isArray()) {
                for (JsonNode perm : permsNode) {
                    permissions.add(perm.asText());
                }
            }

            return new ApiKeyInfo(
                    data.get("appId").asText(),
                    data.get("appName").asText(),
                    data.get("apiSecret").asText(),
                    permissions,
                    data.has("status") ? data.get("status").asText() : "ACTIVE"
            );
        } catch (Exception e) {
            log.warn("Error fetching ApiKey for {}: {}", appId, e.getMessage());
            return null;
        }
    }
}
