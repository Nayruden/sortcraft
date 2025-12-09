package net.sortcraft.audit;

import net.sortcraft.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * Handles writing audit entries to log files.
 * Supports synchronous and asynchronous writing, with daily file rotation.
 */
public final class SortAuditLogger {
    private SortAuditLogger() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String AUDIT_DIR = "logs/sortcraft";
    private static final String AUDIT_FILE_PREFIX = "audit-";
    private static final String AUDIT_FILE_SUFFIX = ".log";

    // Async writing support
    private static ExecutorService asyncExecutor;
    private static BlockingQueue<SortAuditEntry> asyncQueue;

    // Current file state
    private static Path currentFilePath;
    private static LocalDate currentFileDate;
    private static BufferedWriter currentWriter;
    private static long currentFileSize;

    /**
     * Checks if audit logging is enabled.
     */
    public static boolean isEnabled() {
        AuditConfig config = ConfigManager.getAuditConfig();
        return config != null && config.isEnabled();
    }

    /**
     * Checks if preview operations should be logged.
     */
    public static boolean shouldLogPreviews() {
        AuditConfig config = ConfigManager.getAuditConfig();
        return config != null && config.isLogPreviews();
    }

    /**
     * Logs an audit entry.
     */
    public static void log(SortAuditEntry entry) {
        if (!isEnabled()) return;

        AuditConfig config = ConfigManager.getAuditConfig();
        if (entry.operationType() == OperationType.PREVIEW && !config.isLogPreviews()) {
            return;
        }

        // Skip if MINIMAL and operation was successful
        if (config.getDetailLevel() == AuditConfig.DetailLevel.MINIMAL
                && entry.status() == OperationStatus.SUCCESS) {
            return;
        }

        if (config.isAsyncWrite()) {
            logAsync(entry);
        } else {
            logSync(entry);
        }
    }

    private static synchronized void logSync(SortAuditEntry entry) {
        try {
            ensureWriter();
            AuditConfig config = ConfigManager.getAuditConfig();
            String json = entry.toJson(config.getDetailLevel());
            currentWriter.write(json);
            currentWriter.newLine();
            currentWriter.flush();
            currentFileSize += json.length() + 1;

            // Check if rotation is needed
            checkRotation();
        } catch (IOException e) {
            LOGGER.error("[audit] Failed to write audit entry", e);
        }
    }

    private static void logAsync(SortAuditEntry entry) {
        ensureAsyncExecutor();
        if (!asyncQueue.offer(entry)) {
            LOGGER.warn("[audit] Async queue full, dropping audit entry {}", entry.operationId());
        }
    }

    private static synchronized void ensureAsyncExecutor() {
        if (asyncExecutor == null || asyncExecutor.isShutdown()) {
            asyncQueue = new LinkedBlockingQueue<>(1000);
            asyncExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Sortcraft-AuditWriter");
                t.setDaemon(true);
                return t;
            });
            asyncExecutor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        SortAuditEntry entry = asyncQueue.poll(5, TimeUnit.SECONDS);
                        if (entry != null) {
                            logSync(entry);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }

    private static void ensureWriter() throws IOException {
        LocalDate today = LocalDate.now();

        // Check if we need a new file (new day or first time)
        if (currentWriter == null || !today.equals(currentFileDate)) {
            closeWriter();
            currentFileDate = today;
            currentFilePath = getAuditFilePath(today);
            Files.createDirectories(currentFilePath.getParent());
            currentWriter = Files.newBufferedWriter(currentFilePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            currentFileSize = Files.exists(currentFilePath) ? Files.size(currentFilePath) : 0;
            LOGGER.debug("[audit] Opened audit log file: {}", currentFilePath);
        }
    }

    private static void checkRotation() {
        AuditConfig config = ConfigManager.getAuditConfig();
        long maxSizeBytes = (long) config.getMaxFileSizeMb() * 1024 * 1024;

        if (currentFileSize >= maxSizeBytes) {
            rotateFile();
            cleanupOldFiles(config.getMaxFiles());
        }
    }

    private static void rotateFile() {
        try {
            closeWriter();
            // Rename current file with timestamp
            if (currentFilePath != null && Files.exists(currentFilePath)) {
                String rotatedName = currentFilePath.getFileName().toString()
                        .replace(AUDIT_FILE_SUFFIX, "-" + System.currentTimeMillis() + AUDIT_FILE_SUFFIX);
                Files.move(currentFilePath, currentFilePath.resolveSibling(rotatedName));
            }
            currentFilePath = null;
            currentFileDate = null;
        } catch (IOException e) {
            LOGGER.error("[audit] Failed to rotate audit file", e);
        }
    }

    private static void cleanupOldFiles(int maxFiles) {
        try {
            Path auditDir = ConfigManager.getGameDir().resolve(AUDIT_DIR);
            if (!Files.exists(auditDir)) return;

            try (Stream<Path> files = Files.list(auditDir)) {
                files.filter(p -> p.getFileName().toString().startsWith(AUDIT_FILE_PREFIX))
                        .sorted(Comparator.<Path>comparingLong(p -> {
                            try {
                                return Files.getLastModifiedTime(p).toMillis();
                            } catch (IOException e) {
                                return 0;
                            }
                        }).reversed())
                        .skip(maxFiles)
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                                LOGGER.debug("[audit] Deleted old audit file: {}", p);
                            } catch (IOException e) {
                                LOGGER.warn("[audit] Failed to delete old audit file: {}", p, e);
                            }
                        });
            }
        } catch (IOException e) {
            LOGGER.error("[audit] Failed to cleanup old audit files", e);
        }
    }

    private static Path getAuditFilePath(LocalDate date) {
        return ConfigManager.getGameDir()
                .resolve(AUDIT_DIR)
                .resolve(AUDIT_FILE_PREFIX + DATE_FORMATTER.format(date) + AUDIT_FILE_SUFFIX);
    }

    private static void closeWriter() {
        if (currentWriter != null) {
            try {
                currentWriter.close();
            } catch (IOException e) {
                LOGGER.error("[audit] Failed to close audit writer", e);
            }
            currentWriter = null;
        }
    }

    /**
     * Shuts down the audit logger, flushing any pending entries.
     * Should be called on server shutdown.
     */
    public static synchronized void shutdown() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            try {
                // Process remaining entries
                while (!asyncQueue.isEmpty()) {
                    SortAuditEntry entry = asyncQueue.poll();
                    if (entry != null) {
                        logSync(entry);
                    }
                }
                asyncExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            asyncExecutor = null;
            asyncQueue = null;
        }
        closeWriter();
        LOGGER.debug("[audit] Audit logger shut down");
    }
}
