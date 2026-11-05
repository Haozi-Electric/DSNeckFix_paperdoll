package com.hoz.dsneckfix_pdc.client;

import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import com.hoz.dsneckfix_pdc.Config;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Renders a DragonSurvival riding chain (mount -> ... -> target) inside a host paper doll.
 * <p>
 * The chain is rendered into a single PoseStack. Every mount's MountingBone transform is captured
 * while that mount renders, relative to the chain node base, and children are placed with
 * {@code T(seat) * R(bone up tilt) * T(-pivot)}. World seat positions and the host's view yaw are
 * deliberately never used.
 */
public final class RidingChainPaperDoll {
    private static final int DOLL_LIGHT = 0xF000F0;

    private static Entity seatCaptureTarget;
    private static Matrix4f seatCaptureBaseInverse;
    private static Matrix4f seatCaptureBonePose;
    private static boolean seatCaptureActive;
    private static boolean seatCaptured;

    private RidingChainPaperDoll() {
    }

    /**
     * Builds the rendered chain. When the target rides a dragon, the chain is root mounts -> target;
     * the target's own passengers (any level) are appended as well. A target that is itself a dragon
     * mount returns its passengers only. {@code null} means the host keeps its own rendering.
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

    /** Normalizes the root mount to human height; every member keeps its world-relative size. */
    public static float getGroupScale(final LivingEntity root, final float partialTick) {
        if (!Config.PAPER_DOLL_HUMAN_SCALE.get() || !(root instanceof Player player)
                || !DragonStateProvider.isDragon(player)) {
            return 1.0F;
        }
        final double scale = DragonStateProvider.getData(player).getVisualScale(player, partialTick);
        return scale > 1.0E-3 ? (float) (1.0 / scale) : 1.0F;
    }

    /**
     * Screen adaptation for the chain. The doll grows upward from the host anchor, so a tall
     * chain first moves the anchor down (using the free screen space below) and only shrinks
     * when the chain is taller than the whole usable screen.
     */
    public static ChainFit computeFit(final List<LivingEntity> chain, final LivingEntity root,
                                      final float sizePx, final float anchorY, final float screenHeight) {
        if (!Config.PAPER_DOLL_RIDING_CHAIN_AUTO_FIT.get() || chain.size() <= 1) {
            return new ChainFit(1.0F, 0.0F);
        }

        // DS dragon hitboxes (DragonSizeHandler: body height * SCALE) are much taller than the
        // rendered model. With group scaling the model is normalized to about human height.
        final float rootHeight = Config.PAPER_DOLL_HUMAN_SCALE.get()
                ? 1.8F
                : Math.max(1.0F, root.getDimensions(root.getPose()).height());
        final float chainHeight = rootHeight + (chain.size() - 1) * Math.max(1.0F, rootHeight * 0.7F);
        final float needed = chainHeight * sizePx;

        final float margin = 5.0F;
        final float usable = Math.max(1.0F, screenHeight - margin * 2.0F);
        final float fit = needed > usable ? Math.max(0.15F, usable / needed) : 1.0F;
        final float height = needed * fit;

        // Keep the host anchor when it already fits, otherwise slide the feet down so the top
        // sits at the margin. A bottom anchor stays at the bottom.
        final float feet = Math.min(screenHeight - margin, Math.max(anchorY, margin + height));
        return new ChainFit(fit, feet - anchorY);
    }

    public record ChainFit(float scale, float shift) {
    }

    /**
     * Renders the chain into {@code poseStack}. The caller owns the outer frame (view matrix,
     * scale, yaw, lighting, camera orientation). {@code nodePoser} applies the host's per-node
     * body/head orientation right before that node is drawn.
     */
    public static void renderChain(final PoseStack poseStack, final List<LivingEntity> chain,
                                   final Map<LivingEntity, List<LivingEntity>> children,
                                   final EntityRenderDispatcher dispatcher,
                                   final MultiBufferSource.BufferSource bufferSource,
                                   final float partialTick, final Consumer<LivingEntity> nodePoser) {
        final List<NodeState> backups = new ArrayList<>(chain.size());
        for (LivingEntity node : chain) {
            backups.add(NodeState.save(node));
        }

        DsNeckFix.pushChainRendering();
        DsNeckFix.pushCompatRendering();
        try {
            renderNode(poseStack, chain.get(0), children, dispatcher, bufferSource, partialTick, nodePoser);
        } finally {
            DsNeckFix.popCompatRendering();
            DsNeckFix.popChainRendering();
            for (int i = backups.size() - 1; i >= 0; i--) {
                backups.get(i).restore();
            }
        }
    }

    private static void renderNode(final PoseStack poseStack, final LivingEntity node,
                                   final Map<LivingEntity, List<LivingEntity>> children,
                                   final EntityRenderDispatcher dispatcher,
                                   final MultiBufferSource.BufferSource bufferSource,
                                   final float partialTick, final Consumer<LivingEntity> nodePoser) {
        final List<LivingEntity> nodeChildren = children.get(node);
        final boolean captureSeat = nodeChildren != null && !nodeChildren.isEmpty()
                && DragonStateProvider.isDragon(node);

        if (captureSeat) {
            beginSeatCapture(node, poseStack.last().pose());
        }

        SeatTransform seat = null;
        try {
            nodePoser.accept(node);
            dispatcher.render(node, 0.0, 0.0, 0.0, 0.0F, partialTick, poseStack, bufferSource, DOLL_LIGHT);
            if (captureSeat) {
                seat = takeSeatTransform();
            }
        } finally {
            if (captureSeat) {
                endSeatCapture();
            }
        }

        if (nodeChildren == null) {
            return;
        }

        for (LivingEntity child : nodeChildren) {
            poseStack.pushPose();
            try {
                if (seat != null) {
                    poseStack.translate(seat.position().x, seat.position().y, seat.position().z);
                    poseStack.mulPose(seat.tilt());
                } else {
                    // DS fallback without a MountingBone: vanilla PASSENGER point, no tilt.
                    final int index = Math.max(0, node.getPassengers().indexOf(child));
                    final Vec3 attachment = node.getDimensions(node.getPose()).attachments()
                            .getClamped(EntityAttachment.PASSENGER, index, node.getYRot());
                    poseStack.translate(attachment.x, attachment.y, attachment.z);
                }
                final Vec3 pivot = child.getVehicleAttachmentPoint(node);
                poseStack.translate(-pivot.x, -pivot.y, -pivot.z);

                renderNode(poseStack, child, children, dispatcher, bufferSource, partialTick, nodePoser);
            } finally {
                poseStack.popPose();
            }
        }
    }

    // ---- MountingBone capture (filled by DragonRendererMixin#dsneckfix$captureMountingBone) ----

    public static void beginSeatCapture(final Entity mount, final Matrix4f nodeBase) {
        seatCaptureTarget = mount;
        seatCaptureBaseInverse = new Matrix4f(nodeBase).invert();
        seatCaptureBonePose = null;
        seatCaptureActive = true;
        seatCaptured = false;
    }

    public static boolean isSeatCaptureActive() {
        return seatCaptureActive;
    }

    public static boolean isSeatCaptureTarget(final Player player) {
        return seatCaptureActive && player == seatCaptureTarget;
    }

    public static void captureSeatMatrix(final Matrix4f bonePose) {
        if (seatCaptureActive && !seatCaptured) {
            seatCaptureBonePose = new Matrix4f(bonePose);
            seatCaptured = true;
        }
    }

    public static SeatTransform takeSeatTransform() {
        if (!seatCaptureActive || !seatCaptured) {
            return null;
        }

        final Matrix4f local = new Matrix4f(seatCaptureBaseInverse).mul(seatCaptureBonePose);
        final Vector3f position = local.getTranslation(new Vector3f());
        if (!isFinite(position)) {
            return null;
        }

        final Vector3f up = local.transformDirection(new Vector3f(0, 1, 0));
        final float lengthSquared = up.lengthSquared();
        final Quaternionf tilt = new Quaternionf();
        if (Float.isFinite(lengthSquared) && lengthSquared >= 1.0E-8F) {
            up.div((float) Math.sqrt(lengthSquared));
            tilt.rotationTo(0, 1, 0, up.x, up.y, up.z);
            if (!isFinite(tilt)) {
                tilt.identity();
            } else {
                tilt.normalize();
            }
        }

        return new SeatTransform(position, tilt);
    }

    public static void endSeatCapture() {
        seatCaptureActive = false;
        seatCaptureTarget = null;
        seatCaptureBaseInverse = null;
        seatCaptureBonePose = null;
        seatCaptured = false;
    }

    private static boolean isFinite(final Vector3f vector) {
        return Float.isFinite(vector.x) && Float.isFinite(vector.y) && Float.isFinite(vector.z);
    }

    private static boolean isFinite(final Quaternionf quaternion) {
        return Float.isFinite(quaternion.x) && Float.isFinite(quaternion.y)
                && Float.isFinite(quaternion.z) && Float.isFinite(quaternion.w);
    }

    public record SeatTransform(Vector3f position, Quaternionf tilt) {
    }

    private record NodeState(LivingEntity node, float xRot, float xRotO, float yRot, float yRotO,
                             float yHeadRot, float yHeadRotO, float yBodyRot, float yBodyRotO,
                             Vec3 deltaMovement) {
        private static NodeState save(final LivingEntity node) {
            return new NodeState(node, node.getXRot(), node.xRotO, node.getYRot(), node.yRotO,
                    node.yHeadRot, node.yHeadRotO, node.yBodyRot, node.yBodyRotO,
                    node.getDeltaMovement());
        }

        private void restore() {
            node.setXRot(xRot);
            node.xRotO = xRotO;
            node.setYRot(yRot);
            node.yRotO = yRotO;
            node.yHeadRot = yHeadRot;
            node.yHeadRotO = yHeadRotO;
            node.yBodyRot = yBodyRot;
            node.yBodyRotO = yBodyRotO;
            node.setDeltaMovement(deltaMovement);
        }
    }
}
