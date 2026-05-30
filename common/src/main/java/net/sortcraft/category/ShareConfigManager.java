package net.sortcraft.category;

import net.sortcraft.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

/**
 * Manages downloading and caching of CategoryCraft share configurations.
 *
 * <p>Share IDs are 8-character alphanumeric strings (with hyphens/underscores) that
 * reference category configurations hosted on CategoryCraft. Share content is immutable
 * for a given ID, so responses are cached indefinitely.
 *
 * <p>Three-tier caching strategy:
 * <ol>
 *   <li><b>Memory</b> — {@link ConcurrentHashMap} of share ID → {@link CategorySet}</li>
 *   <li><b>Disk</b> — YAML files in {@code config/sortcraft/cache/{id}.yaml}</li>
 *   <li><b>HTTP</b> — Downloaded from the CategoryCraft API on first access</li>
 * </ol>
 *
 * @see CategoryLoader#loadIsolatedFromYaml(String)
 */
public final class ShareConfigManager {
    private ShareConfigManager() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");
    private static final Pattern SHARE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8}$");
    private static final String API_BASE_URL = "https://categories.craftlabs.nexus/api/share/";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

    /** Reject compressed payloads larger than this before attempting to inflate. */
    private static final int MAX_COMPRESSED_BYTES = 1024 * 1024; // 1 MiB
    /** Cap on decompressed output to guard against zlib decompression bombs. */
    private static final int MAX_DECOMPRESSED_BYTES = 8 * 1024 * 1024; // 8 MiB

    /** Reusable HTTP client — avoids leaking threads/selectors on every download. */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    /** Memory cache: share ID → resolved CategorySet. */
    private static final ConcurrentHashMap<String, CategorySet> memoryCache = new ConcurrentHashMap<>();

    /**
     * Validates whether a string is a valid share ID.
     *
     * @param id the string to validate
     * @return true if the string matches the share ID format
     */
    public static boolean isValidShareId(String id) {
        return id != null && SHARE_ID_PATTERN.matcher(id).matches();
    }

    /**
     * Resolves a share ID to a {@link CategorySet}, using the three-tier cache.
     *
     * <p>Checks memory first, then disk, then downloads via HTTP. Successful HTTP
     * downloads are cached to both disk and memory. Disk hits are cached to memory.
     *
     * @param shareId the share ID to resolve (must pass {@link #isValidShareId})
     * @return the resolved CategorySet, or null if resolution fails
     */
    public static CategorySet resolve(String shareId) {
        if (!isValidShareId(shareId)) {
            LOGGER.warn("Invalid share ID format: '{}'", shareId);
            return null;
        }

        // ConcurrentHashMap.computeIfAbsent throws NPE if the function returns null,
        // so we use get-then-putIfAbsent to handle failed resolutions gracefully.
        CategorySet cached = memoryCache.get(shareId);
        if (cached != null) return cached;

        CategorySet resolved = resolveUncached(shareId);
        if (resolved == null) return null;

        CategorySet existing = memoryCache.putIfAbsent(shareId, resolved);
        return existing != null ? existing : resolved;
    }

    /**
     * Resolves a share ID that is not yet in the memory cache.
     * Called at most once per share ID via {@link ConcurrentHashMap#computeIfAbsent}.
     */
    private static CategorySet resolveUncached(String shareId) {
        // Tier 2: Disk cache
        String yamlContent = loadFromDisk(shareId);
        if (yamlContent != null) {
            CategorySet fromDisk = CategoryLoader.loadIsolatedFromYaml(yamlContent);
            if (fromDisk != null) {
                LOGGER.debug("Share config '{}' resolved from disk cache", shareId);
                return fromDisk;
            }
            LOGGER.warn("Disk-cached YAML for share '{}' failed to parse, re-downloading", shareId);
        }

        // Tier 3: HTTP download
        String downloadedYaml = downloadFromApi(shareId);
        if (downloadedYaml == null) return null;

        CategorySet fromHttp = CategoryLoader.loadIsolatedFromYaml(downloadedYaml);
        if (fromHttp == null) {
            LOGGER.error("Downloaded YAML for share '{}' failed to parse as valid categories", shareId);
            return null;
        }

        // Cache to disk
        saveToDisk(shareId, downloadedYaml);
        LOGGER.debug("Share config '{}' downloaded and cached ({} categories)", shareId,
                fromHttp.getCategories().size());
        return fromHttp;
    }

    /**
     * Clears the in-memory cache. Disk cache is retained.
     * Called on {@code /sort reload} to allow re-parsing of cached YAML.
     */
    public static void clearMemoryCache() {
        int size = memoryCache.size();
        memoryCache.clear();
        LOGGER.debug("Cleared {} share config(s) from memory cache", size);
    }

    // ========== Disk Cache ==========

    private static Path getCachePath(String shareId) {
        return ConfigManager.getConfigPath("cache", shareId + ".yaml");
    }

    private static String loadFromDisk(String shareId) {
        Path path = getCachePath(shareId);
        if (!Files.exists(path)) return null;
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Failed to read disk cache for share '{}': {}", shareId, e.getMessage());
            return null;
        }
    }

    private static void saveToDisk(String shareId, String yamlContent) {
        Path path = getCachePath(shareId);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, yamlContent, StandardCharsets.UTF_8);
            LOGGER.debug("Cached share '{}' to disk at {}", shareId, path);
        } catch (IOException e) {
            LOGGER.warn("Failed to write disk cache for share '{}': {}", shareId, e.getMessage());
        }
    }

    // ========== HTTP Download + Decompression ==========

    /**
     * Downloads and decompresses a share config from the CategoryCraft API.
     *
     * <p>The API returns zlib-compressed YAML (standard zlib with 0x78 header).
     * Decompression uses {@link Inflater} with default (non-raw) mode.
     *
     * @param shareId the share ID to download
     * @return decompressed YAML string, or null on failure
     */
    private static String downloadFromApi(String shareId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + shareId))
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();

            LOGGER.debug("Downloading share config '{}' from CategoryCraft API", shareId);
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

            int status = response.statusCode();
            if (status == 404) {
                LOGGER.warn("Share config '{}' not found (HTTP 404)", shareId);
                return null;
            }
            if (status != 200) {
                LOGGER.error("Failed to download share config '{}': HTTP {}", shareId, status);
                return null;
            }

            byte[] compressed = response.body();
            if (compressed == null || compressed.length == 0) {
                LOGGER.error("Empty response body for share config '{}'", shareId);
                return null;
            }
            if (compressed.length > MAX_COMPRESSED_BYTES) {
                LOGGER.error("Share config '{}' response too large ({} bytes, limit {})",
                        shareId, compressed.length, MAX_COMPRESSED_BYTES);
                return null;
            }

            return decompressZlib(compressed, shareId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Download interrupted for share config '{}'", shareId);
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to download share config '{}': {}", shareId, e.getMessage());
            return null;
        }
    }

    /**
     * Decompresses zlib-wrapped data (standard deflate with zlib header).
     */
    private static String decompressZlib(byte[] compressed, String shareId) {
        Inflater inflater = new Inflater(); // default = zlib-wrapped (not raw)
        try {
            inflater.setInput(compressed);
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(compressed.length * 4, MAX_DECOMPRESSED_BYTES));
            byte[] buffer = new byte[4096];

            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && inflater.needsInput()) {
                    LOGGER.error("Incomplete zlib data for share config '{}'", shareId);
                    return null;
                }
                if (out.size() + count > MAX_DECOMPRESSED_BYTES) {
                    LOGGER.error("Share config '{}' exceeded decompression limit of {} bytes (possible zlib bomb)",
                            shareId, MAX_DECOMPRESSED_BYTES);
                    return null;
                }
                out.write(buffer, 0, count);
            }

            String yaml = out.toString(StandardCharsets.UTF_8);
            LOGGER.debug("Decompressed share config '{}': {} bytes -> {} chars",
                    shareId, compressed.length, yaml.length());
            return yaml;
        } catch (DataFormatException e) {
            LOGGER.error("Failed to decompress share config '{}': {}", shareId, e.getMessage());
            return null;
        } finally {
            inflater.end();
        }
    }
}

