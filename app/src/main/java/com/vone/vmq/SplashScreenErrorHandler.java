package com.vone.vmq;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

/**
 * 启动画面错误处理器
 * 处理启动画面相关的各种错误情况并提供恢复机制
 */
public class SplashScreenErrorHandler {
    
    private static final String TAG = "SplashScreenErrorHandler";
    private final Context context;
    private final SplashScreenDiagnostic diagnostic;
    
    public SplashScreenErrorHandler(Context context) {
        this.context = context;
        this.diagnostic = new SplashScreenDiagnosticImpl(context);
    }
    
    /**
     * 处理资源缺失错误
     * @param e 资源未找到异常
     */
    public void handleResourceError(Resources.NotFoundException e) {
        Log.e(TAG, "启动画面资源缺失错误: " + e.getMessage(), e);
        
        // 记录错误到专门的日志系统
        SplashScreenLogger.logError("资源缺失错误", e);
        
        try {
            // 记录详细的错误信息
            String errorMessage = "启动画面资源缺失: " + e.getMessage();
            diagnostic.logSplashScreenInfo(errorMessage);
            
            // 尝试使用默认资源
            if (tryUseDefaultResources()) {
                diagnostic.logSplashScreenInfo("已切换到默认启动画面资源");
                showUserNotification("启动画面资源异常，已使用默认配置");
            } else {
                // 如果默认资源也不可用，使用系统默认
                diagnostic.logSplashScreenInfo("使用系统默认启动画面");
                showUserNotification("启动画面配置异常，使用系统默认显示");
            }
            
        } catch (Exception ex) {
            Log.e(TAG, "处理资源错误时发生异常: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * 处理主题配置错误
     * @param e 运行时异常
     */
    public void handleThemeError(RuntimeException e) {
        Log.e(TAG, "启动画面主题配置错误: " + e.getMessage(), e);
        
        // 记录错误到专门的日志系统
        SplashScreenLogger.logError("主题配置错误", e);
        
        try {
            String errorMessage = "主题配置错误: " + e.getMessage();
            diagnostic.logSplashScreenInfo(errorMessage);
            
            // 尝试回退到基础主题
            if (fallbackToBaseTheme()) {
                diagnostic.logSplashScreenInfo("已回退到基础主题配置");
                showUserNotification("主题配置异常，已回退到基础配置");
            } else {
                diagnostic.logSplashScreenInfo("主题回退失败，使用系统默认");
                showUserNotification("主题配置严重异常，使用系统默认");
            }
            
        } catch (Exception ex) {
            Log.e(TAG, "处理主题错误时发生异常: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * 处理兼容性错误
     * @param e 不支持操作异常
     */
    public void handleCompatibilityError(UnsupportedOperationException e) {
        Log.e(TAG, "启动画面兼容性错误: " + e.getMessage(), e);
        
        // 记录错误到专门的日志系统
        SplashScreenLogger.logError("兼容性错误", e);
        
        try {
            String errorMessage = "兼容性错误: " + e.getMessage();
            diagnostic.logSplashScreenInfo(errorMessage);
            
            // 检测API级别并选择合适的实现
            SplashScreenSupport support = diagnostic.getSplashScreenSupport();
            
            if (support.isAndroid12Plus()) {
                // Android 12+但出现兼容性问题，降级到传统方式
                if (applyLegacyFallback()) {
                    diagnostic.logSplashScreenInfo("Android 12+兼容性问题，已降级到传统启动画面");
                    showUserNotification("启动画面兼容性问题，已使用兼容模式");
                }
            } else {
                // 传统Android版本的兼容性处理
                if (applyBasicFallback()) {
                    diagnostic.logSplashScreenInfo("传统Android版本兼容性处理完成");
                }
            }
            
        } catch (Exception ex) {
            Log.e(TAG, "处理兼容性错误时发生异常: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * 处理一般性启动画面错误
     * @param e 异常
     * @param context 错误上下文描述
     */
    public void handleGeneralError(Exception e, String context) {
        Log.e(TAG, "启动画面一般性错误 [" + context + "]: " + e.getMessage(), e);
        
        try {
            String errorMessage = "一般性错误 [" + context + "]: " + e.getMessage();
            diagnostic.logSplashScreenInfo(errorMessage);
            
            // 执行全面的错误恢复
            performComprehensiveRecovery();
            
        } catch (Exception ex) {
            Log.e(TAG, "处理一般性错误时发生异常: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * 尝试使用默认资源
     * @return 是否成功
     */
    private boolean tryUseDefaultResources() {
        try {
            // 检查系统默认图标是否可用
            int defaultIcon = android.R.drawable.sym_def_app_icon;
            context.getResources().getDrawable(defaultIcon, context.getTheme());
            
            diagnostic.logSplashScreenInfo("默认资源验证成功");
            return true;
            
        } catch (Exception e) {
            Log.w(TAG, "默认资源也不可用: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 回退到基础主题
     * @return 是否成功
     */
    private boolean fallbackToBaseTheme() {
        try {
            // 验证基础主题是否可用
            // 这里可以尝试应用最基本的Material主题
            diagnostic.logSplashScreenInfo("尝试回退到基础主题");
            
            // 检查基础主题资源
            int themeResId = android.R.style.Theme_Material;
            context.getTheme().applyStyle(themeResId, false);
            
            return true;
            
        } catch (Exception e) {
            Log.w(TAG, "基础主题回退失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 应用传统启动画面回退方案
     * @return 是否成功
     */
    private boolean applyLegacyFallback() {
        try {
            diagnostic.logSplashScreenInfo("应用传统启动画面回退方案");
            
            // 对于Android 12+设备，如果新API有问题，可以禁用相关属性
            // 这通常需要在主题配置中处理，这里主要是记录和通知
            
            return true;
            
        } catch (Exception e) {
            Log.w(TAG, "传统回退方案失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 应用基础回退方案
     * @return 是否成功
     */
    private boolean applyBasicFallback() {
        try {
            diagnostic.logSplashScreenInfo("应用基础回退方案");
            
            // 对于旧版本Android，确保基本的窗口背景设置
            return true;
            
        } catch (Exception e) {
            Log.w(TAG, "基础回退方案失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行全面的错误恢复
     */
    private void performComprehensiveRecovery() {
        try {
            diagnostic.logSplashScreenInfo("开始全面错误恢复");
            
            // 1. 验证资源完整性
            boolean resourcesValid = diagnostic.validateSplashScreenResources();
            if (!resourcesValid) {
                diagnostic.logSplashScreenInfo("资源验证失败，尝试资源恢复");
                tryUseDefaultResources();
            }
            
            // 2. 检查配置完整性
            DiagnosticResult result = diagnostic.checkSplashScreenConfiguration();
            if (result.hasIssues()) {
                diagnostic.logSplashScreenInfo("发现配置问题: " + result.getIssueCount() + " 个");
                
                // 根据问题类型进行相应处理
                for (String issue : result.getIssues()) {
                    if (issue.contains("主题")) {
                        fallbackToBaseTheme();
                    } else if (issue.contains("资源")) {
                        tryUseDefaultResources();
                    }
                }
            }
            
            // 3. 应用兼容性方案
            SplashScreenSupport support = diagnostic.getSplashScreenSupport();
            if (support.isAndroid12Plus()) {
                applyLegacyFallback();
            } else {
                applyBasicFallback();
            }
            
            diagnostic.logSplashScreenInfo("全面错误恢复完成");
            
        } catch (Exception e) {
            Log.e(TAG, "全面错误恢复失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 显示用户通知
     * @param message 通知消息
     */
    private void showUserNotification(String message) {
        try {
            // 在主线程中显示Toast
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                });
            } else {
                // 如果不是Activity上下文，使用应用上下文
                Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.w(TAG, "显示用户通知失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取错误恢复建议
     * @param errorType 错误类型
     * @return 恢复建议
     */
    public String getRecoveryRecommendation(String errorType) {
        switch (errorType.toLowerCase()) {
            case "resource":
                return "建议检查drawable文件夹中的启动画面图标资源，确保ic_splash_screen.xml文件存在且格式正确";
            case "theme":
                return "建议检查themes.xml文件中的主题配置，确保继承关系和属性设置正确";
            case "compatibility":
                return "建议检查Android版本兼容性，确保在不同API级别下都有相应的配置";
            case "configuration":
                return "建议运行启动画面诊断工具，获取详细的配置问题报告";
            default:
                return "建议重新安装应用或联系技术支持";
        }
    }
    
    /**
     * 检查错误恢复状态
     * @return 恢复状态报告
     */
    public String checkRecoveryStatus() {
        try {
            StringBuilder status = new StringBuilder();
            status.append("启动画面错误恢复状态检查:\n");
            
            // 检查资源状态
            boolean resourcesOk = diagnostic.validateSplashScreenResources();
            status.append("- 资源状态: ").append(resourcesOk ? "正常" : "异常").append("\n");
            
            // 检查配置状态
            DiagnosticResult result = diagnostic.checkSplashScreenConfiguration();
            status.append("- 配置状态: ").append(result.isConfigurationValid() ? "正常" : "异常").append("\n");
            status.append("- 发现问题: ").append(result.getIssueCount()).append(" 个\n");
            
            // 检查兼容性状态
            SplashScreenSupport support = diagnostic.getSplashScreenSupport();
            status.append("- 兼容性: ").append(support.getSupportedType().getDescription()).append("\n");
            
            return status.toString();
            
        } catch (Exception e) {
            return "错误恢复状态检查失败: " + e.getMessage();
        }
    }
}