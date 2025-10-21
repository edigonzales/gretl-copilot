package ch.so.agi.gretl.copilot.retrieval;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gretl.copilot.retrieval")
public class RetrievalProperties {
    private double alpha = 0.6;
    private int candidateLimit = 60;
    private int rerankTopK = 50;
    private int finalLimit = 8;

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public int getCandidateLimit() {
        return candidateLimit;
    }

    public void setCandidateLimit(int candidateLimit) {
        this.candidateLimit = candidateLimit;
    }

    public int getRerankTopK() {
        return rerankTopK;
    }

    public void setRerankTopK(int rerankTopK) {
        this.rerankTopK = rerankTopK;
    }

    public int getFinalLimit() {
        return finalLimit;
    }

    public void setFinalLimit(int finalLimit) {
        this.finalLimit = finalLimit;
    }
}
