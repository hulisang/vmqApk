package com.vone.vmq;

/**
 * 启动画面支持信息
 * 包含当前设备对启动画面功能的支持详情
 */
public class SplashScreenSupport {
    private SplashScreenType supportedType;
    private int androidApiLevel;
    private boolean isAndroid12Plus;
    private boolean supportsDynamicColors;
    private boolean supportsAnimatedIcon;
    private String deviceInfo;
    
    public SplashScreenSupport(int apiLevel) {
        this.androidApiLevel = apiLevel;
        this.isAndroid12Plus = apiLevel >= 31; // Android 12 = API 31
        this.supportsDynamicColors = apiLevel >= 31;
        this.supportsAnimatedIcon = apiLevel >= 31;
        
        // 根据API级别确定支持的启动画面类型
        if (isAndroid12Plus) {
            this.supportedType = SplashScreenType.ANDROID_12_PLUS;
        } else {
            this.supportedType = SplashScreenType.LEGACY;
        }
        
        this.deviceInfo = "Android API " + apiLevel;
    }
    
    // Getters
    public SplashScreenType getSupportedType() {
        return supportedType;
    }
    
    public int getAndroidApiLevel() {
        return androidApiLevel;
    }
    
    public boolean isAndroid12Plus() {
        return isAndroid12Plus;
    }
    
    public boolean supportsDynamicColors() {
        return supportsDynamicColors;
    }
    
    public boolean supportsAnimatedIcon() {
        return supportsAnimatedIcon;
    }
    
    public String getDeviceInfo() {
        return deviceInfo;
    }
    
    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
    
    /**
     * 获取功能支持摘要
     * @return 功能支持描述
     */
    public String getSupportSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("启动画面类型: ").append(supportedType.getDescription()).append("\n");
        sb.append("Android版本: ").append(deviceInfo).append("\n");
        sb.append("动态颜色支持: ").append(supportsDynamicColors ? "是" : "否").append("\n");
        sb.append("动画图标支持: ").append(supportsAnimatedIcon ? "是" : "否");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "SplashScreenSupport{" +
                "supportedType=" + supportedType +
                ", androidApiLevel=" + androidApiLevel +
                ", isAndroid12Plus=" + isAndroid12Plus +
                ", supportsDynamicColors=" + supportsDynamicColors +
                ", supportsAnimatedIcon=" + supportsAnimatedIcon +
                ", deviceInfo='" + deviceInfo + '\'' +
                '}';
    }
}