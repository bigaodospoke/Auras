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

/** Aneis orbitais com planetas, nebulosas coloridas, estrelas piscando, estrelas cadentes e mapa estelar no chao. */
public final class VisualCosmica extends VisualAura {
    private static final int[] NEBULOSA = {0xFF9B5CFF, 0xFF4F7CFF, 0xFFFF6FD8, 0xFF6FE8FF};
    private static final int[] CADENTE = {0xFFFFFFFF, 0xFF9FE8FF, 0x004F7CFF};
    private static final int ANEIS = 3;
    private static final float[] INCLINACAO = {1.15f, -1.0f, 0.28f};
    private static final float[] RAIO = {0.95f, 0.9f, 1.12f};

    /** Nebulosa girando devagar em volta do corpo (particula relativa). */
    private static final Particula.Comportamento ORBITAR = (p, e) -> {
        double ang = 0.012;
        double x = p.x * Math.cos(ang) - p.z * Math.sin(ang);
        double z = p.x * Math.sin(ang) + p.z * Math.cos(ang);
        p.x = x;
        p.z = z;
    };

    /** Ponto do anel k no angulo a, relativo ao centro do corpo. */
    private static void anel(int k, float a, float t, float[] out) {
        float prec = t * 0.012f * (k == 1 ? -1 : 1) + k * 2.1f;
        float ux = Mth.cos(prec), uz = Mth.sin(prec);
        float tilt = INCLINACAO[k];
        float vx = -uz * Mth.cos(tilt), vy = Mth.sin(tilt), vz = ux * Mth.cos(tilt);
        float r = RAIO[k];
        float ca = Mth.cos(a), sa = Mth.sin(a);
        out[0] = (ux * ca + vx * sa) * r;
        out[1] = vy * sa * r;
        out[2] = (uz * ca + vz * sa) * r;
    }

    private static float cor(int k, float s, float t) {
        // Do ciano ao magenta, passando pelo azul e roxo
        return 0.68f + 0.16f * Mth.sin(s * TAU + t * 0.03f + k * 2f);
    }

    @Override
    public void tick(EstadoAura e) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        float h = c.altura;

        for (int i = 0, n = e.quantos(1.4f); i < n; i++) {
            double[] p = noCorpo(e, 0.3f, 1.35f, -0.05f, 1.25f);
            int cor = r.nextFloat() < 0.6f ? 0xFFFFFFFF : 0xFF000000 | Cor.hsv(0.55f + r.nextFloat() * 0.35f, 0.35f, 1f);
            e.emitirRel(p[0], p[1], p[2]).vida(14 + r.nextInt(18)).tam(0.06f + r.nextFloat() * 0.09f, -1)
                    .cores(cor).sprite(Sprites.ESTRELA).giro(PI / 4, 0f).fade(0.3f, 0.5f).fisica(0, 1).piscar = 0.3f;
        }
        for (int i = 0, n = e.quantos(0.28f); i < n; i++) {
            double[] p = noCorpo(e, 0.45f, 1.1f, 0.3f, 1.05f);
            int base = NEBULOSA[r.nextInt(NEBULOSA.length)] & 0xFFFFFF;
            e.emitirRel(p[0], p[1], p[2]).vida(40 + r.nextInt(25)).tam(0.5f, 1.05f)
                    .cores(Cor.comAlfa(base, 0f), Cor.comAlfa(base, 0.22f), Cor.comAlfa(base, 0f))
                    .sprite(Sprites.FUMACAS[r.nextInt(3)]).fisica(0f, 1f).giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.02f)
                    .fade(0f, 0f).comportamento(ORBITAR);
        }
        if (e.chance(0.03f)) {
            float ang = r.nextFloat() * TAU;
            float sp = 0.18f + r.nextFloat() * 0.1f;
            double sx = c.x + Mth.cos(ang) * 1.8, sz = c.z + Mth.sin(ang) * 1.8;
            Particula p = e.emitir(sx, c.y + h + 0.6 + r.nextFloat() * 0.6, sz)
                    .vel(-Mth.sin(ang) * sp, -0.07f - r.nextFloat() * 0.05f, Mth.cos(ang) * sp).vida(16)
                    .tam(0.07f, 0.03f).cores(CADENTE).sprite(Sprites.FAISCA).forma(Particula.Forma.ESTICADA)
                    .fisica(0f, 1f).fade(0.1f, 0.4f);
            p.esticar = 6f;
        }
        // Poeira estelar soltando dos planetas
        float[] o = new float[3];
        for (int k = 0; k < ANEIS; k++) {
            if (!e.chance(0.3f)) continue;
            anel(k, e.idade * 0.06f * (k + 1) + k, e.idade, o);
            e.emitir(c.x + o[0], c.y + h * 0.5 + o[1], c.z + o[2]).vel(0, -0.004f, 0).vida(20 + r.nextInt(10))
                    .tam(0.05f, 0f).cores(0xFFFFFFFF, 0xFFB09CFF).sprite(Sprites.NUCLEO).fisica(0f, 1f).fade(0f, 0.6f);
        }
        if (r.nextFloat() < 0.005f) e.som(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.4f, 0.6f + r.nextFloat() * 0.3f);
    }

    @Override
    public void anunciar(EstadoAura e) {
        explosao(e, 45, 0.25f, Sprites.ESTRELA, 0.18f, 20, 0f, 0xFFFFFFFF, 0xFFB09CFF, 0x004F7CFF);
        explosao(e, 12, 0.08f, Sprites.FUMACAS[0], 0.6f, 25, 0f, 0x40C070FF, 0x00406CFF);
        onda(e, 0xFFC07CFF, 3.2f, 16, Sprites.ANEL_SUAVE);
        clarao(e, 0xFFE0D0FF, 3.5f, 7);
        e.som(SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 0.7f);
        e.som(SoundEvents.ILLUSIONER_CAST_SPELL, 0.4f, 1.3f);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        float h = d.corpo.altura, t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;
        float p = pulso(t, 0.05f, 0f);

        d.chao(true, cx, cy + 0.015, cz, 1.5f, 0f, Cor.comAlfa(0x6A3CFF, 0.25f * f), Sprites.ANEL_SUAVE, luz);
        d.chao(true, cx, cy + 0.02, cz, 1.35f, t * 0.004f, Cor.comAlfa(0xFFFFFF, (0.75f + 0.2f * p) * f), Sprites.CIRCULO_ESTRELAS, luz);
        if (d.corpo.primeiraPessoa) return;
        d.brilho(cx, cy + h * 0.55, cz, 1.15f, Cor.comAlfa(0x3A2AA0, 0.1f * f), Sprites.BRILHO);

        double my = cy + h * 0.5;
        float[] o = new float[3];
        for (int k = 0; k < ANEIS; k++) {
            int n = 48;
            for (int passo = 0; passo < 2; passo++) {
                float larg = passo == 0 ? 0.1f : 0.022f;
                float alfa = passo == 0 ? 0.16f : 0.6f;
                d.fitaInicio();
                for (int i = 0; i <= n; i++) {
                    float s = i / (float) n;
                    anel(k, s * TAU, t, o);
                    int cor = Cor.hsv(cor(k, s, t), passo == 0 ? 0.7f : 0.35f, 1f);
                    d.fitaPonto(cx + o[0], my + o[1], cz + o[2], larg, Cor.comAlfa(cor, alfa * f));
                }
                d.fitaFim(true, Sprites.FEIXE, luz);
            }
            // Planeta correndo no anel, com cauda
            float a0 = t * 0.06f * (k + 1) + k;
            d.fitaInicio();
            for (int i = 0; i < 12; i++) {
                float q = i / 11f;
                anel(k, a0 - q * 0.9f, t, o);
                d.fitaPonto(cx + o[0], my + o[1], cz + o[2], 0.1f * (1f - q * 0.8f),
                        Cor.comAlfa(Cor.hsv(cor(k, 0, t), 0.5f, 1f), (1f - q) * 0.8f * f));
            }
            d.fitaFim(true, Sprites.FEIXE, luz);
            anel(k, a0, t, o);
            d.brilho(cx + o[0], my + o[1], cz + o[2], 0.32f, Cor.comAlfa(Cor.hsv(cor(k, 0, t), 0.6f, 1f), 0.6f * f), Sprites.BRILHO);
            d.brilho(cx + o[0], my + o[1], cz + o[2], 0.1f, Cor.comAlfa(0xFFFFFF, f), Sprites.NUCLEO);
        }
    }
}
