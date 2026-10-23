package com.auras;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

/** O pouco que muda entre Fabric e NeoForge. Cada loader preenche isso ao iniciar. */
public abstract class Plataforma {
    private static Plataforma atual;
    private static RedeCliente redeCliente;

    public static Plataforma get() {
        return atual;
    }

    public static void definir(Plataforma plataforma) {
        atual = plataforma;
    }

    /** So existe no cliente; fica separado para o servidor dedicado nunca tocar em classes do cliente. */
    public static RedeCliente cliente() {
        return redeCliente;
    }

    public static void definirCliente(RedeCliente rede) {
        redeCliente = rede;
    }

    public abstract Path pastaConfig();

    /** O cliente desse jogador tem o mod (e pode receber o pacote)? */
    public abstract boolean podeEnviar(ServerPlayer jogador, CustomPacketPayload.Type<?> tipo);

    public abstract void enviar(ServerPlayer jogador, CustomPacketPayload pacote);

    public interface RedeCliente {
        /** O servidor em que estou tem o mod? */
        boolean servidorAceita(CustomPacketPayload.Type<?> tipo);

        void enviarAoServidor(CustomPacketPayload pacote);
    }
}
