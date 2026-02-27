package net.sortcraft.test;

import net.sortcraft.category.ShareConfigManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bootstrap tests for share ID format validation.
 * Tests {@link ShareConfigManager#isValidShareId(String)} against the pattern {@code ^[A-Za-z0-9_-]{8}$}.
 */
public class ShareIdValidationTest extends SortCraftBootstrapTestBase {

    @Test
    void validShareIdAccepted() {
        assertTrue(ShareConfigManager.isValidShareId("ksJgx-mb"),
                "Standard share ID with hyphen should be accepted");
    }

    @Test
    void validShareIdWithUnderscore() {
        assertTrue(ShareConfigManager.isValidShareId("abc_defg"),
                "Share ID with underscore should be accepted");
    }

    @Test
    void validShareIdAllDigits() {
        assertTrue(ShareConfigManager.isValidShareId("12345678"),
                "Share ID with all digits should be accepted");
    }

    @Test
    void validShareIdAllLowercase() {
        assertTrue(ShareConfigManager.isValidShareId("abcdefgh"),
                "Share ID with all lowercase should be accepted");
    }

    @Test
    void tooShortRejected() {
        assertFalse(ShareConfigManager.isValidShareId("abc"),
                "Share ID shorter than 8 characters should be rejected");
    }

    @Test
    void tooLongRejected() {
        assertFalse(ShareConfigManager.isValidShareId("abcdefghi"),
                "Share ID longer than 8 characters should be rejected");
    }

    @Test
    void nullRejected() {
        assertFalse(ShareConfigManager.isValidShareId(null),
                "Null should be rejected");
    }

    @Test
    void emptyStringRejected() {
        assertFalse(ShareConfigManager.isValidShareId(""),
                "Empty string should be rejected");
    }

    @Test
    void specialCharsRejected() {
        assertFalse(ShareConfigManager.isValidShareId("abc!defg"),
                "Share ID with special characters should be rejected");
    }

    @Test
    void spacesRejected() {
        assertFalse(ShareConfigManager.isValidShareId("abc defg"),
                "Share ID with spaces should be rejected");
    }

    @Test
    void signTextNotConfusedForShareId() {
        assertFalse(ShareConfigManager.isValidShareId("[input]"),
                "Sign text '[input]' should not be confused for a share ID");
        assertFalse(ShareConfigManager.isValidShareId("[category]"),
                "Sign text '[category]' should not be confused for a share ID");
    }
}

