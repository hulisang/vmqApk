package com.vone.vmq;

/**
 * 启动画面配置数据模型
 * 包含启动画面的各项配置参数
 */
public class SplashScreenConfig {
    private String backgroundColor;
    private String iconResource;
    private int animationDuration;
    private String iconBackgroundColor;
    private boolean isValid;
    private String themeResource;
    private boolean hasAnimatedIcon;
    private boolean hasBrandingImage;
    
    public SplashScreenConfig() {
        this.isValid = true;
        this.animationDuration = 1000; // 默认1秒
        this.hasAnimatedIcon = false;
        this.hasBrandingImage = false;
    }
    
    // Getters and Setters
    public String getBackgroundColor() {
        return backgroundColor;
    }
    
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    public String getIconResource() {
        return iconResource;
    }
    
    public void setIconResource(String iconResource) {
        this.iconResource = iconResource;
    }
    
    public int getAnimationDuration() {
        return animationDuration;
    }
    
    public void setAnimationDuration(int animationDuration) {
        this.animationDuration = animationDuration;
        // 验证动画时长是否合理
        if (animationDuration < 0 || animationDuration > 5000) {
            this.isValid = false;
        }
    }
    
    public String getIconBackgroundColor() {
        return iconBackgroundColor;
    }
    
    public void setIconBackgroundColor(String iconBackgroundColor) {
        this.iconBackgroundColor = iconBackgroundColor;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public String getThemeResource() {
        return themeResource;
    }
    
    public void setThemeResource(String themeResource) {
        this.themeResource = themeResource;
    }
    
    public boolean hasAnimatedIcon() {
        return hasAnimatedIcon;
    }
    
    public void setHasAnimatedIcon(boolean hasAnimatedIcon) {
        this.hasAnimatedIcon = hasAnimatedIcon;
    }
    
    public boolean hasBrandingImage() {
        return hasBrandingImage;
    }
    
    public void setHasBrandingImage(boolean hasBrandingImage) {
        this.hasBrandingImage = hasBrandingImage;
    }
    
    /**
     * 验证配置的完整性
     * @return 配置是否完整有效
     */
    public boolean validateConfiguration() {
        boolean valid = true;
        
        // 检查必要的资源是否存在
        if (iconResource == null || iconResource.isEmpty()) {
            valid = false;
        }
        
        // 检查动画时长是否合理
        if (animationDuration < 0 || animationDuration > 5000) {
            valid = false;
        }
        
        this.isValid = valid;
        return valid;
    }
    
    /**
     * 获取配置摘要
     * @return 配置信息摘要
     */
    public String getConfigSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("启动画面配置摘要:\n");
        sb.append("- 图标资源: ").append(iconResource != null ? iconResource : "未设置").append("\n");
        sb.append("- 背景颜色: ").append(backgroundColor != null ? backgroundColor : "未设置").append("\n");
        sb.append("- 图标背景色: ").append(iconBackgroundColor != null ? iconBackgroundColor : "未设置").append("\n");
        sb.append("- 动画时长: ").append(animationDuration).append("ms\n");
        sb.append("- 动画图标: ").append(hasAnimatedIcon ? "是" : "否").append("\n");
        sb.append("- 品牌图像: ").append(hasBrandingImage ? "是" : "否").append("\n");
        sb.append("- 配置有效: ").append(isValid ? "是" : "否");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "SplashScreenConfig{" +
                "backgroundColor='" + backgroundColor + '\'' +
                ", iconResource='" + iconResource + '\'' +
                ", animationDuration=" + animationDuration +
                ", iconBackgroundColor='" + iconBackgroundColor + '\'' +
                ", isValid=" + isValid +
                ", themeResource='" + themeResource + '\'' +
                ", hasAnimatedIcon=" + hasAnimatedIcon +
                ", hasBrandingImage=" + hasBrandingImage +
                '}';
    }
}