package net.sortcraft.mixin.accessor;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin accessor to allow calling the private setBlockState method on BlockDisplay entities.
 * This is needed for the highlight system to set the displayed block.
 */
@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayAccessor {
    /**
     * Invokes the private setBlockState method on a BlockDisplay entity.
     * @param state The block state to display
     */
    @Invoker("setBlockState")
    void invokeSetBlockState(BlockState state);
}

