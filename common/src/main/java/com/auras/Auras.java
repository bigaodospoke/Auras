package com.auras;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Auras {
    public static final String MOD_ID = "auras";
    public static final Logger LOG = LoggerFactory.getLogger("Auras");

    public static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, caminho);
    }

    /** Liga o autoteste de desenvolvimento (a classe nao existe no jar, entao fora do dev nunca roda). */
    public static void chamarAutoteste(String metodo, Object arg) {
        if (!Boolean.getBoolean("auras.autoteste")) return;
        try {
            // A parte do servidor fica separada para o servidor dedicado nunca carregar classes do cliente
            Class<?> classe = Class.forName(metodo.startsWith("servidor")
                    ? "com.auras.teste.AutoTesteServidor" : "com.auras.teste.AutoTeste");
            for (var m : classe.getMethods()) {
                if (m.getName().equals(metodo)) {
                    m.invoke(null, arg);
                    return;
                }
            }
        } catch (ReflectiveOperationException e) {
            LOG.error("Autoteste falhou em {}", metodo, e);
        }
    }

    private Auras() {
    }
}
