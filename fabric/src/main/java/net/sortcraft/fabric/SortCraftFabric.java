package net.sortcraft.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.sortcraft.SortCraft;

public class SortCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Server lifecycle events
        ServerLifecycleEvents.SERVER_STARTING.register(SortCraft::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(SortCraft::onServerStopping);

        // Server tick
        ServerTickEvents.END_SERVER_TICK.register(SortCraft::onServerTick);

        // Command registration
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SortCraft.onRegisterCommands(dispatcher);
        });

        // Right-click block interaction
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (SortCraft.onRightClickBlock(player, hand, hitResult.getBlockPos())) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}

