package io.cartisan.web.doc;

import com.cartisan.core.exception.CodeMessage;

/**
 * 与 {@code com.cartisan.web.doc.CodeMessageRegistryTest.ScanTargetMessage} 同 code
 * （SCAN_A_001）的冲突枚举。
 *
 * <p>故意放在 {@code io.} 包下——不在 CodeMessageRegistry 默认扫描包
 * （com.cartisan / com.example）内，仅供注册表冲突用例显式引用，
 * 不污染真实扫描路径。</p>
 */
public enum ConflictingMessage implements CodeMessage {
    SCAN_A(409, "SCAN_A_001", "冲突枚举");

    private final int httpStatus;
    private final String code;
    private final String message;

    ConflictingMessage(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }
}
