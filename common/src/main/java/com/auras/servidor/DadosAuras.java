package com.auras.servidor;

import com.auras.TipoAura;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Aura equipada e auras liberadas de cada jogador. Fica salvo no mundo (data/auras.dat). */
public final class DadosAuras extends SavedData {
    private static final String NOME = "auras";

    private static final class Registro {
        @Nullable
        TipoAura equipada;
        final EnumSet<TipoAura> liberadas = EnumSet.noneOf(TipoAura.class);

        boolean vazio() {
            return equipada == null && liberadas.isEmpty();
        }
    }

    private final Map<UUID, Registro> jogadores = new HashMap<>();

    public static DadosAuras de(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DadosAuras::new, DadosAuras::carregar, null), NOME);
    }

    @Nullable
    public TipoAura equipada(UUID jogador) {
        Registro r = jogadores.get(jogador);
        return r == null ? null : r.equipada;
    }

    public void equipar(UUID jogador, @Nullable TipoAura tipo) {
        registro(jogador).equipada = tipo;
        limpar(jogador);
        setDirty();
    }

    public Set<TipoAura> liberadas(UUID jogador) {
        Registro r = jogadores.get(jogador);
        return r == null ? EnumSet.noneOf(TipoAura.class) : EnumSet.copyOf(r.liberadas);
    }

    public boolean liberar(UUID jogador, TipoAura tipo) {
        boolean mudou = registro(jogador).liberadas.add(tipo);
        if (mudou) setDirty();
        return mudou;
    }

    public boolean tirar(UUID jogador, TipoAura tipo) {
        Registro r = jogadores.get(jogador);
        boolean mudou = r != null && r.liberadas.remove(tipo);
        if (mudou) {
            limpar(jogador);
            setDirty();
        }
        return mudou;
    }

    private Registro registro(UUID jogador) {
        return jogadores.computeIfAbsent(jogador, u -> new Registro());
    }

    private void limpar(UUID jogador) {
        Registro r = jogadores.get(jogador);
        if (r != null && r.vazio()) jogadores.remove(jogador);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
        ListTag lista = new ListTag();
        jogadores.forEach((uuid, r) -> {
            CompoundTag j = new CompoundTag();
            j.putUUID("id", uuid);
            if (r.equipada != null) j.putString("equipada", r.equipada.id);
            ListTag lib = new ListTag();
            for (TipoAura t : r.liberadas) lib.add(StringTag.valueOf(t.id));
            j.put("liberadas", lib);
            lista.add(j);
        });
        tag.put("jogadores", lista);
        return tag;
    }

    private static DadosAuras carregar(CompoundTag tag, HolderLookup.Provider registros) {
        DadosAuras dados = new DadosAuras();
        ListTag lista = tag.getList("jogadores", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag j = lista.getCompound(i);
            if (!j.hasUUID("id")) continue;
            Registro r = new Registro();
            r.equipada = TipoAura.porId(j.getString("equipada"));
            ListTag lib = j.getList("liberadas", Tag.TAG_STRING);
            for (int k = 0; k < lib.size(); k++) {
                TipoAura t = TipoAura.porId(lib.getString(k));
                if (t != null) r.liberadas.add(t);
            }
            if (!r.vazio()) dados.jogadores.put(j.getUUID("id"), r);
        }
        return dados;
    }
}
