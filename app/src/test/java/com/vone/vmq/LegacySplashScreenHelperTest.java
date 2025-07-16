package com.vone.vmq;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * 传统启动画面辅助类单元测试
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.O, Build.VERSION_CODES.R})
public class LegacySplashScreenHelperTest {

    private LegacySplashScreenHelper helper;
    private Activity activity;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        activity = Robolectric.buildActivity(Activity.class).create().get();
        helper = new LegacySplashScreenHelper(activity);
    }

    @Test
    public void testHelperCreation() {
        assertNotNull("传统启动画面辅助类不应该为null", helper);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.O)
    public void testNeedsLegacyHandling_Android8() {
        assertTrue("Android 8应该需要传统处理", LegacySplashScreenHelper.needsLegacyHandling());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void testNeedsLegacyHandling_Android11() {
        assertTrue("Android 11应该需要传统处理", LegacySplashScreenHelper.needsLegacyHandling());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.S)
    public void testNeedsLegacyHandling_Android12() {
        assertFalse("Android 12不应该需要传统处理", LegacySplashScreenHelper.needsLegacyHandling());
    }

    @Test
    public void testSetupLegacySplashScreen() {
        // 测试设置传统启动画面不会抛出异常
        assertDoesNotThrow("设置传统启动画面不应该抛出异常", () -> {
            helper.setupLegacySplashScreen();
        });
    }

    @Test
    public void testPerformSplashTransition() {
        // 测试执行启动画面过渡不会抛出异常
        assertDoesNotThrow("执行启动画面过渡不应该抛出异常", () -> {
            helper.performSplashTransition();
        });
    }

    @Test
    public void testPerformSplashTransitionWithCallback() {
        final boolean[] callbackExecuted = {false};
        
        Runnable callback = () -> callbackExecuted[0] = true;
        
        // 测试带回调的过渡
        assertDoesNotThrow("带回调的过渡不应该抛出异常", () -> {
            helper.performSplashTransition(500, callback);
        });
        
        // 注意：由于是异步执行，这里不能立即检查回调是否执行
        // 在实际测试中可能需要使用CountDownLatch等同步机制
    }

    @Test
    public void testGetRecommendedSplashDuration() {
        int duration = LegacySplashScreenHelper.getRecommendedSplashDuration(context);
        
        assertTrue("推荐时长应该大于0", duration > 0);
        assertTrue("推荐时长应该在合理范围内", duration >= 500 && duration <= 2000);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void testGetRecommendedSplashDuration_Android6() {
        int duration = LegacySplashScreenHelper.getRecommendedSplashDuration(context);
        assertEquals("Android 6应该返回800ms", 800, duration);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void testGetRecommendedSplashDuration_Android5() {
        int duration = LegacySplashScreenHelper.getRecommendedSplashDuration(context);
        assertEquals("Android 5应该返回1200ms", 1200, duration);
    }

    @Test
    public void testIsTransitionInProgress() {
        // 初始状态应该没有过渡进行中
        assertFalse("初始状态不应该有过渡进行中", helper.isTransitionInProgress());
    }

    @Test
    public void testCleanup() {
        // 测试清理不会抛出异常
        assertDoesNotThrow("清理不应该抛出异常", () -> {
            helper.cleanup();
        });
    }

    @Test
    public void testMultipleTransitionCalls() {
        // 测试多次调用过渡方法不会出问题
        assertDoesNotThrow("多次调用过渡不应该抛出异常", () -> {
            helper.performSplashTransition(100, null);
            helper.performSplashTransition(100, null);
            helper.performSplashTransition(100, null);
        });
    }

    @Test
    public void testTransitionWithZeroDuration() {
        // 测试零时长过渡
        assertDoesNotThrow("零时长过渡不应该抛出异常", () -> {
            helper.performSplashTransition(0, null);
        });
    }

    @Test
    public void testTransitionWithNegativeDuration() {
        // 测试负数时长过渡
        assertDoesNotThrow("负数时长过渡不应该抛出异常", () -> {
            helper.performSplashTransition(-100, null);
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