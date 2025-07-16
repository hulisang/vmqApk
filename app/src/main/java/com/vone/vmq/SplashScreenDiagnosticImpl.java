package com.vone.vmq;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;

/**
 * 启动画面诊断工具实现类
 * 提供启动画面配置检查和问题诊断的具体实现
 */
public class SplashScreenDiagnosticImpl implements SplashScreenDiagnostic {
    
    private static final String TAG = "SplashScreenDiagnostic";
    private final Context context;
    private final Resources resources;
    
    public SplashScreenDiagnosticImpl(Context context) {
        this.context = context;
        this.resources = context.getResources();
    }
    
    @Override
    public DiagnosticResult checkSplashScreenConfiguration() {
        logSplashScreenInfo("开始启动画面配置检查");
        
        DiagnosticResult result = new DiagnosticResult();
        SplashScreenSupport support = getSplashScreenSupport();
        result.setSupportedType(support.getSupportedType());
        
        // 检查主题配置
        checkThemeConfiguration(result);
        
        // 检查资源文件
        if (!validateSplashScreenResources()) {
            result.addIssue("启动画面资源文件验证失败");
            result.addRecommendation("检查ic_splash_screen.xml文件是否存在且格式正确");
        }
        
        // 检查Android版本特定配置
        if (support.isAndroid12Plus()) {
            checkAndroid12PlusConfiguration(result);
        } else {
            checkLegacyConfiguration(result);
        }
        
        // 检查颜色资源
        checkColorResources(result);
        
        logSplashScreenInfo("启动画面配置检查完成，发现 " + result.getIssueCount() + " 个问题");
        return result;
    }
    
    @Override
    public boolean validateSplashScreenResources() {
        try {
            // 检查启动画面图标资源
            int iconResId = resources.getIdentifier("ic_splash_screen", "drawable", context.getPackageName());
            if (iconResId == 0) {
                logSplashScreenInfo("启动画面图标资源不存在: ic_splash_screen");
                return false;
            }
            
            // 尝试加载资源以验证其有效性
            resources.getDrawable(iconResId, context.getTheme());
            logSplashScreenInfo("启动画面图标资源验证成功");
            return true;
            
        } catch (Resources.NotFoundException e) {
            logSplashScreenInfo("启动画面资源验证失败: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logSplashScreenInfo("启动画面资源验证异常: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public SplashScreenSupport getSplashScreenSupport() {
        SplashScreenSupport support = new SplashScreenSupport(Build.VERSION.SDK_INT);
        
        // 设置设备信息
        String deviceInfo = "Android " + Build.VERSION.RELEASE + 
                           " (API " + Build.VERSION.SDK_INT + "), " +
                           Build.MANUFACTURER + " " + Build.MODEL;
        support.setDeviceInfo(deviceInfo);
        
        logSplashScreenInfo("设备支持信息: " + support.getSupportSummary());
        return support;
    }
    
    @Override
    public void logSplashScreenInfo(String message) {
        Log.d(TAG, message);
        
        // 使用专门的启动画面日志系统
        SplashScreenLogger.logInfo(message);
        
        // 如果可能，也发送到应用的日志广播系统
        try {
            Utils.sendLogBroadcast(context, "[启动画面诊断] " + message);
        } catch (Exception e) {
            // 忽略广播发送失败，避免影响诊断流程
            Log.w(TAG, "发送日志广播失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查主题配置
     */
    private void checkThemeConfiguration(DiagnosticResult result) {
        try {
            // 检查主题是否存在
            TypedValue themeValue = new TypedValue();
            boolean themeExists = context.getTheme().resolveAttribute(android.R.attr.windowBackground, themeValue, true);
            
            if (!themeExists) {
                result.addIssue("主题配置不完整，缺少windowBackground属性");
                result.addRecommendation("检查themes.xml文件中的主题配置");
            }
            
            logSplashScreenInfo("主题配置检查完成");
            
        } catch (Exception e) {
            result.addIssue("主题配置检查异常: " + e.getMessage());
            result.addRecommendation("检查themes.xml文件语法是否正确");
            logSplashScreenInfo("主题配置检查异常: " + e.getMessage());
        }
    }
    
    /**
     * 检查Android 12+特定配置
     */
    private void checkAndroid12PlusConfiguration(DiagnosticResult result) {
        try {
            // 检查Splash Screen API相关属性
            TypedValue typedValue = new TypedValue();
            
            // 检查windowSplashScreenBackground
            boolean hasSplashBackground = context.getTheme().resolveAttribute(
                android.R.attr.windowSplashScreenBackground, typedValue, true);
            if (!hasSplashBackground) {
                result.addIssue("Android 12+配置缺少windowSplashScreenBackground属性");
                result.addRecommendation("在values-v31/themes.xml中添加windowSplashScreenBackground配置");
            }
            
            // 检查windowSplashScreenAnimatedIcon
            boolean hasSplashIcon = context.getTheme().resolveAttribute(
                android.R.attr.windowSplashScreenAnimatedIcon, typedValue, true);
            if (!hasSplashIcon) {
                result.addIssue("Android 12+配置缺少windowSplashScreenAnimatedIcon属性");
                result.addRecommendation("在values-v31/themes.xml中添加windowSplashScreenAnimatedIcon配置");
            }
            
            logSplashScreenInfo("Android 12+配置检查完成");
            
        } catch (Exception e) {
            result.addIssue("Android 12+配置检查异常: " + e.getMessage());
            result.addRecommendation("检查values-v31/themes.xml文件配置");
            logSplashScreenInfo("Android 12+配置检查异常: " + e.getMessage());
        }
    }
    
    /**
     * 检查传统启动画面配置
     */
    private void checkLegacyConfiguration(DiagnosticResult result) {
        try {
            // 检查传统的windowBackground配置
            TypedValue typedValue = new TypedValue();
            boolean hasWindowBackground = context.getTheme().resolveAttribute(
                android.R.attr.windowBackground, typedValue, true);
            
            if (!hasWindowBackground) {
                result.addIssue("传统启动画面配置缺少windowBackground属性");
                result.addRecommendation("在values/themes.xml中添加windowBackground配置");
            }
            
            logSplashScreenInfo("传统启动画面配置检查完成");
            
        } catch (Exception e) {
            result.addIssue("传统启动画面配置检查异常: " + e.getMessage());
            result.addRecommendation("检查values/themes.xml文件配置");
            logSplashScreenInfo("传统启动画面配置检查异常: " + e.getMessage());
        }
    }
    
    /**
     * 检查颜色资源
     */
    private void checkColorResources(DiagnosticResult result) {
        try {
            // 检查Material 3主题颜色
            String[] requiredColors = {
                "md_theme_light_primary",
                "md_theme_light_onPrimary", 
                "md_theme_light_surface",
                "md_theme_light_background"
            };
            
            for (String colorName : requiredColors) {
                int colorResId = resources.getIdentifier(colorName, "color", context.getPackageName());
                if (colorResId == 0) {
                    result.addIssue("缺少颜色资源: " + colorName);
                    result.addRecommendation("检查colors.xml文件中是否定义了" + colorName);
                }
            }
            
            logSplashScreenInfo("颜色资源检查完成");
            
        } catch (Exception e) {
            result.addIssue("颜色资源检查异常: " + e.getMessage());
            result.addRecommendation("检查colors.xml文件配置");
            logSplashScreenInfo("颜色资源检查异常: " + e.getMessage());
        }
    }
    
    /**
     * 创建启动画面配置对象
     */
    public SplashScreenConfig createSplashScreenConfig() {
        SplashScreenConfig config = new SplashScreenConfig();
        
        try {
            // 设置图标资源
            config.setIconResource("@drawable/ic_splash_screen");
            
            // 设置主题资源
            config.setThemeResource("Theme.VMQ");
            
            // 根据Android版本设置不同的配置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                config.setAnimationDuration(1000);
                config.setHasAnimatedIcon(true);
                config.setBackgroundColor("?attr/colorSurface");
                config.setIconBackgroundColor("?attr/colorSurface");
            } else {
                config.setBackgroundColor("?attr/colorBackground");
            }
            
            // 验证配置
            config.validateConfiguration();
            
            logSplashScreenInfo("启动画面配置创建完成: " + config.toString());
            
        } catch (Exception e) {
            config.setValid(false);
            logSplashScreenInfo("启动画面配置创建异常: " + e.getMessage());
        }
        
        return config;
    }
}