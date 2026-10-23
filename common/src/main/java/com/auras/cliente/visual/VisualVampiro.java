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

/**
 * Lorde vampiro: enxame de morcegos astrais voando em volta (batendo as asas, com rastro), lua de
 * sangue atras da cabeca, nevoa carmesim, gotas de sangue subindo, petalas de rosa e um selo gotico.
 * De vez em quando sai uma revoada de morcegos.
 */
public final class VisualVampiro extends VisualAura {
    private static final int MORCEGOS = 7;
    private static final int CARMESIM = 0xFF2448;
    private static final int[] NEVOA = {0x00300008, 0x5C420010, 0x30300008, 0x00200005};
    private static final int[] SANGUE = {0xFFE0203A, 0xFFA00A22, 0x00500010};
    private static final int[] PETALA = {0xFF9E1030, 0xFF7A0C26, 0xFFB8143A};

    /** Morcego k voando: raio e altura mudam o tempo todo, metade gira para cada lado. */
    private static void morcego(int k, float t, float h, float[] out) {
        float dir = k % 2 == 0 ? 1 : -1;
        float w = 0.065f + 0.012f * (k % 3);
        float ang = t * w * dir + k * 0.9f;
        float r = 1.05f + 0.35f * Mth.sin(t * 0.045f + k * 1.7f);
        out[0] = Mth.cos(ang) * r;
        out[1] = h * 0.6f + 0.5f * Mth.sin(t * 0.06f + k * 1.3f) + 0.14f * Mth.sin(t * 0.21f + k);
        out[2] = Mth.sin(ang) * r;
    }

    private static int corMorcego(int k) {
        return k % 3 == 0 ? 0xC060FF : 0xFF4A6E;
    }

    private static void revoada(EstadoAura e, int n, float vel) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        for (int i = 0; i < n; i++) {
            float a = r.nextFloat() * TAU;
            float v = vel * e.aleatorio(0.6f, 1f);
            int cor = corMorcego(i);
            e.emitir(c.x, c.y + c.altura * e.aleatorio(0.4f, 0.9f), c.z)
                    .vel(Mth.cos(a) * v, e.aleatorio(0.03f, 0.12f), Mth.sin(a) * v).vida(26 + r.nextInt(14))
                    .tam(0.2f, 0.12f).cores(Cor.comAlfa(cor, 1f), Cor.comAlfa(cor, 0f))
                    .animada(Sprites.MORCEGO, 0.6f).fisica(-0.002f, 0.95f).fade(0.05f, 0.4f)
                    .comportamento((p, es) -> {
                        p.vx += Mth.sin(p.idade * 0.35f + p.fase) * 0.012f;
                        p.vz += Mth.cos(p.idade * 0.31f + p.fase) * 0.012f;
                    });
        }
    }

    @Override
    public void tick(EstadoAura e) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        float h = c.altura;
        boolean corpo = !c.primeiraPessoa;

        if (corpo && e.chance(0.012f)) {
            revoada(e, 7, 0.14f);
            e.som(SoundEvents.BAT_TAKEOFF, 0.3f, 0.9f + r.nextFloat() * 0.2f);
        }
        // Nevoa carmesim rasteira
        for (int i = 0, n = e.quantos(0.3f); i < n; i++) {
            double[] p = noChao(e, 0.2f, 1.2f);
            float ang = (float) p[3];
            e.emitir(c.x + p[0], c.y + 0.04 + r.nextFloat() * 0.03, c.z + p[2])
                    .vel(Mth.cos(ang) * 0.006f, 0, Mth.sin(ang) * 0.006f).vida(45 + r.nextInt(20))
                    .tam(0.45f, 1.0f).cores(NEVOA).sprite(Sprites.FUMACAS[r.nextInt(3)]).forma(Particula.Forma.CHAO)
                    .translucida(true).fisica(0f, 0.99f).giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.02f).fade(0f, 0f);
        }
        // Gotas de sangue subindo
        for (int i = 0, n = e.quantos(0.5f); i < n; i++) {
            double[] p = noChao(e, corpo ? 0.3f : 0.8f, 1.15f);
            e.emitir(c.x + p[0], c.y + 0.05, c.z + p[2]).vel(0, 0.02f + r.nextFloat() * 0.02f, 0)
                    .vida(35 + r.nextInt(20)).tam(0.05f, 0.02f).cores(SANGUE).sprite(Sprites.NUCLEO)
                    .fisica(-0.0005f, 0.99f).fade(0.1f, 0.5f);
        }
        // Petalas de rosa caindo
        for (int i = 0, n = e.quantos(0.12f); i < n; i++) {
            double[] p = noChao(e, 0f, 1.2f);
            e.emitir(c.x + p[0], c.y + h + 0.4 + r.nextFloat() * 0.3, c.z + p[2]).vel(0, -0.018f, 0)
                    .vida(70 + r.nextInt(20)).tam(0.08f, -1).cores(PETALA[r.nextInt(PETALA.length)])
                    .sprite(Sprites.PETALA).translucida(false).fisica(0f, 1f)
                    .giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.15f).fade(0.1f, 0.25f)
                    .comportamento((pp, es) -> {
                        pp.vx = Mth.sin(pp.idade * 0.1f + pp.fase) * 0.01f;
                        if (pp.y < es.corpo.y + 0.02) pp.matar();
                    });
        }
        // Poeira astral soltando dos morcegos
        float[] o = new float[3];
        for (int k = 0; corpo && k < MORCEGOS; k++) {
            if (!e.chance(0.2f)) continue;
            morcego(k, e.idade, h, o);
            e.emitir(c.x + o[0], c.y + o[1], c.z + o[2]).vel(0, -0.005f, 0).vida(14).tam(0.05f, 0f)
                    .cores(Cor.comAlfa(corMorcego(k), 1f), Cor.comAlfa(corMorcego(k), 0f)).sprite(Sprites.NUCLEO)
                    .fisica(0f, 1f).fade(0f, 0.6f);
        }
        if (r.nextFloat() < 0.006f) e.som(SoundEvents.BAT_AMBIENT, 0.25f, 0.7f + r.nextFloat() * 0.2f);
    }

    @Override
    public void anunciar(EstadoAura e) {
        revoada(e, 24, 0.22f);
        onda(e, 0xFFFF2448, 3.2f, 16, Sprites.ANEL_SUAVE);
        clarao(e, 0xFFFF3050, 3.2f, 7);
        e.som(SoundEvents.BAT_TAKEOFF, 0.8f, 0.8f);
        e.som(SoundEvents.ENDERMAN_STARE, 0.2f, 0.7f);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        Corpo c = d.corpo;
        float h = c.altura, t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;
        float p = pulso(t, 0.06f, 0f);

        d.chao(false, cx, cy + 0.012, cz, 1.5f, 0f, Cor.comAlfa(0x0A0003, 0.45f * f), Sprites.BRILHO, luz);
        d.chao(true, cx, cy + 0.02, cz, 1.45f, 0f, Cor.comAlfa(0xB0102C, (0.2f + 0.08f * p) * f), Sprites.ANEL_SUAVE, luz);
        d.chao(true, cx, cy + 0.025, cz, 1.3f, t * 0.004f, Cor.comAlfa(CARMESIM, (0.65f + 0.2f * p) * f), Sprites.SELO_VAMPIRO, luz);
        if (c.primeiraPessoa) return;
        d.brilho(cx, cy + h * 0.55, cz, 1.1f, Cor.comAlfa(0xC0103A, 0.07f * f), Sprites.BRILHO);

        // Lua de sangue atras da cabeca, de frente para onde o corpo olha
        float yaw = d.yawCorpo;
        float fx = -Mth.sin(yaw), fz = Mth.cos(yaw), dx = -Mth.cos(yaw), dz = -Mth.sin(yaw);
        double lx = cx - fx * 0.42, ly = cy + h + 0.05 + Mth.sin(t * 0.05f) * 0.03, lz = cz - fz * 0.42;
        float rl = 0.62f;
        d.plano(true, lx, ly, lz, dx * rl * 1.5f, 0, dz * rl * 1.5f, 0, -rl * 1.5f, 0, Cor.comAlfa(0xC0102C, 0.35f * f), Sprites.BRILHO, luz, false);
        d.plano(true, lx, ly, lz, dx * rl, 0, dz * rl, 0, -rl, 0, Cor.comAlfa(0xFF3050, (0.85f + 0.1f * p) * f), Sprites.LUA, luz, false);
        d.plano(true, lx, ly, lz, dx * rl * 1.1f, 0, dz * rl * 1.1f, 0, -rl * 1.1f, 0, Cor.comAlfa(0xFF6070, 0.5f * f), Sprites.ANEL_FINO, luz, false);

        // Morcegos astrais com rastro
        float[] o = new float[3];
        for (int k = 0; k < MORCEGOS; k++) {
            int cor = corMorcego(k);
            d.fitaInicio();
            for (int i = 0; i < 10; i++) {
                float q = i / 9f;
                morcego(k, t - i * 0.6f, h, o);
                d.fitaPonto(cx + o[0], cy + o[1], cz + o[2], 0.12f * (1f - q * 0.85f), Cor.comAlfa(cor, (1f - q) * (1f - q) * 0.45f * f));
            }
            d.fitaFim(true, Sprites.FEIXE, luz);
            morcego(k, t, h, o);
            double x = cx + o[0], y = cy + o[1], z = cz + o[2];
            int q = Math.floorMod((int) (t * 0.55f + k * 1.7f), 4);
            d.brilho(x, y, z, 0.42f, Cor.comAlfa(cor, 0.28f * f), Sprites.BRILHO);
            d.billboard(true, x, y, z, 0.21f, Mth.sin(t * 0.1f + k) * 0.25f, Cor.comAlfa(cor, 0.95f * f), Sprites.MORCEGO[q], luz);
            d.billboard(true, x, y, z, 0.16f, Mth.sin(t * 0.1f + k) * 0.25f, Cor.comAlfa(0xFFFFFF, 0.35f * f), Sprites.MORCEGO[q], luz);
        }
    }
}
