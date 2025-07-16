package com.vone.vmq;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Material You升级测试套件
 * 包含所有兼容性和功能测试
 * 需求: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    PerformanceTest.class,
    CompatibilityTest.class,
    CoreFunctionalityTest.class
})
public class MaterialYouTestSuite {
    // 测试套件类，用于批量运行所有相关测试
    
    /**
     * 测试套件说明：
     * 
     * 1. PerformanceTest - 性能测试
     *    - 应用启动时间测试
     *    - 内存使用情况监控
     *    - 资源加载效率验证
     *    - 网络请求性能测试
     *    - 后台服务效率验证
     * 
     * 2. CompatibilityTest - 兼容性测试
     *    - Android版本兼容性验证
     *    - Material You主题兼容性
     *    - 权限系统兼容性
     *    - 后台服务兼容性
     *    - 启动画面兼容性
     *    - 核心功能兼容性
     * 
     * 3. CoreFunctionalityTest - 核心功能测试
     *    - 通知监听服务功能
     *    - 网络通信功能
     *    - 二维码扫描功能
     *    - 配置管理功能
     *    - 前台服务功能
     *    - 数据处理功能
     *    - 错误处理机制
     * 
     * 运行方式：
     * - 在Android Studio中右键点击此类选择"Run MaterialYouTestSuite"
     * - 或使用命令行：./gradlew test --tests MaterialYouTestSuite
     */
}