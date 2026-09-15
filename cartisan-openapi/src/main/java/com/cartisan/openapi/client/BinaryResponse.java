package com.cartisan.openapi.client;

import java.net.http.HttpHeaders;

/**
 * 二进制响应载体：HTTP 状态、响应头（大小写不敏感）、原始字节。
 *
 * <p>headers 为 JDK 不可变 {@link HttpHeaders}，经 {@code firstValue("Content-Disposition")}
 * 取值（header 名大小写不敏感，适配 BFF 透传场景）；body 为全量缓冲的原始字节，
 * 不做任何字符解码——二进制经字符串解码会不可逆损坏（#30）。</p>
 */
public record BinaryResponse(int statusCode, HttpHeaders headers, byte[] body) {
}
