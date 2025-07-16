package com.vone.vmq;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 启动画面手动测试工具Activity
 * 提供完整的启动画面测试和调试功能
 */
public class SplashScreenTestActivity extends Activity {
    
    private static final String TAG = "SplashScreenTest";
    
    private TextView resultTextView;
    private ScrollView scrollView;
    private Handler mainHandler;
    
    // 测试组件
    private SplashScreenDiagnosticImpl diagnostic;
    private SplashScreenErrorHandler errorHandler;
    private SplashScreenThemeAdapter themeAdapter;
    private SplashScreenPerformanceOptimizer performanceOptimizer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 创建测试界面
        createTestUI();
        
        // 初始化测试组件
        initializeTestComponents();
        
        // 显示欢迎信息
        showWelcomeMessage();
    }
    
    /**
     * 创建测试界面
     */
    private void createTestUI() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);
        
        // 标题
        TextView titleView = new TextView(this);
        titleView.setText("启动画面手动测试工具");
        titleView.setTextSize(20);
        titleView.setPadding(0, 0, 0, 24);
        mainLayout.addView(titleView);
        
        // 按钮区域
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        // 诊断按钮
        Button diagnosticButton = new Button(this);
        diagnosticButton.setText("运行诊断");
        diagnosticButton.setOnClickListener(this::runCompleteDiagnostic);
        buttonLayout.addView(diagnosticButton);
        
        // 性能测试按钮
        Button performanceButton = new Button(this);
        performanceButton.setText("性能测试");
        performanceButton.setOnClickListener(this::runPerformanceTest);
        buttonLayout.addView(performanceButton);
        
        // 主题测试按钮
        Button themeButton = new Button(this);
        themeButton.setText("主题测试");
        themeButton.setOnClickListener(this::runThemeTest);
        buttonLayout.addView(themeButton);
        
        mainLayout.addView(buttonLayout);
        
        // 第二行按钮
        LinearLayout buttonLayout2 = new LinearLayout(this);
        buttonLayout2.setOrientation(LinearLayout.HORIZONTAL);
        
        // 错误模拟按钮
        Button errorButton = new Button(this);
        errorButton.setText("错误模拟");
        errorButton.setOnClickListener(this::simulateErrors);
        buttonLayout2.addView(errorButton);
        
        // 清除日志按钮
        Button clearButton = new Button(this);
        clearButton.setText("清除日志");
        clearButton.setOnClickListener(this::clearLogs);
        buttonLayout2.addView(clearButton);
        
        // 重启测试按钮
        Button restartButton = new Button(this);
        restartButton.setText("重启应用");
        restartButton.setOnClickListener(this::restartApp);
        buttonLayout2.addView(restartButton);
        
        mainLayout.addView(buttonLayout2);
        
        // 结果显示区域
        scrollView = new ScrollView(this);
        resultTextView = new TextView(this);
        resultTextView.setTextSize(12);
        resultTextView.setPadding(16, 16, 16, 16);
        resultTextView.setBackgroundColor(0xFF1E1E1E);
        resultTextView.setTextColor(0xFFFFFFFF);
        scrollView.addView(resultTextView);
        
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0);
        scrollParams.weight = 1;
        scrollView.setLayoutParams(scrollParams);
        
        mainLayout.addView(scrollView);
        
        setContentView(mainLayout);
    }
    
    /**
     * 初始化测试组件
     */
    private void initializeTestComponents() {
        try {
            diagnostic = new SplashScreenDiagnosticImpl(this);
            errorHandler = new SplashScreenErrorHandler(this);
            themeAdapter = new SplashScreenThemeAdapter(this);
            performanceOptimizer = new SplashScreenPerformanceOptimizer(this);
            
            appendResult("测试组件初始化完成");
            
        } catch (Exception e) {
            appendResult("测试组件初始化失败: " + e.getMessage());
            Log.e(TAG, "测试组件初始化失败", e);
        }
    }
    
    /**
     * 显示欢迎信息
     */
    private void showWelcomeMessage() {
        StringBuilder welcome = new StringBuilder();
        welcome.append("=== 启动画面手动测试工具 ===\n");
        welcome.append("版本: 1.0\n");
        welcome.append("构建时间: ").append(new java.util.Date()).append("\n");
        welcome.append("设备: ").append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL).append("\n");
        welcome.append("Android: ").append(android.os.Build.VERSION.RELEASE).append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n");
        welcome.append("\n点击按钮开始测试...\n\n");
        
        appendResult(welcome.toString());
    }
    
    /**
     * 运行完整诊断
     */
    public void runCompleteDiagnostic(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        
        appendResult("=== 开始完整诊断 ===");
        
        new Thread(() -> {
            try {
                // 设备支持信息
                SplashScreenSupport support = diagnostic.getSplashScreenSupport();
                mainHandler.post(() -> {
                    appendResult("设备支持信息:");
                    appendResult(support.getSupportSummary());
                    appendResult("");
                });
                
                // 配置检查
                DiagnosticResult result = diagnostic.checkSplashScreenConfiguration();
                mainHandler.post(() -> {
                    appendResult("配置检查结果:");
                    appendResult("- 配置有效: " + (result.isConfigurationValid() ? "是" : "否"));
                    appendResult("- 资源有效: " + (result.areResourcesValid() ? "是" : "否"));
                    appendResult("- 问题数量: " + result.getIssueCount());
                    
                    if (result.hasIssues()) {
                        appendResult("\n发现的问题:");
                        for (String issue : result.getIssues()) {
                            appendResult("• " + issue);
                        }
                        
                        appendResult("\n修复建议:");
                        for (String recommendation : result.getRecommendations()) {
                            appendResult("• " + recommendation);
                        }
                    }
                    appendResult("");
                });
                
                // 配置详情
                SplashScreenConfig config = diagnostic.createSplashScreenConfig();
                mainHandler.post(() -> {
                    appendResult("当前配置:");
                    appendResult(config.getConfigSummary());
                    appendResult("");
                });
                
                // 主题一致性检查
                boolean themeConsistent = themeAdapter.checkThemeConsistency(this);
                mainHandler.post(() -> {
                    appendResult("主题一致性: " + (themeConsistent ? "通过" : "失败"));
                    appendResult("");
                });
                
                mainHandler.post(() -> {
                    appendResult("=== 完整诊断完成 ===\n");
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    appendResult("诊断过程中发生异常: " + e.getMessage());
                    Log.e(TAG, "诊断异常", e);
                });
            }
        }).start();
    }
    
    /**
     * 运行性能测试
     */
    public void runPerformanceTest(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        
        appendResult("=== 开始性能测试 ===");
        
        new Thread(() -> {
            try {
                // 模拟启动画面性能优化
                long startTime = System.currentTimeMillis();
                performanceOptimizer.startOptimization(this);
                long endTime = System.currentTimeMillis();
                
                mainHandler.post(() -> {
                    appendResult("性能优化耗时: " + (endTime - startTime) + "ms");
                });
                
                // 获取性能报告
                String report = performanceOptimizer.getPerformanceReport();
                mainHandler.post(() -> {
                    appendResult("性能报告:");
                    appendResult(report);
                });
                
                // 内存使用情况
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;
                
                mainHandler.post(() -> {
                    appendResult("内存使用:");
                    appendResult("- 已使用: " + (usedMemory / 1024 / 1024) + "MB");
                    appendResult("- 总内存: " + (totalMemory / 1024 / 1024) + "MB");
                    appendResult("- 使用率: " + (usedMemory * 100 / totalMemory) + "%");
                    appendResult("");
                    appendResult("=== 性能测试完成 ===\n");
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    appendResult("性能测试异常: " + e.getMessage());
                    Log.e(TAG, "性能测试异常", e);
                });
            }
        }).start();
    }
    
    /**
     * 运行主题测试
     */
    public void runThemeTest(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        
        appendResult("=== 开始主题测试 ===");
        
        new Thread(() -> {
            try {
                // 主题适配摘要
                String summary = themeAdapter.getThemeAdaptationSummary();
                mainHandler.post(() -> {
                    appendResult("主题适配摘要:");
                    appendResult(summary);
                    appendResult("");
                });
                
                // 创建适配配置
                SplashScreenConfig adaptedConfig = themeAdapter.createAdaptedSplashScreenConfig();
                mainHandler.post(() -> {
                    appendResult("适配后配置:");
                    appendResult(adaptedConfig.getConfigSummary());
                    appendResult("");
                });
                
                // 主题一致性检查
                boolean consistent = themeAdapter.checkThemeConsistency(this);
                mainHandler.post(() -> {
                    appendResult("主题一致性检查: " + (consistent ? "通过" : "失败"));
                    appendResult("");
                    appendResult("=== 主题测试完成 ===\n");
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    appendResult("主题测试异常: " + e.getMessage());
                    Log.e(TAG, "主题测试异常", e);
                });
            }
        }).start();
    }
    
    /**
     * 模拟错误情况
     */
    public void simulateErrors(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        
        appendResult("=== 开始错误模拟测试 ===");
        
        new Thread(() -> {
            try {
                // 模拟资源错误
                mainHandler.post(() -> appendResult("模拟资源缺失错误..."));
                errorHandler.handleResourceError(new android.content.res.Resources.NotFoundException("模拟资源缺失"));
                
                Thread.sleep(500);
                
                // 模拟主题错误
                mainHandler.post(() -> appendResult("模拟主题配置错误..."));
                errorHandler.handleThemeError(new RuntimeException("模拟主题配置错误"));
                
                Thread.sleep(500);
                
                // 模拟兼容性错误
                mainHandler.post(() -> appendResult("模拟兼容性错误..."));
                errorHandler.handleCompatibilityError(new UnsupportedOperationException("模拟兼容性错误"));
                
                Thread.sleep(500);
                
                // 获取恢复状态
                String recoveryStatus = errorHandler.checkRecoveryStatus();
                mainHandler.post(() -> {
                    appendResult("错误恢复状态:");
                    appendResult(recoveryStatus);
                    appendResult("");
                    appendResult("=== 错误模拟测试完成 ===\n");
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    appendResult("错误模拟异常: " + e.getMessage());
                    Log.e(TAG, "错误模拟异常", e);
                });
            }
        }).start();
    }
    
    /**
     * 清除日志
     */
    public void clearLogs(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        
        resultTextView.setText("");
        
        // 清除启动画面日志
        SplashScreenLogger logger = SplashScreenLogger.getInstance(this);
        logger.clearAllLogs();
        
        appendResult("日志已清除\n");
    }
    
    /**
     * 重启应用
     */
    public void restartApp(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        
        appendResult("正在重启应用...");
        
        // 延迟重启，让用户看到消息
        mainHandler.postDelayed(() -> {
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finishAffinity();
                }
            } catch (Exception e) {
                appendResult("重启失败: " + e.getMessage());
            }
        }, 1000);
    }
    
    /**
     * 添加结果到显示区域
     */
    private void appendResult(String text) {
        if (resultTextView != null) {
            String currentText = resultTextView.getText().toString();
            resultTextView.setText(currentText + text + "\n");
            
            // 自动滚动到底部
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 清理资源
        try {
            if (performanceOptimizer != null) {
                performanceOptimizer.cleanup();
            }
        } catch (Exception e) {
            Log.w(TAG, "清理资源失败: " + e.getMessage());
        }
    }
}