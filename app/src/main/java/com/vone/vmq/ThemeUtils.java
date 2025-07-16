package com.vone.vmq;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;

/**
 * 主题工具类，处理Material You动态颜色和主题切换
 */
public class ThemeUtils {
    
    private static final String TAG = "ThemeUtils";
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_DYNAMIC_COLOR_ENABLED = "dynamic_color_enabled";
    
    // 主题模式常量
    public static final int THEME_MODE_SYSTEM = 0;
    public static final int THEME_MODE_LIGHT = 1;
    public static final int THEME_MODE_DARK = 2;
    
    /**
     * 初始化应用主题系统
     * 应在Application.onCreate()中调用
     */
    public static void initializeTheme(Context context) {
        Log.d(TAG, "初始化主题系统");
        
        // 应用保存的主题模式
        int themeMode = getSavedThemeMode(context);
        applyThemeMode(themeMode);
        
        // 检查并应用动态颜色
        if (isDynamicColorSupported() && isDynamicColorEnabled(context)) {
            Log.d(TAG, "应用动态颜色主题");
            DynamicColors.applyToActivitiesIfAvailable(
                (android.app.Application) context.getApplicationContext()
            );
        } else {
            Log.d(TAG, "使用静态颜色主题");
        }
    }
    
    /**
     * 为Activity应用主题
     */
    public static void applyThemeToActivity(Activity activity) {
        if (isDynamicColorSupported() && isDynamicColorEnabled(activity)) {
            Log.d(TAG, "为Activity应用动态颜色");
            DynamicColors.applyToActivityIfAvailable(activity);
        }
    }
    
    /**
     * 检查设备是否支持动态颜色
     */
    public static boolean isDynamicColorSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
               DynamicColors.isDynamicColorAvailable();
    }
    
    /**
     * 检查动态颜色是否已启用
     */
    public static boolean isDynamicColorEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // 默认启用动态颜色（如果支持的话）
        return prefs.getBoolean(KEY_DYNAMIC_COLOR_ENABLED, true);
    }
    
    /**
     * 设置动态颜色启用状态
     */
    public static void setDynamicColorEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR_ENABLED, enabled).apply();
        
        Log.d(TAG, "动态颜色设置已更新: " + enabled);
        
        // 重新初始化主题
        initializeTheme(context);
    }
    
    /**
     * 获取保存的主题模式
     */
    public static int getSavedThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM);
    }
    
    /**
     * 设置主题模式
     */
    public static void setThemeMode(Context context, int themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        
        Log.d(TAG, "主题模式已更新: " + getThemeModeName(themeMode));
        
        // 应用新的主题模式
        applyThemeMode(themeMode);
    }
    
    /**
     * 应用主题模式
     */
    private static void applyThemeMode(int themeMode) {
        switch (themeMode) {
            case THEME_MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_MODE_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
    
    /**
     * 获取当前系统是否为深色模式
     */
    public static boolean isSystemInDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & 
                           Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * 获取主题模式名称（用于调试）
     */
    private static String getThemeModeName(int themeMode) {
        switch (themeMode) {
            case THEME_MODE_LIGHT:
                return "浅色模式";
            case THEME_MODE_DARK:
                return "深色模式";
            case THEME_MODE_SYSTEM:
            default:
                return "跟随系统";
        }
    }
    
    /**
     * 切换到下一个主题模式（用于快速切换）
     */
    public static void toggleThemeMode(Context context) {
        int currentMode = getSavedThemeMode(context);
        int nextMode;
        
        switch (currentMode) {
            case THEME_MODE_SYSTEM:
                nextMode = THEME_MODE_LIGHT;
                break;
            case THEME_MODE_LIGHT:
                nextMode = THEME_MODE_DARK;
                break;
            case THEME_MODE_DARK:
            default:
                nextMode = THEME_MODE_SYSTEM;
                break;
        }
        
        setThemeMode(context, nextMode);
    }
    
    /**
     * 获取当前主题模式的显示名称
     */
    public static String getCurrentThemeModeName(Context context) {
        int currentMode = getSavedThemeMode(context);
        return getThemeModeName(currentMode);
    }
    
    /**
     * 检查是否需要重新创建Activity以应用主题变化
     */
    public static boolean shouldRecreateActivity(Context context, int newThemeMode) {
        int currentMode = getSavedThemeMode(context);
        return currentMode != newThemeMode;
    }
    
    /**
     * 获取主题颜色
     */
    public static int getThemeColor(Activity activity, int attr) {
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            if (activity.getTheme().resolveAttribute(attr, typedValue, true)) {
                return typedValue.data;
            }
        } catch (Exception e) {
            Log.w(TAG, "获取主题颜色失败: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * 检查当前是否为浅色主题
     */
    public static boolean isLightTheme(Activity activity) {
        try {
            int nightModeFlags = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags != Configuration.UI_MODE_NIGHT_YES;
        } catch (Exception e) {
            Log.w(TAG, "检查主题模式失败: " + e.getMessage());
            return true; // 默认返回浅色主题
        }
    }
}