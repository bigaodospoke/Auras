package com.auras.teste;

import com.auras.Auras;
import net.minecraft.server.MinecraftServer;

/** Parte do autoteste que roda no servidor. Nao pode tocar em classes do cliente (servidor dedicado). */
public final class AutoTesteServidor {
    public static void servidorIniciado(MinecraftServer server) {
        Auras.LOG.info("[autoteste] servidor iniciado (dedicado: {})", server.isDedicatedServer());
        if (Boolean.getBoolean("auras.autoteste.parar")) {
            boolean temComando = server.getCommands().getDispatcher().getRoot().getChild("aura") != null;
            Auras.LOG.info("[autoteste] comando /aura registrado: {}", temComando);
            Auras.LOG.info("[autoteste] FIM servidor");
            server.halt(false);
        }
    }

    private AutoTesteServidor() {
    }
}
