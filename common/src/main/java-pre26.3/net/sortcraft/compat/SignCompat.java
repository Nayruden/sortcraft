package net.sortcraft.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/**
 * Pre-26.3 implementation of the sign text version-compat seam. Before 26.3,
 * {@link SignBlockEntity} exposes each side through {@code getFrontText()} /
 * {@code getBackText()} and {@code SignText} reads a single line via
 * {@code getMessage(int, boolean)}. See the 26.3 implementation for the API differences.
 */
public final class SignCompat {
    private SignCompat() {}

    /** Returns the unfiltered text of the given line on the front of the sign. */
    public static String getFrontLine(SignBlockEntity sign, int line) {
        return sign.getFrontText().getMessage(line, false).getString();
    }

    /** Returns the unfiltered text of the given line on the back of the sign. */
    public static String getBackLine(SignBlockEntity sign, int line) {
        return sign.getBackText().getMessage(line, false).getString();
    }

    /** Sets the given line on the front of the sign. */
    public static void setFrontLine(SignBlockEntity sign, int line, Component text) {
        sign.setText(sign.getFrontText().setMessage(line, text), true);
    }
}
