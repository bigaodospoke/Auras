package com.auras.cliente.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Ilusoes de mobs (aura do necromante): os modelos de verdade do Minecraft desenhados translucidos e
 * com o brilho espiral do creeper carregado por cima. Nao entram no mundo: sao entidades "de enfeite"
 * so para dar pose ao modelo.
 */
public final class Fantasmas {
    public static final int ZUMBI = 0, ESQUELETO = 1, WITHER = 2, STRAY = 3, TIPOS = 4;

    private static final ResourceLocation ESPIRAL = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
    private static final ResourceLocation TEX_CRANIO = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
    private static final ResourceLocation TEX_CRANIO_WITHER = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png");

    private static final LivingEntity[] ENFEITES = new LivingEntity[TIPOS];
    private static Level nivel;
    private static SkullModel cranio;

    @Nullable
    private static LivingEntity enfeite(int tipo) {
        Level lvl = Minecraft.getInstance().level;
        if (lvl == null) return null;
        if (lvl != nivel) {
            Arrays.fill(ENFEITES, null);
            nivel = lvl;
        }
        if (ENFEITES[tipo] == null) {
            LivingEntity e = switch (tipo) {
                case ZUMBI -> {
                    Zombie z = new Zombie(EntityType.ZOMBIE, lvl);
                    z.setAggressive(true); // bracos esticados para frente
                    yield z;
                }
                case ESQUELETO -> new Skeleton(EntityType.SKELETON, lvl);
                case WITHER -> new WitherSkeleton(EntityType.WITHER_SKELETON, lvl);
                default -> new Stray(EntityType.STRAY, lvl);
            };
            ENFEITES[tipo] = e;
        }
        return ENFEITES[tipo];
    }

    /**
     * Mob fantasma em pe com os pes no ponto. yaw em graus (como o do Minecraft).
     * cor = ARGB do corpo translucido; brilho = RGB do espiral somado (ja multiplicado pela forca).
     */
    @SuppressWarnings("unchecked")
    public static void mob(Desenho d, int tipo, double x, double y, double z, float yaw, float escala,
                           int cor, int brilho, float tempo) {
        LivingEntity ent = enfeite(tipo);
        if (ent == null) return;
        EntityRenderer<? super LivingEntity> r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(ent);
        if (!(r instanceof LivingEntityRenderer<?, ?> lr)) return;
        EntityModel<LivingEntity> modelo = (EntityModel<LivingEntity>) lr.getModel();
        ResourceLocation tex = r.getTextureLocation(ent);
        float s = escala * (tipo == WITHER ? 1.2f : 1f);

        modelo.young = false;
        modelo.riding = false;
        modelo.attackTime = 0f;
        modelo.prepareMobModel(ent, 0f, 0f, d.parcial);
        modelo.setupAnim(ent, 0f, 0f, tempo, 0f, 0f);

        PoseStack pose = d.pose;
        pose.pushPose();
        pose.translate(x - d.ox, y - d.oy, z - d.oz);
        pose.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        pose.scale(-s, -s, s);
        pose.translate(0f, -1.501f, 0f);
        modelo.renderToBuffer(pose, d.buffers.getBuffer(RenderType.entityTranslucent(tex)), Desenho.LUZ_TOTAL,
                OverlayTexture.NO_OVERLAY, cor);
        // espiral um pouco maior, em volta do centro do corpo
        pose.translate(0f, 0.75f, 0f);
        pose.scale(1.04f, 1.02f, 1.04f);
        pose.translate(0f, -0.75f, 0f);
        float u = tempo * 0.01f;
        modelo.renderToBuffer(pose, d.buffers.getBuffer(RenderType.energySwirl(ESPIRAL, u % 1f, (u * 0.7f) % 1f)),
                Desenho.LUZ_TOTAL, OverlayTexture.NO_OVERLAY, 0xFF000000 | brilho);
        pose.popPose();
    }

    /** Cranio fantasma centrado no ponto. */
    public static void cranio(Desenho d, boolean wither, double x, double y, double z, float yaw, float pitch,
                              float escala, int cor, int brilho, float tempo) {
        if (cranio == null) cranio = new SkullModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SKELETON_SKULL));
        cranio.setupAnim(0f, yaw, pitch);
        PoseStack pose = d.pose;
        pose.pushPose();
        pose.translate(x - d.ox, y - d.oy - 0.25f * escala, z - d.oz);
        pose.scale(-escala, -escala, escala);
        cranio.renderToBuffer(pose, d.buffers.getBuffer(RenderType.entityTranslucent(wither ? TEX_CRANIO_WITHER : TEX_CRANIO)),
                Desenho.LUZ_TOTAL, OverlayTexture.NO_OVERLAY, cor);
        pose.translate(0f, -0.25f, 0f);
        pose.scale(1.08f, 1.08f, 1.08f);
        pose.translate(0f, 0.25f, 0f);
        float u = tempo * 0.012f;
        cranio.renderToBuffer(pose, d.buffers.getBuffer(RenderType.energySwirl(ESPIRAL, u % 1f, (u * 0.7f) % 1f)),
                Desenho.LUZ_TOTAL, OverlayTexture.NO_OVERLAY, 0xFF000000 | brilho);
        pose.popPose();
    }

    private Fantasmas() {
    }
}
