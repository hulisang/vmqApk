package com.vone.vmq;

/**
 * 启动画面诊断接口
 * 提供启动画面配置检查和问题诊断功能
 */
public interface SplashScreenDiagnostic {
    
    /**
     * 检查启动画面配置
     * @return 诊断结果
     */
    DiagnosticResult checkSplashScreenConfiguration();
    
    /**
     * 验证资源文件完整性
     * @return 资源是否有效
     */
    boolean validateSplashScreenResources();
    
    /**
     * 获取当前Android版本的启动画面支持情况
     * @return 启动画面支持类型
     */
    SplashScreenSupport getSplashScreenSupport();
    
    /**
     * 记录启动画面相关日志
     * @param message 日志消息
     */
    void logSplashScreenInfo(String message);
}