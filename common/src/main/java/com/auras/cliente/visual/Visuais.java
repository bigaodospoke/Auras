package com.auras.cliente.visual;

import com.auras.TipoAura;

import java.util.EnumMap;
import java.util.Map;

public final class Visuais {
    private static final Map<TipoAura, VisualAura> MAPA = new EnumMap<>(TipoAura.class);

    static {
        MAPA.put(TipoAura.ELETRICA, new VisualEletrica());
        MAPA.put(TipoAura.FOGO, new VisualFogo());
        MAPA.put(TipoAura.LAVA, new VisualLava());
        MAPA.put(TipoAura.GELO, new VisualGelo());
        MAPA.put(TipoAura.SOMBRA, new VisualSombra());
        MAPA.put(TipoAura.NATUREZA, new VisualNatureza());
        MAPA.put(TipoAura.DIVINA, new VisualDivina());
        MAPA.put(TipoAura.COSMICA, new VisualCosmica());
        MAPA.put(TipoAura.POKEBOLA, new VisualPokebola());
        MAPA.put(TipoAura.KOKUSEN, new VisualKokusen());
        MAPA.put(TipoAura.VAMPIRO, new VisualVampiro());
        MAPA.put(TipoAura.NECROMANTE, new VisualNecromante());
    }

    public static VisualAura de(TipoAura tipo) {
        return MAPA.get(tipo);
    }

    private Visuais() {
    }
}
