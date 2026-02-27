package net.sortcraft.test;

import net.minecraft.core.BlockPos;
import net.sortcraft.audit.AuditConfig;
import net.sortcraft.audit.SortAuditEntry;
import net.sortcraft.audit.SortAuditLog;
import net.sortcraft.sorting.SortingResults;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bootstrap tests for share ID inclusion in audit JSON output.
 * Verifies that {@link SortAuditEntry#toJson} includes/excludes shareId correctly.
 */
public class AuditShareIdTest extends SortCraftBootstrapTestBase {

    private SortAuditEntry createEntryWithShareId(String shareId) {
        SortAuditLog log = SortAuditLog.startForTest(
                "TestPlayer",
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "minecraft:overworld",
                new BlockPos(0, 64, 0),
                10,
                false
        );
        if (shareId != null) {
            log.setShareId(shareId);
        }
        return log.complete(new SortingResults());
    }

    @Test
    void shareIdIncludedInJsonWhenSet() {
        SortAuditEntry entry = createEntryWithShareId("AbCd1234");
        String json = entry.toJson(AuditConfig.DetailLevel.FULL);
        assertTrue(json.contains("\"shareId\""), "JSON should contain shareId key");
        assertTrue(json.contains("AbCd1234"), "JSON should contain the share ID value");
    }

    @Test
    void shareIdOmittedFromJsonWhenNull() {
        SortAuditEntry entry = createEntryWithShareId(null);
        String json = entry.toJson(AuditConfig.DetailLevel.FULL);
        assertFalse(json.contains("\"shareId\""), "JSON should NOT contain shareId key when null");
    }

    @Test
    void shareIdPresentAtAllDetailLevels() {
        SortAuditEntry entry = createEntryWithShareId("Test_-12");
        for (AuditConfig.DetailLevel level : AuditConfig.DetailLevel.values()) {
            String json = entry.toJson(level);
            assertTrue(json.contains("\"shareId\""),
                    "shareId should be in JSON at detail level " + level);
            assertTrue(json.contains("Test_-12"),
                    "share ID value should be in JSON at detail level " + level);
        }
    }
}

