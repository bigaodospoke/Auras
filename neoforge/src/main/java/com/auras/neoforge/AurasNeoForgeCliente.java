package com.auras.neoforge;

import com.auras.Auras;
import com.auras.Plataforma;
import com.auras.cliente.AurasCliente;
import com.auras.cliente.render.RenderAuras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

final class AurasNeoForgeCliente {
    static void iniciar(IEventBus modBus) {
        Plataforma.definirCliente(new Plataforma.RedeCliente() {
            @Override
            public boolean servidorAceita(CustomPacketPayload.Type<?> tipo) {
                ClientPacketListener conexao = Minecraft.getInstance().getConnection();
                return conexao != null && conexao.hasChannel(tipo);
            }

            @Override
            public void enviarAoServidor(CustomPacketPayload pacote) {
                PacketDistributor.sendToServer(pacote);
            }
        });
        AurasCliente.iniciar();

        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(AurasCliente.TECLA_MENU));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            AurasCliente.get().tick(mc);
            Auras.chamarAutoteste("tickCliente", mc);
        });
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                RenderAuras.mundo(e.getCamera(), e.getPartialTick().getGameTimeDeltaPartialTick(true), e.getFrustum());
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> AurasCliente.get().desconectar());
    }

    private AurasNeoForgeCliente() {
    }
}
