package com.hoz.dsneckfix_pdc.client;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Supplies the root mount's rendered body yaw for dragon-on-dragon riding. */
public final class PassengerSync {
    private PassengerSync() {
    }

    /** Returns the root dragon mount, or null when {@code player} is not a dragon rider. */
    public static Player rootDragonMount(final Player player) {
        if (player == null || !player.isPassenger()) {
            return null;
        }
        if (!(player.getVehicle() instanceof Player vehicle) || !DragonStateProvider.isDragon(vehicle)) {
            return null;
        }
        Player root = vehicle;
        while (root.getVehicle() instanceof Player parent && DragonStateProvider.isDragon(parent)) {
            root = parent;
        }
        return root;
    }

    public static double rootBodyYaw(final Player root) {
        return rootBodyYaw(root, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    public static double rootBodyYaw(final Player root, final float partialTick) {
        final DragonEntity dragon = ClientDragonRenderer.getDragon(root);
        if (dragon != null) {
            final Double sampled = DragonAnimationSampler.sampleBodyYaw(dragon, partialTick);
            if (sampled != null) {
                return sampled;
            }
        }
        return MovementData.getData(root).bodyYaw;
    }
}
