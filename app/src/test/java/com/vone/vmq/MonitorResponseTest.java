package com.vone.vmq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 覆盖监控端与后端 PHP 兼容 envelope 的成功、失败和限流契约。
 */
@RunWith(RobolectricTestRunner.class)
public class MonitorResponseTest {

    @Test
    public void httpSuccessAndBusinessSuccessAreRequiredTogether() throws IOException {
        MonitorResponse success = parse(200, "{\"code\":200,\"msg\":\"成功\",\"data\":null}", null);
        assertTrue(success.isSuccessful());

        MonitorResponse businessFailure = parse(200, "{\"code\":400,\"msg\":\"签名校验不通过\",\"data\":null}", null);
        assertFalse(businessFailure.isSuccessful());
        assertTrue(businessFailure.summary().contains("业务 code=400"));

        MonitorResponse transportFailure = parse(500, "{\"code\":500,\"msg\":\"服务异常\",\"data\":null}", null);
        assertFalse(transportFailure.isSuccessful());
    }

    @Test
    public void invalidEnvelopeNeverCountsAsSuccessful() throws IOException {
        MonitorResponse invalid = parse(200, "not-json", null);
        assertFalse(invalid.isSuccessful());
        assertTrue(invalid.summary().contains("响应格式错误"));
    }

    @Test
    public void retryAfterIsAppliedOnlyForRateLimitAndCappedForForeground() throws IOException {
        MonitorResponse limited = parse(429, "{\"code\":429,\"msg\":\"请求过于频繁\",\"data\":null}", "120");
        assertFalse(limited.isSuccessful());
        assertEquals(10_000L, limited.foregroundRetryDelayMillis(2_000L));

        MonitorResponse businessFailure = parse(200, "{\"code\":400,\"msg\":\"签名错误\",\"data\":null}", "120");
        assertEquals(2_000L, businessFailure.foregroundRetryDelayMillis(2_000L));
    }

    @Test
    public void monitorSignatureIsRedactedBeforeLogging() {
        String safeUrl = Utils.redactMonitorUrl(
                "http://example.test/api/monitor/push?t=1&type=1&price=1.00&sign=abc123"
        );
        assertTrue(safeUrl.contains("sign=[redacted]"));
        assertFalse(safeUrl.contains("abc123"));
    }

    private MonitorResponse parse(int httpStatus, String body, String retryAfter) throws IOException {
        Request request = new Request.Builder().url("http://example.test/api/monitor/heart").build();
        Response.Builder builder = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(httpStatus)
                .message("test")
                .body(ResponseBody.create(body, null));
        if (retryAfter != null) {
            builder.header("Retry-After", retryAfter);
        }
        Response response = builder.build();
        try {
            return MonitorResponse.from(response);
        } finally {
            response.close();
        }
    }
}