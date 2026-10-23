package com.auras.neoforge;

import com.auras.Auras;
import com.auras.Plataforma;
import com.auras.rede.Pacotes;
import com.auras.servidor.ComandoAura;
import com.auras.servidor.GerenciadorAuras;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.nio.file.Path;

@Mod(Auras.MOD_ID)
public final class AurasNeoForge {
    public AurasNeoForge(IEventBus modBus, Dist dist) {
        Plataforma.definir(new Plataforma() {
            @Override
            public Path pastaConfig() {
                return FMLPaths.CONFIGDIR.get();
            }

            @Override
            public boolean podeEnviar(ServerPlayer jogador, CustomPacketPayload.Type<?> tipo) {
                return jogador.connection.hasChannel(tipo);
            }

            @Override
            public void enviar(ServerPlayer jogador, CustomPacketPayload pacote) {
                PacketDistributor.sendToPlayer(jogador, pacote);
            }
        });

        modBus.addListener(AurasNeoForge::registrarPacotes);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ComandoAura.registrar(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> GerenciadorAuras.aoIniciarServidor(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer jogador) GerenciadorAuras.aoEntrar(jogador);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer jogador) GerenciadorAuras.aoSair(jogador);
        });

        if (dist.isClient()) AurasNeoForgeCliente.iniciar(modBus);
    }

    private static void registrarPacotes(RegisterPayloadHandlersEvent event) {
        // optional(): jogador sem o mod ainda consegue entrar (so nao ve as auras)
        PayloadRegistrar r = event.registrar("1").optional();
        r.playToClient(Pacotes.Aura.TIPO, Pacotes.Aura.CODEC, (p, ctx) -> {
            if (Pacotes.cliente != null) Pacotes.cliente.aura(p);
        });
        r.playToClient(Pacotes.Todas.TIPO, Pacotes.Todas.CODEC, (p, ctx) -> {
            if (Pacotes.cliente != null) Pacotes.cliente.todas(p);
        });
        r.playToClient(Pacotes.Menu.TIPO, Pacotes.Menu.CODEC, (p, ctx) -> {
            if (Pacotes.cliente != null) Pacotes.cliente.menu(p);
        });
        r.playToServer(Pacotes.Escolher.TIPO, Pacotes.Escolher.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer jogador) GerenciadorAuras.receberEscolha(jogador, p.aura());
        });
        r.playToServer(Pacotes.PedirMenu.TIPO, Pacotes.PedirMenu.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer jogador) GerenciadorAuras.abrirMenu(jogador);
        });
    }
}
