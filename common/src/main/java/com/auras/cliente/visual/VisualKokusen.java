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

/**
 * Inspirada no kokusen (raio negro): raios pretos com contorno vermelho estalando nos punhos. A cada
 * ciclo o punho carrega e vem o impacto: tentaculos de raio negro para todo lado, o chao racha em teia,
 * clarao, onda de choque e estilhacos pretos.
 */
public final class VisualKokusen extends VisualAura {
    private static final int CICLO = 60;
    private static final int CARGA = 18;
    private static final int[] FAISCA = {0xFFFFE0E4, 0xFFFF3048, 0xFFB00818, 0x00400008};
    private static final int[] ESTILHACO = {0xFF000000, 0xF0050003, 0x00000000};

    private static final class Dados {
        final List<Raio> raios = new ArrayList<>();
    }

    @Override
    public Object criarDados(EstadoAura e) {
        return new Dados();
    }

    /** Punho direito (lado 1) ou esquerdo (-1), relativo aos pes. */
    private static void punho(Corpo c, int lado, float[] out) {
        float yaw = c.yaw * Mth.DEG_TO_RAD;
        float fx = -Mth.sin(yaw), fz = Mth.cos(yaw), dx = -Mth.cos(yaw), dz = -Mth.sin(yaw);
        out[0] = dx * lado * 0.37f + fx * 0.12f;
        out[1] = c.altura * 0.4f;
        out[2] = dz * lado * 0.37f + fz * 0.12f;
    }

    /** Raio torto saindo de um ponto numa direcao, com galhos. */
    private static void tentaculo(EstadoAura e, Dados dd, float ox, float oy, float oz, float dx, float dy, float dz,
                                  float comp, float largura, int vida, float tremor) {
        RandomSource r = e.random;
        int n = 12;
        Raio raio = new Raio(n);
        float px = ox, py = oy, pz = oz;
        for (int i = 0; i < n; i++) {
            raio.bx[i] = px;
            raio.by[i] = py;
            raio.bz[i] = pz;
            // vai entortando no caminho
            dx += (r.nextFloat() - 0.5f) * 0.5f;
            dy += (r.nextFloat() - 0.5f) * 0.35f;
            dz += (r.nextFloat() - 0.5f) * 0.5f;
            float l = Mth.sqrt(dx * dx + dy * dy + dz * dz) + 1.0e-4f;
            dx /= l;
            dy /= l;
            dz /= l;
            float passo = comp / (n - 1);
            px += dx * passo;
            py = Math.max(py + dy * passo, 0.02f);
            pz += dz * passo;
        }
        raio.largura = largura;
        raio.vida = vida;
        raio.tremor = tremor;
        raio.tremer(r);
        dd.raios.add(raio);
        // galho
        if (comp > 0.6f && r.nextFloat() < 0.7f) {
            int k = 3 + r.nextInt(n - 6);
            float gx = raio.bx[k + 1] - raio.bx[k], gz = raio.bz[k + 1] - raio.bz[k];
            float l = Mth.sqrt(gx * gx + gz * gz) + 1.0e-4f;
            float lado = r.nextBoolean() ? 1 : -1;
            tentaculo(e, dd, raio.bx[k], raio.by[k], raio.bz[k], (gx / l) * 0.6f - (gz / l) * lado * 0.8f,
                    (r.nextFloat() - 0.4f) * 0.6f, (gz / l) * 0.6f + (gx / l) * lado * 0.8f,
                    comp * 0.4f, largura * 0.6f, vida, tremor * 0.8f);
        }
    }

    @Override
    public void tick(EstadoAura e) {
        Dados dd = (Dados) e.dados;
        Corpo c = e.corpo;
        RandomSource r = e.random;
        for (int i = dd.raios.size() - 1; i >= 0; i--) {
            Raio raio = dd.raios.get(i);
            if (++raio.idade >= raio.vida) dd.raios.remove(i);
            else raio.tremer(r);
        }
        float[] p = new float[3];
        int fase = e.idade % CICLO;
        boolean carregando = fase >= CICLO - CARGA;

        // Estalos nos punhos (mais fortes enquanto carrega o direito)
        for (int lado = -1; lado <= 1; lado += 2) {
            float chance = lado == 1 && carregando ? 0.9f : 0.3f;
            if (!e.chance(chance)) continue;
            punho(c, lado, p);
            float a = r.nextFloat() * TAU, a2 = a + e.aleatorio(1.2f, 2.6f);
            float rr = lado == 1 && carregando ? 0.3f : 0.2f;
            Raio raio = Raio.reto(6, p[0] + Mth.cos(a) * rr, p[1] + (r.nextFloat() - 0.5f) * 0.3f, p[2] + Mth.sin(a) * rr,
                    p[0] + Mth.cos(a2) * rr, p[1] + (r.nextFloat() - 0.5f) * 0.3f, p[2] + Mth.sin(a2) * rr, 0.06f);
            raio.largura = 0.03f;
            raio.vida = 2 + r.nextInt(2);
            raio.tremor = 0.09f;
            raio.tremer(r);
            dd.raios.add(raio);
        }
        // Faiscas vermelhas e estilhacos pretos soltando dos punhos
        for (int i = 0, n = e.quantos(0.8f); i < n; i++) {
            punho(c, r.nextBoolean() ? 1 : -1, p);
            faisca(e, c.x + p[0], c.y + p[1], c.z + p[2], 0.05f + r.nextFloat() * 0.06f, r.nextFloat() < 0.35f);
        }
        // Carga: faiscas sendo puxadas para o punho direito
        if (carregando) {
            punho(c, 1, p);
            for (int i = 0; i < 2; i++) {
                double[] o = noCorpo(e, 0.9f, 1.4f, 0.1f, 0.9f);
                Particula pt = e.emitirRel(o[0], o[1], o[2]).vida(12).tam(0.05f, 0.02f).cores(FAISCA)
                        .sprite(Sprites.FAISCA).forma(Particula.Forma.ESTICADA).fisica(0f, 1f).fade(0.2f, 0.1f);
                pt.vel((p[0] - o[0]) / 11, (p[1] - o[1]) / 11, (p[2] - o[2]) / 11);
                pt.esticar = 2.5f;
            }
        }
        if (fase == 0 && e.idade > 0) impacto(e, dd);
    }

    private static void faisca(EstadoAura e, double x, double y, double z, float vel, boolean preta) {
        RandomSource r = e.random;
        float a = r.nextFloat() * TAU, sobe = e.aleatorio(-0.4f, 1f);
        Particula pt = e.emitir(x, y, z)
                .vel(Mth.cos(a) * vel, sobe * vel + 0.02f, Mth.sin(a) * vel).vida(8 + r.nextInt(8))
                .tam(preta ? 0.05f : 0.04f, 0.01f).sprite(Sprites.FAISCA).forma(Particula.Forma.ESTICADA)
                .fisica(0.02f, 0.9f).fade(0f, 0.4f);
        if (preta) pt.cores(ESTILHACO).escura();
        else pt.cores(FAISCA);
        pt.esticar = 3f;
    }

    private static void impacto(EstadoAura e, Dados dd) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        float[] p = new float[3];
        punho(c, 1, p);
        float yaw = c.yaw * Mth.DEG_TO_RAD;
        float fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        float ix = p[0] + fx * 0.45f, iy = p[1] + 0.05f, iz = p[2] + fz * 0.45f;

        // Tentaculos de raio negro saindo do ponto do soco
        int n = 16;
        for (int i = 0; i < n; i++) {
            float a = i * TAU / n + (r.nextFloat() - 0.5f) * 0.4f;
            float dy = e.aleatorio(-0.4f, 0.75f);
            tentaculo(e, dd, ix, iy, iz, Mth.cos(a), dy, Mth.sin(a), e.aleatorio(1.4f, 2.8f),
                    e.aleatorio(0.06f, 0.09f), 7 + r.nextInt(4), 0.16f);
        }
        // Clarao e onda de choque
        e.emitirRel(ix, iy, iz).sprite(Sprites.BRILHO).tam(2.4f, 3.2f).vida(5).cores(0xFFFF2038, 0x00600008)
                .fade(0f, 0.8f).fisica(0, 1);
        e.emitirRel(ix, iy, iz).sprite(Sprites.NUCLEO).tam(0.9f, 0.3f).vida(3).cores(0xFFFFFFFF, 0x00FF4060)
                .fade(0f, 0.7f).fisica(0, 1);
        e.emitirRel(ix, iy, iz).sprite(Sprites.ANEL).tam(0.2f, 2.4f).vida(7).cores(0xFFFFD0D8, 0xFFFF2040, 0x00800010)
                .fade(0f, 0.6f).fisica(0, 1);
        e.emitirRel(0, 0.05, 0).forma(Particula.Forma.CHAO).sprite(Sprites.ANEL_SUAVE).tam(0.3f, 3.6f).vida(11)
                .cores(0xFFFF2040, 0x80800010, 0x00400008).fade(0f, 0.5f).fisica(0, 1);
        // Chao rachado em teia (fica um pouco e some)
        e.emitirRel(ix * 0.6, 0.03, iz * 0.6).forma(Particula.Forma.CHAO).sprite(Sprites.BRILHO).tam(1.9f, -1)
                .vida(46).cores(0xA0050002, 0x00000000).translucida(true).fade(0f, 0.5f).fisica(0, 1);
        Particula teia = e.emitirRel(ix * 0.6, 0.035, iz * 0.6).forma(Particula.Forma.CHAO).sprite(Sprites.RACHADURA)
                .tam(1.6f, 1.8f).vida(46).cores(0xFFFF4058, 0xFFFF1030, 0x00600008).fade(0f, 0.55f).fisica(0, 1);
        teia.rot = teia.rotO = r.nextFloat() * TAU;
        // Estilhacos pretos e faiscas vermelhas
        for (int i = 0; i < 26; i++) {
            faisca(e, c.x + ix, c.y + iy, c.z + iz, 0.15f + r.nextFloat() * 0.25f, i % 2 == 0);
        }
        e.som(SoundEvents.TRIDENT_THUNDER.value(), 0.3f, 0.55f);
        e.som(SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 0.6f);
    }

    @Override
    public void anunciar(EstadoAura e) {
        impacto(e, (Dados) e.dados);
        clarao(e, 0xFFFF2040, 3.2f, 6);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        Dados dd = (Dados) e.dados;
        Corpo c = d.corpo;
        float t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;

        // Sombra e brilho vermelho fraco no chao
        d.chao(false, cx, cy + 0.015, cz, 1.4f, 0f, Cor.comAlfa(0x050002, 0.45f * f), Sprites.BRILHO, luz);
        d.chao(true, cx, cy + 0.02, cz, 1.35f, 0f, Cor.comAlfa(0xC00818, (0.16f + 0.08f * tremor(t, 0)) * f), Sprites.ANEL_SUAVE, luz);

        // Punhos: brilho vermelho; o direito vira uma esfera negra enquanto carrega
        float[] p = new float[3];
        int fase = e.idade % CICLO;
        float carga = fase >= CICLO - CARGA ? (fase - (CICLO - CARGA) + d.parcial) / CARGA : 0f;
        for (int lado = -1; lado <= 1; lado += 2) {
            punho(c, lado, p);
            double x = cx + p[0], y = cy + p[1], z = cz + p[2];
            float tr = tremor(t, lado);
            d.brilho(x, y, z, 0.32f + (lado == 1 ? carga * 0.4f : 0f), Cor.comAlfa(0xFF1030, (0.3f + 0.25f * tr) * f), Sprites.BRILHO);
            if (lado == 1 && carga > 0) {
                d.billboard(true, x, y, z, 0.16f + carga * 0.22f, t * 0.4f, Cor.comAlfa(0xFF4060, 0.9f * f), Sprites.ANEL, luz);
                d.porCima = true;
                d.billboard(false, x, y, z, 0.07f + carga * 0.12f, 0f, Cor.comAlfa(0x000000, 0.95f * f), Sprites.BRILHO, luz);
                d.porCima = false;
            }
        }

        // Raios: halo vermelho largo, nucleo vermelho e por cima o fio preto
        for (Raio raio : dd.raios) {
            float a = raio.brilho * f * (1f - 0.4f * raio.idade / (float) raio.vida);
            for (int passo = 0; passo < 3; passo++) {
                float larg = passo == 0 ? raio.largura * 7f : passo == 1 ? raio.largura * 2.8f : raio.largura * 1.4f;
                int cor = passo == 0 ? Cor.comAlfa(0xE00820, 0.45f * a) : passo == 1 ? Cor.comAlfa(0xFF3A52, 0.95f * a)
                        : Cor.comAlfa(0x000000, Math.min(1f, a * 1.1f));
                d.porCima = passo == 2;
                d.fitaInicio();
                for (int i = 0; i < raio.n; i++) {
                    float afina = 1f - 0.6f * i / (raio.n - 1f);
                    d.fitaPonto(cx + raio.x[i], cy + raio.y[i], cz + raio.z[i], larg * afina, cor);
                }
                d.fitaFim(passo < 2, Sprites.FEIXE, luz);
            }
            d.porCima = false;
        }
    }
}
