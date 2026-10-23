package com.auras.cliente.visual;

import com.auras.cliente.efeito.Cor;
import com.auras.cliente.efeito.Corpo;
import com.auras.cliente.efeito.EstadoAura;
import com.auras.cliente.efeito.Particula;
import com.auras.cliente.efeito.Sprites;
import com.auras.cliente.render.Desenho;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

/** Poca de magma rachada nos pes, rochas de magma orbitando e pingando, bolhas que estouram e erupcoes. */
public final class VisualLava extends VisualAura {
    private static final int[] GOTA = {0xFFFFF0A0, 0xFFFFA028, 0xFFFF6A10};
    private static final int[] RESPINGO = {0xFFFFE080, 0xFFFF7A18, 0x00C02000};
    private static final int[] BRASA = {0xFFFFD070, 0xFFFF6A14, 0xFFC02408, 0x00601000};
    private static final int[] FUMACA = {0x00241C18, 0x48201816, 0x2A181412, 0x00101010};
    private static final int ROCHAS = 4;
    private static final Quaternionf ROT = new Quaternionf();
    private static BlockState magma;

    private static BlockState magma() {
        if (magma == null) magma = Blocks.MAGMA_BLOCK.defaultBlockState();
        return magma;
    }

    private static void rocha(int k, float t, float[] out) {
        float ang = t * 0.045f + k * TAU / ROCHAS;
        float r = 1.0f + 0.08f * Mth.sin(t * 0.05f + k);
        out[0] = Mth.cos(ang) * r;
        out[1] = 0.5f + (k % 2) * 0.6f + Mth.sin(t * 0.07f + k * 1.7f) * 0.12f;
        out[2] = Mth.sin(ang) * r;
    }

    /** Gota que cai ate o nivel dos pes e espirra. */
    private static final Particula.Comportamento CAINDO = (p, e) -> {
        if (p.y + p.vy <= e.corpo.y + 0.03 && p.vy < 0) {
            respingo(e, p.x, e.corpo.y + 0.03, p.z, 4, 0.18f);
            p.matar();
        }
    };

    private static void gota(EstadoAura e, double x, double y, double z, double vx, double vy, double vz) {
        e.emitir(x, y, z).vel(vx, vy, vz).vida(60).tam(0.075f, 0.06f).cores(GOTA).sprite(Sprites.NUCLEO)
                .fisica(0.03f, 0.98f).fade(0.05f, 0f).comportamento(CAINDO);
    }

    private static void respingo(EstadoAura e, double x, double y, double z, int n, float onda) {
        RandomSource r = e.random;
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * TAU, v = 0.03f + r.nextFloat() * 0.05f;
            e.emitir(x, y, z).vel(Mth.cos(ang) * v, 0.06f + r.nextFloat() * 0.06f, Mth.sin(ang) * v)
                    .vida(8 + r.nextInt(6)).tam(0.05f, 0.015f).cores(RESPINGO).sprite(Sprites.NUCLEO)
                    .fisica(0.03f, 0.95f).fade(0f, 0.5f);
        }
        e.emitir(x, y, z).forma(Particula.Forma.CHAO).sprite(Sprites.ANEL).tam(0.04f, onda).vida(7)
                .cores(0xFFFFB040, 0x00FF4000).fade(0f, 0.6f).fisica(0, 1);
    }

    @Override
    public void tick(EstadoAura e) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        float[] o = new float[3];

        // Rochas pingam e soltam brasas
        for (int k = 0; k < ROCHAS; k++) {
            rocha(k, e.idade, o);
            if (e.chance(0.07f)) gota(e, c.x + o[0], c.y + o[1] - 0.1, c.z + o[2], 0, -0.02, 0);
            if (e.chance(0.35f)) {
                e.emitir(c.x + o[0], c.y + o[1], c.z + o[2])
                        .vel((r.nextFloat() - 0.5f) * 0.02f, 0.01f + r.nextFloat() * 0.02f, (r.nextFloat() - 0.5f) * 0.02f)
                        .vida(12 + r.nextInt(8)).tam(0.08f, 0.01f).cores(BRASA).sprite(Sprites.NUCLEO)
                        .fisica(-0.001f, 0.95f).fade(0f, 0.6f);
            }
        }
        // Lava escorrendo do corpo
        for (int i = 0, n = c.primeiraPessoa ? 0 : e.quantos(0.3f); i < n; i++) {
            double[] p = noCorpo(e, 0.3f, 0.36f, 0.3f, 0.85f);
            gota(e, c.x + p[0], c.y + p[1], c.z + p[2], 0, -0.01, 0);
        }
        // Bolhas na poca
        for (int i = 0, n = e.quantos(0.3f); i < n; i++) {
            double[] p = noChao(e, 0.2f, 1.05f);
            e.emitir(c.x + p[0], c.y + 0.06, c.z + p[2]).vida(14 + r.nextInt(10)).tam(0.03f, 0.17f)
                    .cores(0xFFFFC050, 0xFFFF7020).sprite(Sprites.BOLHA).fade(0.15f, 0.05f).fisica(0, 1)
                    .comportamento((pp, es) -> {
                        if (pp.idade == pp.vida - 1) {
                            respingo(es, pp.x, pp.y, pp.z, 3, 0.3f);
                            if (es.random.nextFloat() < 0.15f) es.som(SoundEvents.LAVA_POP, 0.35f, 0.8f + es.random.nextFloat() * 0.4f);
                        }
                    });
        }
        // Erupcao pequena de vez em quando
        if (e.chance(0.025f)) {
            double[] p = noChao(e, 0.3f, 0.9f);
            for (int i = 0; i < 7; i++) {
                float ang = r.nextFloat() * TAU, v = 0.02f + r.nextFloat() * 0.04f;
                gota(e, c.x + p[0], c.y + 0.08, c.z + p[2], Mth.cos(ang) * v, 0.16f + r.nextFloat() * 0.12f, Mth.sin(ang) * v);
            }
            respingo(e, c.x + p[0], c.y + 0.05, c.z + p[2], 3, 0.4f);
        }
        // Brasas e fumaca escura subindo da poca
        for (int i = 0, n = e.quantos(0.8f); i < n; i++) {
            double[] p = noChao(e, 0f, 1.1f);
            e.emitir(c.x + p[0], c.y + 0.1, c.z + p[2])
                    .vel((r.nextFloat() - 0.5f) * 0.01f, 0.03f + r.nextFloat() * 0.04f, (r.nextFloat() - 0.5f) * 0.01f)
                    .vida(25 + r.nextInt(20)).tam(0.06f, 0.01f).cores(BRASA).sprite(Sprites.NUCLEO)
                    .fisica(-0.0008f, 0.97f).fade(0.05f, 0.5f)
                    .comportamento((pp, es) -> pp.vx += Mth.sin(pp.idade * 0.25f + pp.fase) * 0.003f)
                    .piscar = 0.6f;
        }
        for (int i = 0, n = c.primeiraPessoa ? 0 : e.quantos(0.4f); i < n; i++) {
            double[] p = noChao(e, 0f, 1.0f);
            e.emitir(c.x + p[0], c.y + 0.6 + r.nextFloat() * 0.7, c.z + p[2])
                    .vel((r.nextFloat() - 0.5f) * 0.01f, 0.02f + r.nextFloat() * 0.02f, (r.nextFloat() - 0.5f) * 0.01f)
                    .vida(35 + r.nextInt(20)).tam(0.35f, 0.9f).cores(FUMACA).sprite(Sprites.FUMACAS[r.nextInt(3)])
                    .translucida(false).fisica(0f, 0.97f).giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.04f)
                    .fade(0f, 0f);
        }
        if (r.nextFloat() < 0.004f) e.som(SoundEvents.LAVA_AMBIENT, 0.4f, 0.9f + r.nextFloat() * 0.2f);
    }

    @Override
    public void anunciar(EstadoAura e) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        for (int i = 0; i < 30; i++) {
            float ang = r.nextFloat() * TAU, v = 0.04f + r.nextFloat() * 0.1f;
            gota(e, c.x, c.y + 0.1, c.z, Mth.cos(ang) * v, 0.2f + r.nextFloat() * 0.2f, Mth.sin(ang) * v);
        }
        respingo(e, c.x, c.y + 0.05, c.z, 10, 1.2f);
        onda(e, 0xFFFF6A20, 3.0f, 16, Sprites.ANEL_SUAVE);
        clarao(e, 0xFFFF8A30, 3.2f, 7);
        e.som(SoundEvents.BUCKET_EMPTY_LAVA, 0.8f, 0.8f);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        float h = d.corpo.altura, t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;
        float p = pulso(t, 0.08f, 0f);

        // Poca: escurece o chao e por cima as rachaduras incandescentes
        d.chao(false, cx, cy + 0.015, cz, 1.45f, 0f, Cor.comAlfa(0x0A0402, 0.55f * f), Sprites.BRILHO, luz);
        d.chao(true, cx, cy + 0.02, cz, 1.4f, 0f, Cor.comAlfa(0xFF3A08, (0.25f + 0.1f * p) * f), Sprites.ANEL_SUAVE, luz);
        d.chao(true, cx, cy + 0.025, cz, 1.25f, t * 0.004f, Cor.comAlfa(0xFFFFFF, (0.8f + 0.2f * p) * f), Sprites.LAVA, luz);
        d.chao(true, cx, cy + 0.03, cz, 0.8f, 0f, Cor.comAlfa(0xFF7A20, (0.15f + 0.1f * p) * f), Sprites.BRILHO, luz);
        if (!d.corpo.primeiraPessoa) {
            d.brilho(cx, cy + h * 0.55, cz, 1.1f, Cor.comAlfa(0xFF4010, (0.1f + 0.04f * p) * f), Sprites.BRILHO);
        }

        // Rochas de magma girando, com brilho em volta
        float[] o = new float[3];
        for (int k = 0; k < ROCHAS; k++) {
            rocha(k, t, o);
            double x = cx + o[0], y = cy + o[1], z = cz + o[2];
            d.brilho(x, y, z, 0.5f, Cor.comAlfa(0xFF5A10, 0.5f * f), Sprites.BRILHO);
            ROT.identity().rotateY(t * 0.05f + k).rotateX(t * 0.037f + k * 0.9f).rotateZ(t * 0.021f);
            d.bloco(magma(), x, y, z, 0.24f * f, ROT, luz);
            d.brilho(x, y, z, 0.2f, Cor.comAlfa(0xFFC060, 0.35f * tremor(t, k) * f), Sprites.BRILHO);
        }
    }
}
