package com.vone.vmq;

import android.content.res.Configuration;

/**
 * 主题变化监听器接口
 */
public interface ThemeChangeListener {
    /**
     * 当主题发生变化时调用
     * @param newConfig 新的配置
     */
    void onThemeChanged(Configuration newConfig);
    
    /**
     * 当动态颜色设置发生变化时调用
     * @param enabled 是否启用动态颜色
     */
    void onDynamicColorChanged(boolean enabled);
}