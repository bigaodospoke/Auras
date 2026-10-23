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

import java.util.ArrayList;
import java.util.List;

/** Raios estalando em volta do corpo, orbes de plasma girando, anel eletrico nos pes e faiscas. */
public final class VisualEletrica extends VisualAura {
    private static final int CIANO = 0xFF8FF3FF;
    private static final int AZUL = 0xFF3D8BFF;
    private static final int NUCLEO = 0xFFEFFFFF;
    private static final int ORBES = 3;
    private static final int PONTOS_ANEL = 40;


    private static final class Dados {
        final List<Raio> raios = new ArrayList<>();
        final float[] anel = new float[PONTOS_ANEL];
    }

    @Override
    public Object criarDados(EstadoAura e) {
        return new Dados();
    }

    private static void orbe(int k, float t, float h, float[] out) {
        float ang = t * 0.11f + k * TAU / ORBES;
        float r = 0.85f + 0.07f * Mth.sin(t * 0.21f + k * 2f);
        out[0] = Mth.cos(ang) * r;
        out[1] = h * 0.5f + Mth.sin(t * 0.065f + k * 2.1f) * h * 0.3f;
        out[2] = Mth.sin(ang) * r;
    }

    @Override
    public void tick(EstadoAura e) {
        Dados dd = (Dados) e.dados;
        RandomSource r = e.random;
        for (int i = dd.raios.size() - 1; i >= 0; i--) {
            Raio raio = dd.raios.get(i);
            if (++raio.idade >= raio.vida) dd.raios.remove(i);
            else raio.tremer(r);
        }
        // Em primeira pessoa ficam so o chao e os raios que caem perto: o resto taparia a visao
        boolean corpo = !e.corpo.primeiraPessoa;
        if (corpo) {
            if (e.chance(0.9f)) arcoCorpo(e, dd);
            if (e.chance(0.6f)) arcoCorpo(e, dd);
            if (e.chance(0.35f)) arcoCorpo(e, dd);
            if (e.chance(0.12f)) arcoChao(e, dd);
            if (e.chance(0.15f)) arcoOrbe(e, dd);
        }
        if (e.chance(0.012f)) raioDoCeu(e, dd, false);

        if (e.idade % 2 == 0) {
            for (int i = 0; i < PONTOS_ANEL; i++) {
                dd.anel[i] = (r.nextFloat() - 0.5f) * 0.09f + (r.nextFloat() < 0.08f ? r.nextFloat() * 0.2f : 0f);
            }
        }

        Corpo c = e.corpo;
        for (int i = 0, n = e.quantos(1.4f); i < n; i++) {
            double[] p = corpo ? noCorpo(e, 0.3f, 0.5f, 0.05f, 0.95f) : noCorpo(e, 0.9f, 1.0f, 0f, 0.05f);
            faisca(e, c.x + p[0], c.y + p[1], c.z + p[2], (float) p[3], 0.06f + r.nextFloat() * 0.1f);
        }
        for (int i = 0, n = corpo ? e.quantos(0.5f) : 0; i < n; i++) {
            double[] p = noCorpo(e, 0.3f, 0.75f, 0.05f, 1.0f);
            e.emitirRel(p[0], p[1], p[2]).sprite(Sprites.ESTRELA).vida(2 + r.nextInt(3))
                    .tam(0.16f + r.nextFloat() * 0.16f, 0.04f).cores(0xFFFFFFFF, CIANO)
                    .giro(r.nextFloat() * TAU, 0f).fade(0f, 0.6f).fisica(0, 1);
        }
    }

    private static void faisca(EstadoAura e, double x, double y, double z, float ang, float vel) {
        RandomSource r = e.random;
        Particula p = e.emitir(x, y, z)
                .vel(Mth.cos(ang) * vel + (r.nextFloat() - 0.5f) * 0.05f, 0.03f + r.nextFloat() * 0.12f,
                        Mth.sin(ang) * vel + (r.nextFloat() - 0.5f) * 0.05f)
                .vida(6 + r.nextInt(8)).tam(0.045f, 0.015f).cores(0xFFFFFFFF, CIANO, 0x002A4BFF)
                .sprite(Sprites.FAISCA).forma(Particula.Forma.ESTICADA).fisica(0.035f, 0.92f).fade(0f, 0.4f);
        p.esticar = 3f;
    }

    private static Raio novo(EstadoAura e, int n, float largura, int vida, float tremor) {
        Raio raio = new Raio(n);
        raio.largura = largura;
        raio.vida = vida;
        raio.tremor = tremor;
        return raio;
    }

    /** Arco que abraca o corpo de um ponto a outro. */
    private static void arcoCorpo(EstadoAura e, Dados dd) {
        RandomSource r = e.random;
        float h = e.corpo.altura;
        float a1 = r.nextFloat() * TAU;
        float da = (0.7f + r.nextFloat() * 1.5f) * (r.nextBoolean() ? 1 : -1);
        float y1 = h * (0.05f + r.nextFloat() * 0.9f);
        float y2 = Mth.clamp(y1 + (r.nextFloat() - 0.5f) * h * 0.7f, h * 0.03f, h * 0.98f);
        float raioBase = 0.36f + r.nextFloat() * 0.12f;
        float bojo = 0.12f + r.nextFloat() * 0.22f;
        Raio raio = novo(e, 10, 0.03f + r.nextFloat() * 0.03f, 2 + r.nextInt(3), 0.12f);
        for (int i = 0; i < raio.n; i++) {
            float s = i / (raio.n - 1f);
            float ang = a1 + da * s;
            float rr = raioBase + Mth.sin(PI * s) * bojo;
            raio.bx[i] = Mth.cos(ang) * rr;
            raio.bz[i] = Mth.sin(ang) * rr;
            raio.by[i] = Mth.lerp(s, y1, y2);
        }
        raio.tremer(r);
        dd.raios.add(raio);
        if (r.nextFloat() < 0.35f) ramo(e, dd, raio, 2 + r.nextInt(raio.n - 4));
    }

    /** Galho que sai de um ponto de outro raio para fora. */
    private static void ramo(EstadoAura e, Dados dd, Raio pai, int k) {
        RandomSource r = e.random;
        float ox = pai.bx[k], oy = pai.by[k], oz = pai.bz[k];
        float l = Mth.sqrt(ox * ox + oz * oz) + 1.0e-4f;
        float dx = ox / l, dz = oz / l;
        float comp = 0.22f + r.nextFloat() * 0.25f;
        float dy = (r.nextFloat() - 0.5f) * 0.5f;
        Raio raio = novo(e, 5, pai.largura * 0.6f, pai.vida, 0.08f);
        for (int i = 0; i < raio.n; i++) {
            float s = i / (raio.n - 1f);
            raio.bx[i] = ox + dx * comp * s;
            raio.by[i] = oy + dy * comp * s;
            raio.bz[i] = oz + dz * comp * s;
        }
        raio.tremer(r);
        dd.raios.add(raio);
    }

    /** Descarga do corpo ate o chao, com faiscas onde bate. */
    private static void arcoChao(EstadoAura e, Dados dd) {
        RandomSource r = e.random;
        float h = e.corpo.altura;
        float a = r.nextFloat() * TAU;
        float a2 = a + (r.nextFloat() - 0.5f) * 0.8f;
        float r2 = 1.0f + r.nextFloat() * 0.5f;
        float sx = Mth.cos(a) * 0.32f, sy = h * (0.25f + r.nextFloat() * 0.45f), sz = Mth.sin(a) * 0.32f;
        float ex = Mth.cos(a2) * r2, ey = 0.03f, ez = Mth.sin(a2) * r2;
        Raio raio = novo(e, 11, 0.045f, 3 + r.nextInt(3), 0.18f);
        for (int i = 0; i < raio.n; i++) {
            float s = i / (raio.n - 1f);
            raio.bx[i] = Mth.lerp(s, sx, ex);
            raio.by[i] = Mth.lerp(s, sy, ey) + Mth.sin(PI * s) * 0.25f;
            raio.bz[i] = Mth.lerp(s, sz, ez);
        }
        raio.tremer(r);
        dd.raios.add(raio);
        impacto(e, ex, ez, 0.5f, 6);
    }

    private static void arcoOrbe(EstadoAura e, Dados dd) {
        RandomSource r = e.random;
        float h = e.corpo.altura;
        float[] o = new float[3];
        orbe(r.nextInt(ORBES), e.idade, h, o);
        float l = Mth.sqrt(o[0] * o[0] + o[2] * o[2]) + 1.0e-4f;
        float tx = o[0] / l * 0.33f, tz = o[2] / l * 0.33f, ty = Mth.clamp(o[1] + (r.nextFloat() - 0.5f) * 0.4f, 0.1f, h);
        Raio raio = novo(e, 8, 0.028f, 2, 0.1f);
        for (int i = 0; i < raio.n; i++) {
            float s = i / (raio.n - 1f);
            raio.bx[i] = Mth.lerp(s, o[0], tx);
            raio.by[i] = Mth.lerp(s, o[1], ty) + Mth.sin(PI * s) * 0.08f;
            raio.bz[i] = Mth.lerp(s, o[2], tz);
        }
        raio.tremer(r);
        dd.raios.add(raio);
    }

    /** Raio que cai do alto. O grande (ativacao) cai bem em cima do jogador. */
    private static void raioDoCeu(EstadoAura e, Dados dd, boolean grande) {
        RandomSource r = e.random;
        float h = e.corpo.altura;
        float a = r.nextFloat() * TAU;
        float dist = grande ? 0f : 0.45f + r.nextFloat() * 0.5f;
        float ex = Mth.cos(a) * dist, ez = Mth.sin(a) * dist;
        float sx = ex + (r.nextFloat() - 0.5f) * 0.8f, sz = ez + (r.nextFloat() - 0.5f) * 0.8f;
        float sy = h + (grande ? 4.5f : 2.2f);
        Raio raio = novo(e, 16, grande ? 0.13f : 0.07f, grande ? 7 : 4, grande ? 0.35f : 0.25f);
        for (int i = 0; i < raio.n; i++) {
            float s = i / (raio.n - 1f);
            raio.bx[i] = Mth.lerp(s, sx, ex);
            raio.by[i] = Mth.lerp(s, sy, 0.03f);
            raio.bz[i] = Mth.lerp(s, sz, ez);
        }
        raio.tremer(r);
        dd.raios.add(raio);
        ramo(e, dd, raio, 4 + r.nextInt(6));
        if (grande) ramo(e, dd, raio, 3 + r.nextInt(8));
        impacto(e, ex, ez, grande ? 1.6f : 0.9f, grande ? 26 : 12);
    }

    private static void impacto(EstadoAura e, float rx, float rz, float tam, int faiscas) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        for (int i = 0; i < faiscas; i++) {
            faisca(e, c.x + rx, c.y + 0.05, c.z + rz, r.nextFloat() * TAU, 0.05f + r.nextFloat() * 0.15f);
        }
        e.emitirRel(rx, 0.05, rz).forma(Particula.Forma.CHAO).sprite(Sprites.ANEL).tam(0.1f, tam)
                .vida(7).cores(0xFFFFFFFF, CIANO, 0x003D8BFF).fade(0f, 0.6f).fisica(0, 1);
        e.emitirRel(rx, 0.15, rz).sprite(Sprites.BRILHO).tam(tam * 0.8f, tam * 0.3f).vida(4)
                .cores(0xFFDFFBFF, 0x003D8BFF).fade(0f, 0.7f).fisica(0, 1);
    }

    @Override
    public void anunciar(EstadoAura e) {
        Dados dd = (Dados) e.dados;
        raioDoCeu(e, dd, true);
        Corpo c = e.corpo;
        for (int i = 0; i < 30; i++) {
            double[] p = noCorpo(e, 0.2f, 0.4f, 0.1f, 0.9f);
            faisca(e, c.x + p[0], c.y + p[1], c.z + p[2], (float) p[3], 0.15f + e.random.nextFloat() * 0.2f);
        }
        onda(e, CIANO, 3.2f, 14, Sprites.ANEL_SUAVE);
        clarao(e, 0xFFBFF6FF, 3.5f, 7);
        e.som(SoundEvents.TRIDENT_THUNDER.value(), 0.35f, 1.7f);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        Dados dd = (Dados) e.dados;
        float h = d.corpo.altura, t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;

        // Chao: brilho, circuito girando e o anel de eletricidade
        d.chao(true, cx, cy + 0.02, cz, 1.4f, 0f, Cor.comAlfa(0x3D7BFF, 0.3f * f), Sprites.ANEL_SUAVE, luz);
        d.chao(true, cx, cy + 0.025, cz, 1.2f, t * 0.012f,
                Cor.comAlfa(0x8FE8FF, (0.4f + 0.3f * tremor(t, 0f)) * f), Sprites.CIRCULO_RAIOS, luz);
        for (int passo = 0; passo < 2; passo++) {
            float larg = passo == 0 ? 0.17f : 0.05f;
            int cor = passo == 0 ? Cor.comAlfa(0x3D8BFF, 0.4f * f) : Cor.comAlfa(0xD8FBFF, 0.95f * f);
            d.fitaInicio();
            for (int i = 0; i <= PONTOS_ANEL; i++) {
                float a = TAU * i / PONTOS_ANEL;
                float rr = 0.95f + dd.anel[i % PONTOS_ANEL];
                d.fitaPonto(cx + Mth.cos(a) * rr, cy + 0.07, cz + Mth.sin(a) * rr, larg, cor);
            }
            d.fitaFim(true, Sprites.FEIXE, luz);
        }

        boolean corpo = !d.corpo.primeiraPessoa;
        if (corpo) {
            d.brilho(cx, cy + h * 0.55, cz, 1.1f, Cor.comAlfa(0x4FA0FF, (0.1f + 0.08f * tremor(t, 1f)) * f), Sprites.BRILHO);
        }

        // Raios: halo azul largo + nucleo branco fino
        for (Raio raio : dd.raios) {
            float a = raio.brilho * f * (1f - 0.5f * raio.idade / (float) raio.vida);
            for (int passo = 0; passo < 2; passo++) {
                float larg = passo == 0 ? raio.largura * 5f : raio.largura;
                int cor = passo == 0 ? Cor.comAlfa(0x3D8BFF, 0.5f * a) : Cor.comAlfa(0xF0FFFF, a);
                d.fitaInicio();
                for (int i = 0; i < raio.n; i++) {
                    float afina = i == raio.n - 1 ? 0.6f : 1f;
                    d.fitaPonto(cx + raio.x[i], cy + raio.y[i], cz + raio.z[i], larg * afina, cor);
                }
                d.fitaFim(true, Sprites.FEIXE, luz);
            }
        }

        if (!corpo) return;
        // Orbes de plasma com rastro
        float[] o = new float[3];
        for (int k = 0; k < ORBES; k++) {
            int n = 14;
            d.fitaInicio();
            for (int i = 0; i < n; i++) {
                float s = i / (n - 1f);
                orbe(k, t - i * 0.45f, h, o);
                float a = (1f - s);
                d.fitaPonto(cx + o[0], cy + o[1], cz + o[2], 0.14f * (1f - s * 0.9f),
                        Cor.comAlfa(Cor.misturar(CIANO, AZUL, s) & 0xFFFFFF, a * a * 0.85f * f));
            }
            d.fitaFim(true, Sprites.FEIXE, luz);
            orbe(k, t, h, o);
            float tr = tremor(t, k * 1.7f);
            double ox = cx + o[0], oy = cy + o[1], oz = cz + o[2];
            d.brilho(ox, oy, oz, 0.34f * (0.8f + 0.4f * tr), Cor.comAlfa(0x6FE0FF, 0.55f * f), Sprites.BRILHO);
            d.billboard(true, ox, oy, oz, 0.22f, t * 0.6f + k, Cor.comAlfa(0xFFFFFF, 0.7f * tr * f), Sprites.ESTRELA, luz);
            d.brilho(ox, oy, oz, 0.1f, Cor.comAlfa(NUCLEO & 0xFFFFFF, f), Sprites.NUCLEO);
        }
    }
}
