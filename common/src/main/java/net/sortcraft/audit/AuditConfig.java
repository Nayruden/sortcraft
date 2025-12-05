package net.sortcraft.audit;

/**
 * Configuration settings for audit logging.
 */
public class AuditConfig {

    /**
     * Detail level for audit logging.
     */
    public enum DetailLevel {
        /**
         * Log all item movements with full details.
         */
        FULL,
        /**
         * Log only operation summary, no individual movements.
         */
        SUMMARY,
        /**
         * Log only failures and warnings.
         */
        MINIMAL
    }

    private boolean enabled = false;
    private DetailLevel detailLevel = DetailLevel.FULL;
    private boolean logPreviews = false;
    private int maxFileSizeMb = 50;
    private int maxFiles = 7;
    private boolean asyncWrite = false;

    /**
     * Creates an AuditConfig with default values (disabled).
     */
    public AuditConfig() {
    }

    /**
     * Creates an AuditConfig with specified values.
     */
    public AuditConfig(boolean enabled, DetailLevel detailLevel, boolean logPreviews,
                       int maxFileSizeMb, int maxFiles, boolean asyncWrite) {
        this.enabled = enabled;
        this.detailLevel = detailLevel;
        this.logPreviews = logPreviews;
        this.maxFileSizeMb = maxFileSizeMb;
        this.maxFiles = maxFiles;
        this.asyncWrite = asyncWrite;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DetailLevel getDetailLevel() {
        return detailLevel;
    }

    public void setDetailLevel(DetailLevel detailLevel) {
        this.detailLevel = detailLevel;
    }

    public boolean isLogPreviews() {
        return logPreviews;
    }

    public void setLogPreviews(boolean logPreviews) {
        this.logPreviews = logPreviews;
    }

    public int getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(int maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public boolean isAsyncWrite() {
        return asyncWrite;
    }

    public void setAsyncWrite(boolean asyncWrite) {
        this.asyncWrite = asyncWrite;
    }

    @Override
    public String toString() {
        return "AuditConfig{" +
                "enabled=" + enabled +
                ", detailLevel=" + detailLevel +
                ", logPreviews=" + logPreviews +
                ", maxFileSizeMb=" + maxFileSizeMb +
                ", maxFiles=" + maxFiles +
                ", asyncWrite=" + asyncWrite +
                '}';
    }
}

