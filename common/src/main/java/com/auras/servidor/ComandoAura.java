package com.auras.servidor;

import com.auras.TipoAura;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * /aura                      abre o menu
 * /aura usar <aura>          equipa uma aura
 * /aura desligar             tira a aura
 * /aura lista                mostra todas e quais voce pode usar
 * /aura dar <jogadores> <aura|todas>       (admin) libera aura
 * /aura tirar <jogadores> <aura|todas>     (admin) remove a liberacao
 * /aura definir <jogadores> <aura|nenhuma> (admin) coloca a aura na hora, ignorando liberacao
 * /aura recarregar                         (admin) le a config de novo
 */
public final class ComandoAura {
    private static final String TODAS = "todas";
    private static final String NENHUMA = "nenhuma";

    public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
        SuggestionProvider<CommandSourceStack> minhas = (ctx, b) -> {
            ServerPlayer p = ctx.getSource().getPlayer();
            List<String> ids = new ArrayList<>();
            for (TipoAura t : p == null ? List.of(TipoAura.values()) : GerenciadorAuras.disponiveis(p)) ids.add(t.id);
            return SharedSuggestionProvider.suggest(ids, b);
        };
        SuggestionProvider<CommandSourceStack> comTodas = (ctx, b) ->
                SharedSuggestionProvider.suggest(Stream.concat(ids(), Stream.of(TODAS)), b);
        SuggestionProvider<CommandSourceStack> comNenhuma = (ctx, b) ->
                SharedSuggestionProvider.suggest(Stream.concat(ids(), Stream.of(NENHUMA)), b);

        dispatcher.register(Commands.literal("aura")
                .executes(ctx -> menu(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("menu")
                        .executes(ctx -> menu(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("usar")
                        .then(Commands.argument("aura", StringArgumentType.word()).suggests(minhas)
                                .executes(ComandoAura::usar)))
                .then(Commands.literal("desligar")
                        .executes(ctx -> {
                            GerenciadorAuras.escolher(ctx.getSource().getPlayerOrException(), null);
                            return 1;
                        }))
                .then(Commands.literal("lista")
                        .executes(ctx -> lista(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("dar").requires(ComandoAura::admin)
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .then(Commands.argument("aura", StringArgumentType.word()).suggests(comTodas)
                                        .executes(ctx -> liberar(ctx, true)))))
                .then(Commands.literal("tirar").requires(ComandoAura::admin)
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .then(Commands.argument("aura", StringArgumentType.word()).suggests(comTodas)
                                        .executes(ctx -> liberar(ctx, false)))))
                .then(Commands.literal("definir").requires(ComandoAura::admin)
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .then(Commands.argument("aura", StringArgumentType.word()).suggests(comNenhuma)
                                        .executes(ComandoAura::definir))))
                .then(Commands.literal("recarregar").requires(ComandoAura::admin)
                        .executes(ctx -> {
                            GerenciadorAuras.recarregar(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.translatable("msg.auras.recarregada")
                                    .withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        })));
    }

    private static boolean admin(CommandSourceStack fonte) {
        return fonte.hasPermission(GerenciadorAuras.config().nivelAdmin);
    }

    private static Stream<String> ids() {
        return Stream.of(TipoAura.values()).map(t -> t.id);
    }

    private static int menu(ServerPlayer jogador) {
        GerenciadorAuras.abrirMenu(jogador);
        return 1;
    }

    private static int usar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer jogador = ctx.getSource().getPlayerOrException();
        TipoAura tipo = TipoAura.porId(StringArgumentType.getString(ctx, "aura"));
        if (tipo == null) return desconhecida(ctx);
        GerenciadorAuras.escolher(jogador, tipo);
        return 1;
    }

    private static int lista(ServerPlayer jogador) {
        TipoAura atual = GerenciadorAuras.equipada(jogador);
        jogador.sendSystemMessage(Component.translatable("msg.auras.lista_titulo").withStyle(ChatFormatting.GOLD));
        for (TipoAura t : TipoAura.values()) {
            MutableComponent linha = Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY).append(t.nome());
            if (t == atual) {
                linha.append(Component.translatable("msg.auras.lista_equipada").withStyle(ChatFormatting.GREEN));
            } else if (!GerenciadorAuras.podeUsar(jogador, t)) {
                linha.append(Component.translatable("msg.auras.lista_bloqueada").withStyle(ChatFormatting.RED));
            }
            jogador.sendSystemMessage(linha);
        }
        return 1;
    }

    private static int liberar(CommandContext<CommandSourceStack> ctx, boolean dar) throws CommandSyntaxException {
        Collection<ServerPlayer> alvos = EntityArgument.getPlayers(ctx, "jogadores");
        String arg = StringArgumentType.getString(ctx, "aura");
        List<TipoAura> tipos;
        if (arg.equals(TODAS)) {
            tipos = List.of(TipoAura.values());
        } else {
            TipoAura t = TipoAura.porId(arg);
            if (t == null) return desconhecida(ctx);
            tipos = List.of(t);
        }
        DadosAuras dados = DadosAuras.de(ctx.getSource().getServer());
        for (ServerPlayer p : alvos) {
            boolean mudou = false;
            for (TipoAura t : tipos) mudou |= dar ? dados.liberar(p.getUUID(), t) : dados.tirar(p.getUUID(), t);
            if (dar && mudou) {
                p.sendSystemMessage(Component.translatable("msg.auras.ganhou", nomeDe(tipos))
                        .withStyle(ChatFormatting.GREEN));
            }
            if (!dar) GerenciadorAuras.revalidar(p);
        }
        Component nome = nomeDe(tipos);
        int n = alvos.size();
        ctx.getSource().sendSuccess(() -> Component.translatable(dar ? "msg.auras.dada" : "msg.auras.tirada", nome, n), true);
        return n;
    }

    private static int definir(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> alvos = EntityArgument.getPlayers(ctx, "jogadores");
        String arg = StringArgumentType.getString(ctx, "aura");
        TipoAura tipo = null;
        if (!arg.equals(NENHUMA)) {
            tipo = TipoAura.porId(arg);
            if (tipo == null) return desconhecida(ctx);
        }
        for (ServerPlayer p : alvos) GerenciadorAuras.aplicar(p, tipo);
        Component nome = tipo == null ? Component.translatable("msg.auras.nenhuma") : tipo.nome();
        int n = alvos.size();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.auras.definida", nome, n), true);
        return n;
    }

    private static Component nomeDe(List<TipoAura> tipos) {
        return tipos.size() == 1 ? tipos.get(0).nome() : Component.translatable("msg.auras.todas");
    }

    private static int desconhecida(CommandContext<CommandSourceStack> ctx) {
        String ids = String.join(", ", ids().toList());
        ctx.getSource().sendFailure(Component.translatable("msg.auras.desconhecida", ids));
        return 0;
    }

    private ComandoAura() {
    }
}
