package com.vone.vmq;

import java.util.ArrayList;
import java.util.List;

/**
 * 诊断结果数据模型
 * 包含启动画面配置检查的详细结果
 */
public class DiagnosticResult {
    private boolean isConfigurationValid;
    private boolean areResourcesValid;
    private List<String> issues;
    private List<String> recommendations;
    private SplashScreenType supportedType;
    
    public DiagnosticResult() {
        this.issues = new ArrayList<>();
        this.recommendations = new ArrayList<>();
        this.isConfigurationValid = true;
        this.areResourcesValid = true;
    }
    
    // Getters and Setters
    public boolean isConfigurationValid() {
        return isConfigurationValid;
    }
    
    public void setConfigurationValid(boolean configurationValid) {
        isConfigurationValid = configurationValid;
    }
    
    public boolean areResourcesValid() {
        return areResourcesValid;
    }
    
    public void setResourcesValid(boolean resourcesValid) {
        areResourcesValid = resourcesValid;
    }
    
    public List<String> getIssues() {
        return issues;
    }
    
    public void addIssue(String issue) {
        this.issues.add(issue);
        // 有问题时标记配置无效
        if (issue.contains("配置") || issue.contains("主题")) {
            this.isConfigurationValid = false;
        }
        if (issue.contains("资源") || issue.contains("图标")) {
            this.areResourcesValid = false;
        }
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }
    
    public SplashScreenType getSupportedType() {
        return supportedType;
    }
    
    public void setSupportedType(SplashScreenType supportedType) {
        this.supportedType = supportedType;
    }
    
    /**
     * 检查是否有任何问题
     * @return true如果存在问题
     */
    public boolean hasIssues() {
        return !issues.isEmpty() || !isConfigurationValid || !areResourcesValid;
    }
    
    /**
     * 获取问题总数
     * @return 问题数量
     */
    public int getIssueCount() {
        return issues.size();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DiagnosticResult{");
        sb.append("configurationValid=").append(isConfigurationValid);
        sb.append(", resourcesValid=").append(areResourcesValid);
        sb.append(", supportedType=").append(supportedType);
        sb.append(", issueCount=").append(issues.size());
        sb.append(", recommendationCount=").append(recommendations.size());
        sb.append('}');
        return sb.toString();
    }
}