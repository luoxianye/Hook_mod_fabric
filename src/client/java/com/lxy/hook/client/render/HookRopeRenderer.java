package com.lxy.hook.client.render;

import com.lxy.hook.HookMod;
import com.lxy.hook.entity.HookProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

public final class HookRopeRenderer {
    private static final int FULL_BRIGHT = 0xF000F0;

    /*
     * 对应资源路径：
     * src/main/resources/assets/hook/textures/entity/rope.png
     */
    private static final Identifier ROPE_TEXTURE =
            Identifier.fromNamespaceAndPath(HookMod.MOD_ID, "textures/entity/rope.png");

    /*
     * 绳子外观参数
     */
    private static final float ROPE_HALF_WIDTH = 0.03F;

    /*
     * 每多少格重复一次 rope.png。
     * 数值越小，纹理重复越密。
     * 数值越大，纹理重复越疏。
     */
    private static final double TEXTURE_REPEAT_LENGTH = 0.3D;

    /*
     * 绳子下垂程度。
     * 0.0D = 完全直线。
     */
    private static final double SAG = 0.15D;

    /*
     * 最多分段数量，防止绳子过长时生成过多顶点。
     */
    private static final int MAX_SEGMENTS = 64;

    private HookRopeRenderer() {
    }

    public static void register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(HookRopeRenderer::renderRopes);

        /*
         * 如果你的 Fabric API 环境中 AFTER_TRANSLUCENT_FEATURES 报错，
         * 就改成下面这一行：
         *
         * LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(HookRopeRenderer::renderRopes);
         */
    }

    private static void renderRopes(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            return;
        }

        PoseStack poseStack = context.poseStack();
        MultiBufferSource.BufferSource bufferSource = context.bufferSource();

        if (poseStack == null || bufferSource == null) {
            return;
        }

        Vec3 camera = context.levelState().cameraRenderState.pos;

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        Matrix4f matrix = poseStack.last().pose();

        /*
         * 关键：
         * RenderTypes.g(ROPE_TEXTURE) 在你当前映射下对应 entityCutoutNoCull(texture)。
         * 它会真正使用 assets/hook/textures/entity/rope.png。
         */
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.entityCutout(ROPE_TEXTURE));

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof HookProjectileEntity hook)) {
                continue;
            }

            Player owner = findOwner(hook, mc);
            if (owner == null) {
                continue;
            }

            Vec3 ropeStart = getRopeStart(owner);
            Vec3 ropeEnd = getRopeEnd(hook);

            renderRope(matrix, consumer, ropeStart, ropeEnd, camera);
        }

        poseStack.popPose();
    }

    private static Player findOwner(HookProjectileEntity hook, Minecraft mc) {
        if (mc.level == null) {
            return null;
        }

        UUID ownerId = hook.getOwnerId();
        if (ownerId == null) {
            return null;
        }

        Entity owner = mc.level.getEntity(ownerId);
        return owner instanceof Player player ? player : null;
    }

    private static Vec3 getRopeStart(Player owner) {
        /*
         * 绳子起点。
         * 这里从玩家眼睛稍微往下发出，避免第一人称时绳子从屏幕正中心穿出来。
         */
        return owner.getEyePosition().add(0.0D, -0.15D, 0.0D);
    }

    private static Vec3 getRopeEnd(HookProjectileEntity hook) {
        /*
         * 绳子终点。
         * 如需让绳子连接到钩爪图片中心偏上/偏下，可以在这里加偏移。
         */
        return hook.position();
    }

    private static void renderRope(
            Matrix4f matrix,
            VertexConsumer consumer,
            Vec3 from,
            Vec3 to,
            Vec3 camera
    ) {
        Vec3 ropeVector = to.subtract(from);
        double ropeLength = ropeVector.length();

        if (ropeLength < 1.0E-6D) {
            return;
        }

        int segments = Math.max(1, Math.min(MAX_SEGMENTS, (int) Math.ceil(ropeLength / TEXTURE_REPEAT_LENGTH)));

        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;

            Vec3 p0 = interpolateWithSag(from, to, t0);
            Vec3 p1 = interpolateWithSag(from, to, t1);

            Vec3 segmentVector = p1.subtract(p0);
            if (segmentVector.lengthSqr() < 1.0E-8D) {
                continue;
            }

            Vec3 segmentDir = segmentVector.normalize();

            /*
             * billboard 偏移：
             * 让绳子的面始终尽量朝向摄像机，
             * 避免只从某些角度能看到。
             */
            Vec3 center = p0.add(p1).scale(0.5D);
            Vec3 cameraDir = camera.subtract(center);

            if (cameraDir.lengthSqr() < 1.0E-8D) {
                cameraDir = new Vec3(0.0D, 1.0D, 0.0D);
            } else {
                cameraDir = cameraDir.normalize();
            }

            Vec3 side = segmentDir.cross(cameraDir);

            if (side.lengthSqr() < 1.0E-8D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                side = side.normalize();
            }

            Vec3 offset = side.scale(ROPE_HALF_WIDTH);

            /*
             * 每个 segment 都完整贴一次 rope.png。
             * 这样不依赖 UV 大于 1 的重复采样，避免部分环境下纹理被拉伸或钳制。
             */
            emitTexturedQuad(matrix, consumer, p0, p1, offset);
        }
    }

    private static Vec3 interpolateWithSag(Vec3 from, Vec3 to, float t) {
        double x = lerp(from.x, to.x, t);
        double y = lerp(from.y, to.y, t) - Math.sin(t * Math.PI) * SAG;
        double z = lerp(from.z, to.z, t);

        return new Vec3(x, y, z);
    }

    private static double lerp(double start, double end, float delta) {
        return start + (end - start) * delta;
    }

    private static void emitTexturedQuad(
            Matrix4f matrix,
            VertexConsumer consumer,
            Vec3 p0,
            Vec3 p1,
            Vec3 offset
    ) {
        /*
         * 顶点顺序：
         * p0 左下 -> p0 右上 -> p1 右上 -> p1 左下
         *
         * U 方向：沿绳子长度
         * V 方向：绳子宽度
         */
        vertex(matrix, consumer, p0.subtract(offset), 0.0F, 0.0F);
        vertex(matrix, consumer, p0.add(offset), 0.0F, 1.0F);
        vertex(matrix, consumer, p1.add(offset), 1.0F, 1.0F);
        vertex(matrix, consumer, p1.subtract(offset), 1.0F, 0.0F);
    }

    private static void vertex(
            Matrix4f matrix,
            VertexConsumer consumer,
            Vec3 pos,
            float u,
            float v
    ) {
        /*
         * 使用实体贴图 RenderType 时，必须补齐 overlay。
         * 少 setOverlay(...) 时，运行时很容易因为顶点格式不完整而 crash。
         */
        consumer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}