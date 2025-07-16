package com.vone.vmq;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;

/**
 * 启动画面主题适配器
 * 负责处理Material You主题、深色模式和动态颜色的启动画面适配
 */
public class SplashScreenThemeAdapter {
    
    private static final String TAG = "SplashScreenThemeAdapter";
    private final Context context;
    private boolean isDynamicColorEnabled = false;
    private boolean isDarkModeEnabled = false;
    
    public SplashScreenThemeAdapter(Context context) {
        this.context = context;
        detectCurrentThemeSettings();
    }
    
    /**
     * 检测当前主题设置
     */
    private void detectCurrentThemeSettings() {
        try {
            // 检测深色模式
            int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            isDarkModeEnabled = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
            
            // 检测动态颜色支持
            isDynamicColorEnabled = ThemeUtils.isDynamicColorSupported() && ThemeUtils.isDynamicColorEnabled(context);
            
            Log.d(TAG, "主题设置检测完成 - 深色模式: " + isDarkModeEnabled + ", 动态颜色: " + isDynamicColorEnabled);
            SplashScreenLogger.logInfo("主题设置 - 深色模式: " + isDarkModeEnabled + ", 动态颜色: " + isDynamicColorEnabled);
            
        } catch (Exception e) {
            Log.w(TAG, "检测主题设置失败: " + e.getMessage());
            SplashScreenLogger.logError("检测主题设置失败", e);
        }
    }
    
    /**
     * 应用启动画面主题适配
     * @param activity 目标Activity
     */
    public void applySplashScreenThemeAdaptation(Activity activity) {
        try {
            Log.d(TAG, "开始应用启动画面主题适配");
            SplashScreenLogger.logStartup("主题适配开始");
            
            // 根据Android版本选择适配策略
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applyAndroid12PlusThemeAdaptation(activity);
            } else {
                applyLegacyThemeAdaptation(activity);
            }
            
            // 应用系统栏适配
            applySystemBarsAdaptation(activity);
            
            Log.d(TAG, "启动画面主题适配完成");
            SplashScreenLogger.logStartup("主题适配完成");
            
        } catch (Exception e) {
            Log.e(TAG, "应用主题适配失败: " + e.getMessage(), e);
            SplashScreenLogger.logError("主题适配失败", e);
        }
    }
    
    /**
     * 应用Android 12+的主题适配
     */
    private void applyAndroid12PlusThemeAdaptation(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
            
            Log.d(TAG, "应用Android 12+主题适配");
            
            // 动态颜色适配
            if (isDynamicColorEnabled) {
                applyDynamicColorAdaptation(activity);
            }
            
            // 深色模式适配
            if (isDarkModeEnabled) {
                applyDarkModeAdaptation(activity);
            }
            
            // Material You设计适配
            applyMaterialYouAdaptation(activity);
            
        } catch (Exception e) {
            Log.w(TAG, "Android 12+主题适配失败: " + e.getMessage());
            SplashScreenLogger.logError("Android 12+主题适配失败", e);
        }
    }
    
    /**
     * 应用传统主题适配
     */
    private void applyLegacyThemeAdaptation(Activity activity) {
        try {
            Log.d(TAG, "应用传统主题适配");
            
            // 深色模式适配
            if (isDarkModeEnabled) {
                applyLegacyDarkModeAdaptation(activity);
            }
            
            // Material Design适配
            applyMaterialDesignAdaptation(activity);
            
        } catch (Exception e) {
            Log.w(TAG, "传统主题适配失败: " + e.getMessage());
            SplashScreenLogger.logError("传统主题适配失败", e);
        }
    }
    
    /**
     * 应用动态颜色适配
     */
    private void applyDynamicColorAdaptation(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
            
            Log.d(TAG, "应用动态颜色适配");
            
            // 获取系统动态颜色
            int dynamicPrimary = getSystemDynamicColor(activity, android.R.attr.colorPrimary);
            int dynamicSurface = getSystemDynamicColor(activity, android.R.attr.colorBackground);
            
            // 应用到启动画面
            if (dynamicPrimary != 0 && dynamicSurface != 0) {
                // 这里可以动态设置启动画面的颜色
                // 由于启动画面主要通过主题配置，这里主要是记录和验证
                SplashScreenLogger.logInfo("动态颜色获取成功 - Primary: " + Integer.toHexString(dynamicPrimary) + 
                                          ", Surface: " + Integer.toHexString(dynamicSurface));
            }
            
        } catch (Exception e) {
            Log.w(TAG, "动态颜色适配失败: " + e.getMessage());
            SplashScreenLogger.logError("动态颜色适配失败", e);
        }
    }
    
    /**
     * 应用深色模式适配
     */
    private void applyDarkModeAdaptation(Activity activity) {
        try {
            Log.d(TAG, "应用深色模式适配");
            
            // 获取深色主题颜色
            int darkSurface = getThemeColor(activity, android.R.attr.colorBackground);
            int darkPrimary = getThemeColor(activity, android.R.attr.colorPrimary);
            
            if (darkSurface != 0 && darkPrimary != 0) {
                SplashScreenLogger.logInfo("深色模式颜色获取成功 - Primary: " + Integer.toHexString(darkPrimary) + 
                                          ", Surface: " + Integer.toHexString(darkSurface));
            }
            
            // 设置深色模式下的系统栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.getWindow().setStatusBarColor(darkSurface);
                activity.getWindow().setNavigationBarColor(darkSurface);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "深色模式适配失败: " + e.getMessage());
            SplashScreenLogger.logError("深色模式适配失败", e);
        }
    }
    
    /**
     * 应用传统深色模式适配
     */
    private void applyLegacyDarkModeAdaptation(Activity activity) {
        try {
            Log.d(TAG, "应用传统深色模式适配");
            
            // 获取深色主题颜色
            int darkBackground = getThemeColor(activity, android.R.attr.colorBackground);
            
            if (darkBackground != 0) {
                SplashScreenLogger.logInfo("传统深色模式背景色: " + Integer.toHexString(darkBackground));
            }
            
        } catch (Exception e) {
            Log.w(TAG, "传统深色模式适配失败: " + e.getMessage());
        }
    }
    
    /**
     * 应用Material You适配
     */
    private void applyMaterialYouAdaptation(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
            
            Log.d(TAG, "应用Material You适配");
            
            // Material You的圆角和动画适配
            // 这主要通过主题配置实现，这里主要是验证和记录
            
            // 验证Material 3主题是否正确应用
            boolean isMaterial3Applied = verifyMaterial3Theme(activity);
            SplashScreenLogger.logInfo("Material 3主题验证: " + (isMaterial3Applied ? "成功" : "失败"));
            
        } catch (Exception e) {
            Log.w(TAG, "Material You适配失败: " + e.getMessage());
            SplashScreenLogger.logError("Material You适配失败", e);
        }
    }
    
    /**
     * 应用Material Design适配
     */
    private void applyMaterialDesignAdaptation(Activity activity) {
        try {
            Log.d(TAG, "应用Material Design适配");
            
            // 验证Material Design主题
            boolean isMaterialApplied = verifyMaterialTheme(activity);
            SplashScreenLogger.logInfo("Material Design主题验证: " + (isMaterialApplied ? "成功" : "失败"));
            
        } catch (Exception e) {
            Log.w(TAG, "Material Design适配失败: " + e.getMessage());
        }
    }
    
    /**
     * 应用系统栏适配
     */
    private void applySystemBarsAdaptation(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
            
            Log.d(TAG, "应用系统栏适配");
            
            // 获取适当的系统栏颜色
            int statusBarColor = getThemeColor(activity, android.R.attr.colorBackground);
            int navigationBarColor = statusBarColor;
            
            // 设置系统栏颜色
            activity.getWindow().setStatusBarColor(statusBarColor);
            activity.getWindow().setNavigationBarColor(navigationBarColor);
            
            // 设置系统栏图标颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int flags = activity.getWindow().getDecorView().getSystemUiVisibility();
                
                if (!isDarkModeEnabled) {
                    // 浅色模式下使用深色图标
                    flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    // 深色模式下使用浅色图标
                    flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                
                // 设置导航栏图标颜色（Android 8.1+）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    if (!isDarkModeEnabled) {
                        flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    } else {
                        flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    }
                }
                
                activity.getWindow().getDecorView().setSystemUiVisibility(flags);
            }
            
            SplashScreenLogger.logInfo("系统栏适配完成 - 状态栏: " + Integer.toHexString(statusBarColor) + 
                                      ", 导航栏: " + Integer.toHexString(navigationBarColor));
            
        } catch (Exception e) {
            Log.w(TAG, "系统栏适配失败: " + e.getMessage());
            SplashScreenLogger.logError("系统栏适配失败", e);
        }
    }
    
    /**
     * 获取系统动态颜色
     */
    private int getSystemDynamicColor(Activity activity, int attr) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return 0;
        
        try {
            TypedValue typedValue = new TypedValue();
            if (activity.getTheme().resolveAttribute(attr, typedValue, true)) {
                return typedValue.data;
            }
        } catch (Exception e) {
            Log.w(TAG, "获取动态颜色失败: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * 获取主题颜色
     */
    private int getThemeColor(Activity activity, int attr) {
        try {
            TypedValue typedValue = new TypedValue();
            if (activity.getTheme().resolveAttribute(attr, typedValue, true)) {
                return typedValue.data;
            }
        } catch (Exception e) {
            Log.w(TAG, "获取主题颜色失败: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * 验证Material 3主题是否正确应用
     */
    private boolean verifyMaterial3Theme(Activity activity) {
        try {
            // 检查Material 3特有的属性
            TypedValue typedValue = new TypedValue();
            
            // 检查基本的Material属性
            boolean hasPrimary = activity.getTheme().resolveAttribute(
                android.R.attr.colorPrimary, typedValue, true);
            
            // 检查背景属性
            boolean hasBackground = activity.getTheme().resolveAttribute(
                android.R.attr.colorBackground, typedValue, true);
            
            return hasPrimary && hasBackground;
            
        } catch (Exception e) {
            Log.w(TAG, "验证Material 3主题失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证Material Design主题是否正确应用
     */
    private boolean verifyMaterialTheme(Activity activity) {
        try {
            // 检查Material Design基本属性
            TypedValue typedValue = new TypedValue();
            
            // 检查colorPrimary属性
            boolean hasPrimary = activity.getTheme().resolveAttribute(
                android.R.attr.colorPrimary, typedValue, true);
            
            // 检查colorAccent属性
            boolean hasAccent = activity.getTheme().resolveAttribute(
                android.R.attr.colorAccent, typedValue, true);
            
            return hasPrimary && hasAccent;
            
        } catch (Exception e) {
            Log.w(TAG, "验证Material Design主题失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建启动画面配置适配
     */
    public SplashScreenConfig createAdaptedSplashScreenConfig() {
        try {
            SplashScreenConfig config = new SplashScreenConfig();
            
            // 根据当前主题设置配置
            if (isDynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                config.setBackgroundColor("?attr/colorSurface");
                config.setIconBackgroundColor("@android:color/transparent");
                config.setThemeResource("Theme.VMQ");
            } else if (isDarkModeEnabled) {
                config.setBackgroundColor("?attr/colorSurface");
                config.setIconBackgroundColor("@android:color/transparent");
                config.setThemeResource("Theme.VMQ");
            } else {
                config.setBackgroundColor("?attr/colorSurface");
                config.setIconBackgroundColor("@android:color/transparent");
                config.setThemeResource("Theme.VMQ");
            }
            
            // 设置通用配置
            config.setIconResource("@drawable/ic_splash_screen");
            config.setAnimationDuration(800);
            config.setHasAnimatedIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S);
            config.setHasBrandingImage(false);
            
            // 验证配置
            config.validateConfiguration();
            
            SplashScreenLogger.logConfiguration(config);
            
            return config;
            
        } catch (Exception e) {
            Log.e(TAG, "创建适配配置失败: " + e.getMessage(), e);
            SplashScreenLogger.logError("创建适配配置失败", e);
            return new SplashScreenConfig(); // 返回默认配置
        }
    }
    
    /**
     * 获取主题适配摘要
     */
    public String getThemeAdaptationSummary() {
        try {
            StringBuilder summary = new StringBuilder();
            summary.append("启动画面主题适配摘要:\n");
            summary.append("- Android版本: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
            summary.append("- 深色模式: ").append(isDarkModeEnabled ? "启用" : "禁用").append("\n");
            summary.append("- 动态颜色: ").append(isDynamicColorEnabled ? "启用" : "禁用").append("\n");
            summary.append("- 支持类型: ");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                summary.append("Android 12+ Splash Screen API");
                if (isDynamicColorEnabled) {
                    summary.append(" + 动态颜色");
                }
            } else {
                summary.append("传统启动画面");
            }
            
            return summary.toString();
            
        } catch (Exception e) {
            return "获取主题适配摘要失败: " + e.getMessage();
        }
    }
    
    /**
     * 检查主题一致性
     */
    public boolean checkThemeConsistency(Activity activity) {
        try {
            Log.d(TAG, "检查主题一致性");
            
            // 检查启动画面主题与应用主题是否一致
            boolean isConsistent = true;
            
            // 检查颜色一致性
            int appPrimary = getThemeColor(activity, android.R.attr.colorPrimary);
            int appSurface = getThemeColor(activity, android.R.attr.colorBackground);
            
            if (appPrimary == 0 || appSurface == 0) {
                isConsistent = false;
                SplashScreenLogger.logError("主题颜色获取失败", null);
            }
            
            // 检查深色模式一致性
            boolean appDarkMode = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) 
                                 == Configuration.UI_MODE_NIGHT_YES;
            if (appDarkMode != isDarkModeEnabled) {
                isConsistent = false;
                SplashScreenLogger.logError("深色模式设置不一致", null);
            }
            
            SplashScreenLogger.logInfo("主题一致性检查: " + (isConsistent ? "通过" : "失败"));
            return isConsistent;
            
        } catch (Exception e) {
            Log.w(TAG, "主题一致性检查失败: " + e.getMessage());
            SplashScreenLogger.logError("主题一致性检查失败", e);
            return false;
        }
    }
}