package com.auras.cliente.visual;

import com.auras.cliente.efeito.Cor;
import com.auras.cliente.efeito.Corpo;
import com.auras.cliente.efeito.EstadoAura;
import com.auras.cliente.efeito.Sprites;
import com.auras.cliente.render.Desenho;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Chamas subindo pelo corpo e num anel nos pes, duas serpentes de fogo em espiral, brasas e fumaca. */
public final class VisualFogo extends VisualAura {
    static final int[] CHAMA = {0xFFFFF6C8, 0xFFFFC23A, 0xFFFF7A14, 0xC0E0300A, 0x00801808};
    static final int[] BRASA = {0xFFFFF0A0, 0xFFFFA030, 0xFFE8400F, 0x00A01000};
    static final int[] FUMACA = {0x00403C3A, 0x2C3A3634, 0x1C302C2A, 0x00202020};

    /** Cabeca de uma das serpentes de fogo: sobe enrolando no corpo e recomeca embaixo. */
    private static float fase(int k, float t) {
        float f = t * 0.028f + k * 0.5f;
        return f - Mth.floor(f);
    }

    private static void serpente(int k, float t, float h, float[] out) {
        float f = fase(k, t);
        float ang = t * 0.2f + k * PI;
        float r = 0.72f - 0.28f * f;
        out[0] = Mth.cos(ang) * r;
        out[1] = f * (h + 0.6f) - 0.05f;
        out[2] = Mth.sin(ang) * r;
    }

    @Override
    public void tick(EstadoAura e) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        float h = c.altura;
        // Em primeira pessoa so o anel do chao: o resto ficaria na frente da camera
        boolean corpo = !c.primeiraPessoa;

        // Chamas no corpo
        for (int i = 0, n = corpo ? e.quantos(4.5f) : 0; i < n; i++) {
            double[] p = noCorpo(e, 0.22f, 0.45f, 0f, 0.62f);
            float ang = (float) p[3];
            e.emitir(c.x + p[0], c.y + p[1], c.z + p[2])
                    .vel(-Mth.cos(ang) * 0.01f + (r.nextFloat() - 0.5f) * 0.015f, 0.045f + r.nextFloat() * 0.04f,
                            -Mth.sin(ang) * 0.01f + (r.nextFloat() - 0.5f) * 0.015f)
                    .vida(13 + r.nextInt(9)).tam(0.3f + r.nextFloat() * 0.16f, 0.05f).cores(CHAMA)
                    .sprite(Sprites.CHAMAS[r.nextInt(4)]).fisica(-0.0025f, 0.94f)
                    .giro((r.nextFloat() - 0.5f) * 0.4f, (r.nextFloat() - 0.5f) * 0.04f).fade(0.15f, 0.5f);
        }
        // Anel de fogo nos pes
        for (int i = 0, n = e.quantos(4f); i < n; i++) {
            float ang = r.nextFloat() * TAU;
            float rr = 0.88f + (r.nextFloat() - 0.5f) * 0.14f;
            e.emitir(c.x + Mth.cos(ang) * rr, c.y + 0.04, c.z + Mth.sin(ang) * rr)
                    .vel((r.nextFloat() - 0.5f) * 0.01f, 0.035f + r.nextFloat() * 0.03f, (r.nextFloat() - 0.5f) * 0.01f)
                    .vida(9 + r.nextInt(7)).tam(0.26f + r.nextFloat() * 0.1f, 0.04f).cores(CHAMA)
                    .sprite(Sprites.CHAMAS[r.nextInt(4)]).fisica(-0.002f, 0.94f)
                    .giro((r.nextFloat() - 0.5f) * 0.3f, 0f).fade(0.15f, 0.5f);
        }
        // Rastro de faiscas das serpentes
        float[] s = new float[3];
        for (int k = 0; k < 2; k++) {
            if (!corpo || !e.chance(0.9f)) continue;
            serpente(k, e.idade, h, s);
            e.emitir(c.x + s[0], c.y + s[1], c.z + s[2])
                    .vel((r.nextFloat() - 0.5f) * 0.03f, 0.01f + r.nextFloat() * 0.02f, (r.nextFloat() - 0.5f) * 0.03f)
                    .vida(10 + r.nextInt(8)).tam(0.12f, 0.02f).cores(BRASA).sprite(Sprites.NUCLEO)
                    .fisica(-0.001f, 0.93f).fade(0f, 0.6f);
        }
        // Brasas que sobem dancando
        for (int i = 0, n = e.quantos(0.9f); i < n; i++) {
            double[] p = noCorpo(e, corpo ? 0.2f : 0.8f, corpo ? 0.7f : 1.1f, 0f, corpo ? 0.8f : 0.2f);
            e.emitir(c.x + p[0], c.y + p[1], c.z + p[2])
                    .vel((r.nextFloat() - 0.5f) * 0.02f, 0.03f + r.nextFloat() * 0.04f, (r.nextFloat() - 0.5f) * 0.02f)
                    .vida(30 + r.nextInt(25)).tam(0.06f + r.nextFloat() * 0.03f, 0.01f).cores(BRASA)
                    .sprite(Sprites.NUCLEO).fisica(-0.0008f, 0.97f).fade(0.05f, 0.5f)
                    .comportamento((pp, es) -> {
                        pp.vx += Mth.sin(pp.idade * 0.3f + pp.fase) * 0.004f;
                        pp.vz += Mth.cos(pp.idade * 0.27f + pp.fase) * 0.004f;
                    }).piscar = 0.5f;
        }
        // Fumaca por cima das chamas
        for (int i = 0, n = corpo ? e.quantos(0.3f) : 0; i < n; i++) {
            double[] p = noCorpo(e, 0f, 0.3f, 0.9f, 1.15f);
            e.emitir(c.x + p[0], c.y + p[1], c.z + p[2])
                    .vel((r.nextFloat() - 0.5f) * 0.01f, 0.025f + r.nextFloat() * 0.015f, (r.nextFloat() - 0.5f) * 0.01f)
                    .vida(30 + r.nextInt(15)).tam(0.3f, 0.75f).cores(FUMACA).sprite(Sprites.FUMACAS[r.nextInt(3)])
                    .translucida(false).fisica(0f, 0.97f).giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.05f)
                    .fade(0f, 0f);
        }
        if (r.nextFloat() < 0.012f) e.som(SoundEvents.FIRE_AMBIENT, 0.35f, 0.9f + r.nextFloat() * 0.2f);
    }

    @Override
    public void anunciar(EstadoAura e) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        // Coluna de fogo que sobe de uma vez
        for (int i = 0; i < 45; i++) {
            float ang = r.nextFloat() * TAU, rr = r.nextFloat() * 0.6f;
            e.emitir(c.x + Mth.cos(ang) * rr, c.y + r.nextFloat() * 0.3f, c.z + Mth.sin(ang) * rr)
                    .vel(Mth.cos(ang) * 0.04f, 0.12f + r.nextFloat() * 0.18f, Mth.sin(ang) * 0.04f)
                    .vida(14 + r.nextInt(10)).tam(0.45f + r.nextFloat() * 0.3f, 0.05f).cores(CHAMA)
                    .sprite(Sprites.CHAMAS[r.nextInt(4)]).fisica(0f, 0.92f).fade(0.05f, 0.5f);
        }
        explosao(e, 30, 0.25f, Sprites.NUCLEO, 0.1f, 20, 0.004f, BRASA);
        onda(e, 0xFFFF9A30, 3.2f, 14, Sprites.ANEL_SUAVE);
        clarao(e, 0xFFFFC060, 3.5f, 7);
        e.som(SoundEvents.BLAZE_SHOOT, 0.6f, 0.8f);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        float h = d.corpo.altura, t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;
        float tr = tremor(t, 0f);

        d.chao(true, cx, cy + 0.02, cz, 1.35f, 0f, Cor.comAlfa(0xFF6A10, (0.35f + 0.15f * tr) * f), Sprites.ANEL_SUAVE, luz);
        d.chao(true, cx, cy + 0.025, cz, 1.15f, -t * 0.01f, Cor.comAlfa(0xFFA040, (0.45f + 0.2f * tr) * f), Sprites.CIRCULO_FOGO, luz);
        d.chao(true, cx, cy + 0.03, cz, 0.95f, 0f, Cor.comAlfa(0xFF8020, 0.25f * f), Sprites.BRILHO, luz);
        if (d.corpo.primeiraPessoa) return;
        d.brilho(cx, cy + h * 0.55, cz, 1.1f, Cor.comAlfa(0xFF6A1A, (0.1f + 0.06f * tr) * f), Sprites.BRILHO);

        // Serpentes de fogo
        float[] s = new float[3];
        for (int k = 0; k < 2; k++) {
            float faseAgora = fase(k, t);
            int n = 18;
            d.fitaInicio();
            for (int i = 0; i < n; i++) {
                float tt = t - i * 0.55f;
                if (fase(k, tt) > faseAgora + 1.0e-3f) break; // nao liga o fim de uma volta ao comeco
                serpente(k, tt, h, s);
                float q = i / (n - 1f);
                int cor = Cor.gradiente(BRASA, q * 0.9f);
                d.fitaPonto(cx + s[0], cy + s[1], cz + s[2], 0.24f * (1f - q * 0.85f),
                        Cor.multAlfa(cor, (1f - q) * f * fadeSerpente(faseAgora)));
            }
            d.fitaFim(true, Sprites.FEIXE, luz);
            serpente(k, t, h, s);
            float fs = fadeSerpente(faseAgora) * f;
            d.brilho(cx + s[0], cy + s[1], cz + s[2], 0.42f, Cor.comAlfa(0xFF9A2A, 0.6f * fs), Sprites.BRILHO);
            d.brilho(cx + s[0], cy + s[1], cz + s[2], 0.13f, Cor.comAlfa(0xFFF6D0, fs), Sprites.NUCLEO);
        }
    }

    /** A serpente aparece embaixo e some la em cima. */
    private static float fadeSerpente(float fase) {
        return Mth.clamp(Math.min(fase / 0.08f, (1f - fase) / 0.15f), 0f, 1f);
    }
}
