package com.auras.fabric;

import com.auras.Plataforma;
import com.auras.rede.Pacotes;
import com.auras.servidor.ComandoAura;
import com.auras.servidor.GerenciadorAuras;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public final class AurasFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Plataforma.definir(new Plataforma() {
            @Override
            public Path pastaConfig() {
                return FabricLoader.getInstance().getConfigDir();
            }

            @Override
            public boolean podeEnviar(ServerPlayer jogador, CustomPacketPayload.Type<?> tipo) {
                return ServerPlayNetworking.canSend(jogador, tipo);
            }

            @Override
            public void enviar(ServerPlayer jogador, CustomPacketPayload pacote) {
                ServerPlayNetworking.send(jogador, pacote);
            }
        });

        PayloadTypeRegistry.playS2C().register(Pacotes.Aura.TIPO, Pacotes.Aura.CODEC);
        PayloadTypeRegistry.playS2C().register(Pacotes.Todas.TIPO, Pacotes.Todas.CODEC);
        PayloadTypeRegistry.playS2C().register(Pacotes.Menu.TIPO, Pacotes.Menu.CODEC);
        PayloadTypeRegistry.playC2S().register(Pacotes.Escolher.TIPO, Pacotes.Escolher.CODEC);
        PayloadTypeRegistry.playC2S().register(Pacotes.PedirMenu.TIPO, Pacotes.PedirMenu.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Pacotes.Escolher.TIPO, (pacote, ctx) ->
                ctx.server().execute(() -> GerenciadorAuras.receberEscolha(ctx.player(), pacote.aura())));
        ServerPlayNetworking.registerGlobalReceiver(Pacotes.PedirMenu.TIPO, (pacote, ctx) ->
                ctx.server().execute(() -> GerenciadorAuras.abrirMenu(ctx.player())));

        CommandRegistrationCallback.EVENT.register((dispatcher, registros, ambiente) -> ComandoAura.registrar(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(GerenciadorAuras::aoIniciarServidor);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> GerenciadorAuras.aoEntrar(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> GerenciadorAuras.aoSair(handler.getPlayer()));
    }
}
