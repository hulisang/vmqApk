package com.vone.vmq;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Process;
import android.util.Log;

/**
 * 性能监控工具类
 * 用于监控应用的启动时间、内存使用和电池消耗
 * 需求: 5.1, 5.2
 */
public class PerformanceMonitor {
    
    private static final String TAG = "PerformanceMonitor";
    private static PerformanceMonitor instance;
    private long applicationStartTime;
    private long lastMemoryCheck;
    private Context context;
    
    private PerformanceMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.applicationStartTime = System.currentTimeMillis();
        this.lastMemoryCheck = System.currentTimeMillis();
    }
    
    public static synchronized PerformanceMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new PerformanceMonitor(context);
        }
        return instance;
    }
    
    /**
     * 记录应用启动开始时间
     */
    public void recordApplicationStart() {
        applicationStartTime = System.currentTimeMillis();
        Log.d(TAG, "应用启动开始时间: " + applicationStartTime);
    }
    
    /**
     * 记录应用启动完成时间并计算启动耗时
     */
    public void recordApplicationReady() {
        long currentTime = System.currentTimeMillis();
        long startupTime = currentTime - applicationStartTime;
        
        Log.i(TAG, "应用启动完成，耗时: " + startupTime + "ms");
        
        // 如果启动时间超过3秒，记录警告
        if (startupTime > 3000) {
            Log.w(TAG, "应用启动时间过长: " + startupTime + "ms");
        }
    }
    
    /**
     * 获取当前内存使用情况
     * @return 内存使用信息
     */
    public MemoryInfo getCurrentMemoryUsage() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        // 获取当前进程的内存使用
        Debug.MemoryInfo debugMemoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(debugMemoryInfo);
        
        MemoryInfo info = new MemoryInfo();
        info.totalMemory = memoryInfo.totalMem;
        info.availableMemory = memoryInfo.availMem;
        info.usedMemory = memoryInfo.totalMem - memoryInfo.availMem;
        info.processMemory = debugMemoryInfo.getTotalPss() * 1024; // PSS in KB, convert to bytes
        info.isLowMemory = memoryInfo.lowMemory;
        
        return info;
    }
    
    /**
     * 监控内存使用情况
     */
    public void monitorMemoryUsage() {
        long currentTime = System.currentTimeMillis();
        
        // 每30秒检查一次内存使用
        if (currentTime - lastMemoryCheck > 30000) {
            MemoryInfo memoryInfo = getCurrentMemoryUsage();
            
            Log.d(TAG, "内存使用情况:");
            Log.d(TAG, "  总内存: " + formatBytes(memoryInfo.totalMemory));
            Log.d(TAG, "  可用内存: " + formatBytes(memoryInfo.availableMemory));
            Log.d(TAG, "  已用内存: " + formatBytes(memoryInfo.usedMemory));
            Log.d(TAG, "  进程内存: " + formatBytes(memoryInfo.processMemory));
            Log.d(TAG, "  内存不足: " + memoryInfo.isLowMemory);
            
            // 如果进程内存使用超过100MB，记录警告
            if (memoryInfo.processMemory > 100 * 1024 * 1024) {
                Log.w(TAG, "进程内存使用过高: " + formatBytes(memoryInfo.processMemory));
            }
            
            // 如果系统内存不足，记录警告
            if (memoryInfo.isLowMemory) {
                Log.w(TAG, "系统内存不足");
            }
            
            lastMemoryCheck = currentTime;
        }
    }
    
    /**
     * 获取CPU使用情况
     * @return CPU使用百分比
     */
    public float getCpuUsage() {
        try {
            // 获取当前进程的CPU使用情况
            int pid = Process.myPid();
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            
            // 这里简化处理，实际实现可能需要更复杂的CPU监控逻辑
            return 0.0f; // 返回模拟值
        } catch (Exception e) {
            Log.e(TAG, "获取CPU使用情况失败", e);
            return 0.0f;
        }
    }
    
    /**
     * 监控网络请求性能
     * @param url 请求URL
     * @param startTime 请求开始时间
     * @param endTime 请求结束时间
     * @param success 请求是否成功
     */
    public void monitorNetworkRequest(String url, long startTime, long endTime, boolean success) {
        long requestTime = endTime - startTime;
        
        Log.d(TAG, "网络请求性能:");
        Log.d(TAG, "  URL: " + url);
        Log.d(TAG, "  耗时: " + requestTime + "ms");
        Log.d(TAG, "  状态: " + (success ? "成功" : "失败"));
        
        // 如果请求时间超过5秒，记录警告
        if (requestTime > 5000) {
            Log.w(TAG, "网络请求耗时过长: " + requestTime + "ms, URL: " + url);
        }
        
        // 如果请求失败，记录错误
        if (!success) {
            Log.e(TAG, "网络请求失败: " + url);
        }
    }
    
    /**
     * 监控后台服务性能
     * @param serviceName 服务名称
     * @param operation 操作类型
     * @param duration 操作耗时
     */
    public void monitorBackgroundService(String serviceName, String operation, long duration) {
        Log.d(TAG, "后台服务性能:");
        Log.d(TAG, "  服务: " + serviceName);
        Log.d(TAG, "  操作: " + operation);
        Log.d(TAG, "  耗时: " + duration + "ms");
        
        // 如果操作耗时超过1秒，记录警告
        if (duration > 1000) {
            Log.w(TAG, "后台服务操作耗时过长: " + duration + "ms, 服务: " + serviceName + ", 操作: " + operation);
        }
    }
    
    /**
     * 优化内存使用
     */
    public void optimizeMemoryUsage() {
        // 建议垃圾回收
        System.gc();
        
        Log.d(TAG, "执行内存优化");
        
        // 检查优化后的内存使用
        MemoryInfo memoryInfo = getCurrentMemoryUsage();
        Log.d(TAG, "优化后进程内存: " + formatBytes(memoryInfo.processMemory));
    }
    
    /**
     * 格式化字节数为可读格式
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 内存信息数据类
     */
    public static class MemoryInfo {
        public long totalMemory;      // 总内存
        public long availableMemory;  // 可用内存
        public long usedMemory;       // 已用内存
        public long processMemory;    // 进程内存
        public boolean isLowMemory;   // 是否内存不足
    }
}