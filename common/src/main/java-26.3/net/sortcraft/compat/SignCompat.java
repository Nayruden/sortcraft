package net.sortcraft.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

/**
 * Minecraft 26.3+ implementation of the sign text version-compat seam. Compared with
 * earlier 26.x versions, 26.3:
 * <ul>
 *   <li>replaced {@code SignBlockEntity#getFrontText()} / {@code getBackText()} and
 *       {@code setText(SignText, boolean)} with {@code getText(SignTextSlot)} /
 *       {@code setText(SignText, SignTextSlot)};</li>
 *   <li>replaced {@code SignText#getMessage(int, boolean)} with
 *       {@code getMessages(boolean)}, which returns every line as a list;</li>
 *   <li>made {@code SignText} edits go through {@code SignText.Mutable}
 *       ({@code asMutable().setLine(...).asImmutable()}) instead of {@code setMessage}.</li>
 * </ul>
 */
public final class SignCompat {
    private SignCompat() {}

    /** Returns the unfiltered text of the given line on the front of the sign. */
    public static String getFrontLine(SignBlockEntity sign, int line) {
        return sign.getText(SignTextSlot.FRONT).getMessages(false).get(line).getString();
    }

    /** Returns the unfiltered text of the given line on the back of the sign. */
    public static String getBackLine(SignBlockEntity sign, int line) {
        return sign.getText(SignTextSlot.BACK).getMessages(false).get(line).getString();
    }

    /** Sets the given line on the front of the sign. */
    public static void setFrontLine(SignBlockEntity sign, int line, Component text) {
        sign.setText(sign.getText(SignTextSlot.FRONT).asMutable().setLine(line, text).asImmutable(),
                SignTextSlot.FRONT);
    }
}
