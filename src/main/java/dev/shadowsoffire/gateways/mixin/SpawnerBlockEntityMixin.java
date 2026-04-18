package dev.shadowsoffire.gateways.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.block.entity.BlockEntityType;

@Mixin(BlockEntityType.class)
public class SpawnerBlockEntityMixin {

    /**
     * @reason Gateways generates a Wither Skeleton Spawner as a gate reward, and it is unplaceable without this change.
     */
    @Inject(at = @At("HEAD"), method = "onlyOpCanSetNbt()Z", require = 1, remap = false, cancellable = true)
    private void gateways_allowSetNbt(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == BlockEntityType.MOB_SPAWNER) {
            cir.setReturnValue(false);
        }
    }

}
