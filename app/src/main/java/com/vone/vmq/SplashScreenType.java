package com.vone.vmq;

/**
 * 启动画面类型枚举
 * 定义不同Android版本支持的启动画面类型
 */
public enum SplashScreenType {
    /**
     * Android 12+ Splash Screen API
     */
    ANDROID_12_PLUS("Android 12+ Splash Screen API"),
    
    /**
     * 传统启动画面
     */
    LEGACY("传统启动画面"),
    
    /**
     * 回退方案
     */
    FALLBACK("回退方案");
    
    private final String description;
    
    SplashScreenType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}