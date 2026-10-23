package com.auras.cliente.efeito;

import com.auras.TipoAura;
import com.auras.cliente.ConfigCliente;
import com.auras.cliente.visual.VisualAura;
import com.auras.cliente.visual.Visuais;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** Tudo o que uma aura ativa precisa entre um tick e outro: particulas, idade e os dados do visual. */
public final class EstadoAura {
    private static final int LIMITE = 450;
    /** Devolvida quando a aura ja tem particulas demais: o visual mexe nela e ela some. */
    private static final Particula DESCARTE = new Particula();

    public final TipoAura tipo;
    public final VisualAura visual;
    public final Corpo corpo = new Corpo();
    public final RandomSource random = RandomSource.create();
    public final List<Particula> particulas = new ArrayList<>();
    public final Object dados;
    public int idade;
    /** Toca o efeito de ativacao no primeiro tick. */
    public boolean anunciar;
    /** Multiplica quantas particulas nascem (qualidade da config x distancia). */
    public float densidade = 1f;

    public EstadoAura(TipoAura tipo) {
        this.tipo = tipo;
        this.visual = Visuais.de(tipo);
        this.dados = visual.criarDados(this);
    }

    public void tick() {
        for (int i = 0; i < particulas.size(); ) {
            if (particulas.get(i).tick(this)) {
                i++;
            } else {
                int fim = particulas.size() - 1;
                particulas.set(i, particulas.get(fim));
                particulas.remove(fim);
            }
        }
        if (idade == 0 && anunciar) visual.anunciar(this);
        visual.tick(this);
        idade++;
    }

    /** Nova particula na posicao do mundo dada (ou relativa aos pes, se o visual marcar relativa()). */
    public Particula emitir(double x, double y, double z) {
        Particula p;
        if (particulas.size() >= LIMITE) {
            p = DESCARTE;
            p.comportamento = null;
        } else {
            p = new Particula();
            particulas.add(p);
        }
        p.colocar(x, y, z);
        p.fase = random.nextFloat() * 6.28f;
        return p;
    }

    /** Emite relativa aos pes do dono. */
    public Particula emitirRel(double dx, double dy, double dz) {
        return emitir(dx, dy, dz).relativa();
    }

    /** Media de particulas por tick ajustada pela densidade, com arredondamento aleatorio. */
    public int quantos(float media) {
        float n = media * densidade * forca();
        int base = (int) n;
        return base + (random.nextFloat() < n - base ? 1 : 0);
    }

    /** Sorteio de nascimento que respeita a densidade. */
    public boolean chance(float p) {
        return random.nextFloat() < p * densidade * forca();
    }

    /** Sobe de 0 a 1 no primeiro meio segundo: a aura "acende". */
    public float forca() {
        return Mth.clamp((idade + 1) / 12f, 0f, 1f);
    }

    public float aleatorio(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    /**
     * Toca um som de outro mod se ele estiver instalado (ex.: "cobblemon:poke_ball.send_out"),
     * senao o som do vanilla. Nao cria dependencia nenhuma.
     */
    public void somDeMod(String id, SoundEvent padrao, float volume, float tom) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        SoundEvent externo = rl == null ? null : BuiltInRegistries.SOUND_EVENT.get(rl);
        som(externo != null ? externo : padrao, volume, tom);
    }

    public void som(SoundEvent som, float volume, float tom) {
        ConfigCliente cfg = ConfigCliente.get();
        Minecraft mc = Minecraft.getInstance();
        if (corpo.gui || !cfg.sons || mc.level == null) return;
        mc.level.playLocalSound(corpo.x, corpo.y + corpo.altura * 0.5, corpo.z, som, SoundSource.PLAYERS,
                volume * cfg.volume, tom, false);
    }
}
