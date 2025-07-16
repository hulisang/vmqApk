package com.vone.vmq;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * 传统启动画面辅助类
 * 为Android 12以下版本提供优化的启动画面体验
 */
public class LegacySplashScreenHelper {
    
    private static final String TAG = "LegacySplashScreenHelper";
    private static final int DEFAULT_SPLASH_DURATION = 1000; // 1秒
    
    private final Activity activity;
    private final Handler mainHandler;
    private boolean isTransitionInProgress = false;
    
    public LegacySplashScreenHelper(Activity activity) {
        this.activity = activity;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 设置传统启动画面
     * 在Activity的onCreate中调用
     */
    public void setupLegacySplashScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+使用新的Splash Screen API，不需要传统处理
            return;
        }
        
        try {
            Log.d(TAG, "设置传统启动画面");
            
            // 优化窗口属性
            optimizeWindowAttributes();
            
            // 设置状态栏和导航栏
            setupSystemBars();
            
            Log.d(TAG, "传统启动画面设置完成");
            
        } catch (Exception e) {
            Log.e(TAG, "设置传统启动画面失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行启动画面到主界面的过渡
     * 在Activity初始化完成后调用
     */
    public void performSplashTransition() {
        performSplashTransition(DEFAULT_SPLASH_DURATION, null);
    }
    
    /**
     * 执行启动画面到主界面的过渡
     * @param duration 启动画面显示时长（毫秒）
     * @param callback 过渡完成回调
     */
    public void performSplashTransition(int duration, Runnable callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+由系统处理过渡
            if (callback != null) {
                callback.run();
            }
            return;
        }
        
        if (isTransitionInProgress) {
            Log.w(TAG, "过渡已在进行中，忽略重复调用");
            return;
        }
        
        isTransitionInProgress = true;
        
        try {
            Log.d(TAG, "开始启动画面过渡，持续时间: " + duration + "ms");
            
            // 延迟执行过渡，确保启动画面显示足够时间
            mainHandler.postDelayed(() -> {
                try {
                    // 执行淡出动画
                    performFadeOutTransition(callback);
                } catch (Exception e) {
                    Log.e(TAG, "执行过渡动画失败: " + e.getMessage(), e);
                    isTransitionInProgress = false;
                    if (callback != null) {
                        callback.run();
                    }
                }
            }, Math.max(duration, 500)); // 最少显示500ms
            
        } catch (Exception e) {
            Log.e(TAG, "启动过渡失败: " + e.getMessage(), e);
            isTransitionInProgress = false;
            if (callback != null) {
                callback.run();
            }
        }
    }
    
    /**
     * 优化窗口属性
     */
    private void optimizeWindowAttributes() {
        try {
            Window window = activity.getWindow();
            if (window == null) return;
            
            // 设置窗口标志，优化启动体验
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            
            // 防止窗口内容重叠
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }
            
            Log.d(TAG, "窗口属性优化完成");
            
        } catch (Exception e) {
            Log.w(TAG, "窗口属性优化失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置系统栏（状态栏和导航栏）
     */
    private void setupSystemBars() {
        try {
            Window window = activity.getWindow();
            if (window == null) return;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 获取主题颜色
                int surfaceColor = ThemeUtils.getThemeColor(activity, android.R.attr.colorBackground);
                
                // 设置状态栏和导航栏颜色
                window.setStatusBarColor(surfaceColor);
                window.setNavigationBarColor(surfaceColor);
                
                // 设置系统栏图标颜色
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    View decorView = window.getDecorView();
                    int flags = decorView.getSystemUiVisibility();
                    
                    // 根据主题设置状态栏图标颜色
                    if (ThemeUtils.isLightTheme(activity)) {
                        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    } else {
                        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    }
                    
                    // 设置导航栏图标颜色（Android 8.1+）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        if (ThemeUtils.isLightTheme(activity)) {
                            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                        } else {
                            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                        }
                    }
                    
                    decorView.setSystemUiVisibility(flags);
                }
            }
            
            Log.d(TAG, "系统栏设置完成");
            
        } catch (Exception e) {
            Log.w(TAG, "系统栏设置失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行淡出过渡动画
     */
    private void performFadeOutTransition(Runnable callback) {
        try {
            View contentView = activity.findViewById(android.R.id.content);
            if (contentView == null) {
                Log.w(TAG, "无法获取内容视图，跳过动画");
                isTransitionInProgress = false;
                if (callback != null) {
                    callback.run();
                }
                return;
            }
            
            // 执行淡出动画
            contentView.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        // 恢复透明度
                        contentView.setAlpha(1f);
                        isTransitionInProgress = false;
                        
                        Log.d(TAG, "启动画面过渡完成");
                        
                        if (callback != null) {
                            callback.run();
                        }
                    })
                    .start();
                    
        } catch (Exception e) {
            Log.e(TAG, "淡出动画执行失败: " + e.getMessage(), e);
            isTransitionInProgress = false;
            if (callback != null) {
                callback.run();
            }
        }
    }
    
    /**
     * 检查是否需要传统启动画面处理
     * @return true如果需要传统处理
     */
    public static boolean needsLegacyHandling() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S;
    }
    
    /**
     * 获取推荐的启动画面显示时长
     * @param context 上下文
     * @return 推荐时长（毫秒）
     */
    public static int getRecommendedSplashDuration(Context context) {
        try {
            // 根据设备性能调整显示时长
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 较新设备，启动较快
                return 800;
            } else {
                // 较老设备，需要更多时间
                return 1200;
            }
        } catch (Exception e) {
            Log.w(TAG, "获取推荐时长失败: " + e.getMessage());
            return DEFAULT_SPLASH_DURATION;
        }
    }
    
    /**
     * 清理资源
     * 在Activity销毁时调用
     */
    public void cleanup() {
        try {
            // 移除所有待执行的任务
            mainHandler.removeCallbacksAndMessages(null);
            isTransitionInProgress = false;
            
            Log.d(TAG, "传统启动画面辅助类资源清理完成");
            
        } catch (Exception e) {
            Log.w(TAG, "资源清理失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前过渡状态
     * @return true如果过渡正在进行
     */
    public boolean isTransitionInProgress() {
        return isTransitionInProgress;
    }
}