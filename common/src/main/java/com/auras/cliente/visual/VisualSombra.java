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

/** Poca de escuridao com vortice roxo, particulas sugadas para o corpo, fumaca negra e orbes do vazio. */
public final class VisualSombra extends VisualAura {
    private static final int ROXO = 0xFFA44BFF;
    private static final int[] SUGADA = {0x00E070FF, 0xFFE070FF, 0xFFA44BFF, 0xFF5A18C0};
    private static final int[] TREVA = {0x00100418, 0xB0140820, 0x80100418, 0x00000000};
    private static final int[] ESPIRITO = {0xFFD58CFF, 0xFF8A2BE2, 0x003A0A80};

    /** Acelera ate o eixo do corpo e some quando chega. */
    private static final Particula.Comportamento SUGAR = (p, e) -> {
        double alvoY = p.c;
        double dx = -p.x, dy = alvoY - p.y, dz = -p.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.22) {
            p.matar();
            return;
        }
        double k = 0.014 / dist;
        // um pouco de giro para fazer espiral
        p.vx += dx * k - dz * 0.004 / dist;
        p.vy += dy * k;
        p.vz += dz * k + dx * 0.004 / dist;
    };

    /** Fumaca que sobe girando em volta do corpo (particula relativa). */
    private static final Particula.Comportamento GIRAR = (p, e) -> {
        double ang = 0.035;
        double x = p.x * Math.cos(ang) - p.z * Math.sin(ang);
        double z = p.x * Math.sin(ang) + p.z * Math.cos(ang);
        p.x = x;
        p.z = z;
    };

    private static void orbe(int k, float t, float h, float[] out) {
        float ang = t * 0.07f + k * PI;
        out[0] = Mth.cos(ang) * 0.9f;
        out[1] = h * 0.55f + Mth.sin(t * 0.05f + k * PI) * 0.35f;
        out[2] = Mth.sin(ang) * 0.9f;
    }

    private static void sugada(EstadoAura e, float rMin, float rMax) {
        RandomSource r = e.random;
        float h = e.corpo.altura;
        double[] p = noCorpo(e, rMin, rMax, -0.05f, 1.15f);
        Particula pt = e.emitirRel(p[0], p[1], p[2]).vida(45).tam(0.055f, 0.03f).cores(SUGADA)
                .sprite(Sprites.FAISCA).forma(Particula.Forma.ESTICADA).fisica(0f, 0.95f).fade(0.25f, 0.1f)
                .comportamento(SUGAR);
        pt.c = h * (0.35f + r.nextFloat() * 0.3f);
        pt.esticar = 3.5f;
    }

    @Override
    public void tick(EstadoAura e) {
        RandomSource r = e.random;

        for (int i = 0, n = e.quantos(2.4f); i < n; i++) sugada(e, 1.3f, 1.9f);
        // Tentaculos de fumaca negra
        for (int i = 0, n = e.quantos(0.9f); i < n; i++) {
            double[] p = noChao(e, 0.35f, 0.9f);
            e.emitirRel(p[0], r.nextFloat() * 0.3f, p[2]).vel(0, 0.02f + r.nextFloat() * 0.015f, 0)
                    .vida(30 + r.nextInt(20)).tam(0.35f, 0.85f).cores(TREVA).sprite(Sprites.FUMACAS[r.nextInt(3)])
                    .translucida(true).fisica(0f, 0.99f).giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.05f)
                    .fade(0f, 0f).comportamento(GIRAR);
        }
        // Espiritos roxos subindo em espiral
        for (int i = 0, n = e.quantos(0.45f); i < n; i++) {
            double[] p = noChao(e, 0.4f, 0.9f);
            e.emitirRel(p[0], 0.05, p[2]).vel(0, 0.03f + r.nextFloat() * 0.02f, 0).vida(28 + r.nextInt(12))
                    .tam(0.16f, 0.04f).cores(ESPIRITO).sprite(Sprites.BRILHO).fisica(0f, 0.99f).fade(0.15f, 0.5f)
                    .comportamento(GIRAR);
        }
        // Runas piscando
        for (int i = 0, n = e.quantos(0.3f); i < n; i++) {
            double[] p = noCorpo(e, 0.4f, 0.9f, 0.1f, 1f);
            e.emitirRel(p[0], p[1], p[2]).vida(8 + r.nextInt(6)).tam(0.2f, 0.05f).cores(0xFFFFFFFF, 0xFFE070FF)
                    .sprite(Sprites.ESTRELA).giro(r.nextFloat() * TAU, 0f).fade(0.2f, 0.6f).fisica(0, 1);
        }
        if (r.nextFloat() < 0.004f) e.som(SoundEvents.SOUL_ESCAPE.value(), 0.6f, 0.5f + r.nextFloat() * 0.2f);
    }

    @Override
    public void anunciar(EstadoAura e) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        for (int i = 0; i < 50; i++) sugada(e, 2.2f, 3.4f);
        // Nuvem de treva que explode para fora
        for (int i = 0; i < 22; i++) {
            float ang = r.nextFloat() * TAU, v = 0.08f + r.nextFloat() * 0.08f;
            e.emitir(c.x, c.y + 0.2 + r.nextFloat() * 1.2, c.z).vel(Mth.cos(ang) * v, 0.01f, Mth.sin(ang) * v)
                    .vida(25 + r.nextInt(10)).tam(0.5f, 1.2f).cores(TREVA).sprite(Sprites.FUMACAS[r.nextInt(3)])
                    .translucida(true).fisica(0f, 0.9f).giro(r.nextFloat() * TAU, 0.03f).fade(0f, 0f);
        }
        onda(e, 0xFF8A2BE2, 3.0f, 16, Sprites.ANEL_SUAVE);
        e.som(SoundEvents.ENDERMAN_TELEPORT, 0.6f, 0.5f);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        float h = d.corpo.altura, t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;
        float p = pulso(t, 0.07f, 0f);

        // Poca escura e o vortice girando por cima
        d.chao(false, cx, cy + 0.015, cz, 1.6f, 0f, Cor.comAlfa(0x05000A, 0.85f * f), Sprites.BRILHO, luz);
        d.chao(true, cx, cy + 0.02, cz, 1.4f, -t * 0.03f, Cor.comAlfa(0xB060FF, (0.6f + 0.25f * p) * f), Sprites.VORTICE, luz);
        d.chao(true, cx, cy + 0.025, cz, 0.85f, t * 0.05f, Cor.comAlfa(0xE090FF, 0.4f * f), Sprites.VORTICE, luz);
        d.chao(true, cx, cy + 0.02, cz, 1.55f, 0f, Cor.comAlfa(0x7A20D0, (0.2f + 0.15f * p) * f), Sprites.ANEL_SUAVE, luz);
        if (!d.corpo.primeiraPessoa) {
            d.brilho(cx, cy + h * 0.55, cz, 1.1f, Cor.comAlfa(0x7A20D0, 0.08f * f), Sprites.BRILHO);
        }

        // Orbes do vazio: miolo preto, anel roxo e rastro
        float[] o = new float[3];
        for (int k = 0; k < 2; k++) {
            int n = 14;
            d.fitaInicio();
            for (int i = 0; i < n; i++) {
                float s = i / (n - 1f);
                orbe(k, t - i * 0.6f, h, o);
                d.fitaPonto(cx + o[0], cy + o[1], cz + o[2], 0.16f * (1f - s * 0.9f),
                        Cor.comAlfa(0xA44BFF, (1f - s) * (1f - s) * 0.7f * f));
            }
            d.fitaFim(true, Sprites.FEIXE, luz);
            orbe(k, t, h, o);
            double x = cx + o[0], y = cy + o[1], z = cz + o[2];
            d.billboard(false, x, y, z, 0.15f, 0f, Cor.comAlfa(0x000000, 0.95f * f), Sprites.BRILHO, luz);
            d.brilho(x, y, z, 0.45f, Cor.comAlfa(ROXO & 0xFFFFFF, 0.4f * f), Sprites.BRILHO);
            d.billboard(true, x, y, z, 0.24f, t * 0.1f, Cor.comAlfa(0xE090FF, (0.7f + 0.3f * tremor(t, k)) * f), Sprites.ANEL, luz);
        }
    }
}
