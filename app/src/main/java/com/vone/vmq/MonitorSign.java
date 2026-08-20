package com.vone.vmq;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 挂机端与服务端之间的 v2 签名协议：HMAC-SHA-256，商户密钥只作为 HMAC 密钥使用，不再拼入待签明文。
 *
 * <p>这里的规范串必须与后端 internal/domain/payment 的实现逐字符一致，任何一侧调整字段顺序或分隔符都会导致签名不通过。
 * 服务端按毫秒解析 t 并做双向新鲜度校验，超出窗口的请求会被拒绝，因此设备时钟必须准确。
 */
public final class MonitorSign {

    private static final String ALGORITHM = "HmacSHA256";
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private MonitorSign() {
    }

    public static String heartbeat(String timestamp, String key) {
        return hmacSha256Hex(key, "t=" + timestamp);
    }

    public static String push(String type, String price, String timestamp, String key) {
        return hmacSha256Hex(key, "type=" + type + "&price=" + price + "&t=" + timestamp);
    }

    public static String timestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * 金额定标两位，保证参与签名的金额文本与查询参数里的金额文本必然是同一个字符串，
     * 同时避免 double 直接字符串化时出现科学计数法或超过两位小数被服务端判为金额格式无效。
     */
    public static String priceText(double price) {
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String hmacSha256Hex(String key, String canonical) {
        if (key == null || key.isEmpty() || canonical == null) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return toHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            // 返回空串让服务端以签名错误拒绝，而不是让后台心跳线程因异常整体退出。
            return "";
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder text = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            text.append(HEX_DIGITS[(value >> 4) & 0x0f]).append(HEX_DIGITS[value & 0x0f]);
        }
        return text.toString();
    }
}