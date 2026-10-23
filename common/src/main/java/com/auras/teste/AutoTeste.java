package com.auras.teste;

import com.auras.Auras;
import com.auras.TipoAura;
import com.auras.cliente.render.RenderAuras;
import com.auras.cliente.tela.TelaAuras;
import com.auras.servidor.GerenciadorAuras;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * So existe no desenvolvimento (fica fora do jar). gradlew :fabric:runAutoteste ou :neoforge:runAutoteste.
 * Cria um mundo plano, liga cada aura, grava quadros para GIF em run/autoteste/quadros/<aura>/
 * e tira prints em run/autoteste/screenshots/.
 *
 * -Dauras.autoteste.auras=eletrica,fogo  escolhe quais (padrao: todas)
 * -Dauras.autoteste.quadros=56           quadros por aura (0 = so print)
 * -Dauras.autoteste.hora=13000           hora do dia no mundo
 */
public final class AutoTeste {
    private static final String MUNDO = "AurasTeste";
    private static final int POR_AURA = 95;
    /** Comeca a gravar depois da explosao de ativacao, para o GIF ficar em loop continuo. */
    private static final int INICIO_QUADROS = 26;

    private static boolean pediuMundo;
    private static int t;
    private static int ultimoQuadro = -1;
    private static int numeroQuadro;
    private static List<TipoAura> auras;
    private static int quadrosPorAura;

    public static void tickCliente(Minecraft mc) {
        if (auras == null) {
            String lista = System.getProperty("auras.autoteste.auras", "");
            auras = new ArrayList<>();
            if (lista.isBlank()) auras.addAll(Arrays.asList(TipoAura.values()));
            else for (String id : lista.split(",")) {
                TipoAura tipo = TipoAura.porId(id.trim());
                if (tipo != null) auras.add(tipo);
            }
            quadrosPorAura = Integer.getInteger("auras.autoteste.quadros", 60);
        }
        if (mc.level == null || mc.player == null) {
            if (!pediuMundo && mc.screen instanceof TitleScreen) {
                pediuMundo = true;
                abrirMundo(mc);
            }
            return;
        }
        t++;
        if (t == 40) preparar(mc);
        // O corpo fica atrasado em relacao a cabeca depois do teleporte: alinha para as fotos
        if (t > 44) {
            mc.player.yBodyRot = mc.player.yBodyRotO = mc.player.getYRot();
            mc.player.yHeadRot = mc.player.yHeadRotO = mc.player.getYRot();
        }
        int base = 70;
        for (int i = 0; i < auras.size(); i++) {
            TipoAura tipo = auras.get(i);
            int ini = base + i * POR_AURA;
            if (t == ini) {
                numeroQuadro = 0;
                noServidor(mc, p -> GerenciadorAuras.aplicar(p, tipo));
            }
            if (t >= ini + INICIO_QUADROS && t < ini + INICIO_QUADROS + quadrosPorAura) quadro(mc, tipo.id);
            if (t == ini + POR_AURA - 4) print(mc, "aura_" + tipo.id);
        }
        int fim = base + auras.size() * POR_AURA;
        // Costas (asas da divina) e primeira pessoa
        if (t == fim) {
            noServidor(mc, p -> GerenciadorAuras.aplicar(p, TipoAura.DIVINA));
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.player.setXRot(22f);
        }
        if (t == fim + 40) print(mc, "divina_costas");
        if (t == fim + 42) {
            noServidor(mc, p -> GerenciadorAuras.aplicar(p, TipoAura.FOGO));
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.setXRot(35f);
        }
        if (t == fim + 80) print(mc, "fogo_primeira_pessoa");
        if (t == fim + 82) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.options.hideGui = false;
            List<String> todas = new ArrayList<>();
            for (TipoAura a : TipoAura.values()) todas.add(a.id);
            mc.setScreen(new TelaAuras(TipoAura.COSMICA, todas));
        }
        if (t == fim + 130) print(mc, "menu");
        if (t == fim + 135) {
            Auras.LOG.info("[autoteste] FIM");
            mc.stop();
        }
    }

    private static void abrirMundo(Minecraft mc) {
        if (mc.getLevelSource().levelExists(MUNDO)) {
            mc.createWorldOpenFlows().openWorld(MUNDO, () -> mc.setScreen(new TitleScreen()));
            return;
        }
        GameRules regras = new GameRules();
        regras.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        regras.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        regras.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings ajustes = new LevelSettings(MUNDO, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, regras,
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(MUNDO, ajustes, new WorldOptions(20261019L, false, false),
                reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value()
                        .createWorldDimensions(), new TitleScreen());
    }

    private static void preparar(Minecraft mc) {
        long hora = Long.getLong("auras.autoteste.hora", 13000L);
        noServidor(mc, p -> {
            MinecraftServer s = p.server;
            for (String cmd : List.of("time set " + hora, "weather clear", "gamerule doDaylightCycle false",
                    "tp @s 0.5 -60 0.5 200 -24")) {
                s.getCommands().performPrefixedCommand(p.createCommandSourceStack().withSuppressedOutput().withPermission(4), cmd);
            }
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        });
        mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        mc.options.hideGui = true;
        mc.options.fov().set(Integer.getInteger("auras.autoteste.fov", 70));
    }

    private static void quadro(Minecraft mc, String pasta) {
        if (RenderAuras.quadros == ultimoQuadro) return;
        ultimoQuadro = RenderAuras.quadros;
        // Le a tela aqui (thread de render) e reduz/salva em outra thread para nao derrubar o FPS
        NativeImage tela = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        Path arq = mc.gameDirectory.toPath().resolve("quadros").resolve(pasta)
                .resolve(String.format("%03d.png", numeroQuadro++));
        Util.ioPool().execute(() -> {
            int w = 640, h = Math.round(640f * tela.getHeight() / tela.getWidth());
            try (tela; NativeImage menor = new NativeImage(w, h, false)) {
                tela.resizeSubRectTo(0, 0, tela.getWidth(), tela.getHeight(), menor);
                Files.createDirectories(arq.getParent());
                menor.writeToFile(arq);
            } catch (Exception e) {
                Auras.LOG.error("[autoteste] quadro falhou", e);
            }
        });
    }

    private static void print(Minecraft mc, String nome) {
        Screenshot.grab(mc.gameDirectory, "auras_" + nome + ".png", mc.getMainRenderTarget(),
                msg -> Auras.LOG.info("[autoteste] print {}: {}", nome, msg.getString()));
    }

    private static void noServidor(Minecraft mc, Consumer<ServerPlayer> acao) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return;
        java.util.UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) acao.accept(p);
        });
    }

    private AutoTeste() {
    }
}
