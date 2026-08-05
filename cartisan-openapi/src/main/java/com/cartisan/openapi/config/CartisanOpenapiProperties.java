package com.cartisan.openapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/**
 * cartisan-openapi 配置属性。
 */
@ConfigurationProperties(prefix = "cartisan.openapi")
public class CartisanOpenapiProperties {

    private Self self = new Self();
    private String apikeyServiceUrl;
    private long timestampTolerance = 300;
    private long nonceTtl = 300;
    private Cache cache = new Cache();
    private DataSize maxBodySize = DataSize.ofMegabytes(1);
    private Timeout timeout = new Timeout();

    public Self getSelf() { return self; }
    public void setSelf(Self self) { this.self = self; }

    public Timeout getTimeout() { return timeout; }
    public void setTimeout(Timeout timeout) { this.timeout = timeout; }

    public String getApikeyServiceUrl() { return apikeyServiceUrl; }
    public void setApikeyServiceUrl(String apikeyServiceUrl) { this.apikeyServiceUrl = apikeyServiceUrl; }

    public long getTimestampTolerance() { return timestampTolerance; }
    public void setTimestampTolerance(long timestampTolerance) { this.timestampTolerance = timestampTolerance; }

    public long getNonceTtl() { return nonceTtl; }
    public void setNonceTtl(long nonceTtl) { this.nonceTtl = nonceTtl; }

    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }

    public DataSize getMaxBodySize() { return maxBodySize; }
    public void setMaxBodySize(DataSize maxBodySize) { this.maxBodySize = maxBodySize; }

    public static class Self {
        private String apiKey;
        private String apiSecret;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    }

    public static class Cache {
        private Duration expireAfterAccess = Duration.ofMinutes(30);
        private long maximumSize = 1000;

        public Duration getExpireAfterAccess() { return expireAfterAccess; }
        public void setExpireAfterAccess(Duration expireAfterAccess) { this.expireAfterAccess = expireAfterAccess; }

        public long getMaximumSize() { return maximumSize; }
        public void setMaximumSize(long maximumSize) { this.maximumSize = maximumSize; }
    }

    /**
     * 出站 HTTP 超时（{@code OpenApiClient}）。默认沿用历史硬编码值（connect 10s / read 30s），
     * 调用方按 {@code cartisan.openapi.timeout.connect-seconds / read-seconds} 收窄。
     */
    public static class Timeout {
        /** 连接超时（秒），{@code HttpClient.connectTimeout}。 */
        private long connectSeconds = 10;

        /** 读超时（秒），单请求 {@code HttpRequest.timeout}。 */
        private long readSeconds = 30;

        public long getConnectSeconds() { return connectSeconds; }
        public void setConnectSeconds(long connectSeconds) { this.connectSeconds = connectSeconds; }

        public long getReadSeconds() { return readSeconds; }
        public void setReadSeconds(long readSeconds) { this.readSeconds = readSeconds; }
    }
}
