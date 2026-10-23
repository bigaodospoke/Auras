package com.auras.cliente.render;

import com.auras.Auras;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Os dois jeitos de desenhar as auras, usando o atlas de efeitos com filtro suave.
 * Estende RenderType so para enxergar os estados protegidos do vanilla.
 */
public final class TiposRender extends RenderType {
    public static final ResourceLocation ATLAS = Auras.id("textures/efeitos/atlas.png");
    private static final TextureStateShard TEXTURA = new TextureStateShard(ATLAS, true, false);

    /** Luz somada: cor * alfa vai para cima do que ja esta na tela. Sem descarte de alfa, com neblina. */
    public static final RenderType BRILHO = new RenderType("auras_brilho", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, false, false,
            () -> {
                RENDERTYPE_EYES_SHADER.setupRenderState();
                TEXTURA.setupRenderState();
                LIGHTNING_TRANSPARENCY.setupRenderState();
                NO_CULL.setupRenderState();
                LEQUAL_DEPTH_TEST.setupRenderState();
                COLOR_WRITE.setupRenderState();
            },
            () -> {
                COLOR_WRITE.clearRenderState();
                LEQUAL_DEPTH_TEST.clearRenderState();
                NO_CULL.clearRenderState();
                LIGHTNING_TRANSPARENCY.clearRenderState();
                TEXTURA.clearRenderState();
                RENDERTYPE_EYES_SHADER.clearRenderState();
            }) {
    };

    /** Translucido comum (fumaca, folhas, neve), com luz do mundo e ordenado por distancia. */
    public static final RenderType TRANSLUCIDO = new RenderType("auras_translucido", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, false, true,
            () -> {
                RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
                TEXTURA.setupRenderState();
                TRANSLUCENT_TRANSPARENCY.setupRenderState();
                NO_CULL.setupRenderState();
                LEQUAL_DEPTH_TEST.setupRenderState();
                LIGHTMAP.setupRenderState();
                OVERLAY.setupRenderState();
                COLOR_WRITE.setupRenderState();
            },
            () -> {
                COLOR_WRITE.clearRenderState();
                OVERLAY.clearRenderState();
                LIGHTMAP.clearRenderState();
                LEQUAL_DEPTH_TEST.clearRenderState();
                NO_CULL.clearRenderState();
                TRANSLUCENT_TRANSPARENCY.clearRenderState();
                TEXTURA.clearRenderState();
                RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
            }) {
    };

    /**
     * Igual ao translucido, mas desenhado depois do brilho: um nucleo preto (raio negro, estilhacos)
     * fica preto mesmo com o brilho vermelho somado em volta.
     */
    public static final RenderType ESCURO = new RenderType("auras_escuro", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, false, true,
            () -> {
                RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
                TEXTURA.setupRenderState();
                TRANSLUCENT_TRANSPARENCY.setupRenderState();
                NO_CULL.setupRenderState();
                LEQUAL_DEPTH_TEST.setupRenderState();
                LIGHTMAP.setupRenderState();
                OVERLAY.setupRenderState();
                COLOR_WRITE.setupRenderState();
            },
            () -> {
                COLOR_WRITE.clearRenderState();
                OVERLAY.clearRenderState();
                LIGHTMAP.clearRenderState();
                LEQUAL_DEPTH_TEST.clearRenderState();
                NO_CULL.clearRenderState();
                TRANSLUCENT_TRANSPARENCY.clearRenderState();
                TEXTURA.clearRenderState();
                RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
            }) {
    };

    private TiposRender() {
        super("auras_nunca", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 0, false, false, () -> {
        }, () -> {
        });
    }
}
