package com.cartisan.web.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 地址工具类。
 *
 * <p>提供获取客户端真实 IP 地址的工具方法，支持反向代理场景。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * String clientIp = IpUtil.getClientIp(request);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class IpUtil {

    private IpUtil() {
        // 工具类，不允许实例化
    }

    /**
     * 获取客户端 IP 地址。
     *
     * <p>仅当请求来自受信任的反向代理（回环或私有 IP）时，才读取 X-Forwarded-For，
     * 防止攻击者伪造头绕过 IP 限流。</p>
     *
     * <p>受信任的 IP 地址范围包括：</p>
     * <ul>
     *   <li>回环地址：127.0.0.1、::1</li>
     *   <li>私有网络 A 类：10.0.0.0/8</li>
     *   <li>私有网络 B 类：172.16.0.0/12</li>
     *   <li>私有网络 C 类：192.168.0.0/16</li>
     * </ul>
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址，如果无法获取则返回 127.0.0.1
     */
    public static String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                // 取第一个 IP（最原始的客户端 IP）
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr != null ? remoteAddr : "127.0.0.1";
    }

    /**
     * 判断指定 IP 地址是否为受信任的代理服务器。
     *
     * <p>受信任的代理服务器包括：</p>
     * <ul>
     *   <li>回环地址：127.0.0.1、::1、0:0:0:0:0:0:0:1</li>
     *   <li>私有网络 A 类：10.0.0.0 - 10.255.255.255</li>
     *   <li>私有网络 B 类：172.16.0.0 - 172.31.255.255</li>
     *   <li>私有网络 C 类：192.168.0.0 - 192.168.255.255</li>
     * </ul>
     *
     * @param remoteAddr IP 地址
     * @return 如果是受信任的代理服务器返回 true，否则返回 false
     */
    private static boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null) {
            return false;
        }

        // 回环地址
        if (remoteAddr.startsWith("127.")
                || remoteAddr.startsWith("10.")
                || remoteAddr.startsWith("192.168.")
                || remoteAddr.equals("0:0:0:0:0:0:0:1")
                || remoteAddr.equals("::1")) {
            return true;
        }

        // 172.16.0.0/12: 172.16.x.x – 172.31.x.x
        if (remoteAddr.startsWith("172.")) {
            String[] parts = remoteAddr.split("\\.", 3);
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    // 忽略格式错误的 IP
                }
            }
        }

        return false;
    }
}
