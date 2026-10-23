package com.auras.cliente.visual;

import com.auras.cliente.efeito.Cor;
import com.auras.cliente.efeito.Corpo;
import com.auras.cliente.efeito.EstadoAura;
import com.auras.cliente.efeito.Sprites;
import com.auras.cliente.render.Desenho;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** Aureola dourada, asas de luz nas costas, feixes de luz subindo, penas e circulo sagrado. */
public final class VisualDivina extends VisualAura {
    private static final int[] CENTELHA = {0xFFFFF6D0, 0xFFFFD25A, 0x00FFA020};
    private static final int[] PENA = {0xFFFFFFFF, 0xF0FFF4D0};

    /** Feixe vertical de luz: angulo, raio, idade, vida. */
    private static final class Feixe {
        float ang, raio;
        int idade, vida;
    }

    private static final class Dados {
        final List<Feixe> feixes = new ArrayList<>();
    }

    @Override
    public Object criarDados(EstadoAura e) {
        return new Dados();
    }

    private static void feixe(EstadoAura e, Dados dd, float raioMin, float raioMax) {
        Feixe fx = new Feixe();
        fx.ang = e.random.nextFloat() * TAU;
        fx.raio = e.aleatorio(raioMin, raioMax);
        fx.vida = 18 + e.random.nextInt(14);
        dd.feixes.add(fx);
    }

    @Override
    public void tick(EstadoAura e) {
        Dados dd = (Dados) e.dados;
        Corpo c = e.corpo;
        RandomSource r = e.random;
        float h = c.altura;

        dd.feixes.removeIf(fx -> ++fx.idade >= fx.vida);
        if (e.chance(0.09f)) feixe(e, dd, 0.55f, 1.15f);

        // Centelhas douradas subindo
        for (int i = 0, n = e.quantos(1.0f); i < n; i++) {
            double[] p = noCorpo(e, 0.3f, 0.75f, 0f, 1f);
            e.emitir(c.x + p[0], c.y + p[1], c.z + p[2]).vel(0, 0.015f + r.nextFloat() * 0.015f, 0)
                    .vida(30 + r.nextInt(16)).tam(0.13f + r.nextFloat() * 0.07f, 0f).cores(CENTELHA)
                    .sprite(r.nextFloat() < 0.5f ? Sprites.ESTRELA : Sprites.NUCLEO).fisica(0f, 0.99f)
                    .giro(PI / 4, (r.nextFloat() - 0.5f) * 0.05f).fade(0.15f, 0.5f).piscar = 0.4f;
        }
        // Penas caindo das asas
        if (!c.primeiraPessoa && e.chance(0.07f)) {
            float yaw = c.yaw * Mth.DEG_TO_RAD;
            float lado = r.nextBoolean() ? 1 : -1;
            float abre = e.aleatorio(0.2f, 0.9f);
            double bx = Mth.sin(yaw) * 0.35 - Mth.cos(yaw) * lado * abre;
            double bz = -Mth.cos(yaw) * 0.35 - Mth.sin(yaw) * lado * abre;
            e.emitir(c.x + bx, c.y + h * e.aleatorio(0.45f, 0.95f), c.z + bz).vel(0, -0.012f, 0)
                    .vida(70 + r.nextInt(20)).tam(0.12f, -1).cores(PENA).sprite(Sprites.PENA).translucida(true)
                    .fisica(0f, 1f).giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.08f).fade(0.1f, 0.3f)
                    .comportamento((pp, es) -> {
                        pp.vx = Mth.sin(pp.idade * 0.09f + pp.fase) * 0.012f;
                        if (pp.y < es.corpo.y + 0.03) pp.matar();
                    });
        }
        if (r.nextFloat() < 0.004f) e.som(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 1.5f + r.nextFloat() * 0.3f);
    }

    @Override
    public void anunciar(EstadoAura e) {
        Dados dd = (Dados) e.dados;
        for (int i = 0; i < 7; i++) feixe(e, dd, 0.3f, 1.4f);
        explosao(e, 40, 0.22f, Sprites.ESTRELA, 0.16f, 22, -0.002f, CENTELHA);
        onda(e, 0xFFFFE08A, 3.2f, 16, Sprites.ANEL_SUAVE);
        clarao(e, 0xFFFFF4C0, 4f, 8);
        e.som(SoundEvents.BEACON_ACTIVATE, 0.7f, 1.4f);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        Dados dd = (Dados) e.dados;
        float h = d.corpo.altura, t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;
        float p = pulso(t, 0.06f, 0f);

        // Circulo sagrado (dois girando ao contrario) e luz quente no chao
        d.chao(true, cx, cy + 0.015, cz, 1.5f, 0f, Cor.comAlfa(0xFFC860, 0.25f * f), Sprites.ANEL_SUAVE, luz);
        d.chao(true, cx, cy + 0.02, cz, 1.3f, t * 0.006f, Cor.comAlfa(0xFFE08A, (0.6f + 0.2f * p) * f), Sprites.CIRCULO_RUNAS, luz);
        d.chao(true, cx, cy + 0.025, cz, 0.72f, -t * 0.012f, Cor.comAlfa(0xFFF0B0, 0.45f * f), Sprites.CIRCULO_RUNAS, luz);
        d.chao(true, cx, cy + 0.03, cz, 1.0f, 0f, Cor.comAlfa(0xFFE8A0, 0.15f * f), Sprites.BRILHO, luz);

        // Feixes de luz subindo do chao
        for (Feixe fx : dd.feixes) {
            float s = (fx.idade + d.parcial) / fx.vida;
            float a = Mth.sin(Mth.clamp(s, 0f, 1f) * PI) * f;
            double x = cx + Mth.cos(fx.ang) * fx.raio, z = cz + Mth.sin(fx.ang) * fx.raio;
            float alto = h + 1.4f;
            d.fitaInicio();
            for (int i = 0; i <= 6; i++) {
                float q = i / 6f;
                d.fitaPonto(x, cy + q * alto, z, 0.16f * (1f - q * 0.4f), Cor.comAlfa(0xFFF0B0, a * 0.55f * (1f - q)));
            }
            d.fitaFim(true, Sprites.FEIXE, luz);
        }

        if (d.corpo.primeiraPessoa) return;
        d.brilho(cx, cy + h * 0.55, cz, 1.1f, Cor.comAlfa(0xFFE8A0, (0.07f + 0.03f * p) * f), Sprites.BRILHO);

        // Aureola: anel que sempre aparece (fita) com brilho
        float yHalo = h + 0.2f + Mth.sin(t * 0.08f) * 0.025f;
        for (int passo = 0; passo < 2; passo++) {
            float larg = passo == 0 ? 0.15f : 0.045f;
            int cor = passo == 0 ? Cor.comAlfa(0xFFC040, 0.45f * f) : Cor.comAlfa(0xFFFBE0, 0.95f * f);
            anelFita(d, cy + yHalo, 0.27f, 28, larg, cor, Sprites.FEIXE);
        }

        // Asas: saem dos ombros, batem devagar
        float yaw = d.yawCorpo;
        float fwdX = -Mth.sin(yaw), fwdZ = Mth.cos(yaw);
        float dirX = -Mth.cos(yaw), dirZ = -Mth.sin(yaw);
        double bx = cx - fwdX * 0.22, by = cy + h * 0.78, bz = cz - fwdZ * 0.22;
        float batida = Mth.sin(t * 0.09f);
        float bate = 0.45f + batida * 0.17f;
        float ergue = 0.32f + Mth.sin(t * 0.09f + 0.7f) * 0.08f;
        float comp = 1.5f, alt = 1.6f;
        for (int lado = -1; lado <= 1; lado += 2) {
            // para fora e para tras, levantada um pouco
            float hx = dirX * lado * Mth.cos(bate) - fwdX * Mth.sin(bate);
            float hz = dirZ * lado * Mth.cos(bate) - fwdZ * Mth.sin(bate);
            float ox = hx * Mth.cos(ergue), oy = Mth.sin(ergue), oz = hz * Mth.cos(ergue);
            // eixo "cima" do plano da asa, perpendicular a direcao dela
            float vx = -ox * oy, vy = 1 - oy * oy, vz = -oz * oy;
            float vl = Mth.sqrt(vx * vx + vy * vy + vz * vz);
            vx /= vl;
            vy /= vl;
            vz /= vl;
            float ax = ox * comp * 0.5f, ay = oy * comp * 0.5f, az = oz * comp * 0.5f;
            // o ombro da textura fica a 39% do topo: centraliza o quadrado para ele cair no ombro
            double wx = bx + ax - vx * alt * 0.11, wy = by + ay - vy * alt * 0.11, wz = bz + az - vz * alt * 0.11;
            for (int camada = 0; camada < 2; camada++) {
                float s = camada == 0 ? 1.07f : 1f;
                int cor = camada == 0 ? Cor.comAlfa(0xFFC050, 0.35f * f) : Cor.comAlfa(0xFFF6DC, (0.85f + 0.1f * p) * f);
                d.plano(true, wx, wy, wz, ax * s, ay * s, az * s, -vx * alt * 0.5f * s, -vy * alt * 0.5f * s,
                        -vz * alt * 0.5f * s, cor, Sprites.ASA, luz, false);
            }
        }
    }
}
