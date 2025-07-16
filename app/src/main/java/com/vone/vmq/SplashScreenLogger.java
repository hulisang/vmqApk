package com.vone.vmq;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 启动画面日志记录系统
 * 专门用于记录启动画面相关的日志信息，包括性能监控和错误追踪
 */
public class SplashScreenLogger {
    
    private static final String TAG = "SplashScreenLogger";
    private static final String LOG_FILE_NAME = "splash_screen_log.txt";
    private static final String PERFORMANCE_LOG_FILE_NAME = "splash_performance_log.txt";
    private static final int MAX_LOG_FILE_SIZE = 1024 * 1024; // 1MB
    
    private static SplashScreenLogger instance;
    private final Context context;
    private final ExecutorService logExecutor;
    private final SimpleDateFormat dateFormat;
    private final Object logLock = new Object();
    
    // 性能监控相关
    private long splashStartTime = 0;
    private long splashEndTime = 0;
    private boolean performanceMonitoringEnabled = true;
    
    private SplashScreenLogger(Context context) {
        this.context = context.getApplicationContext();
        this.logExecutor = Executors.newSingleThreadExecutor();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized SplashScreenLogger getInstance(Context context) {
        if (instance == null) {
            instance = new SplashScreenLogger(context);
        }
        return instance;
    }
    
    /**
     * 记录启动画面启动阶段
     */
    public static void logStartup(String phase) {
        logStartup(phase, null);
    }
    
    /**
     * 记录启动画面启动阶段
     */
    public static void logStartup(String phase, String details) {
        try {
            long currentTime = System.currentTimeMillis();
            String message = String.format("[STARTUP] %s - %s", phase, 
                details != null ? details : "时间: " + currentTime);
            
            Log.d(TAG, message);
            
            if (instance != null) {
                instance.writeToLogFile(message, LogType.STARTUP);
                
                // 性能监控
                if (instance.performanceMonitoringEnabled) {
                    if ("开始".equals(phase) || "启动画面显示".equals(phase)) {
                        instance.splashStartTime = currentTime;
                    } else if ("完成".equals(phase) || "主界面显示".equals(phase)) {
                        instance.splashEndTime = currentTime;
                        instance.logPerformanceMetrics();
                    }
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "记录启动日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录启动画面错误
     */
    public static void logError(String error, Throwable throwable) {
        try {
            String message = String.format("[ERROR] %s", error);
            if (throwable != null) {
                message += " - " + throwable.getMessage();
            }
            
            Log.e(TAG, message, throwable);
            
            if (instance != null) {
                instance.writeToLogFile(message, LogType.ERROR);
                
                // 记录详细的异常堆栈
                if (throwable != null) {
                    instance.writeToLogFile(Log.getStackTraceString(throwable), LogType.ERROR);
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "记录错误日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录启动画面配置信息
     */
    public static void logConfiguration(SplashScreenConfig config) {
        try {
            String message = String.format("[CONFIG] %s", config != null ? config.toString() : "null");
            Log.i(TAG, message);
            
            if (instance != null) {
                instance.writeToLogFile(message, LogType.CONFIGURATION);
                
                if (config != null) {
                    instance.writeToLogFile(config.getConfigSummary(), LogType.CONFIGURATION);
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "记录配置日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录诊断结果
     */
    public static void logDiagnosticResult(DiagnosticResult result) {
        try {
            String message = String.format("[DIAGNOSTIC] %s", result != null ? result.toString() : "null");
            Log.i(TAG, message);
            
            if (instance != null) {
                instance.writeToLogFile(message, LogType.DIAGNOSTIC);
                
                if (result != null && result.hasIssues()) {
                    for (String issue : result.getIssues()) {
                        instance.writeToLogFile("[ISSUE] " + issue, LogType.DIAGNOSTIC);
                    }
                    for (String recommendation : result.getRecommendations()) {
                        instance.writeToLogFile("[RECOMMENDATION] " + recommendation, LogType.DIAGNOSTIC);
                    }
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "记录诊断日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录性能指标
     */
    public static void logPerformance(String metric, long value) {
        try {
            String message = String.format("[PERFORMANCE] %s: %d ms", metric, value);
            Log.d(TAG, message);
            
            if (instance != null) {
                instance.writeToPerformanceLog(message);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "记录性能日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录设备信息
     */
    public static void logDeviceInfo(SplashScreenSupport support) {
        try {
            String message = String.format("[DEVICE] %s", support != null ? support.toString() : "null");
            Log.i(TAG, message);
            
            if (instance != null) {
                instance.writeToLogFile(message, LogType.DEVICE_INFO);
                
                if (support != null) {
                    instance.writeToLogFile(support.getSupportSummary(), LogType.DEVICE_INFO);
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "记录设备信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录一般信息
     */
    public static void logInfo(String message) {
        try {
            String logMessage = String.format("[INFO] %s", message);
            Log.i(TAG, logMessage);
            
            if (instance != null) {
                instance.writeToLogFile(logMessage, LogType.INFO);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "记录信息日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 写入日志文件
     */
    private void writeToLogFile(String message, LogType type) {
        logExecutor.execute(() -> {
            synchronized (logLock) {
                try {
                    File logFile = getLogFile(LOG_FILE_NAME);
                    if (logFile == null) return;
                    
                    // 检查文件大小，如果过大则清理
                    if (logFile.length() > MAX_LOG_FILE_SIZE) {
                        cleanupLogFile(logFile);
                    }
                    
                    String timestamp = dateFormat.format(new Date());
                    String logEntry = String.format("%s [%s] %s%n", timestamp, type.name(), message);
                    
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                        writer.write(logEntry);
                        writer.flush();
                    }
                    
                    // 同时发送到应用的日志广播系统
                    Utils.sendLogBroadcast(context, "[启动画面] " + message);
                    
                } catch (IOException e) {
                    Log.w(TAG, "写入日志文件失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 写入性能日志
     */
    private void writeToPerformanceLog(String message) {
        logExecutor.execute(() -> {
            synchronized (logLock) {
                try {
                    File logFile = getLogFile(PERFORMANCE_LOG_FILE_NAME);
                    if (logFile == null) return;
                    
                    String timestamp = dateFormat.format(new Date());
                    String logEntry = String.format("%s %s%n", timestamp, message);
                    
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                        writer.write(logEntry);
                        writer.flush();
                    }
                    
                } catch (IOException e) {
                    Log.w(TAG, "写入性能日志失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 记录性能指标
     */
    private void logPerformanceMetrics() {
        if (splashStartTime > 0 && splashEndTime > 0) {
            long duration = splashEndTime - splashStartTime;
            logPerformance("启动画面总时长", duration);
            
            // 记录设备信息以便分析性能
            String deviceInfo = String.format("设备: %s %s, Android %s (API %d)", 
                Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT);
            writeToPerformanceLog("[DEVICE] " + deviceInfo);
            
            // 重置计时器
            splashStartTime = 0;
            splashEndTime = 0;
        }
    }
    
    /**
     * 获取日志文件
     */
    private File getLogFile(String fileName) {
        try {
            File logDir = context.getExternalFilesDir("splash_logs");
            if (logDir == null) {
                logDir = new File(context.getFilesDir(), "splash_logs");
            }
            
            if (!logDir.exists() && !logDir.mkdirs()) {
                Log.w(TAG, "无法创建日志目录");
                return null;
            }
            
            return new File(logDir, fileName);
            
        } catch (Exception e) {
            Log.w(TAG, "获取日志文件失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 清理日志文件
     */
    private void cleanupLogFile(File logFile) {
        try {
            if (logFile.exists() && logFile.delete()) {
                Log.d(TAG, "已清理日志文件: " + logFile.getName());
            }
        } catch (Exception e) {
            Log.w(TAG, "清理日志文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取日志文件内容
     */
    public String getLogFileContent() {
        return getLogFileContent(LOG_FILE_NAME);
    }
    
    /**
     * 获取性能日志文件内容
     */
    public String getPerformanceLogContent() {
        return getLogFileContent(PERFORMANCE_LOG_FILE_NAME);
    }
    
    /**
     * 获取指定日志文件内容
     */
    private String getLogFileContent(String fileName) {
        try {
            File logFile = getLogFile(fileName);
            if (logFile == null || !logFile.exists()) {
                return "日志文件不存在";
            }
            
            StringBuilder content = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            return content.toString();
            
        } catch (Exception e) {
            Log.w(TAG, "读取日志文件失败: " + e.getMessage());
            return "读取日志文件失败: " + e.getMessage();
        }
    }
    
    /**
     * 清理所有日志文件
     */
    public void clearAllLogs() {
        logExecutor.execute(() -> {
            synchronized (logLock) {
                try {
                    File logFile = getLogFile(LOG_FILE_NAME);
                    if (logFile != null && logFile.exists()) {
                        cleanupLogFile(logFile);
                    }
                    
                    File performanceLogFile = getLogFile(PERFORMANCE_LOG_FILE_NAME);
                    if (performanceLogFile != null && performanceLogFile.exists()) {
                        cleanupLogFile(performanceLogFile);
                    }
                    
                    Log.d(TAG, "已清理所有启动画面日志");
                    
                } catch (Exception e) {
                    Log.w(TAG, "清理日志失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 设置性能监控开关
     */
    public void setPerformanceMonitoringEnabled(boolean enabled) {
        this.performanceMonitoringEnabled = enabled;
        Log.d(TAG, "性能监控已" + (enabled ? "启用" : "禁用"));
    }
    
    /**
     * 获取日志统计信息
     */
    public String getLogStatistics() {
        try {
            File logFile = getLogFile(LOG_FILE_NAME);
            File performanceLogFile = getLogFile(PERFORMANCE_LOG_FILE_NAME);
            
            StringBuilder stats = new StringBuilder();
            stats.append("启动画面日志统计:\n");
            
            if (logFile != null && logFile.exists()) {
                stats.append("- 主日志文件大小: ").append(logFile.length()).append(" 字节\n");
                stats.append("- 主日志文件路径: ").append(logFile.getAbsolutePath()).append("\n");
            } else {
                stats.append("- 主日志文件: 不存在\n");
            }
            
            if (performanceLogFile != null && performanceLogFile.exists()) {
                stats.append("- 性能日志文件大小: ").append(performanceLogFile.length()).append(" 字节\n");
                stats.append("- 性能日志文件路径: ").append(performanceLogFile.getAbsolutePath()).append("\n");
            } else {
                stats.append("- 性能日志文件: 不存在\n");
            }
            
            stats.append("- 性能监控: ").append(performanceMonitoringEnabled ? "启用" : "禁用");
            
            return stats.toString();
            
        } catch (Exception e) {
            return "获取日志统计失败: " + e.getMessage();
        }
    }
    
    /**
     * 关闭日志系统
     */
    public void shutdown() {
        try {
            logExecutor.shutdown();
            Log.d(TAG, "启动画面日志系统已关闭");
        } catch (Exception e) {
            Log.w(TAG, "关闭日志系统失败: " + e.getMessage());
        }
    }
    
    /**
     * 日志类型枚举
     */
    private enum LogType {
        STARTUP,        // 启动相关
        ERROR,          // 错误
        CONFIGURATION,  // 配置
        DIAGNOSTIC,     // 诊断
        PERFORMANCE,    // 性能
        DEVICE_INFO,    // 设备信息
        INFO           // 一般信息
    }
}