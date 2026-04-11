package com.cartisan.openapi.signature;

/**
 * 签名计算器接口。
 */
public interface SignatureCalculator {
    /**
     * 计算签名。
     *
     * @param stringToSign 待签名字符串
     * @param secret       密钥
     * @return 签名结果（hex 字符串）
     */
    String calculate(String stringToSign, String secret);
}
