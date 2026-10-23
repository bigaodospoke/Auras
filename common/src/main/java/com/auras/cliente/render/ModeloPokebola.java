package com.auras.cliente.render;

import com.auras.Auras;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Modelo 3D de verdade das pokebolas do Cobblemon (bedrock/poke_balls/models/poke_ball.geo.json e as
 * texturas dele), lido dos recursos em tempo de execucao. Nao depende do Cobblemon para compilar:
 * se ele nao estiver instalado, disponivel() da false e a aura usa as pokebolas desenhadas no atlas.
 */
public final class ModeloPokebola {
    private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath("cobblemon", "bedrock/poke_balls/models/poke_ball.geo.json");
    private static final String[] BOLAS = {"poke_ball", "great_ball", "ultra_ball", "master_ball"};

    /** Caixa com UV no formato "box" do Bedrock. Medidas em pixels do modelo. */
    private record Cubo(float x0, float y0, float z0, float x1, float y1, float z1, float u, float v, float w, float h, float d) {
    }

    private static final class Modelo {
        final List<Cubo> base = new ArrayList<>();
        final List<Cubo> tampa = new ArrayList<>();
        float pivoY, pivoZ;
        float texW = 64, texH = 32;
        RenderType[] tipos;
    }

    @Nullable
    private static Modelo modelo;
    private static boolean tentou;

    public static boolean disponivel() {
        if (!tentou) {
            tentou = true;
            try {
                modelo = carregar(Minecraft.getInstance().getResourceManager());
            } catch (Exception e) {
                Auras.LOG.warn("Nao consegui ler o modelo de pokebola do Cobblemon, usando o desenho proprio", e);
                modelo = null;
            }
            if (modelo != null) Auras.LOG.info("Usando o modelo 3D de pokebola do Cobblemon na aura Pokebola");
        }
        return modelo != null;
    }

    @Nullable
    private static Modelo carregar(ResourceManager rm) throws Exception {
        Optional<Resource> geo = rm.getResource(GEO);
        if (geo.isEmpty()) return null;
        Modelo m = new Modelo();
        m.tipos = new RenderType[BOLAS.length];
        for (int i = 0; i < BOLAS.length; i++) {
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/poke_balls/" + BOLAS[i] + ".png");
            if (rm.getResource(tex).isEmpty()) return null;
            m.tipos[i] = RenderType.entityTranslucent(tex);
        }
        JsonObject raiz;
        try (Reader r = geo.get().openAsReader()) {
            raiz = JsonParser.parseReader(r).getAsJsonObject();
        }
        JsonObject g = raiz.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject desc = g.getAsJsonObject("description");
        if (desc != null) {
            if (desc.has("texture_width")) m.texW = desc.get("texture_width").getAsFloat();
            if (desc.has("texture_height")) m.texH = desc.get("texture_height").getAsFloat();
        }
        for (JsonElement el : g.getAsJsonArray("bones")) {
            JsonObject osso = el.getAsJsonObject();
            String nome = osso.has("name") ? osso.get("name").getAsString() : "";
            boolean tampa = nome.equals("lid");
            if (tampa && osso.has("pivot")) {
                JsonArray p = osso.getAsJsonArray("pivot");
                m.pivoY = p.get(1).getAsFloat();
                m.pivoZ = p.get(2).getAsFloat();
            }
            if (!osso.has("cubes")) continue;
            for (JsonElement ce : osso.getAsJsonArray("cubes")) {
                JsonObject c = ce.getAsJsonObject();
                // So UV "box" (array). UV por face nao aparece nesse modelo.
                if (!c.has("uv") || !c.get("uv").isJsonArray()) continue;
                JsonArray o = c.getAsJsonArray("origin"), s = c.getAsJsonArray("size"), uv = c.getAsJsonArray("uv");
                float inf = c.has("inflate") ? c.get("inflate").getAsFloat() : 0f;
                float ox = o.get(0).getAsFloat(), oy = o.get(1).getAsFloat(), oz = o.get(2).getAsFloat();
                float sw = s.get(0).getAsFloat(), sh = s.get(1).getAsFloat(), sd = s.get(2).getAsFloat();
                Cubo cubo = new Cubo(ox - inf, oy - inf, oz - inf, ox + sw + inf, oy + sh + inf, oz + sd + inf,
                        uv.get(0).getAsFloat(), uv.get(1).getAsFloat(), sw, sh, sd);
                (tampa ? m.tampa : m.base).add(cubo);
            }
        }
        return m.base.isEmpty() ? null : m;
    }

    /**
     * Desenha a pokebola centrada no ponto. yaw gira a bola (a frente, com o botao, fica para -Z do
     * modelo); abertura em radianos levanta a tampa na dobradica de tras. tamanho = lado da bola em blocos.
     */
    public static void desenhar(Desenho d, int tipo, double wx, double wy, double wz, float yaw, float inclina,
                                float abertura, float tamanho, int luz) {
        Modelo m = modelo;
        if (m == null) return;
        PoseStack pose = d.pose;
        pose.pushPose();
        pose.translate(wx - d.ox, wy - d.oy, wz - d.oz);
        pose.mulPose(Axis.YP.rotation(yaw));
        pose.mulPose(Axis.XP.rotation(inclina));
        float s = tamanho / 8f;
        pose.scale(s, s, s);
        pose.translate(0f, -4f, 0f);
        VertexConsumer vc = d.buffers.getBuffer(m.tipos[tipo % m.tipos.length]);
        for (Cubo c : m.base) cubo(vc, pose.last(), c, m, luz);
        pose.translate(0f, m.pivoY, m.pivoZ);
        pose.mulPose(Axis.XP.rotation(abertura));
        pose.translate(0f, -m.pivoY, -m.pivoZ);
        for (Cubo c : m.tampa) cubo(vc, pose.last(), c, m, luz);
        pose.popPose();
    }

    private static void cubo(VertexConsumer vc, PoseStack.Pose p, Cubo c, Modelo m, int luz) {
        float x0 = c.x0(), y0 = c.y0(), z0 = c.z0(), x1 = c.x1(), y1 = c.y1(), z1 = c.z1();
        float u = c.u(), v = c.v(), w = c.w(), h = c.h(), d = c.d();
        // Layout "box": em cima [d][cima w][baixo w]; embaixo [leste d][norte w][oeste d][sul w]
        if (w > 0 && d > 0) {
            face(vc, p, m, luz, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, u + d, v, u + d + w, v + d, 0, 1, 0);
            face(vc, p, m, luz, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, u + d + w, v, u + d + 2 * w, v + d, 0, -1, 0);
        }
        if (w > 0 && h > 0) {
            face(vc, p, m, luz, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, u + d, v + d, u + d + w, v + d + h, 0, 0, -1);
            face(vc, p, m, luz, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h, 0, 0, 1);
        }
        if (d > 0 && h > 0) {
            face(vc, p, m, luz, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, u, v + d, u + d, v + d + h, 1, 0, 0);
            face(vc, p, m, luz, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, u + d + w, v + d, u + 2 * d + w, v + d + h, -1, 0, 0);
        }
    }

    /** Quatro cantos na ordem: cima-esq, baixo-esq, baixo-dir, cima-dir (vistos de fora). */
    private static void face(VertexConsumer vc, PoseStack.Pose p, Modelo m, int luz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1, float nx, float ny, float nz) {
        Matrix4f mat = p.pose();
        float tu0 = u0 / m.texW, tv0 = v0 / m.texH, tu1 = u1 / m.texW, tv1 = v1 / m.texH;
        vertice(vc, p, mat, ax, ay, az, tu0, tv0, luz, nx, ny, nz);
        vertice(vc, p, mat, bx, by, bz, tu0, tv1, luz, nx, ny, nz);
        vertice(vc, p, mat, cx, cy, cz, tu1, tv1, luz, nx, ny, nz);
        vertice(vc, p, mat, dx, dy, dz, tu1, tv0, luz, nx, ny, nz);
    }

    private static void vertice(VertexConsumer vc, PoseStack.Pose p, Matrix4f mat, float x, float y, float z,
                                float u, float v, int luz, float nx, float ny, float nz) {
        vc.addVertex(mat, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz)
                .setNormal(p, nx, ny, nz);
    }

    private ModeloPokebola() {
    }
}
