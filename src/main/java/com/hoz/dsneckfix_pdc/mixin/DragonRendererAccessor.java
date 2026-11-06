package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Exposes the private bone caches so a paper doll render can restore them afterwards. */
@Mixin(value = DragonRenderer.class, remap = false)
public interface DragonRendererAccessor {

    @Accessor("BONE_OFFSETS")
    static Map<Integer, Map<String, Vec3>> ds$getBoneOffsets() {
        throw new AssertionError();
    }

    @Accessor("BONE_ROTATIONS")
    static Map<Integer, Map<String, Quaternionf>> ds$getBoneRotations() {
        throw new AssertionError();
    }

    @Accessor("BONE_UPDATE_TICKS")
    static Map<Integer, Map<String, Long>> ds$getBoneUpdateTicks() {
        throw new AssertionError();
    }
}
