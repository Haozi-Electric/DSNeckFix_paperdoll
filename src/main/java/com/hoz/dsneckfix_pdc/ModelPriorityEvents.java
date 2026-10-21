package com.hoz.dsneckfix_pdc;

import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;

@EventBusSubscriber(Dist.CLIENT)
public final class ModelPriorityEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(final RenderArmEvent event) {
        if (event.isCanceled()) {
            return;
        }
        final Player player = event.getPlayer();
        if (ModelPriority.shouldSuppressYsm(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * With self=YSM Dragon Survival no longer renders the local dragon, so
     * its animation controller never clears MovementData.bite after a click.
     * The stale flag is synced to other clients and loops the dragon bite
     * animation there. Clear it before DS sends the movement packet.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTickPost(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Player player = minecraft.player;
        if (player == null
                || !ModelPriority.isYsmLoaded()
                || !ModelPriority.useYsm(player)
                || !DragonStateProvider.isDragon(player)) {
            return;
        }
        MovementData.getData(player).bite = false;
    }

    private ModelPriorityEvents() {
    }
}
