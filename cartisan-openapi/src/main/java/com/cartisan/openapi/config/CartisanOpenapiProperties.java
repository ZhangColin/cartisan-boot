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

    public Self getSelf() { return self; }
    public void setSelf(Self self) { this.self = self; }

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
        private String appKey;
        private String appSecret;

        public String getAppKey() { return appKey; }
        public void setAppKey(String appKey) { this.appKey = appKey; }

        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    }

    public static class Cache {
        private Duration expireAfterAccess = Duration.ofMinutes(30);
        private long maximumSize = 1000;

        public Duration getExpireAfterAccess() { return expireAfterAccess; }
        public void setExpireAfterAccess(Duration expireAfterAccess) { this.expireAfterAccess = expireAfterAccess; }

        public long getMaximumSize() { return maximumSize; }
        public void setMaximumSize(long maximumSize) { this.maximumSize = maximumSize; }
    }
}
