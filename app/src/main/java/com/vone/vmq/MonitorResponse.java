package com.vone.vmq;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 解析监控接口的 HTTP 状态和 PHP 兼容 envelope，避免把业务失败误判为请求成功。
 */
final class MonitorResponse {

    static final long MAX_FOREGROUND_RETRY_DELAY_MILLIS = 10_000L;

    private static final int SUCCESS_BUSINESS_CODE = 200;
    private static final int UNKNOWN_BUSINESS_CODE = Integer.MIN_VALUE;
    private static final int RATE_LIMIT_STATUS = 429;
    private static final long MAX_RETRY_AFTER_MILLIS = 5 * 60 * 1000L;
    private static final int MAX_MESSAGE_LENGTH = 120;

    private final boolean successful;
    private final int httpStatus;
    private final int businessCode;
    private final String message;
    private final long retryAfterMillis;

    private MonitorResponse(
            boolean successful,
            int httpStatus,
            int businessCode,
            String message,
            long retryAfterMillis
    ) {
        this.successful = successful;
        this.httpStatus = httpStatus;
        this.businessCode = businessCode;
        this.message = message;
        this.retryAfterMillis = retryAfterMillis;
    }

    /**
     * 监控接口必须同时满足 HTTP 成功和 envelope code=200，才代表服务端已接受请求。
     */
    static MonitorResponse from(Response response) throws IOException {
        if (response == null) {
            return new MonitorResponse(false, 0, UNKNOWN_BUSINESS_CODE, "未收到服务端响应", 0L);
        }

        int httpStatus = response.code();
        long retryAfterMillis = parseRetryAfterMillis(response.header("Retry-After"));
        ResponseBody body = response.body();
        String rawBody = body == null ? "" : body.string();
        int businessCode = UNKNOWN_BUSINESS_CODE;
        String message = "";
        try {
            JSONObject envelope = new JSONObject(rawBody);
            if (envelope.has("code")) {
                businessCode = envelope.optInt("code", UNKNOWN_BUSINESS_CODE);
            }
            message = sanitizeMessage(envelope.optString("msg", ""));
        } catch (JSONException ignored) {
            message = rawBody.trim().isEmpty() ? "服务端未返回业务结果" : "服务端响应格式错误";
        }

        boolean successful = response.isSuccessful() && businessCode == SUCCESS_BUSINESS_CODE;
        if (message.isEmpty()) {
            if (successful) {
                message = "成功";
            } else if (!response.isSuccessful()) {
                message = "HTTP 请求失败";
            } else if (businessCode == UNKNOWN_BUSINESS_CODE) {
                message = "服务端响应格式错误";
            } else {
                message = "服务端业务处理失败";
            }
        }

        return new MonitorResponse(successful, httpStatus, businessCode, message, retryAfterMillis);
    }

    boolean isSuccessful() {
        return successful;
    }

    /**
     * 429 时使用服务端 Retry-After；前台服务最长只等待十秒，避免异常响应让服务长期滞留。
     */
    long foregroundRetryDelayMillis(long fallbackMillis) {
        long fallback = Math.max(0L, fallbackMillis);
        if (!isRateLimited() || retryAfterMillis <= 0L) {
            return fallback;
        }
        return Math.max(fallback, Math.min(retryAfterMillis, MAX_FOREGROUND_RETRY_DELAY_MILLIS));
    }

    /**
     * 读取 Intent 中的延迟参数时同样限制上界，防止异常数据导致前台服务无限等待。
     */
    static long normalizeForegroundRetryDelayMillis(long requestedMillis, long fallbackMillis) {
        long fallback = Math.max(0L, fallbackMillis);
        if (requestedMillis <= 0L) {
            return fallback;
        }
        return Math.max(fallback, Math.min(requestedMillis, MAX_FOREGROUND_RETRY_DELAY_MILLIS));
    }

    String summary() {
        StringBuilder summary = new StringBuilder();
        if (httpStatus > 0) {
            summary.append("HTTP ").append(httpStatus);
        } else {
            summary.append("未取得 HTTP 状态");
        }
        if (businessCode != UNKNOWN_BUSINESS_CODE) {
            summary.append("，业务 code=").append(businessCode);
        }
        if (!message.isEmpty()) {
            summary.append("，").append(message);
        }
        return summary.toString();
    }

    private boolean isRateLimited() {
        return httpStatus == RATE_LIMIT_STATUS || businessCode == RATE_LIMIT_STATUS;
    }

    private static long parseRetryAfterMillis(String rawValue) {
        if (rawValue == null) {
            return 0L;
        }
        try {
            long seconds = Long.parseLong(rawValue.trim());
            if (seconds <= 0L) {
                return 0L;
            }
            long maxSeconds = MAX_RETRY_AFTER_MILLIS / 1000L;
            return Math.min(seconds, maxSeconds) * 1000L;
        } catch (NumberFormatException ignored) {
            // 后端约定 Retry-After 使用秒数；无效值回退到原有的短暂重试间隔。
            return 0L;
        }
    }

    private static String sanitizeMessage(String value) {
        String message = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH) + "…";
    }
}