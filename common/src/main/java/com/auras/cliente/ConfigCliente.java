package com.auras.cliente;

import com.auras.Auras;
import com.auras.Plataforma;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/auras/cliente.json (tambem da para mudar pelo menu). */
public final class ConfigCliente {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigCliente atual = new ConfigCliente();

    /** Qualidade: multiplica quantas particulas nascem (0.25 a 2). */
    public float densidade = 1f;
    /** Ate quantos blocos de distancia as auras aparecem. */
    public int distancia = 48;
    public boolean mostrarOutros = true;
    /** Mostra a sua aura (chao e particulas) quando esta em primeira pessoa. */
    public boolean primeiraPessoa = true;
    public boolean sons = true;
    public float volume = 0.7f;

    public static ConfigCliente get() {
        return atual;
    }

    private static Path arquivo() {
        return Plataforma.get().pastaConfig().resolve("auras").resolve("cliente.json");
    }

    public static void carregar() {
        Path arq = arquivo();
        ConfigCliente c = null;
        if (Files.exists(arq)) {
            try (Reader r = Files.newBufferedReader(arq, StandardCharsets.UTF_8)) {
                c = GSON.fromJson(r, ConfigCliente.class);
            } catch (Exception e) {
                Auras.LOG.error("Nao consegui ler {}, usando o padrao", arq, e);
            }
        }
        atual = c == null ? new ConfigCliente() : c;
        atual.ajustar();
        atual.salvar();
    }

    private void ajustar() {
        densidade = Mth.clamp(densidade, 0.25f, 2f);
        distancia = Mth.clamp(distancia, 8, 128);
        volume = Mth.clamp(volume, 0f, 1f);
    }

    public void salvar() {
        Path arq = arquivo();
        try {
            Files.createDirectories(arq.getParent());
            try (Writer w = Files.newBufferedWriter(arq, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            Auras.LOG.error("Nao consegui salvar {}", arq, e);
        }
    }
}
