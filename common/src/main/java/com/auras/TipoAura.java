package com.auras;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum TipoAura {
    ELETRICA(0x5CDCFF),
    FOGO(0xFF7A1A),
    LAVA(0xFF4A12),
    GELO(0xA6ECFF),
    SOMBRA(0xA44BFF),
    NATUREZA(0x6FD84E),
    DIVINA(0xFFD25A),
    COSMICA(0xD27BFF),
    POKEBOLA(0xFF4F4F),
    KOKUSEN(0xE8203A),
    VAMPIRO(0xD42452),
    NECROMANTE(0x5CFFA8);

    public final String id;
    /** Cor principal, usada no menu e nas mensagens. */
    public final int cor;

    TipoAura(int cor) {
        this.id = name().toLowerCase(Locale.ROOT);
        this.cor = cor;
    }

    public Component nome() {
        return Component.translatable("aura.auras." + id).withStyle(s -> s.withColor(cor));
    }

    public Component descricao() {
        return Component.translatable("aura.auras." + id + ".desc");
    }

    @Nullable
    public static TipoAura porId(@Nullable String id) {
        if (id == null || id.isEmpty()) return null;
        for (TipoAura t : values()) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }
}
