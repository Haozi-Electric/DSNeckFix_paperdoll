package com.hoz.dsneckfix_pdc.client;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import com.hoz.dsneckfix_pdc.mixin.DragonRendererAccessor;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A paper doll renders a live dragon, which makes DS write its shared MountingBone/seat caches
 * from GUI space. Snapshot the entries before the deferred render and restore them afterwards
 * so world seats and the riding camera are not polluted.
 */
public final class BoneCacheGuard {
    private static final Map<Integer, SavedEntry> SAVED = new LinkedHashMap<>();

    private BoneCacheGuard() {
    }

    public static void begin(final int dragonId) {
        if (SAVED.containsKey(dragonId)) {
            return;
        }
        SAVED.put(dragonId, new SavedEntry(
                copy(DragonRenderer.BONE_POSITIONS.get(dragonId)),
                copy(DragonRendererAccessor.ds$getBoneOffsets().get(dragonId)),
                copy(DragonRendererAccessor.ds$getBoneRotations().get(dragonId)),
                copy(DragonRendererAccessor.ds$getBoneUpdateTicks().get(dragonId))
        ));
    }

    public static void restoreAll() {
        if (SAVED.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, SavedEntry> entry : SAVED.entrySet()) {
            final int dragonId = entry.getKey();
            final SavedEntry saved = entry.getValue();
            restore(DragonRenderer.BONE_POSITIONS, dragonId, saved.positions());
            restore(DragonRendererAccessor.ds$getBoneOffsets(), dragonId, saved.offsets());
            restore(DragonRendererAccessor.ds$getBoneRotations(), dragonId, saved.rotations());
            restore(DragonRendererAccessor.ds$getBoneUpdateTicks(), dragonId, saved.updateTicks());
        }
        SAVED.clear();
    }

    private static <T> Map<String, T> copy(final Map<String, T> map) {
        return map == null ? null : new HashMap<>(map);
    }

    private static <T> void restore(final Map<Integer, Map<String, T>> cache,
                                    final int dragonId, final Map<String, T> saved) {
        if (saved == null) {
            cache.remove(dragonId);
        } else {
            cache.put(dragonId, saved);
        }
    }

    private record SavedEntry(Map<String, Vec3> positions,
                              Map<String, Vec3> offsets,
                              Map<String, Quaternionf> rotations,
                              Map<String, Long> updateTicks) {
    }
}
