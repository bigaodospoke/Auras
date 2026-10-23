package com.auras.servidor;

import com.auras.Auras;
import com.auras.Plataforma;
import com.auras.TipoAura;
import com.auras.rede.Pacotes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Regras do lado do servidor: quem pode usar o que, e avisar os clientes quando algo muda. */
public final class GerenciadorAuras {
    private static ConfigServidor config = new ConfigServidor();

    public static ConfigServidor config() {
        return config;
    }

    public static void aoIniciarServidor(MinecraftServer server) {
        config = ConfigServidor.carregar();
        Auras.chamarAutoteste("servidorIniciado", server);
    }

    public static void recarregar(MinecraftServer server) {
        config = ConfigServidor.carregar();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) revalidar(p);
    }

    public static boolean ehAdmin(ServerPlayer jogador) {
        return jogador.hasPermissions(config.nivelAdmin);
    }

    public static boolean podeUsar(ServerPlayer jogador, TipoAura tipo) {
        if (config.desativada(tipo)) return false;
        if (!config.precisaLiberar || ehAdmin(jogador)) return true;
        return DadosAuras.de(jogador.server).liberadas(jogador.getUUID()).contains(tipo);
    }

    public static List<TipoAura> disponiveis(ServerPlayer jogador) {
        List<TipoAura> lista = new ArrayList<>();
        for (TipoAura t : TipoAura.values()) {
            if (podeUsar(jogador, t)) lista.add(t);
        }
        return lista;
    }

    @Nullable
    public static TipoAura equipada(ServerPlayer jogador) {
        return DadosAuras.de(jogador.server).equipada(jogador.getUUID());
    }

    /** Troca a aura do jogador (sem checar permissao) e avisa todo mundo. */
    public static void aplicar(ServerPlayer jogador, @Nullable TipoAura tipo) {
        DadosAuras dados = DadosAuras.de(jogador.server);
        if (dados.equipada(jogador.getUUID()) == tipo) return;
        dados.equipar(jogador.getUUID(), tipo);
        transmitir(jogador.server, new Pacotes.Aura(jogador.getUUID(), id(tipo), true), null);
    }

    /** Escolha feita pelo proprio jogador (menu ou /aura usar). */
    public static void escolher(ServerPlayer jogador, @Nullable TipoAura tipo) {
        if (tipo != null && !podeUsar(jogador, tipo)) {
            jogador.displayClientMessage(Component.translatable("msg.auras.bloqueada", tipo.nome())
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        aplicar(jogador, tipo);
        jogador.displayClientMessage(tipo == null
                ? Component.translatable("msg.auras.desligada").withStyle(ChatFormatting.GRAY)
                : Component.translatable("msg.auras.ativada", tipo.nome()), true);
    }

    /** Pacote do menu: "" desliga, id desconhecido e ignorado. */
    public static void receberEscolha(ServerPlayer jogador, String id) {
        TipoAura tipo = TipoAura.porId(id);
        if (tipo == null && !id.isEmpty()) return;
        escolher(jogador, tipo);
    }

    /** Se o jogador perdeu acesso a aura que esta usando, desliga ela. */
    public static void revalidar(ServerPlayer jogador) {
        TipoAura atual = equipada(jogador);
        if (atual != null && !podeUsar(jogador, atual)) aplicar(jogador, null);
    }

    public static void abrirMenu(ServerPlayer jogador) {
        if (!Plataforma.get().podeEnviar(jogador, Pacotes.Menu.TIPO)) {
            jogador.sendSystemMessage(Component.translatable("msg.auras.sem_mod").withStyle(ChatFormatting.YELLOW));
            return;
        }
        List<String> ids = new ArrayList<>();
        for (TipoAura t : disponiveis(jogador)) ids.add(t.id);
        Plataforma.get().enviar(jogador, new Pacotes.Menu(id(equipada(jogador)), ids));
    }

    public static void aoEntrar(ServerPlayer jogador) {
        revalidar(jogador);
        MinecraftServer server = jogador.server;
        DadosAuras dados = DadosAuras.de(server);
        if (Plataforma.get().podeEnviar(jogador, Pacotes.Todas.TIPO)) {
            Map<UUID, String> mapa = new HashMap<>();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                TipoAura t = dados.equipada(p.getUUID());
                if (t != null) mapa.put(p.getUUID(), t.id);
            }
            Plataforma.get().enviar(jogador, new Pacotes.Todas(mapa));
        }
        TipoAura minha = dados.equipada(jogador.getUUID());
        if (minha != null) transmitir(server, new Pacotes.Aura(jogador.getUUID(), minha.id, false), jogador);
    }

    public static void aoSair(ServerPlayer jogador) {
        if (equipada(jogador) != null) {
            transmitir(jogador.server, new Pacotes.Aura(jogador.getUUID(), "", false), jogador);
        }
    }

    private static void transmitir(MinecraftServer server, CustomPacketPayload pacote, @Nullable ServerPlayer exceto) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p != exceto && Plataforma.get().podeEnviar(p, pacote.type())) Plataforma.get().enviar(p, pacote);
        }
    }

    public static String id(@Nullable TipoAura tipo) {
        return tipo == null ? "" : tipo.id;
    }

    private GerenciadorAuras() {
    }
}
