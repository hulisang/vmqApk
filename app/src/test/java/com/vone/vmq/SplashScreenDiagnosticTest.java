package com.vone.vmq;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 启动画面诊断功能单元测试
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.O, Build.VERSION_CODES.S})
public class SplashScreenDiagnosticTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private Resources mockResources;
    
    private SplashScreenDiagnosticImpl diagnostic;
    private Context realContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        realContext = RuntimeEnvironment.getApplication();
        diagnostic = new SplashScreenDiagnosticImpl(realContext);
    }

    @Test
    public void testValidateSplashScreenResources_Success() {
        // 测试资源验证成功的情况
        boolean result = diagnostic.validateSplashScreenResources();
        
        // 在真实环境中，资源应该存在
        assertTrue("启动画面资源应该存在", result);
    }

    @Test
    public void testValidateSplashScreenResources_Failure() {
        // 使用Mock Context测试资源不存在的情况
        when(mockContext.getPackageName()).thenReturn("com.vone.vmq");
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getIdentifier(anyString(), anyString(), anyString())).thenReturn(0);
        
        SplashScreenDiagnosticImpl mockDiagnostic = new SplashScreenDiagnosticImpl(mockContext);
        boolean result = mockDiagnostic.validateSplashScreenResources();
        
        assertFalse("资源不存在时应该返回false", result);
    }

    @Test
    public void testGetSplashScreenSupport_Android12Plus() {
        SplashScreenSupport support = diagnostic.getSplashScreenSupport();
        
        assertNotNull("支持信息不应该为null", support);
        assertTrue("API级别应该大于0", support.getAndroidApiLevel() > 0);
        assertNotNull("支持类型不应该为null", support.getSupportedType());
        assertNotNull("设备信息不应该为null", support.getDeviceInfo());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.S)
    public void testGetSplashScreenSupport_Android12Plus_Features() {
        SplashScreenSupport support = diagnostic.getSplashScreenSupport();
        
        assertTrue("Android 12+应该支持动态颜色", support.supportsDynamicColors());
        assertTrue("Android 12+应该支持动画图标", support.supportsAnimatedIcon());
        assertEquals("应该是Android 12+类型", SplashScreenType.ANDROID_12_PLUS, support.getSupportedType());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.O)
    public void testGetSplashScreenSupport_Legacy() {
        SplashScreenSupport support = diagnostic.getSplashScreenSupport();
        
        assertFalse("Android 8应该不支持动态颜色", support.supportsDynamicColors());
        assertFalse("Android 8应该不支持动画图标", support.supportsAnimatedIcon());
        assertEquals("应该是传统类型", SplashScreenType.LEGACY, support.getSupportedType());
    }

    @Test
    public void testCheckSplashScreenConfiguration_Success() {
        DiagnosticResult result = diagnostic.checkSplashScreenConfiguration();
        
        assertNotNull("诊断结果不应该为null", result);
        assertNotNull("问题列表不应该为null", result.getIssues());
        assertNotNull("建议列表不应该为null", result.getRecommendations());
        assertNotNull("支持类型不应该为null", result.getSupportedType());
    }

    @Test
    public void testCheckSplashScreenConfiguration_WithIssues() {
        // 使用Mock Context模拟配置问题
        when(mockContext.getPackageName()).thenReturn("com.vone.vmq");
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getIdentifier(anyString(), anyString(), anyString())).thenReturn(0);
        when(mockContext.getTheme()).thenReturn(realContext.getTheme());
        
        SplashScreenDiagnosticImpl mockDiagnostic = new SplashScreenDiagnosticImpl(mockContext);
        DiagnosticResult result = mockDiagnostic.checkSplashScreenConfiguration();
        
        assertTrue("应该检测到问题", result.hasIssues());
        assertFalse("资源应该无效", result.areResourcesValid());
        assertTrue("问题数量应该大于0", result.getIssueCount() > 0);
    }

    @Test
    public void testCreateSplashScreenConfig() {
        SplashScreenConfig config = diagnostic.createSplashScreenConfig();
        
        assertNotNull("配置不应该为null", config);
        assertNotNull("图标资源不应该为null", config.getIconResource());
        assertTrue("动画时长应该大于0", config.getAnimationDuration() > 0);
        assertNotNull("主题资源不应该为null", config.getThemeResource());
    }

    @Test
    public void testSplashScreenConfig_Validation() {
        SplashScreenConfig config = new SplashScreenConfig();
        
        // 测试默认配置
        assertTrue("默认配置应该有效", config.isValid());
        
        // 测试无效配置
        config.setAnimationDuration(-1);
        config.validateConfiguration();
        assertFalse("负数动画时长应该无效", config.isValid());
        
        // 测试过长动画时长
        config.setAnimationDuration(10000);
        config.validateConfiguration();
        assertFalse("过长动画时长应该无效", config.isValid());
        
        // 测试有效配置
        config.setAnimationDuration(1000);
        config.setIconResource("@drawable/ic_splash_screen");
        config.validateConfiguration();
        assertTrue("有效配置应该通过验证", config.isValid());
    }

    @Test
    public void testDiagnosticResult_IssueHandling() {
        DiagnosticResult result = new DiagnosticResult();
        
        // 测试初始状态
        assertFalse("初始状态不应该有问题", result.hasIssues());
        assertEquals("初始问题数量应该为0", 0, result.getIssueCount());
        assertTrue("初始配置应该有效", result.isConfigurationValid());
        assertTrue("初始资源应该有效", result.areResourcesValid());
        
        // 添加配置问题
        result.addIssue("配置错误测试");
        assertTrue("添加问题后应该有问题", result.hasIssues());
        assertEquals("问题数量应该为1", 1, result.getIssueCount());
        assertFalse("配置应该无效", result.isConfigurationValid());
        
        // 添加资源问题
        result.addIssue("资源缺失测试");
        assertEquals("问题数量应该为2", 2, result.getIssueCount());
        assertFalse("资源应该无效", result.areResourcesValid());
        
        // 添加建议
        result.addRecommendation("修复建议测试");
        assertEquals("建议数量应该为1", 1, result.getRecommendations().size());
    }

    @Test
    public void testSplashScreenSupport_Summary() {
        SplashScreenSupport support = diagnostic.getSplashScreenSupport();
        String summary = support.getSupportSummary();
        
        assertNotNull("摘要不应该为null", summary);
        assertTrue("摘要应该包含启动画面类型", summary.contains("启动画面类型"));
        assertTrue("摘要应该包含Android版本", summary.contains("Android版本"));
        assertTrue("摘要应该包含动态颜色支持", summary.contains("动态颜色支持"));
        assertTrue("摘要应该包含动画图标支持", summary.contains("动画图标支持"));
    }

    @Test
    public void testSplashScreenConfig_Summary() {
        SplashScreenConfig config = diagnostic.createSplashScreenConfig();
        String summary = config.getConfigSummary();
        
        assertNotNull("配置摘要不应该为null", summary);
        assertTrue("摘要应该包含图标资源", summary.contains("图标资源"));
        assertTrue("摘要应该包含动画时长", summary.contains("动画时长"));
        assertTrue("摘要应该包含配置有效性", summary.contains("配置有效"));
    }

    @Test
    public void testSplashScreenType_Description() {
        assertEquals("Android 12+类型描述应该正确", 
                    "Android 12+ Splash Screen API", 
                    SplashScreenType.ANDROID_12_PLUS.getDescription());
        
        assertEquals("传统类型描述应该正确", 
                    "传统启动画面", 
                    SplashScreenType.LEGACY.getDescription());
        
        assertEquals("回退类型描述应该正确", 
                    "回退方案", 
                    SplashScreenType.FALLBACK.getDescription());
    }

    @Test
    public void testLogSplashScreenInfo() {
        // 测试日志记录功能
        assertDoesNotThrow("日志记录不应该抛出异常", () -> {
            diagnostic.logSplashScreenInfo("测试日志消息");
        });
    }

    /**
     * 断言不抛出异常的辅助方法
     */
    private void assertDoesNotThrow(String message, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            fail(message + ": " + e.getMessage());
        }
    }
}