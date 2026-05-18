package com.lxy.hook.client.render;

import com.lxy.hook.entity.HookProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class HookRopeRenderer {

    public static void register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(HookRopeRenderer::renderRopes);
    }

    private static void renderRopes(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        PoseStack poseStack = context.poseStack();
        MultiBufferSource.BufferSource bufferSource = context.bufferSource();
        Vec3 camera = mc.gameRenderer.getMainCamera().position();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof HookProjectileEntity hook)) continue;

            Player owner = findOwner(hook);
            if (owner == null) continue;

            Vec3 hookPos = hook.position().subtract(camera);
            Vec3 ownerPos = owner.getEyePosition().subtract(camera);

            renderRopeSegment(poseStack, bufferSource, ownerPos, hookPos);
        }
    }

    private static Player findOwner(HookProjectileEntity hook) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Entity e = mc.level.getEntity(hook.getOwnerId());
        return e instanceof Player p ? p : null;
    }

    private static void renderRopeSegment(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                          Vec3 from, Vec3 to) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.leash());
        Matrix4f matrix = poseStack.last().pose();

        int segments = 16;
        float ropeWidth = 0.05F;

        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;

            Vec3 dir = to.subtract(from);
            Vec3 p0 = from.add(dir.scale(t0));
            Vec3 p1 = from.add(dir.scale(t1));

            // 垂坠效果
            double sag0 = Math.sin(t0 * Math.PI) * 0.3;
            double sag1 = Math.sin(t1 * Math.PI) * 0.3;
            p0 = p0.add(0, -sag0, 0);
            p1 = p1.add(0, -sag1, 0);

            consumer.addVertex(matrix, (float) p0.x - ropeWidth, (float) p0.y, (float) p0.z).setUv(t0, 0).setColor(-1).setLight(15728880).setNormal(0, 1, 0);
            consumer.addVertex(matrix, (float) p0.x + ropeWidth, (float) p0.y, (float) p0.z).setUv(t0, 1).setColor(-1).setLight(15728880).setNormal(0, 1, 0);
            consumer.addVertex(matrix, (float) p1.x + ropeWidth, (float) p1.y, (float) p1.z).setUv(t1, 1).setColor(-1).setLight(15728880).setNormal(0, 1, 0);
            consumer.addVertex(matrix, (float) p1.x - ropeWidth, (float) p1.y, (float) p1.z).setUv(t1, 0).setColor(-1).setLight(15728880).setNormal(0, 1, 0);
        }
    }
}
