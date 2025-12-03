package net.sortcraft.config;

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

    public static int getSearchRadius() {
        return searchRadius;
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
                }
            }

            // Configure Log4j2 logger level
            Configurator.setLevel(MODID, logLevel);
            LOGGER.info("Loaded config: logLevel={}, searchRadius={}", logLevel, searchRadius);
        } catch (IOException e) {
            LOGGER.error("Error loading config.yaml", e);
        }
    }
}

