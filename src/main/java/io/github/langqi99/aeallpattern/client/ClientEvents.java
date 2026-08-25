package io.github.langqi99.aeallpattern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.langqi99.aeallpattern.network.BindingRenderEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientEvents {
    private static final double MAX_RENDER_DISTANCE_SQUARED = 96.0 * 96.0;

    private ClientEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientEvents::renderBindings);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLogout);
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBindingState.clear();
    }

    private static void renderBindings(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        for (BindingRenderEntry binding : ClientBindingState.bindings()) {
            if (!binding.dimension().equals(minecraft.level.dimension())
                    || minecraft.player.distanceToSqr(binding.pos().getCenter()) > MAX_RENDER_DISTANCE_SQUARED) {
                continue;
            }
            AABB bounds = blockBounds(minecraft, binding);
            float pulse = 0.72F + 0.18F * (float) Math.sin((minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false)) * 0.12F);
            LevelRenderer.renderLineBox(poses, lines, bounds.inflate(0.004), 0.68F, 0.25F, 1.0F, pulse);
        }
        poses.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static AABB blockBounds(Minecraft minecraft, BindingRenderEntry binding) {
        var state = minecraft.level.getBlockState(binding.pos());
        var shape = state.getShape(minecraft.level, binding.pos());
        return (shape.isEmpty() ? new AABB(binding.pos()) : shape.bounds().move(binding.pos()));
    }
}
