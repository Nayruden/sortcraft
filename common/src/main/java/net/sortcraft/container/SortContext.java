package net.sortcraft.container;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-command context that caches sign and container positions.
 * Built at command start and discarded after command completes.
 */
public class SortContext {
    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");

    // Maps sign text (e.g., "[food]") to the closest sign with that text
    private final Map<String, SignBlockEntity> signCache = new HashMap<>();
    // Maps block position to container for whereis command
    private final Map<BlockPos, Container> containerCache = new HashMap<>();
    private final ServerLevel world;
    private final BlockPos centerPos;
    private final int signRadius;
    private boolean signsScanned = false;

    public SortContext(ServerLevel world, BlockPos centerPos, int signRadius) {
        this.world = world;
        this.centerPos = centerPos;
        this.signRadius = signRadius;
    }

    public ServerLevel getWorld() {
        return world;
    }

    public BlockPos getCenterPos() {
        return centerPos;
    }

    /**
     * Scans all signs in radius and caches their positions by text.
     * Only keeps the closest sign for each unique text.
     */
    private void buildSignCache() {
        if (signsScanned) return;
        signsScanned = true;

        BlockPos min = centerPos.offset(-signRadius, -signRadius, -signRadius);
        BlockPos max = centerPos.offset(signRadius, signRadius, signRadius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = world.getBlockEntity(pos);
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof WallSignBlock)) continue;
            if (!(be instanceof SignBlockEntity sign)) continue;

            // Get all text lines from the sign
            for (int i = 0; i < 4; i++) {
                String frontLine = sign.getFrontText().getMessage(i, false).getString().trim();
                String backLine = sign.getBackText().getMessage(i, false).getString().trim();

                cacheSignText(frontLine, sign, pos);
                cacheSignText(backLine, sign, pos);
            }
        }
        LOGGER.debug("[SortContext] Sign cache built with {} unique sign texts", signCache.size());
    }

    private void cacheSignText(String text, SignBlockEntity sign, BlockPos pos) {
        if (text.isEmpty()) return;
        String lowerText = text.toLowerCase();

        // Only keep the closest sign for each text
        SignBlockEntity existing = signCache.get(lowerText);
        if (existing == null) {
            signCache.put(lowerText, sign);
        } else {
            double existingDist = existing.getBlockPos().distSqr(centerPos);
            double newDist = pos.distSqr(centerPos);
            if (newDist < existingDist) {
                signCache.put(lowerText, sign);
            }
        }
    }

    /**
     * Finds the closest sign with the given text, using the cache.
     */
    public SignBlockEntity findSign(String text) {
        buildSignCache();
        return signCache.get(text.toLowerCase());
    }

    /**
     * Builds the container cache for whereis command.
     */
    public void buildContainerCache() {
        BlockPos min = centerPos.offset(-signRadius, -signRadius, -signRadius);
        BlockPos max = centerPos.offset(signRadius, signRadius, signRadius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof Container inv) {
                containerCache.put(pos.immutable(), inv);
            }
        }
        LOGGER.debug("[SortContext] Container cache built with {} containers", containerCache.size());
    }

    public Map<BlockPos, Container> getContainerCache() {
        return containerCache;
    }
}

