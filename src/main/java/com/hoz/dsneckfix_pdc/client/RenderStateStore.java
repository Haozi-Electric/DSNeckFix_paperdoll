package com.hoz.dsneckfix_pdc.client;

import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;

import java.util.HashMap;
import java.util.Map;

/** Per-player, depth-counted render state. Lives outside the mixin package so mixins may reference it. */
public final class RenderStateStore {
    private static final Map<Integer, State> STATES = new HashMap<>();

    private RenderStateStore() {
    }

    public static State getOrCreate(final int playerId) {
        return STATES.computeIfAbsent(playerId, key -> new State());
    }

    public static State get(final int playerId) {
        return STATES.get(playerId);
    }

    public static void remove(final int playerId) {
        STATES.remove(playerId);
    }

    public static final class State {
        public int depth;
        public boolean movementSaved;
        public MovementDataCompat.Snapshot movement;
        public DragonEntity damageDragon;
        public int hurtTime;
        public int deathTime;
    }
}
