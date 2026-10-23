package com.auras.servidor;

import com.auras.Auras;
import com.auras.Plataforma;
import com.auras.TipoAura;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** config/auras/servidor.json */
public final class ConfigServidor {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @SuppressWarnings("unused")
    private List<String> _ajuda = List.of(
            "precisaLiberar: false = todo mundo usa todas as auras. true = cada jogador so usa as que um admin liberou com /aura dar.",
            "nivelAdmin: nivel de OP para os comandos de admin (dar, tirar, definir, recarregar). Admins sempre podem usar todas.",
            "desativadas: ids de auras que ninguem pode usar. Ids: eletrica, fogo, lava, gelo, sombra, natureza, divina, cosmica, pokebola, kokusen, vampiro, necromante.",
            "Depois de editar, use /aura recarregar.");

    public boolean precisaLiberar = false;
    public int nivelAdmin = 2;
    public List<String> desativadas = new ArrayList<>();

    public boolean desativada(TipoAura tipo) {
        return desativadas != null && desativadas.contains(tipo.id);
    }

    public static ConfigServidor carregar() {
        Path arquivo = Plataforma.get().pastaConfig().resolve("auras").resolve("servidor.json");
        ConfigServidor config = null;
        if (Files.exists(arquivo)) {
            try (Reader r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(r, ConfigServidor.class);
            } catch (Exception e) {
                Auras.LOG.error("Nao consegui ler {}, usando o padrao", arquivo, e);
            }
        }
        if (config == null) config = new ConfigServidor();
        if (config.desativadas == null) config.desativadas = new ArrayList<>();
        config.salvar(arquivo);
        return config;
    }

    private void salvar(Path arquivo) {
        try {
            Files.createDirectories(arquivo.getParent());
            try (Writer w = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            Auras.LOG.error("Nao consegui salvar {}", arquivo, e);
        }
    }
}
