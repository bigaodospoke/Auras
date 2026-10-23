package com.auras.cliente.render;

import com.auras.cliente.AurasCliente;
import com.auras.cliente.efeito.Cor;
import com.auras.cliente.efeito.EstadoAura;
import com.auras.cliente.efeito.Particula;
import com.auras.cliente.efeito.Sprite;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.LinkedHashMap;

/** Desenha as auras no mundo (depois das entidades) e na previa do menu. */
public final class RenderAuras {
    private static MultiBufferSource.BufferSource fonte;
    private static final Desenho DESENHO = new Desenho();
    private static final Quaternionf ROT_BLOCO = new Quaternionf();
    /** Conta os frames do mundo desenhados (o autoteste usa para nao gravar quadro repetido). */
    public static int quadros;

    private static MultiBufferSource.BufferSource fonte() {
        if (fonte == null) {
            // Ordem do desenho no fim: blocos e modelos (buffer compartilhado), translucido, brilho e o escuro por cima de tudo
            LinkedHashMap<RenderType, ByteBufferBuilder> fixos = new LinkedHashMap<>();
            fixos.put(TiposRender.TRANSLUCIDO, new ByteBufferBuilder(1 << 17));
            fixos.put(TiposRender.BRILHO, new ByteBufferBuilder(1 << 18));
            fixos.put(TiposRender.ESCURO, new ByteBufferBuilder(1 << 15));
            fonte = MultiBufferSource.immediateWithBuffers(fixos, new ByteBufferBuilder(1 << 16));
        }
        return fonte;
    }

    public static void mundo(Camera camera, float parcial, @Nullable Frustum frustum) {
        Minecraft mc = Minecraft.getInstance();
        AurasCliente cli = AurasCliente.get();
        quadros++;
        if (mc.level == null || !cli.temEstados()) return;
        Vec3 cam = camera.getPosition();
        MultiBufferSource.BufferSource f = fonte();
        PoseStack pose = new PoseStack();
        DESENHO.comecar(pose, f, f.getBuffer(TiposRender.BRILHO), f.getBuffer(TiposRender.TRANSLUCIDO), f.getBuffer(TiposRender.ESCURO),
                cam.x, cam.y, cam.z, parcial, false);
        DESENHO.visao.mundo(camera);
        for (AbstractClientPlayer p : mc.level.players()) {
            EstadoAura e = cli.estado(p);
            if (e == null) continue;
            if (frustum != null && !frustum.isVisible(p.getBoundingBox().inflate(2.0, 1.5, 2.0))) continue;
            DESENHO.luzMundo = LevelRenderer.getLightColor(mc.level, BlockPos.containing(p.getX(), p.getEyeY(), p.getZ()));
            desenhar(DESENHO, e);
        }
        f.endBatch();
    }

    /**
     * Previa no menu, com os pes do modelo em (x, y) da tela. Use a mesma escala e rotacao com que o
     * jogador foi desenhado.
     */
    public static void gui(GuiGraphics g, EstadoAura e, float x, float y, float escala, Quaternionf rot, float parcial) {
        MultiBufferSource.BufferSource f = fonte();
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 50.0f);
        pose.scale(escala, escala, -escala);
        pose.mulPose(rot);
        DESENHO.comecar(pose, f, f.getBuffer(TiposRender.BRILHO), f.getBuffer(TiposRender.TRANSLUCIDO), f.getBuffer(TiposRender.ESCURO),
                0, 0, 0, parcial, true);
        DESENHO.visao.gui(rot);
        DESENHO.luzMundo = Desenho.LUZ_TOTAL;
        desenhar(DESENHO, e);
        f.endBatch();
        pose.popPose();
    }

    private static void desenhar(Desenho d, EstadoAura e) {
        d.preparar(e);
        e.visual.desenhar(d, e);
        for (Particula p : e.particulas) particula(d, p);
    }

    private static void particula(Desenho d, Particula p) {
        float parcial = d.parcial;
        float t = p.progresso(parcial);
        double x = Mth.lerp(parcial, p.xo, p.x), y = Mth.lerp(parcial, p.yo, p.y), z = Mth.lerp(parcial, p.zo, p.z);
        if (p.relativo) {
            x += d.cx;
            y += d.cy;
            z += d.cz;
        }
        int cor = Cor.multAlfa(Cor.gradiente(p.cores, t), p.envelope(t, d.tempo));
        if ((cor >>> 24) == 0) return;
        float tam = p.tamanho(t);
        if (p.forma == Particula.Forma.BILLBOARD) {
            // Quadrado grande perto do chao entraria na grama e deixaria uma borda reta: encolhe
            float max = (float) (y - d.cy) * 1.2f + 0.12f;
            if (tam > max) tam = Math.max(max, 0f);
        }
        int luz = p.aditivo || p.luzPropria ? Desenho.LUZ_TOTAL : d.luzMundo;
        Sprite s = p.sprite;
        if (p.animacao != null) {
            int q = (int) ((p.idade + parcial) * p.quadrosPorTick + p.fase * 2f);
            s = p.animacao[Math.floorMod(q, p.animacao.length)];
        }
        d.porCima = p.porCima;
        switch (p.forma) {
            case BILLBOARD -> d.billboard(p.aditivo, x, y, z, tam, Mth.lerp(parcial, p.rotO, p.rot), cor, s, luz);
            case ESTICADA -> d.esticada(p.aditivo, x, y, z, (p.x - p.xo) * p.esticar, (p.y - p.yo) * p.esticar,
                    (p.z - p.zo) * p.esticar, tam, cor, s, luz);
            case CHAO -> d.chao(p.aditivo, x, y, z, tam, Mth.lerp(parcial, p.rotO, p.rot), cor, s, luz);
            case BLOCO -> {
                // Para blocos a posicao e a base (flor nasce do chao)
                if (p.bloco != null && tam > 0.001f) {
                    ROT_BLOCO.identity().rotateY(Mth.lerp(parcial, p.rotO, p.rot));
                    d.bloco(p.bloco, x, y + tam * 0.5, z, tam, ROT_BLOCO, p.luzPropria ? Desenho.LUZ_TOTAL : d.luzMundo);
                }
            }
        }
        d.porCima = false;
    }

    private RenderAuras() {
    }
}
