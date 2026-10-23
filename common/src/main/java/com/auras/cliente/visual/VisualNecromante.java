package com.auras.cliente.visual;

import com.auras.cliente.efeito.Cor;
import com.auras.cliente.efeito.Corpo;
import com.auras.cliente.efeito.EstadoAura;
import com.auras.cliente.efeito.Particula;
import com.auras.cliente.efeito.Sprites;
import com.auras.cliente.render.Desenho;
import com.auras.cliente.render.Fantasmas;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Necromante: ilusoes de mobs (zumbi, esqueleto, wither skeleton, stray) sobem do chao em volta,
 * ficam um tempo e se desfazem em almas. Cranios fantasmas orbitam, fogo de alma no circulo,
 * nevoa escura e o pentagrama no chao.
 */
public final class VisualNecromante extends VisualAura {
    private static final int MAX_ILUSOES = 3;
    private static final int VIDA_ILUSAO = 120;
    private static final int SUBIR = 18;
    private static final int SUMIR = 25;
    private static final int[] ALMA = {0xFFD8FFF0, 0xFF7CFFD0, 0xFF20C090, 0x00106040};
    private static final int[] FOGO_ALMA = {0xFFE8FFF8, 0xFF7CFFE0, 0xFF20C0A0, 0x00106050};
    private static final int[] TREVA = {0x00020805, 0x66041810, 0x3A031008, 0x00000000};

    private static final class Ilusao {
        int tipo;
        /** Angulo relativo as costas do dono. */
        float ang, raio;
        int idade;
    }

    private static final class Dados {
        final List<Ilusao> ilusoes = new ArrayList<>();
    }

    @Override
    public Object criarDados(EstadoAura e) {
        return new Dados();
    }

    /** Gira a particula relativa em volta do corpo enquanto sobe. */
    private static final Particula.Comportamento GIRAR = (p, e) -> {
        double a = 0.05;
        double x = p.x * Math.cos(a) - p.z * Math.sin(a);
        double z = p.x * Math.sin(a) + p.z * Math.cos(a);
        p.x = x;
        p.z = z;
    };

    /** Angulo no mundo de uma ilusao: rel = 0 e bem atras do dono, positivo/negativo vai para os lados. */
    private static float angMundo(float yawRad, float rel) {
        return yawRad - PI / 2 + rel;
    }

    private static void invocar(EstadoAura e, Dados dd, float rel) {
        RandomSource r = e.random;
        Corpo c = e.corpo;
        Ilusao il = new Ilusao();
        il.tipo = r.nextInt(Fantasmas.TIPOS);
        il.ang = rel;
        il.raio = c.gui ? 1.2f : e.aleatorio(1.7f, 2.1f);
        dd.ilusoes.add(il);
        float ang = angMundo(c.yaw * Mth.DEG_TO_RAD, rel);
        double x = Mth.cos(ang) * il.raio, z = Mth.sin(ang) * il.raio;
        e.emitirRel(x, 0.04, z).forma(Particula.Forma.CHAO).sprite(Sprites.ANEL).tam(0.1f, 1.1f).vida(14)
                .cores(0xFFB0FFE0, 0xFF30E0A0, 0x0010A060).fade(0f, 0.5f).fisica(0, 1);
        for (int i = 0; i < 8; i++) {
            float a = r.nextFloat() * TAU, v = 0.03f + r.nextFloat() * 0.03f;
            e.emitirRel(x, 0.1, z).vel(Mth.cos(a) * v, 0.01f, Mth.sin(a) * v).vida(25 + r.nextInt(10)).tam(0.35f, 0.7f)
                    .cores(TREVA).sprite(Sprites.FUMACAS[r.nextInt(3)]).translucida(true).fisica(0f, 0.92f)
                    .giro(r.nextFloat() * TAU, 0.03f).fade(0f, 0f);
        }
        SoundEvent som = switch (il.tipo) {
            case Fantasmas.ZUMBI -> SoundEvents.ZOMBIE_AMBIENT;
            case Fantasmas.WITHER -> SoundEvents.WITHER_SKELETON_AMBIENT;
            case Fantasmas.STRAY -> SoundEvents.STRAY_AMBIENT;
            default -> SoundEvents.SKELETON_AMBIENT;
        };
        e.som(som, 0.25f, 0.55f);
    }

    private static float angLivre(EstadoAura e, Dados dd) {
        // Atras e dos lados do dono, como um exercito que o segue
        float melhor = e.aleatorio(-1.9f, 1.9f);
        for (int tentativa = 0; tentativa < 8; tentativa++) {
            float a = e.aleatorio(-1.9f, 1.9f);
            boolean longe = true;
            for (Ilusao il : dd.ilusoes) {
                if (Math.abs(a - il.ang) < 1.1f) longe = false;
            }
            if (longe) return a;
            melhor = a;
        }
        return melhor;
    }

    private static void cranio(int k, float t, float h, float[] out) {
        float ang = t * 0.05f + k * TAU / 3;
        out[0] = Mth.cos(ang) * 0.85f;
        out[1] = h * 0.85f + Mth.sin(t * 0.08f + k * 2f) * 0.12f;
        out[2] = Mth.sin(ang) * 0.85f;
        out[3] = ang;
    }

    @Override
    public void tick(EstadoAura e) {
        Dados dd = (Dados) e.dados;
        Corpo c = e.corpo;
        RandomSource r = e.random;
        float h = c.altura;

        // Ilusoes envelhecem; no fim se desfazem em almas
        for (int i = dd.ilusoes.size() - 1; i >= 0; i--) {
            Ilusao il = dd.ilusoes.get(i);
            il.idade++;
            float ang = angMundo(c.yaw * Mth.DEG_TO_RAD, il.ang);
            double x = Mth.cos(ang) * il.raio, z = Mth.sin(ang) * il.raio;
            if (il.idade > VIDA_ILUSAO - SUMIR && e.chance(0.9f)) {
                for (int k = 0; k < 2; k++) {
                    e.emitirRel(x + (r.nextFloat() - 0.5f) * 0.5f, r.nextFloat() * 1.9f, z + (r.nextFloat() - 0.5f) * 0.5f)
                            .vel(0, 0.03f + r.nextFloat() * 0.03f, 0).vida(20 + r.nextInt(10)).tam(0.08f, 0.02f)
                            .cores(ALMA).sprite(Sprites.NUCLEO).fisica(-0.001f, 0.98f).fade(0f, 0.5f);
                }
            } else if (e.chance(0.12f)) {
                e.emitirRel(x + (r.nextFloat() - 0.5f) * 0.4f, r.nextFloat() * 1.8f, z + (r.nextFloat() - 0.5f) * 0.4f)
                        .vel(0, 0.015f, 0).vida(20).tam(0.06f, 0.01f).cores(ALMA).sprite(Sprites.NUCLEO)
                        .fisica(0f, 0.98f).fade(0.1f, 0.5f);
            }
            if (il.idade >= VIDA_ILUSAO) dd.ilusoes.remove(i);
        }
        if (e.idade % 40 == 20 && dd.ilusoes.size() < MAX_ILUSOES) invocar(e, dd, angLivre(e, dd));

        // Almas subindo em espiral
        for (int i = 0, n = e.quantos(0.8f); i < n; i++) {
            double[] p = noChao(e, c.primeiraPessoa ? 0.8f : 0.4f, 1.2f);
            e.emitirRel(p[0], 0.05, p[2]).vel(0, 0.025f + r.nextFloat() * 0.02f, 0).vida(35 + r.nextInt(15))
                    .tam(0.09f, 0.02f).cores(ALMA).sprite(Sprites.NUCLEO).fisica(0f, 0.99f).fade(0.1f, 0.5f)
                    .comportamento(GIRAR);
        }
        // Fogo de alma no circulo
        for (int i = 0, n = e.quantos(1.6f); i < n; i++) {
            float a = r.nextFloat() * TAU, rr = 1.22f + (r.nextFloat() - 0.5f) * 0.08f;
            e.emitir(c.x + Mth.cos(a) * rr, c.y + 0.04, c.z + Mth.sin(a) * rr)
                    .vel(0, 0.03f + r.nextFloat() * 0.025f, 0).vida(9 + r.nextInt(6)).tam(0.2f, 0.03f)
                    .cores(FOGO_ALMA).sprite(Sprites.CHAMAS[r.nextInt(4)]).fisica(-0.002f, 0.94f)
                    .giro((r.nextFloat() - 0.5f) * 0.3f, 0f).fade(0.15f, 0.5f);
        }
        // Nevoa escura no chao
        for (int i = 0, n = e.quantos(0.35f); i < n; i++) {
            double[] p = noChao(e, 0.2f, 1.25f);
            float ang = (float) p[3];
            e.emitir(c.x + p[0], c.y + 0.04 + r.nextFloat() * 0.03, c.z + p[2])
                    .vel(Mth.cos(ang) * 0.006f, 0, Mth.sin(ang) * 0.006f).vida(45 + r.nextInt(20)).tam(0.45f, 1.0f)
                    .cores(TREVA).sprite(Sprites.FUMACAS[r.nextInt(3)]).forma(Particula.Forma.CHAO).translucida(true)
                    .fisica(0f, 0.99f).giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.02f).fade(0f, 0f);
        }
        // Chama verde atras dos cranios
        float[] o = new float[4];
        for (int k = 0; !c.primeiraPessoa && k < 3; k++) {
            if (!e.chance(0.6f)) continue;
            cranio(k, e.idade, h, o);
            e.emitir(c.x + o[0], c.y + o[1] + 0.08, c.z + o[2]).vel(0, 0.02f, 0).vida(10 + r.nextInt(5))
                    .tam(0.16f, 0.03f).cores(FOGO_ALMA).sprite(Sprites.CHAMAS[r.nextInt(4)]).fisica(-0.002f, 0.92f)
                    .fade(0.1f, 0.5f);
        }
        if (r.nextFloat() < 0.004f) e.som(SoundEvents.SOUL_ESCAPE.value(), 0.5f, 0.6f);
    }

    @Override
    public void anunciar(EstadoAura e) {
        Dados dd = (Dados) e.dados;
        for (int k = 0; k < MAX_ILUSOES; k++) invocar(e, dd, (k - 1) * 1.5f);
        explosao(e, 30, 0.16f, Sprites.NUCLEO, 0.1f, 25, -0.002f, ALMA);
        onda(e, 0xFF40FFA0, 3.2f, 16, Sprites.ANEL_SUAVE);
        clarao(e, 0xFF80FFC8, 3f, 7);
        e.som(SoundEvents.EVOKER_PREPARE_SUMMON, 0.7f, 0.8f);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        Dados dd = (Dados) e.dados;
        Corpo c = d.corpo;
        float h = c.altura, t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;
        float p = pulso(t, 0.06f, 0f);

        d.chao(false, cx, cy + 0.012, cz, 1.55f, 0f, Cor.comAlfa(0x010603, 0.55f * f), Sprites.BRILHO, luz);
        d.chao(true, cx, cy + 0.02, cz, 1.45f, 0f, Cor.comAlfa(0x20C080, (0.18f + 0.08f * p) * f), Sprites.ANEL_SUAVE, luz);
        d.chao(true, cx, cy + 0.025, cz, 1.3f, -t * 0.004f, Cor.comAlfa(0x60FFA0, (0.65f + 0.2f * p) * f), Sprites.CIRCULO_NECRO, luz);

        // Ilusoes: sobem do chao, ficam balancando e somem
        for (Ilusao il : dd.ilusoes) {
            float idade = il.idade + d.parcial;
            float sobe = Mth.clamp(idade / SUBIR, 0f, 1f);
            sobe = 1f - (1f - sobe) * (1f - sobe);
            float some = Mth.clamp((idade - (VIDA_ILUSAO - SUMIR)) / SUMIR, 0f, 1f);
            float alfa = Mth.clamp(idade / 8f, 0f, 1f) * (1f - some) * f;
            if (alfa <= 0.01f) continue;
            float ang = angMundo(d.yawCorpo, il.ang);
            double x = cx + Mth.cos(ang) * il.raio, z = cz + Mth.sin(ang) * il.raio;
            double y = cy + (c.gui ? 0 : -2.1 * (1f - sobe)) + some * 0.5 + Mth.sin(t * 0.1f + il.ang) * 0.03 * sobe;
            int corpoCor = Cor.comAlfa(0xB8FFE0, 0.38f * alfa);
            int brilho = rgbVezes(0x30E080, 0.4f * alfa);
            // olham para onde o dono olha, um pouco abertos para os lados
            float yaw = d.yawCorpo * Mth.RAD_TO_DEG + il.ang * 12f + Mth.sin(t * 0.04f + il.ang) * 6f;
            Fantasmas.mob(d, il.tipo, x, y, z, yaw, 1f + some * 0.08f,
                    corpoCor, brilho, t);
        }

        if (c.primeiraPessoa) return;
        // Cranios fantasmas olhando para onde vao
        float[] o = new float[4];
        for (int k = 0; k < 3; k++) {
            cranio(k, t, h, o);
            double x = cx + o[0], y = cy + o[1], z = cz + o[2];
            d.brilho(x, y, z, 0.45f, Cor.comAlfa(0x40FFA0, 0.22f * f), Sprites.BRILHO);
            Fantasmas.cranio(d, k == 1, x, y, z, -o[3] * Mth.RAD_TO_DEG, Mth.sin(t * 0.07f + k) * 10f, 0.5f,
                    Cor.comAlfa(0xB0FFD8, 0.7f * f), rgbVezes(0x40FF90, 0.8f * f), t);
        }
        d.brilho(cx, cy + h * 0.55, cz, 1.1f, Cor.comAlfa(0x20C080, 0.06f * f), Sprites.BRILHO);
    }

    /** O espiral e somado (sem alfa), entao a transparencia vai na propria cor. */
    private static int rgbVezes(int rgb, float k) {
        int r = (int) ((rgb >> 16 & 255) * k), g = (int) ((rgb >> 8 & 255) * k), b = (int) ((rgb & 255) * k);
        return Mth.clamp(r, 0, 255) << 16 | Mth.clamp(g, 0, 255) << 8 | Mth.clamp(b, 0, 255);
    }
}
