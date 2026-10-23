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

/** Cristais de gelo orbitando em dois aneis, neve caindo, nevoa fria no chao e brilhos cintilando. */
public final class VisualGelo extends VisualAura {
    private static final int GELO = 0xFFA6ECFF;
    private static final int[] NEVE = {0xFFFFFFFF, 0xF0EAF8FF};
    private static final int[] NEVOA = {0x00E0F8FF, 0x30D8F4FF, 0x1CC0EEFF, 0x00A0E0FF};
    private static final int[] CINTILA = {0xFFFFFFFF, 0xFFBFF4FF};
    private static final int CRISTAIS = 6;

    /** Posicao (relativa aos pes) e giro de um cristal. Metade gira para um lado, metade para o outro. */
    private static void cristal(int k, float t, float h, float[] out) {
        boolean baixo = k < 3;
        int i = k % 3;
        float ang = baixo ? t * 0.04f + i * TAU / 3 : -t * 0.055f + i * TAU / 3 + 0.5f;
        float r = baixo ? 0.95f : 0.75f;
        out[0] = Mth.cos(ang) * r;
        out[1] = (baixo ? 0.42f : h * 0.75f) + Mth.sin(t * 0.05f + k * 1.3f) * 0.1f;
        out[2] = Mth.sin(ang) * r;
        out[3] = ang;
    }

    @Override
    public void tick(EstadoAura e) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        float h = c.altura;

        // Neve caindo de cima da cabeca
        for (int i = 0, n = e.quantos(0.6f); i < n; i++) {
            double[] p = noChao(e, c.primeiraPessoa ? 0.6f : 0.3f, 1.2f);
            e.emitir(c.x + p[0], c.y + h + 0.2 + r.nextFloat() * 0.5, c.z + p[2])
                    .vel(0, -0.018f - r.nextFloat() * 0.012f, 0).vida(60 + r.nextInt(20))
                    .tam(0.035f + r.nextFloat() * 0.03f, -1).cores(NEVE).sprite(Sprites.FLOCO).translucida(true)
                    .fisica(0f, 1f).giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.06f).fade(0.1f, 0.25f)
                    .comportamento((pp, es) -> {
                        pp.vx = Mth.sin(pp.idade * 0.12f + pp.fase) * 0.006f;
                        if (pp.y < es.corpo.y + 0.02) pp.matar();
                    });
        }
        // Brilhos cintilando em volta
        for (int i = 0, n = e.quantos(0.6f); i < n; i++) {
            double[] p = noCorpo(e, 0.35f, 1.0f, 0f, 1.1f);
            e.emitirRel(p[0], p[1], p[2]).vida(8 + r.nextInt(7)).tam(0.24f + r.nextFloat() * 0.1f, 0f)
                    .cores(CINTILA).sprite(Sprites.ESTRELA).giro(PI / 4, 0.02f).fade(0.35f, 0.65f).fisica(0, 1);
        }
        // Nevoa rasteira: deitada no chao, se espalhando devagar
        for (int i = 0, n = e.quantos(0.25f); i < n; i++) {
            double[] p = noChao(e, 0.3f, 1.1f);
            float ang = (float) p[3];
            e.emitir(c.x + p[0], c.y + 0.04 + r.nextFloat() * 0.03, c.z + p[2])
                    .vel(Mth.cos(ang) * 0.008f, 0, Mth.sin(ang) * 0.008f).vida(45 + r.nextInt(20))
                    .tam(0.4f, 0.9f).cores(NEVOA).sprite(Sprites.FUMACAS[r.nextInt(3)]).forma(Particula.Forma.CHAO)
                    .fisica(0f, 0.99f).giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.02f).fade(0f, 0f);
        }
        // Pontinhos de geada no chao
        for (int i = 0, n = e.quantos(0.3f); i < n; i++) {
            double[] p = noChao(e, 0.3f, 1.25f);
            e.emitir(c.x + p[0], c.y + 0.05, c.z + p[2]).vida(18 + r.nextInt(10)).tam(0.05f, -1)
                    .cores(0xFFFFFFFF).sprite(Sprites.NUCLEO).fade(0.2f, 0.5f).fisica(0, 1).piscar = 0.7f;
        }
        // Rastro gelado dos cristais
        float[] o = new float[4];
        for (int k = 0; k < CRISTAIS; k++) {
            if ((c.primeiraPessoa && k >= 3) || !e.chance(0.2f)) continue;
            cristal(k, e.idade, h, o);
            e.emitir(c.x + o[0], c.y + o[1], c.z + o[2]).vel(0, -0.008f, 0).vida(16 + r.nextInt(10))
                    .tam(0.06f, 0.01f).cores(0xFFFFFFFF, GELO, 0x0060B0FF).sprite(Sprites.NUCLEO)
                    .fisica(0f, 0.98f).fade(0f, 0.6f);
        }
        if (r.nextFloat() < 0.008f) e.som(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.2f + r.nextFloat() * 0.6f);
    }

    @Override
    public void anunciar(EstadoAura e) {
        explosao(e, 26, 0.3f, Sprites.CRISTAL, 0.16f, 16, 0.02f, 0xFFFFFFFF, GELO, 0x0060C0FF);
        explosao(e, 24, 0.18f, Sprites.FLOCO, 0.1f, 22, 0.002f, NEVE[0], 0x00FFFFFF);
        onda(e, 0xFFCFF6FF, 3.0f, 16, Sprites.ANEL_SUAVE);
        clarao(e, 0xFFDFF8FF, 3.0f, 7);
        e.som(SoundEvents.AMETHYST_CLUSTER_BREAK, 1f, 0.8f);
        e.som(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        float h = d.corpo.altura, t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;
        float p = pulso(t, 0.06f, 0f);

        d.chao(true, cx, cy + 0.02, cz, 1.4f, 0f, Cor.comAlfa(0x7FD8FF, 0.25f * f), Sprites.ANEL_SUAVE, luz);
        d.chao(true, cx, cy + 0.025, cz, 1.3f, t * 0.003f, Cor.comAlfa(0xCFF4FF, (0.55f + 0.2f * p) * f), Sprites.CIRCULO_GELO, luz);
        boolean corpo = !d.corpo.primeiraPessoa;
        if (corpo) d.brilho(cx, cy + h * 0.55, cz, 1.1f, Cor.comAlfa(0x9FE4FF, 0.08f * f), Sprites.BRILHO);

        // Cristais: dois planos cruzados girando no proprio eixo, inclinados para fora
        float[] o = new float[4];
        for (int k = 0; k < CRISTAIS; k++) {
            if (!corpo && k >= 3) continue; // o anel de cima passaria na cara
            cristal(k, t, h, o);
            double x = cx + o[0], y = cy + o[1], z = cz + o[2];
            float radX = Mth.cos(o[3]), radZ = Mth.sin(o[3]);
            float incl = 0.35f;
            // eixo do cristal: quase vertical, inclinado para fora
            float ex = radX * Mth.sin(incl), ey = Mth.cos(incl), ez = radZ * Mth.sin(incl);
            float comp = 0.27f, larg = 0.12f;
            float giro = t * 0.08f + k;
            for (int plano = 0; plano < 2; plano++) {
                float g = giro + plano * PI / 2;
                // lado perpendicular ao eixo, girando em volta dele
                float px = -radZ * Mth.cos(g) + radX * Mth.cos(incl) * Mth.sin(g) * -1;
                float py = Mth.sin(incl) * Mth.sin(g);
                float pz = radX * Mth.cos(g) + radZ * Mth.cos(incl) * Mth.sin(g) * -1;
                d.plano(true, x, y, z, px * larg, py * larg, pz * larg, ex * comp, ey * comp, ez * comp,
                        Cor.comAlfa(0xB8F2FF, 0.95f * f), Sprites.CRISTAL, luz, false);
            }
            d.brilho(x, y, z, 0.42f, Cor.comAlfa(0x6FD0FF, (0.35f + 0.15f * pulso(t, 0.2f, k)) * f), Sprites.BRILHO);
            d.brilho(x, y + ey * comp * 0.6, z, 0.12f, Cor.comAlfa(0xFFFFFF, 0.6f * pulso(t, 0.3f, k * 2) * f), Sprites.ESTRELA);
        }
    }
}
