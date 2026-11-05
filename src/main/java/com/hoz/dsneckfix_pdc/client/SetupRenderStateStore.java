package com.hoz.dsneckfix_pdc.client;

import java.util.HashMap;
import java.util.Map;

/** Per-dragon, depth-counted setupRender pin state. Lives outside the mixin package. */
public final class SetupRenderStateStore {
    private static final Map<Integer, State> STATES = new HashMap<>();

    private SetupRenderStateStore() {
    }

    public static State getOrCreate(final int dragonId) {
        return STATES.computeIfAbsent(dragonId, key -> new State());
    }

    public static State get(final int dragonId) {
        return STATES.get(dragonId);
    }

    public static void remove(final int dragonId) {
        STATES.remove(dragonId);
    }

    public static final class State {
        public int depth;
        public boolean restore;
        public boolean forcedInInventory;
        public boolean savedInInventory;
        public double savedBodyYaw;
        public float savedPrevXRot;
        public float savedPrevZRot;
    }
}
