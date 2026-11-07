package com.hoz.dsneckfix_pdc.client;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import com.geckolib.renderer.base.GeoRenderState;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.mixin.DragonRenderDataAccessor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Renders a DragonSurvival riding chain (mount -> ... -> target -> passengers) inside the Fuzs
 * paper doll. The host queues one entity render state; the root mount is queued normally and every
 * other node is submitted into the same picture-in-picture texture from its parent's MountingBone
 * matrix. World seat positions and the host's view yaw are deliberately never used.
 */
public final class RidingChainPaperDoll {
    private static final int DOLL_LIGHT = 15728880;
    private static final Map<EntityRenderState, List<LivingEntity>> PLANS = new WeakHashMap<>();

    private static boolean seatCaptureActive;
    private static Matrix4f seatCapture;

    private RidingChainPaperDoll() {
    }

    /**
     * Builds the chain for the paper-doll target. A target riding a dragon yields root mounts ->
     * target; the target's own passengers (any depth) are appended. A target that is itself a
     * dragon mount yields itself plus its passengers. {@code null} means the host keeps its own
     * single-entity rendering.
     */
    public static List<LivingEntity> buildChain(final Entity target) {
        if (!(target instanceof Player playerTarget)) {
            return null;
        }

        final List<LivingEntity> chain = new ArrayList<>();
        if (playerTarget.isPassenger()) {
            for (Entity current = target; current != null; current = current.getVehicle()) {
                if (!(current instanceof Player)) {
                    return null;
                }
                chain.add((LivingEntity) current);
            }
            // Every mount (everything except the target) needs a dragon MountingBone.
            for (int i = 0; i < chain.size() - 1; i++) {
                if (!DragonStateProvider.isDragon(chain.get(i))) {
                    return null;
                }
            }
            Collections.reverse(chain);
        } else {
            chain.add(playerTarget);
        }

        collectPassengers(playerTarget, chain, Collections.newSetFromMap(new IdentityHashMap<>()));
        return chain.size() > 1 ? chain : null;
    }

    private static void collectPassengers(final LivingEntity mount, final List<LivingEntity> chain,
                                          final Set<LivingEntity> seen) {
        // Only dragons provide a MountingBone anchor for the next node.
        if (!DragonStateProvider.isDragon(mount) || !seen.add(mount)) {
            return;
        }
        for (Entity passenger : mount.getPassengers()) {
            if (passenger instanceof LivingEntity living && !chain.contains(living)) {
                chain.add(living);
                collectPassengers(living, chain, seen);
            }
        }
    }

    /** Builds the parent -> children map from the actual vehicle relationships in the chain. */
    public static Map<LivingEntity, List<LivingEntity>> buildChildren(final List<LivingEntity> chain) {
        final Set<LivingEntity> included = Collections.newSetFromMap(new IdentityHashMap<>());
        included.addAll(chain);
        final Map<LivingEntity, List<LivingEntity>> children = new IdentityHashMap<>();
        for (LivingEntity node : chain) {
            if (node.getVehicle() instanceof LivingEntity parent && included.contains(parent)) {
                children.computeIfAbsent(parent, key -> new ArrayList<>(1)).add(node);
            }
        }
        return children;
    }

    /** Associates the chain with the exact render state returned to the host. */
    public static void plan(final EntityRenderState state, final List<LivingEntity> chain) {
        PLANS.put(state, chain);
    }

    /** Takes (and removes) the chain planned for this render state, if any. */
    public static List<LivingEntity> takePlan(final EntityRenderState state) {
        return PLANS.remove(state);
    }

    public static void beginSeatCapture() {
        seatCaptureActive = true;
        seatCapture = null;
    }

    public static void captureSeat(final Matrix4fc matrix) {
        if (seatCaptureActive && seatCapture == null) {
            seatCapture = new Matrix4f(matrix);
        }
    }

    public static Matrix4f takeSeat() {
        seatCaptureActive = false;
        final Matrix4f result = seatCapture;
        seatCapture = null;
        return result;
    }

    public static void cancelSeatCapture() {
        seatCaptureActive = false;
        seatCapture = null;
    }

    /**
     * Appends the chain nodes above the root mount into the paper doll's picture-in-picture texture.
     * The caller has already rendered the root and pushed the compat flag; this method owns the
     * extra submissions and the second feature flush.
     */
    public static void renderExtras(final GuiEntityRenderState entityState, final PoseStack poseStack,
                                    final List<LivingEntity> chain, final Matrix4f rootSeat) {
        if (chain == null || chain.size() < 2 || rootSeat == null) {
            return;
        }
        final EntityRenderState mainState = entityState.renderState();
        if (!(mainState instanceof LivingEntityRenderState mainLiving)) {
            return;
        }

        FeatureRenderDispatcher features = null;
        boolean submitted = false;
        try {
            final Map<LivingEntity, List<LivingEntity>> children = buildChildren(chain);
            final LivingEntity root = chain.get(0);
            final List<LivingEntity> rootChildren = children.get(root);
            if (rootChildren == null || rootChildren.isEmpty()) {
                return;
            }

            final Minecraft minecraft = Minecraft.getInstance();
            final EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
            features = minecraft.gameRenderer.getFeatureRenderDispatcher();
            final SubmitNodeCollector collector = features.getSubmitNodeStorage();
            final CameraRenderState camera = new CameraRenderState();
            final Quaternionf override = entityState.overrideCameraAngle();
            if (override != null) {
                camera.orientation = override.conjugate(new Quaternionf()).rotateY((float) Math.PI);
            }
            final float partialTick = mainLiving.partialTick;
            final float groupScale = groupScale(root, mainLiving, partialTick);
            final Matrix4f baseInverse = new Matrix4f(poseStack.last().pose()).invert();

            guardBoneCaches(chain);
            // The root's pass set this already; re-assert it for the extra flushes.
            minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);

            for (LivingEntity child : rootChildren) {
                renderNode(child, root, rootSeat, children, mainLiving, partialTick, groupScale,
                        baseInverse, dispatcher, collector, camera, features, poseStack);
            }
            submitted = true;
        } catch (final RuntimeException | LinkageError ignored) {
            // Any host/DS change degrades to the already-rendered root mount.
        } finally {
            cancelSeatCapture();
            if (features != null && !submitted) {
                try {
                    features.clearSubmitNodes();
                } catch (final RuntimeException | LinkageError ignored) {
                    // Keep the root mount; the extra nodes are optional.
                }
            }
        }
    }

    private static void renderNode(final LivingEntity node, final LivingEntity parent, final Matrix4f seat,
                                   final Map<LivingEntity, List<LivingEntity>> children,
                                   final LivingEntityRenderState mainLiving, final float partialTick,
                                   final float groupScale, final Matrix4f baseInverse,
                                   final EntityRenderDispatcher dispatcher, final SubmitNodeCollector collector,
                                   final CameraRenderState camera, final FeatureRenderDispatcher features,
                                   final PoseStack poseStack) {
        final EntityRenderState state = createNodeState(node, mainLiving, partialTick, groupScale);
        if (state == null) {
            return;
        }

        Matrix4f childSeat = null;
        poseStack.pushPose();
        try {
            // Compose the seat onto the current base with mulPose, then rebuild a unit normal:
            // the absolute inverse-transpose carries the picture-in-picture scale (guiScale *
            // doll scale), and GeckoLib multiplies cube normals by this matrix WITHOUT
            // renormalizing, which made riders uniformly darker than the root.
            final Matrix4f relative = new Matrix4f(baseInverse).mul(seat);
            final Matrix3f baseNormal = new Matrix3f(poseStack.last().normal());
            poseStack.mulPose(relative);
            final Quaternionf seatRotation = new Quaternionf();
            relative.getNormalizedRotation(seatRotation);
            poseStack.last().normal().set(baseNormal).mul(new Matrix3f().rotation(seatRotation));
            final Vec3 pivot = node.getVehicleAttachmentPoint(parent);
            // The host normalized the root by the same group scale; apply it to the pivot too so
            // riders keep their natural attachment offset inside the scaled chain.
            poseStack.translate(-pivot.x * groupScale, -pivot.y * groupScale, -pivot.z * groupScale);

            beginSeatCapture();
            DsNeckFix.pushChainRendering();
            try {
                dispatcher.submit(state, camera, 0.0D, 0.0D, 0.0D, poseStack, collector);
                // GeckoLib renders the model during the feature flush; flush this node now so the
                // MountingBone callback runs and its children can be placed from the fresh matrix.
                features.renderAllFeatures();
            } finally {
                DsNeckFix.popChainRendering();
                childSeat = takeSeat();
            }
        } finally {
            poseStack.popPose();
        }

        final List<LivingEntity> grandchildren = children.get(node);
        if (childSeat != null && grandchildren != null) {
            for (LivingEntity grandchild : grandchildren) {
                renderNode(grandchild, node, childSeat, children, mainLiving, partialTick, groupScale,
                        baseInverse, dispatcher, collector, camera, features, poseStack);
            }
        }
    }

    /**
     * Fuzs normalizes the root mount to human height (its state scale becomes 1). Every chain
     * member keeps its world-relative size by sharing the factor the host applied to the root,
     * matching the 1.21.1 riding-chain rule.
     */
    private static float groupScale(final LivingEntity root, final LivingEntityRenderState main,
                                    final float partialTick) {
        if (!(root instanceof Player player) || !DragonStateProvider.isDragon(player)) {
            return 1.0F;
        }
        try {
            final double visualScale = DragonStateProvider.getData(player).getVisualScale(player, partialTick);
            final float hostScale = main.scale > 1.0E-3F ? main.scale : 1.0F;
            return visualScale > 1.0E-3D ? (float) (hostScale / visualScale) : 1.0F;
        } catch (final RuntimeException | LinkageError ignored) {
            return 1.0F;
        }
    }

    private static EntityRenderState createNodeState(final LivingEntity node,
                                                     final LivingEntityRenderState main,
                                                     final float partialTick,
                                                     final float groupScale) {
        final EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderState state;

        if (node instanceof Player player && DragonStateProvider.isDragon(player)) {
            final DragonEntity dragon = ClientDragonRenderer.getDragon(player);
            if (dragon == null) {
                return null;
            }
            LegacyMovementRefresh.refresh(player);
            final EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(dragon);
            state = renderer.createRenderState(dragon, partialTick);
            if (state instanceof GeoRenderState geo) {
                final DragonRenderer.DragonRenderData data = geo.getGeckolibData(DragonRenderer.DRAGON_RENDER_DATA);
                if (data != null) {
                    final DragonRenderDataAccessor accessor = (DragonRenderDataAccessor) (Object) data;
                    accessor.ds$setInUI(false);
                    accessor.ds$setBodyYaw(0.0D);
                }
            }
        } else {
            final EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(node);
            state = renderer.createRenderState(node, partialTick);
        }

        if (state instanceof LivingEntityRenderState living) {
            final float naturalScale = living.scale > 1.0E-3F ? living.scale : 1.0F;
            living.scale = naturalScale * groupScale;
            living.boundingBoxWidth *= groupScale;
            living.boundingBoxHeight *= groupScale;
            // The seat matrix already carries the mount's facing. GeckoLib/vanilla rotate the
            // model by (180 - bodyRot), so 180 turns the node's own body yaw into identity and
            // avoids applying the mount's yaw twice.
            living.bodyRot = 180.0F;
            living.xRot = main.xRot;
            living.yRot = main.yRot;
        }
        state.lightCoords = DOLL_LIGHT;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        return state;
    }

    private static void guardBoneCaches(final List<LivingEntity> chain) {
        for (LivingEntity node : chain) {
            if (node instanceof Player player && DragonStateProvider.isDragon(player)) {
                final DragonEntity dragon = ClientDragonRenderer.getDragon(player);
                if (dragon != null) {
                    BoneCacheGuard.begin(dragon.getId());
                }
            }
        }
    }
}
