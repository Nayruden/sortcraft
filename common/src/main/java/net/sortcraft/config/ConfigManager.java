package net.sortcraft.config;

import net.sortcraft.audit.AuditConfig;
import net.sortcraft.platform.Platform;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Manages SortCraft configuration loading and storage.
 */
public final class ConfigManager {
    private ConfigManager() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");
    private static final String MODID = "sortcraft";

    // Configuration values with defaults
    private static int searchRadius = 64;
    private static AuditConfig auditConfig = new AuditConfig();

    public static int getSearchRadius() {
        return searchRadius;
    }

    public static AuditConfig getAuditConfig() {
        return auditConfig;
    }

    /**
     * Gets the game directory (parent of config directory).
     */
    public static Path getGameDir() {
        return Platform.getConfigDir().getParent();
    }

    /**
     * Gets the configuration path for SortCraft.
     */
    public static Path getConfigPath(String... subPaths) {
        Path base = Platform.getConfigDir().resolve("sortcraft");
        for (String subPath : subPaths) {
            base = base.resolve(subPath);
        }
        return base;
    }

    /**
     * Loads the configuration from config.yaml.
     */
    @SuppressWarnings("unchecked")
    public static void loadConfig() {
        Path configPath = getConfigPath("config.yaml");
        org.apache.logging.log4j.Level logLevel = org.apache.logging.log4j.Level.WARN;
        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                String defaultConfig = """
                        # SortCraft Configuration

                        # Log level: TRACE, DEBUG, INFO, WARN, ERROR
                        # Default: WARN
                        logLevel: WARN

                        # Search radius for finding signs (in blocks)
                        # Default: 64
                        searchRadius: 64

                        # Audit logging configuration
                        audit:
                          # Enable audit logging of sort operations
                          enabled: false
                          # Detail level: FULL, SUMMARY, MINIMAL
                          detailLevel: FULL
                          # Whether to log preview operations
                          logPreviews: false
                          # Maximum size of each audit file in MB before rotation
                          maxFileSizeMb: 50
                          # Maximum number of audit files to keep
                          maxFiles: 7
                          # Use async writing for better performance on busy servers
                          asyncWrite: false
                        """;
                Files.write(configPath, defaultConfig.getBytes(StandardCharsets.UTF_8));
                LOGGER.info("Created default config.yaml at {}", configPath);
            }

            Yaml yaml = new Yaml();
            try (InputStream in = Files.newInputStream(configPath)) {
                Map<String, Object> config = yaml.load(in);
                if (config != null) {
                    Object logLevelValue = config.get("logLevel");
                    if (logLevelValue instanceof String logLevelStr) {
                        try {
                            logLevel = org.apache.logging.log4j.Level.valueOf(logLevelStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Invalid logLevel '{}' in config.yaml, using WARN", logLevelStr);
                        }
                    }

                    Object searchRadiusValue = config.get("searchRadius");
                    if (searchRadiusValue instanceof Integer radius) {
                        searchRadius = radius;
                    } else if (searchRadiusValue instanceof Number radius) {
                        searchRadius = radius.intValue();
                    }

                    // Load audit configuration
                    Object auditValue = config.get("audit");
                    if (auditValue instanceof Map<?, ?> auditMap) {
                        auditConfig = loadAuditConfig((Map<String, Object>) auditMap);
                    }
                }
            }

            // Configure Log4j2 logger level
            Configurator.setLevel(MODID, logLevel);
            LOGGER.info("Loaded config: logLevel={}, searchRadius={}, audit.enabled={}",
                    logLevel, searchRadius, auditConfig.isEnabled());
        } catch (IOException e) {
            LOGGER.error("Error loading config.yaml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static AuditConfig loadAuditConfig(Map<String, Object> auditMap) {
        boolean enabled = false;
        AuditConfig.DetailLevel detailLevel = AuditConfig.DetailLevel.FULL;
        boolean logPreviews = false;
        int maxFileSizeMb = 50;
        int maxFiles = 7;
        boolean asyncWrite = false;

        Object enabledValue = auditMap.get("enabled");
        if (enabledValue instanceof Boolean b) {
            enabled = b;
        }

        Object detailLevelValue = auditMap.get("detailLevel");
        if (detailLevelValue instanceof String s) {
            try {
                detailLevel = AuditConfig.DetailLevel.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid audit.detailLevel '{}', using FULL", s);
            }
        }

        Object logPreviewsValue = auditMap.get("logPreviews");
        if (logPreviewsValue instanceof Boolean b) {
            logPreviews = b;
        }

        Object maxFileSizeMbValue = auditMap.get("maxFileSizeMb");
        if (maxFileSizeMbValue instanceof Number n) {
            maxFileSizeMb = n.intValue();
        }

        Object maxFilesValue = auditMap.get("maxFiles");
        if (maxFilesValue instanceof Number n) {
            maxFiles = n.intValue();
        }

        Object asyncWriteValue = auditMap.get("asyncWrite");
        if (asyncWriteValue instanceof Boolean b) {
            asyncWrite = b;
        }

        return new AuditConfig(enabled, detailLevel, logPreviews, maxFileSizeMb, maxFiles, asyncWrite);
    }
}

