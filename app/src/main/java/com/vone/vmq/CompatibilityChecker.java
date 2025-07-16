package com.vone.vmq;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

/**
 * 版本兼容性检查工具
 * 提供功能降级方案和兼容性检查
 * 需求: 2.4, 4.2
 */
public class CompatibilityChecker {
    
    private static final String TAG = "CompatibilityChecker";
    private static final int MIN_SUPPORTED_SDK = 23; // Android 6.0
    private static final int TARGET_SDK = 34; // Android 14
    
    private Context context;
    
    public CompatibilityChecker(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * 检查设备兼容性
     * @return 兼容性检查结果
     */
    public CompatibilityResult checkCompatibility() {
        CompatibilityResult result = new CompatibilityResult();
        
        // 检查Android版本
        result.androidVersion = Build.VERSION.SDK_INT;
        result.isAndroidVersionSupported = result.androidVersion >= MIN_SUPPORTED_SDK;
        
        // 检查Material You支持
        result.supportsMaterialYou = result.androidVersion >= Build.VERSION_CODES.S; // Android 12+
        
        // 检查动态颜色支持
        result.supportsDynamicColors = result.androidVersion >= Build.VERSION_CODES.S;
        
        // 检查通知权限要求
        result.requiresNotificationPermission = result.androidVersion >= Build.VERSION_CODES.TIRAMISU; // Android 13+
        
        // 检查启动画面API支持
        result.supportsSplashScreenApi = result.androidVersion >= Build.VERSION_CODES.S;
        
        // 检查前台服务类型限制
        result.hasForegroundServiceRestrictions = result.androidVersion >= Build.VERSION_CODES.S;
        
        // 检查应用版本
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            result.appVersionCode = packageInfo.versionCode;
            result.appVersionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "无法获取应用版本信息", e);
        }
        
        // 生成兼容性报告
        result.compatibilityReport = generateCompatibilityReport(result);
        
        Log.i(TAG, "兼容性检查完成: " + result.compatibilityReport);
        
        return result;
    }
    
    /**
     * 检查特定功能是否可用
     * @param feature 功能名称
     * @return 是否可用
     */
    public boolean isFeatureAvailable(String feature) {
        switch (feature) {
            case "MATERIAL_YOU":
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
            case "DYNAMIC_COLORS":
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
            case "NOTIFICATION_PERMISSION":
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
            case "SPLASH_SCREEN_API":
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
            case "FOREGROUND_SERVICE_TYPES":
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
            case "MANAGE_EXTERNAL_STORAGE":
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
            default:
                return true;
        }
    }
    
    /**
     * 获取功能降级方案
     * @param feature 功能名称
     * @return 降级方案描述
     */
    public String getFallbackStrategy(String feature) {
        switch (feature) {
            case "MATERIAL_YOU":
                return "使用静态Material 3主题，不支持动态颜色";
            case "DYNAMIC_COLORS":
                return "使用预定义的颜色方案";
            case "NOTIFICATION_PERMISSION":
                return "使用系统默认通知权限处理";
            case "SPLASH_SCREEN_API":
                return "使用传统启动画面实现";
            case "FOREGROUND_SERVICE_TYPES":
                return "使用通用前台服务配置";
            case "MANAGE_EXTERNAL_STORAGE":
                return "使用传统存储权限";
            default:
                return "功能完全可用";
        }
    }
    
    /**
     * 生成兼容性报告
     * @param result 兼容性检查结果
     * @return 报告字符串
     */
    private String generateCompatibilityReport(CompatibilityResult result) {
        StringBuilder report = new StringBuilder();
        
        report.append("设备兼容性报告:\n");
        report.append("Android版本: ").append(result.androidVersion)
              .append(" (").append(Build.VERSION.RELEASE).append(")\n");
        report.append("最低支持版本: ").append(MIN_SUPPORTED_SDK).append("\n");
        report.append("目标版本: ").append(TARGET_SDK).append("\n");
        report.append("版本兼容: ").append(result.isAndroidVersionSupported ? "是" : "否").append("\n");
        
        report.append("\n功能支持情况:\n");
        report.append("Material You: ").append(result.supportsMaterialYou ? "支持" : "不支持").append("\n");
        report.append("动态颜色: ").append(result.supportsDynamicColors ? "支持" : "不支持").append("\n");
        report.append("通知权限: ").append(result.requiresNotificationPermission ? "需要" : "不需要").append("\n");
        report.append("启动画面API: ").append(result.supportsSplashScreenApi ? "支持" : "不支持").append("\n");
        report.append("前台服务限制: ").append(result.hasForegroundServiceRestrictions ? "有" : "无").append("\n");
        
        if (result.appVersionName != null) {
            report.append("\n应用版本: ").append(result.appVersionName)
                  .append(" (").append(result.appVersionCode).append(")\n");
        }
        
        return report.toString();
    }
    
    /**
     * 兼容性检查结果数据类
     */
    public static class CompatibilityResult {
        public int androidVersion;
        public boolean isAndroidVersionSupported;
        public boolean supportsMaterialYou;
        public boolean supportsDynamicColors;
        public boolean requiresNotificationPermission;
        public boolean supportsSplashScreenApi;
        public boolean hasForegroundServiceRestrictions;
        public int appVersionCode;
        public String appVersionName;
        public String compatibilityReport;
    }
}