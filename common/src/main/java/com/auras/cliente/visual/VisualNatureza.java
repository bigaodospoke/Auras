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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Folhas subindo em espiral, petalas caindo, vaga-lumes e flores brotando do chao por onde passa. */
public final class VisualNatureza extends VisualAura {
    private static final int[] FOLHAS = {0xFF5FBF3A, 0xFF86D24A, 0xFF3E9E3A, 0xFFB5E05A, 0xFF2F8F45};
    private static final int[] PETALAS = {0xFFFFB0D0, 0xFFFF8FC0, 0xFFFFF0F5, 0xFFFFD6E8};
    private static Block[] flores;

    private static Block[] flores() {
        if (flores == null) {
            flores = new Block[]{Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.AZURE_BLUET,
                    Blocks.OXEYE_DAISY, Blocks.ALLIUM, Blocks.PINK_TULIP, Blocks.LILY_OF_THE_VALLEY, Blocks.BLUE_ORCHID};
        }
        return flores;
    }

    /** Folha relativa girando em volta do corpo enquanto sobe. a = angulo, b = raio, c = velocidade angular. */
    private static final Particula.Comportamento ESPIRAL = (p, e) -> {
        p.a += p.c;
        p.b += 0.002;
        p.x = Math.cos(p.a) * p.b;
        p.z = Math.sin(p.a) * p.b;
        if (p.y > e.corpo.altura + 0.45 && p.vida - p.idade > 8) p.vida = p.idade + 8;
    };

    /** Vaga-lume vagando perto do corpo. */
    private static final Particula.Comportamento VAGAR = (p, e) -> {
        RandomSource r = e.random;
        p.vx += (r.nextFloat() - 0.5f) * 0.006f;
        p.vy += (r.nextFloat() - 0.5f) * 0.004f;
        p.vz += (r.nextFloat() - 0.5f) * 0.006f;
        double d = Math.sqrt(p.x * p.x + p.z * p.z);
        if (d > 1.3) {
            p.vx -= p.x * 0.004;
            p.vz -= p.z * 0.004;
        }
        if (p.y < 0.2) p.vy += 0.003;
        if (p.y > e.corpo.altura + 0.4) p.vy -= 0.003;
    };

    private static void flor(EstadoAura e, double x, double z, int vida) {
        RandomSource r = e.random;
        Particula p = e.emitir(x, e.corpo.y, z).forma(Particula.Forma.BLOCO).tam(0.5f + r.nextFloat() * 0.15f, -1)
                .vida(vida).fisica(0, 1).fade(0.15f, 0.2f).giro(r.nextFloat() * TAU, 0f);
        p.bloco = flores()[r.nextInt(flores().length)].defaultBlockState();
        p.crescer = true;
        p.luzPropria = false;
        p.aditivo = false;
    }

    @Override
    public void tick(EstadoAura e) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        float h = c.altura;

        for (int i = 0, n = e.quantos(0.9f); i < n; i++) {
            Particula p = e.emitirRel(0, r.nextFloat() * 0.3f, 0).vel(0, 0.016f + r.nextFloat() * 0.01f, 0)
                    .vida(110).tam(0.11f + r.nextFloat() * 0.06f, -1).cores(FOLHAS[r.nextInt(FOLHAS.length)])
                    .sprite(Sprites.FOLHAS[r.nextInt(2)]).translucida(false).fisica(0f, 1f)
                    .giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.25f).fade(0.1f, 0.15f).comportamento(ESPIRAL);
            p.a = r.nextFloat() * TAU;
            p.b = 0.62f + r.nextFloat() * 0.3f;
            p.c = 0.06f + r.nextFloat() * 0.03f;
        }
        for (int i = 0, n = e.quantos(0.2f); i < n; i++) {
            double[] p = noChao(e, 0f, 1.2f);
            e.emitir(c.x + p[0], c.y + h + 0.3 + r.nextFloat() * 0.4, c.z + p[2]).vel(0, -0.02f, 0)
                    .vida(70 + r.nextInt(20)).tam(0.09f + r.nextFloat() * 0.04f, -1).cores(PETALAS[r.nextInt(PETALAS.length)])
                    .sprite(Sprites.PETALA).translucida(false).fisica(0f, 1f)
                    .giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.15f).fade(0.1f, 0.25f)
                    .comportamento((pp, es) -> {
                        pp.vx = Mth.sin(pp.idade * 0.1f + pp.fase) * 0.012f;
                        pp.vz = Mth.cos(pp.idade * 0.08f + pp.fase) * 0.008f;
                        if (pp.y < es.corpo.y + 0.02) pp.matar();
                    });
        }
        if (e.chance(0.07f)) {
            double[] p = noCorpo(e, 0.5f, 1.2f, 0.15f, 1.1f);
            e.emitirRel(p[0], p[1], p[2]).vida(80 + r.nextInt(60)).tam(0.13f, -1).cores(0xFFE8FF70, 0xFFC8FF50)
                    .sprite(Sprites.NUCLEO).fisica(0f, 0.95f).fade(0.15f, 0.25f).comportamento(VAGAR).piscar = 0.9f;
        }
        for (int i = 0, n = e.quantos(0.4f); i < n; i++) {
            double[] p = noCorpo(e, 0.2f, 1.0f, 0f, 0.9f);
            e.emitir(c.x + p[0], c.y + p[1], c.z + p[2]).vel(0, 0.008f, 0).vida(40 + r.nextInt(20))
                    .tam(0.04f, 0.02f).cores(0xFFE8FFB0, 0x00A0E060).sprite(Sprites.NUCLEO).fisica(0f, 1f)
                    .fade(0.2f, 0.5f).comportamento((pp, es) -> pp.vx = Mth.sin(pp.idade * 0.15f + pp.fase) * 0.005f)
                    .piscar = 0.5f;
        }
        // Flores brotam por onde anda; parado, nascem em volta
        if (c.noChao) {
            if (c.velocidade() > 0.05) {
                if (e.chance(0.45f)) flor(e, c.x + (r.nextFloat() - 0.5f) * 0.7, c.z + (r.nextFloat() - 0.5f) * 0.7, 45 + r.nextInt(25));
            } else if (e.chance(0.05f)) {
                double[] p = noChao(e, 0.55f, 1.05f);
                flor(e, c.x + p[0], c.z + p[2], 80 + r.nextInt(50));
            }
        }
    }

    @Override
    public void anunciar(EstadoAura e) {
        Corpo c = e.corpo;
        for (int i = 0; i < 8; i++) {
            float ang = TAU * i / 8;
            flor(e, c.x + Mth.cos(ang) * 0.85, c.z + Mth.sin(ang) * 0.85, 70 + e.random.nextInt(30));
        }
        explosao(e, 28, 0.2f, Sprites.FOLHAS[0], 0.13f, 30, 0.006f, FOLHAS[0], FOLHAS[3]);
        onda(e, 0xFF8BE05A, 3.0f, 16, Sprites.ANEL_SUAVE);
        e.som(SoundEvents.BONE_MEAL_USE, 1f, 0.9f);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        float h = d.corpo.altura, t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;
        float p = pulso(t, 0.05f, 0f);
        d.chao(true, cx, cy + 0.02, cz, 1.35f, 0f, Cor.comAlfa(0x5FD040, 0.22f * f), Sprites.ANEL_SUAVE, luz);
        d.chao(true, cx, cy + 0.025, cz, 1.2f, t * 0.004f, Cor.comAlfa(0xA8F070, (0.5f + 0.2f * p) * f), Sprites.CIRCULO_NATUREZA, luz);
        if (!d.corpo.primeiraPessoa) {
            d.brilho(cx, cy + h * 0.55, cz, 1.1f, Cor.comAlfa(0x8BE05A, 0.06f * f), Sprites.BRILHO);
        }
    }
}
