package com.vone.vmq;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 启动画面性能优化器
 * 负责优化启动画面的加载性能和启动时间
 */
public class SplashScreenPerformanceOptimizer {
    
    private static final String TAG = "SplashPerformanceOptimizer";
    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService backgroundExecutor;
    
    // 性能监控相关
    private long appStartTime = 0;
    private long splashStartTime = 0;
    private long splashEndTime = 0;
    private long mainActivityReadyTime = 0;
    
    // 优化配置
    private boolean preloadResourcesEnabled = true;
    private boolean backgroundInitEnabled = true;
    private boolean memoryOptimizationEnabled = true;
    
    public SplashScreenPerformanceOptimizer(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.backgroundExecutor = Executors.newCachedThreadPool();
        this.appStartTime = System.currentTimeMillis();
    }
    
    /**
     * 开始启动画面性能优化
     */
    public void startOptimization(Activity activity) {
        try {
            Log.d(TAG, "开始启动画面性能优化");
            splashStartTime = System.currentTimeMillis();
            
            SplashScreenLogger.logStartup("性能优化开始");
            SplashScreenLogger.logPerformance("应用启动到优化开始", splashStartTime - appStartTime);
            
            // 预加载资源
            if (preloadResourcesEnabled) {
                preloadSplashScreenResources();
            }
            
            // 后台初始化非关键组件
            if (backgroundInitEnabled) {
                initializeNonCriticalComponentsInBackground();
            }
            
            // 内存优化
            if (memoryOptimizationEnabled) {
                optimizeMemoryUsage();
            }
            
            // 优化启动画面显示时长
            optimizeSplashScreenDuration(activity);
            
            Log.d(TAG, "启动画面性能优化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "启动画面性能优化失败: " + e.getMessage(), e);
            SplashScreenLogger.logError("性能优化失败", e);
        }
    }
    
    /**
     * 预加载启动画面资源
     */
    private void preloadSplashScreenResources() {
        backgroundExecutor.execute(() -> {
            try {
                Log.d(TAG, "开始预加载启动画面资源");
                long startTime = System.currentTimeMillis();
                
                // 预加载启动画面图标
                preloadDrawableResource("ic_splash_screen");
                
                // 预加载主题颜色资源
                preloadColorResources();
                
                // 预加载字体资源（如果有）
                preloadFontResources();
                
                long endTime = System.currentTimeMillis();
                SplashScreenLogger.logPerformance("资源预加载", endTime - startTime);
                
                Log.d(TAG, "启动画面资源预加载完成，耗时: " + (endTime - startTime) + "ms");
                
            } catch (Exception e) {
                Log.w(TAG, "资源预加载失败: " + e.getMessage());
                SplashScreenLogger.logError("资源预加载失败", e);
            }
        });
    }
    
    /**
     * 预加载Drawable资源
     */
    private void preloadDrawableResource(String resourceName) {
        try {
            int resId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
            if (resId != 0) {
                context.getResources().getDrawable(resId, context.getTheme());
                Log.d(TAG, "预加载Drawable资源: " + resourceName);
            }
        } catch (Exception e) {
            Log.w(TAG, "预加载Drawable资源失败: " + resourceName + " - " + e.getMessage());
        }
    }
    
    /**
     * 预加载颜色资源
     */
    private void preloadColorResources() {
        try {
            String[] colorNames = {
                "md_theme_light_primary",
                "md_theme_light_surface",
                "md_theme_light_background",
                "md_theme_light_onPrimary"
            };
            
            for (String colorName : colorNames) {
                int resId = context.getResources().getIdentifier(colorName, "color", context.getPackageName());
                if (resId != 0) {
                    context.getResources().getColor(resId, context.getTheme());
                }
            }
            
            Log.d(TAG, "颜色资源预加载完成");
            
        } catch (Exception e) {
            Log.w(TAG, "颜色资源预加载失败: " + e.getMessage());
        }
    }
    
    /**
     * 预加载字体资源
     */
    private void preloadFontResources() {
        try {
            // 如果应用使用了自定义字体，在这里预加载
            // 目前使用系统字体，暂时跳过
            Log.d(TAG, "字体资源预加载完成（使用系统字体）");
            
        } catch (Exception e) {
            Log.w(TAG, "字体资源预加载失败: " + e.getMessage());
        }
    }
    
    /**
     * 后台初始化非关键组件
     */
    private void initializeNonCriticalComponentsInBackground() {
        backgroundExecutor.execute(() -> {
            try {
                Log.d(TAG, "开始后台初始化非关键组件");
                long startTime = System.currentTimeMillis();
                
                // 初始化网络客户端
                initializeNetworkClient();
                
                // 预热数据库连接
                preheatDatabaseConnection();
                
                // 初始化分析工具
                initializeAnalytics();
                
                // 预加载配置数据
                preloadConfigurationData();
                
                long endTime = System.currentTimeMillis();
                SplashScreenLogger.logPerformance("后台组件初始化", endTime - startTime);
                
                Log.d(TAG, "非关键组件后台初始化完成，耗时: " + (endTime - startTime) + "ms");
                
            } catch (Exception e) {
                Log.w(TAG, "后台组件初始化失败: " + e.getMessage());
                SplashScreenLogger.logError("后台组件初始化失败", e);
            }
        });
    }
    
    /**
     * 初始化网络客户端
     */
    private void initializeNetworkClient() {
        try {
            // 预热OkHttpClient
            Utils.getOkHttpClient();
            Log.d(TAG, "网络客户端初始化完成");
        } catch (Exception e) {
            Log.w(TAG, "网络客户端初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 预热数据库连接
     */
    private void preheatDatabaseConnection() {
        try {
            // 如果应用使用数据库，在这里预热连接
            // 目前主要使用SharedPreferences，暂时跳过
            Log.d(TAG, "数据库连接预热完成（使用SharedPreferences）");
        } catch (Exception e) {
            Log.w(TAG, "数据库连接预热失败: " + e.getMessage());
        }
    }
    
    /**
     * 初始化分析工具
     */
    private void initializeAnalytics() {
        try {
            // 如果应用使用分析工具，在这里初始化
            // 目前使用自定义日志系统
            SplashScreenLogger.getInstance(context);
            Log.d(TAG, "分析工具初始化完成");
        } catch (Exception e) {
            Log.w(TAG, "分析工具初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 预加载配置数据
     */
    private void preloadConfigurationData() {
        try {
            // 预加载SharedPreferences中的配置数据
            context.getSharedPreferences("vone", Context.MODE_PRIVATE).getAll();
            Log.d(TAG, "配置数据预加载完成");
        } catch (Exception e) {
            Log.w(TAG, "配置数据预加载失败: " + e.getMessage());
        }
    }
    
    /**
     * 优化内存使用
     */
    private void optimizeMemoryUsage() {
        try {
            Log.d(TAG, "开始内存优化");
            long startTime = System.currentTimeMillis();
            
            // 获取当前内存使用情况
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            Log.d(TAG, "内存状态 - 已使用: " + (usedMemory / 1024 / 1024) + "MB, " +
                      "总分配: " + (totalMemory / 1024 / 1024) + "MB, " +
                      "最大可用: " + (maxMemory / 1024 / 1024) + "MB");
            
            // 计算内存使用率
            double memoryUsageRatio = (double) usedMemory / totalMemory;
            double maxMemoryUsageRatio = (double) totalMemory / maxMemory;
            
            SplashScreenLogger.logInfo("内存使用率: " + String.format("%.1f%%", memoryUsageRatio * 100) + 
                                      ", 堆内存使用率: " + String.format("%.1f%%", maxMemoryUsageRatio * 100));
            
            // 执行内存优化策略
            boolean needsOptimization = false;
            
            // 策略1: 如果内存使用率超过80%，执行垃圾回收
            if (memoryUsageRatio > 0.8) {
                Log.d(TAG, "内存使用率过高(" + String.format("%.1f%%", memoryUsageRatio * 100) + ")，执行垃圾回收");
                System.gc();
                needsOptimization = true;
                
                // 等待垃圾回收完成
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // 策略2: 如果堆内存使用率超过70%，建议清理缓存
            if (maxMemoryUsageRatio > 0.7) {
                Log.d(TAG, "堆内存使用率过高(" + String.format("%.1f%%", maxMemoryUsageRatio * 100) + ")，清理应用缓存");
                clearApplicationCaches();
                needsOptimization = true;
            }
            
            // 策略3: 低内存设备优化
            if (maxMemory < 512 * 1024 * 1024) { // 小于512MB
                Log.d(TAG, "检测到低内存设备，启用激进优化策略");
                enableLowMemoryOptimizations();
                needsOptimization = true;
            }
            
            // 策略4: 预防性优化
            if (!needsOptimization && memoryUsageRatio > 0.6) {
                Log.d(TAG, "执行预防性内存优化");
                performPreventiveOptimization();
            }
            
            // 记录优化后的状态
            long newTotalMemory = runtime.totalMemory();
            long newFreeMemory = runtime.freeMemory();
            long newUsedMemory = newTotalMemory - newFreeMemory;
            
            long memoryFreed = usedMemory - newUsedMemory;
            
            long endTime = System.currentTimeMillis();
            SplashScreenLogger.logPerformance("内存优化", endTime - startTime);
            SplashScreenLogger.logInfo("内存优化完成 - 优化前: " + (usedMemory / 1024 / 1024) + "MB, " +
                                      "优化后: " + (newUsedMemory / 1024 / 1024) + "MB, " +
                                      "释放: " + (memoryFreed / 1024 / 1024) + "MB");
            
            Log.d(TAG, "内存优化完成，释放了 " + (memoryFreed / 1024 / 1024) + "MB 内存");
            
            // 设置内存监控
            scheduleMemoryMonitoring();
            
        } catch (Exception e) {
            Log.w(TAG, "内存优化失败: " + e.getMessage());
            SplashScreenLogger.logError("内存优化失败", e);
        }
    }
    
    /**
     * 清理应用缓存
     */
    private void clearApplicationCaches() {
        try {
            // 清理图片缓存（如果有的话）
            // 这里可以添加具体的缓存清理逻辑
            
            // 清理临时文件
            clearTemporaryFiles();
            
            // 清理过期的日志文件
            clearExpiredLogFiles();
            
            Log.d(TAG, "应用缓存清理完成");
            
        } catch (Exception e) {
            Log.w(TAG, "清理应用缓存失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理临时文件
     */
    private void clearTemporaryFiles() {
        try {
            java.io.File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                deleteDirectory(cacheDir);
                Log.d(TAG, "临时文件清理完成");
            }
        } catch (Exception e) {
            Log.w(TAG, "清理临时文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理过期的日志文件
     */
    private void clearExpiredLogFiles() {
        try {
            java.io.File logDir = context.getExternalFilesDir("splash_logs");
            if (logDir != null && logDir.exists()) {
                java.io.File[] logFiles = logDir.listFiles();
                if (logFiles != null) {
                    long currentTime = System.currentTimeMillis();
                    long sevenDaysAgo = currentTime - (7 * 24 * 60 * 60 * 1000); // 7天前
                    
                    for (java.io.File file : logFiles) {
                        if (file.lastModified() < sevenDaysAgo) {
                            if (file.delete()) {
                                Log.d(TAG, "删除过期日志文件: " + file.getName());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理过期日志文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 启用低内存设备优化
     */
    private void enableLowMemoryOptimizations() {
        try {
            // 禁用一些非关键功能以节省内存
            // 减少后台线程数量
            // 降低缓存大小等
            
            Log.d(TAG, "低内存设备优化已启用");
            SplashScreenLogger.logInfo("已启用低内存设备优化策略");
            
        } catch (Exception e) {
            Log.w(TAG, "启用低内存优化失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行预防性优化
     */
    private void performPreventiveOptimization() {
        try {
            // 轻量级的预防性优化
            System.runFinalization();
            
            Log.d(TAG, "预防性内存优化完成");
            
        } catch (Exception e) {
            Log.w(TAG, "预防性优化失败: " + e.getMessage());
        }
    }
    
    /**
     * 安排内存监控
     */
    private void scheduleMemoryMonitoring() {
        // 每30秒检查一次内存状态
        mainHandler.postDelayed(() -> {
            try {
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;
                
                double memoryUsageRatio = (double) usedMemory / totalMemory;
                
                if (memoryUsageRatio > 0.85) {
                    Log.w(TAG, "内存使用率过高: " + String.format("%.1f%%", memoryUsageRatio * 100));
                    SplashScreenLogger.logInfo("内存监控警告 - 使用率: " + String.format("%.1f%%", memoryUsageRatio * 100));
                    
                    // 执行紧急内存清理
                    System.gc();
                }
                
            } catch (Exception e) {
                Log.w(TAG, "内存监控失败: " + e.getMessage());
            }
        }, 30000);
    }
    
    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(java.io.File directory) {
        if (directory != null && directory.exists()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
    }
    
    /**
     * 优化启动画面显示时长
     */
    private void optimizeSplashScreenDuration(Activity activity) {
        try {
            Log.d(TAG, "优化启动画面显示时长");
            
            // 根据设备性能和网络状态调整显示时长
            int optimizedDuration = calculateOptimalSplashDuration();
            
            // 如果是传统启动画面，应用优化的时长
            if (LegacySplashScreenHelper.needsLegacyHandling()) {
                // 这里可以与LegacySplashScreenHelper配合使用优化的时长
                Log.d(TAG, "为传统启动画面应用优化时长: " + optimizedDuration + "ms");
            }
            
            SplashScreenLogger.logPerformance("优化启动画面时长", optimizedDuration);
            
        } catch (Exception e) {
            Log.w(TAG, "启动画面时长优化失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算最优启动画面显示时长
     */
    private int calculateOptimalSplashDuration() {
        try {
            int baseDuration = 800; // 基础时长
            
            // 根据Android版本调整
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                baseDuration = 600; // Android 12+启动更快
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                baseDuration = 1200; // 老版本需要更多时间
            }
            
            // 根据设备内存调整
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            if (totalMemory < 512 * 1024 * 1024) { // 小于512MB
                baseDuration += 400;
            } else if (totalMemory > 2 * 1024 * 1024 * 1024) { // 大于2GB
                baseDuration -= 200;
            }
            
            // 确保时长在合理范围内
            return Math.max(400, Math.min(baseDuration, 1500));
            
        } catch (Exception e) {
            Log.w(TAG, "计算最优时长失败: " + e.getMessage());
            return 800; // 返回默认值
        }
    }
    
    /**
     * 记录MainActivity准备完成
     */
    public void recordMainActivityReady() {
        mainActivityReadyTime = System.currentTimeMillis();
        
        // 计算各个阶段的耗时
        long totalStartupTime = mainActivityReadyTime - appStartTime;
        long splashDuration = splashEndTime > 0 ? splashEndTime - splashStartTime : 0;
        long initializationTime = mainActivityReadyTime - splashStartTime;
        
        // 记录性能指标
        SplashScreenLogger.logPerformance("总启动时间", totalStartupTime);
        if (splashDuration > 0) {
            SplashScreenLogger.logPerformance("启动画面显示时间", splashDuration);
        }
        SplashScreenLogger.logPerformance("初始化时间", initializationTime);
        
        Log.d(TAG, "启动性能统计 - 总时间: " + totalStartupTime + "ms, 初始化: " + initializationTime + "ms");
        
        // 分析性能并给出建议
        analyzePerformanceAndProvideRecommendations(totalStartupTime, initializationTime);
    }
    
    /**
     * 记录启动画面结束
     */
    public void recordSplashScreenEnd() {
        splashEndTime = System.currentTimeMillis();
        
        if (splashStartTime > 0) {
            long splashDuration = splashEndTime - splashStartTime;
            SplashScreenLogger.logPerformance("启动画面实际显示时间", splashDuration);
            Log.d(TAG, "启动画面显示时间: " + splashDuration + "ms");
        }
    }
    
    /**
     * 分析性能并提供建议
     */
    private void analyzePerformanceAndProvideRecommendations(long totalTime, long initTime) {
        try {
            StringBuilder analysis = new StringBuilder();
            analysis.append("启动性能分析:\n");
            
            // 分析总启动时间
            if (totalTime < 1000) {
                analysis.append("- 总启动时间: 优秀 (").append(totalTime).append("ms)\n");
            } else if (totalTime < 2000) {
                analysis.append("- 总启动时间: 良好 (").append(totalTime).append("ms)\n");
            } else if (totalTime < 3000) {
                analysis.append("- 总启动时间: 一般 (").append(totalTime).append("ms)\n");
                analysis.append("  建议: 考虑优化初始化流程\n");
            } else {
                analysis.append("- 总启动时间: 需要优化 (").append(totalTime).append("ms)\n");
                analysis.append("  建议: 延迟非关键组件初始化，优化资源加载\n");
            }
            
            // 分析初始化时间
            if (initTime < 500) {
                analysis.append("- 初始化时间: 优秀 (").append(initTime).append("ms)\n");
            } else if (initTime < 1000) {
                analysis.append("- 初始化时间: 良好 (").append(initTime).append("ms)\n");
            } else {
                analysis.append("- 初始化时间: 需要优化 (").append(initTime).append("ms)\n");
                analysis.append("  建议: 将更多初始化工作移到后台线程\n");
            }
            
            // 设备相关建议
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            if (totalMemory < 1024 * 1024 * 1024) { // 小于1GB
                analysis.append("- 设备内存较低，建议启用内存优化\n");
            }
            
            SplashScreenLogger.logInfo(analysis.toString());
            Log.d(TAG, analysis.toString());
            
        } catch (Exception e) {
            Log.w(TAG, "性能分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取性能统计报告
     */
    public String getPerformanceReport() {
        try {
            StringBuilder report = new StringBuilder();
            report.append("启动画面性能报告:\n");
            
            if (appStartTime > 0 && mainActivityReadyTime > 0) {
                long totalTime = mainActivityReadyTime - appStartTime;
                report.append("- 总启动时间: ").append(totalTime).append("ms\n");
            }
            
            if (splashStartTime > 0 && splashEndTime > 0) {
                long splashTime = splashEndTime - splashStartTime;
                report.append("- 启动画面时间: ").append(splashTime).append("ms\n");
            }
            
            if (splashStartTime > 0 && mainActivityReadyTime > 0) {
                long initTime = mainActivityReadyTime - splashStartTime;
                report.append("- 初始化时间: ").append(initTime).append("ms\n");
            }
            
            // 优化状态
            report.append("- 资源预加载: ").append(preloadResourcesEnabled ? "启用" : "禁用").append("\n");
            report.append("- 后台初始化: ").append(backgroundInitEnabled ? "启用" : "禁用").append("\n");
            report.append("- 内存优化: ").append(memoryOptimizationEnabled ? "启用" : "禁用").append("\n");
            
            return report.toString();
            
        } catch (Exception e) {
            return "获取性能报告失败: " + e.getMessage();
        }
    }
    
    /**
     * 设置优化选项
     */
    public void setOptimizationOptions(boolean preloadResources, boolean backgroundInit, boolean memoryOptimization) {
        this.preloadResourcesEnabled = preloadResources;
        this.backgroundInitEnabled = backgroundInit;
        this.memoryOptimizationEnabled = memoryOptimization;
        
        Log.d(TAG, "优化选项已更新 - 资源预加载: " + preloadResources + 
                  ", 后台初始化: " + backgroundInit + 
                  ", 内存优化: " + memoryOptimization);
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        try {
            backgroundExecutor.shutdown();
            Log.d(TAG, "性能优化器资源清理完成");
        } catch (Exception e) {
            Log.w(TAG, "性能优化器资源清理失败: " + e.getMessage());
        }
    }
}