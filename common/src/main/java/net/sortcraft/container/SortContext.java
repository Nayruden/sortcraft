package net.sortcraft.container;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Per-command context that caches sign and container positions.
 * Built at command start and discarded after command completes.
 */
public class SortContext {
    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");

    // Maps sign text (e.g., "[food]") to the closest sign with that text
    private final Map<String, SignBlockEntity> signCache = new HashMap<>();
    // Maps block position to storage for whereis command
    private final Map<BlockPos, SortCraftStorage> containerCache = new HashMap<>();
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

        LOGGER.trace("[SortContext] Building sign cache: center={}, radius={}, searchArea=[{} to {}]",
                centerPos, signRadius, min, max);

        int wallSignsFound = 0;
        int signEntitiesFound = 0;

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = world.getBlockEntity(pos);
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof WallSignBlock)) continue;
            wallSignsFound++;
            if (!(be instanceof SignBlockEntity sign)) {
                LOGGER.warn("[SortContext] WallSignBlock at {} has no SignBlockEntity!", pos);
                continue;
            }
            signEntitiesFound++;

            LOGGER.trace("[SortContext] Found sign at {}: front line 0 = '{}'",
                    pos, sign.getFrontText().getMessage(0, false).getString().trim());

            // Get all text lines from the sign
            for (int i = 0; i < 4; i++) {
                String frontLine = sign.getFrontText().getMessage(i, false).getString().trim();
                String backLine = sign.getBackText().getMessage(i, false).getString().trim();

                cacheSignText(frontLine, sign, pos);
                cacheSignText(backLine, sign, pos);
            }
        }
        LOGGER.trace("[SortContext] Sign cache built: {} unique texts, {} wall signs found, {} sign entities",
                signCache.size(), wallSignsFound, signEntitiesFound);
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
        SignBlockEntity result = signCache.get(text.toLowerCase());
        return result;
    }

    /**
     * Builds the container cache for whereis command.
     * Uses StorageLookup for platform-agnostic storage detection.
     */
    public void buildContainerCache() {
        BlockPos min = centerPos.offset(-signRadius, -signRadius, -signRadius);
        BlockPos max = centerPos.offset(signRadius, signRadius, signRadius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            // Skip air blocks early — most blocks in the scan volume are air
            if (world.getBlockState(pos).isAir()) continue;
            // Skip positions already cached (e.g., second half of a double chest)
            BlockPos immutablePos = pos.immutable();
            if (containerCache.containsKey(immutablePos)) continue;

            Optional<SortCraftStorage> storageOpt = StorageLookup.getStorageAt(world, immutablePos);
            storageOpt.ifPresent(storage -> containerCache.put(immutablePos, storage));
        }
        LOGGER.debug("[SortContext] Container cache built with {} storages", containerCache.size());
    }

    public Map<BlockPos, SortCraftStorage> getContainerCache() {
        return Collections.unmodifiableMap(containerCache);
    }
}

