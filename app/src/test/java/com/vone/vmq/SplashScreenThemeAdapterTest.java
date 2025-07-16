package com.vone.vmq;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 启动画面主题适配器单元测试
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.O, Build.VERSION_CODES.S})
public class SplashScreenThemeAdapterTest {

    @Mock
    private Activity mockActivity;

    private SplashScreenThemeAdapter themeAdapter;
    private Context context;
    private Activity realActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        realActivity = Robolectric.buildActivity(Activity.class).create().get();
        themeAdapter = new SplashScreenThemeAdapter(context);
    }

    @Test
    public void testThemeAdapterCreation() {
        assertNotNull("主题适配器不应该为null", themeAdapter);
    }

    @Test
    public void testApplySplashScreenThemeAdaptation() {
        // 测试主题适配不会抛出异常
        assertDoesNotThrow("主题适配不应该抛出异常", () -> {
            themeAdapter.applySplashScreenThemeAdaptation(realActivity);
        });
    }

    @Test
    public void testCreateAdaptedSplashScreenConfig() {
        SplashScreenConfig config = themeAdapter.createAdaptedSplashScreenConfig();
        
        assertNotNull("适配配置不应该为null", config);
        assertNotNull("图标资源不应该为null", config.getIconResource());
        assertTrue("动画时长应该大于0", config.getAnimationDuration() > 0);
        assertNotNull("主题资源不应该为null", config.getThemeResource());
    }

    @Test
    public void testGetThemeAdaptationSummary() {
        String summary = themeAdapter.getThemeAdaptationSummary();
        
        assertNotNull("主题适配摘要不应该为null", summary);
        assertTrue("摘要应该包含Android版本", summary.contains("Android版本"));
        assertTrue("摘要应该包含深色模式", summary.contains("深色模式"));
        assertTrue("摘要应该包含动态颜色", summary.contains("动态颜色"));
        assertTrue("摘要应该包含支持类型", summary.contains("支持类型"));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.S)
    public void testAndroid12PlusAdaptation() {
        SplashScreenConfig config = themeAdapter.createAdaptedSplashScreenConfig();
        
        assertTrue("Android 12+应该支持动画图标", config.hasAnimatedIcon());
        assertEquals("动画时长应该为800ms", 800, config.getAnimationDuration());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.O)
    public void testLegacyAdaptation() {
        SplashScreenConfig config = themeAdapter.createAdaptedSplashScreenConfig();
        
        assertFalse("传统版本不应该支持动画图标", config.hasAnimatedIcon());
        assertEquals("动画时长应该为800ms", 800, config.getAnimationDuration());
    }

    @Test
    public void testCheckThemeConsistency() {
        boolean isConsistent = themeAdapter.checkThemeConsistency(realActivity);
        
        // 在测试环境中，主题一致性检查应该能够正常执行
        // 结果可能为true或false，但不应该抛出异常
        assertNotNull("一致性检查结果不应该为null", isConsistent);
    }

    @Test
    public void testDarkModeDetection() {
        // 测试深色模式检测
        String summary = themeAdapter.getThemeAdaptationSummary();
        assertTrue("摘要应该包含深色模式状态", 
                  summary.contains("深色模式: 启用") || summary.contains("深色模式: 禁用"));
    }

    @Test
    public void testDynamicColorDetection() {
        // 测试动态颜色检测
        String summary = themeAdapter.getThemeAdaptationSummary();
        assertTrue("摘要应该包含动态颜色状态", 
                  summary.contains("动态颜色: 启用") || summary.contains("动态颜色: 禁用"));
    }

    @Test
    public void testConfigValidation() {
        SplashScreenConfig config = themeAdapter.createAdaptedSplashScreenConfig();
        
        assertTrue("适配后的配置应该有效", config.isValid());
        assertTrue("配置验证应该通过", config.validateConfiguration());
    }

    @Test
    public void testConfigSummary() {
        SplashScreenConfig config = themeAdapter.createAdaptedSplashScreenConfig();
        String summary = config.getConfigSummary();
        
        assertNotNull("配置摘要不应该为null", summary);
        assertTrue("摘要应该包含配置信息", summary.contains("启动画面配置摘要"));
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