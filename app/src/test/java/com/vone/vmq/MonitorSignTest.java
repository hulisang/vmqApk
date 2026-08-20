package com.vone.vmq;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * 黄金向量与后端 internal/domain/payment/payment_test.go 完全相同，由 OpenSSL 独立计算。
 * 只要挂机端与服务端有任何一侧改动了规范串，这里就会先失败，而不是等到线上心跳掉线才发现。
 */
public class MonitorSignTest {

    private static final String KEY = "testkey123456";
    private static final String TIMESTAMP = "1773500000000";

    @Test
    public void heartbeatMatchesBackendGoldenVector() {
        assertEquals("71ec70d2383ec10bdb2fe39f42f60721a1808fa31884deebc9b8c88b5b527bbe",
                MonitorSign.heartbeat(TIMESTAMP, KEY));
    }

    @Test
    public void pushMatchesBackendGoldenVector() {
        assertEquals("61174e2503aec9c2ffe8430ba322d03b8e3c5f46c3d08f29e69ee35d0b34a51e",
                MonitorSign.push("1", "1.00", TIMESTAMP, KEY));
    }

    @Test
    public void signIsLowercaseHexOfFixedLength() {
        String sign = MonitorSign.heartbeat(TIMESTAMP, KEY);
        assertEquals(64, sign.length());
        assertTrue(sign.matches("[0-9a-f]{64}"));
    }

    @Test
    public void differentKeyProducesDifferentSign() {
        assertNotEquals(MonitorSign.heartbeat(TIMESTAMP, KEY),
                MonitorSign.heartbeat(TIMESTAMP, KEY + "x"));
    }

    @Test
    public void pushSignCoversEveryField() {
        String base = MonitorSign.push("1", "1.00", TIMESTAMP, KEY);
        assertNotEquals(base, MonitorSign.push("2", "1.00", TIMESTAMP, KEY));
        assertNotEquals(base, MonitorSign.push("1", "1.01", TIMESTAMP, KEY));
        assertNotEquals(base, MonitorSign.push("1", "1.00", "1773500001000", KEY));
    }

    @Test
    public void missingKeyYieldsEmptySignInsteadOfCrash() {
        assertEquals("", MonitorSign.heartbeat(TIMESTAMP, ""));
        assertEquals("", MonitorSign.heartbeat(TIMESTAMP, null));
    }

    @Test
    public void priceTextIsAlwaysTwoDecimalsWithoutScientificNotation() {
        assertEquals("1.00", MonitorSign.priceText(1.0d));
        assertEquals("0.01", MonitorSign.priceText(0.01d));
        assertEquals("10.00", MonitorSign.priceText(10d));
        assertEquals("1234.50", MonitorSign.priceText(1234.5d));
        assertEquals("0.01", MonitorSign.priceText(0.005d));
    }

    @Test
    public void timestampIsMillisecondsBecauseServerParsesMillis() {
        long before = System.currentTimeMillis();
        long value = Long.parseLong(MonitorSign.timestamp());
        long after = System.currentTimeMillis();
        assertTrue(value >= before && value <= after);
    }
}