package com.auras.cliente.visual;

import com.auras.cliente.efeito.Cor;
import com.auras.cliente.efeito.Corpo;
import com.auras.cliente.efeito.EstadoAura;
import com.auras.cliente.efeito.Particula;
import com.auras.cliente.efeito.Sprites;
import com.auras.cliente.render.Desenho;
import com.auras.cliente.render.ModeloPokebola;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/**
 * Quatro pokebolas (poke, great, ultra, master) orbitando. De vez em quando uma se abre e solta um
 * feixe de energia vermelha no chao, como quando o treinador manda o Pokemon. Brilhos shiny e o
 * emblema da pokebola no chao. Se o Cobblemon estiver instalado, usa os sons dele.
 */
public final class VisualPokebola extends VisualAura {
    private static final int VERMELHO = 0xFFFF3B3B;
    private static final int[] ENERGIA = {0xFFFFFFFF, 0xFFFF6A6A, 0xFFFF2A2A, 0x00C00000};
    private static final int[] SHINY = {0xFFFFFFFF, 0xFFFFF4A8, 0xFFA8E8FF, 0x0080C0FF};
    private static final int BOLAS = 4;
    private static final int DURACAO_ABRIR = 16;

    private static final class Dados {
        int abrindo = -1;
        int idadeAbrir;
        /** Onde o feixe bate no chao (relativo aos pes). */
        float alvoX, alvoZ;
    }

    @Override
    public Object criarDados(EstadoAura e) {
        return new Dados();
    }

    private static void bola(int k, float t, float h, float[] out) {
        float ang = t * 0.05f + k * TAU / BOLAS;
        float r = 0.98f;
        out[0] = Mth.cos(ang) * r;
        // orbita inclinada: sobe de um lado e desce do outro
        out[1] = h * 0.5f + Mth.sin(ang) * 0.3f + Mth.sin(t * 0.07f + k * 1.9f) * 0.08f;
        out[2] = Mth.sin(ang) * r;
        out[3] = ang;
    }

    /** Estouro de estrelinhas do shiny: um anel de estrelas que sai do ponto e para. */
    private static void shiny(EstadoAura e, double x, double y, double z, float escala) {
        RandomSource r = e.random;
        // O anel fica de frente para a camera de quem olha (na previa do menu, de frente para a tela)
        float lx = 1, ly = 0, lz = 0, ux = 0, uy = 1, uz = 0;
        if (!e.corpo.gui) {
            Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vector3f esq = cam.getLeftVector(), cima = cam.getUpVector();
            lx = esq.x;
            ly = esq.y;
            lz = esq.z;
            ux = cima.x;
            uy = cima.y;
            uz = cima.z;
        }
        int n = 8;
        float giro = r.nextFloat() * TAU;
        for (int i = 0; i < n; i++) {
            float a = giro + i * TAU / n;
            float v = 0.07f * escala;
            float ca = Mth.cos(a) * v, sa = Mth.sin(a) * v;
            e.emitir(x, y, z).vel(lx * ca + ux * sa, ly * ca + uy * sa, lz * ca + uz * sa)
                    .vida(14).tam(0.13f * escala, 0.02f).cores(SHINY).sprite(Sprites.ESTRELA)
                    .giro(PI / 4, 0.1f).fisica(0f, 0.84f).fade(0f, 0.5f);
        }
        e.emitir(x, y, z).vida(8).tam(0.35f * escala, 0.05f).cores(0xFFFFFFFF, 0x00A8E8FF)
                .sprite(Sprites.ESTRELA_SUAVE).giro(0f, 0.05f).fisica(0, 1).fade(0f, 0.7f);
    }

    @Override
    public void tick(EstadoAura e) {
        Dados dd = (Dados) e.dados;
        Corpo c = e.corpo;
        RandomSource r = e.random;
        float h = c.altura;
        boolean corpo = !c.primeiraPessoa;
        float[] o = new float[4];

        // A cada 2,5 s a proxima bola da fila se abre (poke, great, ultra, master)
        if (dd.abrindo >= 0 && ++dd.idadeAbrir >= DURACAO_ABRIR) dd.abrindo = -1;
        if (corpo && dd.abrindo < 0 && e.idade % 50 == 25) {
            dd.abrindo = (e.idade / 50) % BOLAS;
            dd.idadeAbrir = 0;
            bola(dd.abrindo, e.idade, h, o);
            float dist = e.aleatorio(1.5f, 1.9f);
            dd.alvoX = Mth.cos(o[3]) * dist;
            dd.alvoZ = Mth.sin(o[3]) * dist;
            e.emitirRel(o[0], o[1], o[2]).vida(6).tam(0.7f, 0.2f).cores(0xFFFFFFFF, 0x00FF4040)
                    .sprite(Sprites.BRILHO).fisica(0, 1).fade(0f, 0.7f);
            e.somDeMod("cobblemon:poke_ball.send_out", SoundEvents.ENDER_EYE_LAUNCH, 0.45f, 1.1f);
        }
        if (dd.abrindo >= 0 && dd.idadeAbrir == 4) {
            // a energia "materializa" no chao
            double ax = dd.alvoX, az = dd.alvoZ;
            for (int i = 0; i < 14; i++) {
                float a = r.nextFloat() * TAU, rr = r.nextFloat() * 0.3f;
                e.emitirRel(ax + Mth.cos(a) * rr, 0.05, az + Mth.sin(a) * rr)
                        .vel(0, 0.04f + r.nextFloat() * 0.07f, 0).vida(14 + r.nextInt(8)).tam(0.09f, 0.02f)
                        .cores(ENERGIA).sprite(Sprites.NUCLEO).fisica(0f, 0.93f).fade(0f, 0.5f);
            }
            e.emitirRel(ax, 0.05, az).forma(Particula.Forma.CHAO).sprite(Sprites.ANEL).tam(0.1f, 0.9f).vida(10)
                    .cores(0xFFFFFFFF, 0xFFFF4040, 0x00FF0000).fade(0f, 0.5f).fisica(0, 1);
            e.emitirRel(ax, 0.6, az).sprite(Sprites.BRILHO).tam(1.0f, 0.3f).vida(7)
                    .cores(0xC0FF6060, 0x00FF0000).fade(0f, 0.7f).fisica(0, 1);
            shiny(e, c.x + ax, c.y + 0.7, c.z + az, 1.1f);
        }

        // Brilho shiny aparecendo em volta
        if (e.chance(0.035f)) {
            double[] p = noCorpo(e, corpo ? 0.5f : 0.9f, 1.05f, corpo ? 0.2f : 0f, corpo ? 1f : 0.3f);
            shiny(e, c.x + p[0], c.y + p[1], c.z + p[2], 1f);
            if (r.nextFloat() < 0.4f) e.somDeMod("cobblemon:particle.shiny_chime", SoundEvents.AMETHYST_BLOCK_CHIME, 0.25f, 1.4f);
        }
        // Faisquinhas vermelhas e brancas subindo
        for (int i = 0, n = e.quantos(0.7f); i < n; i++) {
            double[] p = noChao(e, 0.3f, 1.2f);
            boolean branca = r.nextBoolean();
            e.emitir(c.x + p[0], c.y + 0.05, c.z + p[2]).vel(0, 0.02f + r.nextFloat() * 0.03f, 0)
                    .vida(25 + r.nextInt(20)).tam(0.06f, 0.01f)
                    .cores(branca ? 0xFFFFFFFF : 0xFFFF5A5A, branca ? 0x00FFFFFF : 0x00FF2020)
                    .sprite(Sprites.NUCLEO).fisica(0f, 0.98f).fade(0.1f, 0.5f)
                    .comportamento((pp, es) -> pp.vx = Mth.sin(pp.idade * 0.2f + pp.fase) * 0.006f).piscar = 0.4f;
        }
        // Rastro das bolas
        for (int k = 0; corpo && k < BOLAS; k++) {
            if (!e.chance(0.25f)) continue;
            bola(k, e.idade, h, o);
            e.emitir(c.x + o[0], c.y + o[1], c.z + o[2]).vida(12).tam(0.05f, 0f).cores(0xFFFFFFFF, 0x00FF6060)
                    .sprite(Sprites.NUCLEO).fisica(0f, 1f).fade(0f, 0.6f);
        }
    }

    @Override
    public void anunciar(EstadoAura e) {
        Corpo c = e.corpo;
        double y = c.y + c.altura * 0.5;
        clarao(e, 0xFFFFFFFF, 3.2f, 7);
        onda(e, 0xFFFF4040, 3.2f, 16, Sprites.ANEL_SUAVE);
        shiny(e, c.x, y, c.z, 2.2f);
        shiny(e, c.x, y + 0.4, c.z, 1.6f);
        explosao(e, 30, 0.2f, Sprites.NUCLEO, 0.1f, 18, 0.004f, ENERGIA);
        e.somDeMod("cobblemon:poke_ball.shiny_send_out", SoundEvents.PLAYER_LEVELUP, 0.6f, 1.2f);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        Dados dd = (Dados) e.dados;
        float h = d.corpo.altura, t = d.tempo, f = d.forca;
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;
        float p = pulso(t, 0.07f, 0f);

        // Emblema da pokebola em neon no chao
        d.chao(true, cx, cy + 0.015, cz, 1.45f, 0f, Cor.comAlfa(0xFF7070, (0.1f + 0.05f * p) * f), Sprites.ANEL_SUAVE, luz);
        d.chao(true, cx, cy + 0.02, cz, 1.25f, t * 0.006f, Cor.comAlfa(0xFFFFFF, (0.8f + 0.2f * p) * f), Sprites.EMBLEMA_POKEBOLA, luz);
        if (d.corpo.primeiraPessoa) return;
        d.brilho(cx, cy + h * 0.55, cz, 1.1f, Cor.comAlfa(0xFF5050, 0.06f * f), Sprites.BRILHO);

        float[] o = new float[4];
        for (int k = 0; k < BOLAS; k++) {
            bola(k, t, h, o);
            double x = cx + o[0], y = cy + o[1], z = cz + o[2];
            float ang = o[3];
            // rastro curto atras da bola
            d.fitaInicio();
            for (int i = 0; i < 10; i++) {
                float q = i / 9f;
                bola(k, t - i * 0.7f, h, o);
                d.fitaPonto(cx + o[0], cy + o[1], cz + o[2], 0.12f * (1f - q * 0.8f),
                        Cor.comAlfa(Cor.misturar(0xFFFFFFFF, VERMELHO, q) & 0xFFFFFF, (1f - q) * (1f - q) * 0.5f * f));
            }
            d.fitaFim(true, Sprites.FEIXE, luz);

            boolean abrindo = dd.abrindo == k;
            float abre = abrindo ? Mth.sin(Mth.clamp((dd.idadeAbrir + d.parcial) / DURACAO_ABRIR, 0f, 1f) * PI) : 0f;
            if (ModeloPokebola.disponivel()) {
                // Modelo 3D do Cobblemon: a frente (botao) olha para fora e a tampa abre de verdade
                float yaw = -ang - PI / 2 + Mth.sin(t * 0.05f + k * 1.7f) * 0.35f;
                float inclina = Mth.sin(t * 0.07f + k) * 0.2f - 0.15f;
                ModeloPokebola.desenhar(d, k, x, y, z, yaw, inclina, abre * 1.3f, 0.27f * f, luz);
            } else {
                float tam = 0.21f * (1f + 0.25f * abre) * f;
                d.billboard(false, x, y, z, tam, Mth.sin(t * 0.08f + k * 1.3f) * 0.5f, 0xFFFFFFFF, Sprites.POKEBOLAS[k], luz);
            }
            d.brilho(x, y, z, 0.5f + 0.5f * abre, Cor.comAlfa(abrindo ? 0xFF4040 : 0xFFFFFF, (0.1f + 0.6f * abre) * f), Sprites.BRILHO);

            // Feixe vermelho da bola ate o chao enquanto abre
            if (abrindo && dd.idadeAbrir < 11) {
                float a = (1f - dd.idadeAbrir / 11f) * f;
                double tx = cx + dd.alvoX, tz = cz + dd.alvoZ;
                for (int passo = 0; passo < 2; passo++) {
                    float larg = passo == 0 ? 0.32f : 0.09f;
                    int cor = passo == 0 ? Cor.comAlfa(0xFF2A2A, 0.55f * a) : Cor.comAlfa(0xFFE0E0, 0.95f * a);
                    d.fitaInicio();
                    for (int i = 0; i <= 8; i++) {
                        float s = i / 8f;
                        double bx = Mth.lerp(s, x, tx), bz = Mth.lerp(s, z, tz);
                        double by = Mth.lerp(s, y, cy + 0.05) + Mth.sin(s * PI) * 0.35;
                        d.fitaPonto(bx, by, bz, larg * (0.6f + 0.4f * s), cor);
                    }
                    d.fitaFim(true, Sprites.FEIXE, luz);
                }
            }
        }
    }
}
