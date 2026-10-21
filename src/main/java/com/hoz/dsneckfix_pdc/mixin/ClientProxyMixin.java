package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.util.proxy.ClientProxy;
import com.hoz.dsneckfix_pdc.ModelPriority;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DS gates first-person body / shadow / camera rendering on this flag.
 * The render event is not fired for the local player in first person, so
 * the priority switch has to be applied here as well.
 */
@Mixin(value = ClientProxy.class, remap = false)
public abstract class ClientProxyMixin {

    @Inject(method = "dragonRenderingWasCancelled", at = @At("HEAD"),
            cancellable = true, remap = false)
    private void dsneckfix$priorityGate(final Player player,
                                        final CallbackInfoReturnable<Boolean> cir) {
        if (!DragonStateProvider.isDragon(player)) {
            return;
        }
        if (!ModelPriority.isYsmLoaded()) {
            return;
        }
        if (ModelPriority.useYsm(player)) {
            cir.setReturnValue(true);
        } else {
            // Respect a dragon body that opts out of model rendering entirely.
            cir.setReturnValue(!ModelPriority.dragonAllowsModel(player));
        }
    }
}
