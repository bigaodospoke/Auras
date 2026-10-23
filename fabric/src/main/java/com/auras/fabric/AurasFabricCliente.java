package com.auras.fabric;

import com.auras.Auras;
import com.auras.Plataforma;
import com.auras.cliente.AurasCliente;
import com.auras.cliente.render.RenderAuras;
import com.auras.rede.Pacotes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class AurasFabricCliente implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Plataforma.definirCliente(new Plataforma.RedeCliente() {
            @Override
            public boolean servidorAceita(CustomPacketPayload.Type<?> tipo) {
                return ClientPlayNetworking.canSend(tipo);
            }

            @Override
            public void enviarAoServidor(CustomPacketPayload pacote) {
                ClientPlayNetworking.send(pacote);
            }
        });
        AurasCliente.iniciar();
        KeyBindingHelper.registerKeyBinding(AurasCliente.TECLA_MENU);

        ClientPlayNetworking.registerGlobalReceiver(Pacotes.Aura.TIPO, (pacote, ctx) ->
                ctx.client().execute(() -> AurasCliente.get().aura(pacote)));
        ClientPlayNetworking.registerGlobalReceiver(Pacotes.Todas.TIPO, (pacote, ctx) ->
                ctx.client().execute(() -> AurasCliente.get().todas(pacote)));
        ClientPlayNetworking.registerGlobalReceiver(Pacotes.Menu.TIPO, (pacote, ctx) ->
                ctx.client().execute(() -> AurasCliente.get().menu(pacote)));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            AurasCliente.get().tick(mc);
            Auras.chamarAutoteste("tickCliente", mc);
        });
        WorldRenderEvents.AFTER_ENTITIES.register(ctx ->
                RenderAuras.mundo(ctx.camera(), ctx.tickCounter().getGameTimeDeltaPartialTick(true), ctx.frustum()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> mc.execute(() -> AurasCliente.get().desconectar()));
    }
}
