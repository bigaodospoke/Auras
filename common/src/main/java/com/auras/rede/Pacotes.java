package com.auras.rede;

import com.auras.Auras;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Todos os pacotes do mod. Os ids de aura viajam como texto ("" = sem aura). */
public final class Pacotes {

    /** Servidor -> cliente: a aura de um jogador mudou. {@code anunciar} toca o efeito de ativacao. */
    public record Aura(UUID jogador, String aura, boolean anunciar) implements CustomPacketPayload {
        public static final Type<Aura> TIPO = new Type<>(Auras.id("aura"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Aura> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Aura::jogador,
                ByteBufCodecs.STRING_UTF8, Aura::aura,
                ByteBufCodecs.BOOL, Aura::anunciar,
                Aura::new);

        @Override
        public Type<Aura> type() {
            return TIPO;
        }
    }

    /** Servidor -> cliente: auras de todo mundo que esta online (enviado ao entrar). */
    public record Todas(Map<UUID, String> auras) implements CustomPacketPayload {
        public static final Type<Todas> TIPO = new Type<>(Auras.id("todas"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Todas> CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.STRING_UTF8), Todas::auras,
                Todas::new);

        @Override
        public Type<Todas> type() {
            return TIPO;
        }
    }

    /** Servidor -> cliente: abre o menu com a aura atual e as que o jogador pode usar. */
    public record Menu(String atual, List<String> liberadas) implements CustomPacketPayload {
        public static final Type<Menu> TIPO = new Type<>(Auras.id("menu"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Menu> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Menu::atual,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Menu::liberadas,
                Menu::new);

        @Override
        public Type<Menu> type() {
            return TIPO;
        }
    }

    /** Cliente -> servidor: o jogador escolheu uma aura no menu ("" = desligar). */
    public record Escolher(String aura) implements CustomPacketPayload {
        public static final Type<Escolher> TIPO = new Type<>(Auras.id("escolher"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Escolher> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Escolher::aura,
                Escolher::new);

        @Override
        public Type<Escolher> type() {
            return TIPO;
        }
    }

    /** Cliente -> servidor: apertou a tecla do menu. */
    public record PedirMenu() implements CustomPacketPayload {
        public static final PedirMenu INSTANCIA = new PedirMenu();
        public static final Type<PedirMenu> TIPO = new Type<>(Auras.id("pedir_menu"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PedirMenu> CODEC = StreamCodec.unit(INSTANCIA);

        @Override
        public Type<PedirMenu> type() {
            return TIPO;
        }
    }

    /**
     * Quem trata os pacotes no cliente. Fica atras de uma interface para o servidor dedicado nunca
     * carregar classes do cliente.
     */
    public interface ReceptorCliente {
        void aura(Aura pacote);

        void todas(Todas pacote);

        void menu(Menu pacote);
    }

    public static ReceptorCliente cliente;

    private Pacotes() {
    }
}
