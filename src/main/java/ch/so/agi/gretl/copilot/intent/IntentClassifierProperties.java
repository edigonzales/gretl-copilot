package ch.so.agi.gretl.copilot.intent;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gretl.copilot.intent")
public class IntentClassifierProperties {
    private int topK = 5;
    private double minConfidence = 0.45;
    private String fallbackLabel = "general.help";
    private double fallbackConfidence = 0.25;
    private int maxLabels = 3;
    private double secondaryMinConfidence = 0.35;

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public String getFallbackLabel() {
        return fallbackLabel;
    }

    public void setFallbackLabel(String fallbackLabel) {
        this.fallbackLabel = fallbackLabel;
    }

    public double getFallbackConfidence() {
        return fallbackConfidence;
    }

    public void setFallbackConfidence(double fallbackConfidence) {
        this.fallbackConfidence = fallbackConfidence;
    }

    public int getMaxLabels() {
        return maxLabels;
    }

    public void setMaxLabels(int maxLabels) {
        this.maxLabels = maxLabels;
    }

    public double getSecondaryMinConfidence() {
        return secondaryMinConfidence;
    }

    public void setSecondaryMinConfidence(double secondaryMinConfidence) {
        this.secondaryMinConfidence = secondaryMinConfidence;
    }
}