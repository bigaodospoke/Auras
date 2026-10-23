package com.auras.cliente;

import com.auras.Auras;
import com.auras.Plataforma;
import com.auras.TipoAura;
import com.auras.cliente.efeito.EstadoAura;
import com.auras.cliente.tela.TelaAuras;
import com.auras.rede.Pacotes;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Lado do cliente: quem esta com qual aura, e o estado animado de cada uma. */
public final class AurasCliente implements Pacotes.ReceptorCliente {
    private static final AurasCliente INSTANCIA = new AurasCliente();

    public static final KeyMapping TECLA_MENU = new KeyMapping("key.auras.menu", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, "key.categories.auras");

    private final Map<UUID, TipoAura> auras = new HashMap<>();
    private final Map<UUID, EstadoAura> estados = new HashMap<>();
    private final Set<UUID> anunciar = new HashSet<>();
    private final Set<UUID> vistos = new HashSet<>();

    public static AurasCliente get() {
        return INSTANCIA;
    }

    /** Chamado pelo loader quando o cliente inicia. */
    public static void iniciar() {
        Pacotes.cliente = INSTANCIA;
        ConfigCliente.carregar();
    }

    @Nullable
    public TipoAura auraDe(UUID jogador) {
        return auras.get(jogador);
    }

    public boolean temEstados() {
        return !estados.isEmpty();
    }

    /** Estado da aura desse jogador, se ela deve aparecer agora. */
    @Nullable
    public EstadoAura estado(Player p) {
        return estados.get(p.getUUID());
    }

    @Override
    public void aura(Pacotes.Aura pacote) {
        TipoAura tipo = TipoAura.porId(pacote.aura());
        if (tipo == null) auras.remove(pacote.jogador());
        else auras.put(pacote.jogador(), tipo);
        estados.remove(pacote.jogador());
        if (pacote.anunciar() && tipo != null) anunciar.add(pacote.jogador());
    }

    @Override
    public void todas(Pacotes.Todas pacote) {
        auras.clear();
        estados.clear();
        pacote.auras().forEach((uuid, id) -> {
            TipoAura t = TipoAura.porId(id);
            if (t != null) auras.put(uuid, t);
        });
    }

    @Override
    public void menu(Pacotes.Menu pacote) {
        Minecraft.getInstance().setScreen(new TelaAuras(TipoAura.porId(pacote.atual()), pacote.liberadas()));
    }

    public void desconectar() {
        auras.clear();
        estados.clear();
        anunciar.clear();
    }

    /** Fim de cada tick do cliente. */
    public void tick(Minecraft mc) {
        while (TECLA_MENU.consumeClick()) pedirMenu(mc);
        if (mc.level == null || mc.player == null) {
            if (!estados.isEmpty()) estados.clear();
            return;
        }
        ConfigCliente cfg = ConfigCliente.get();
        double max = cfg.distancia;
        vistos.clear();
        for (AbstractClientPlayer p : mc.level.players()) {
            TipoAura tipo = auras.get(p.getUUID());
            if (tipo == null) continue;
            boolean eu = p == mc.player;
            boolean primeiraPessoa = eu && mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == p;
            if (!eu && !cfg.mostrarOutros) continue;
            if (primeiraPessoa && !cfg.primeiraPessoa) continue;
            if (p.isSpectator() || p.isSleeping() || p.isInvisibleTo(mc.player)) continue;
            double dist = eu ? 0 : p.distanceTo(mc.player);
            if (dist > max) continue;

            EstadoAura e = estados.get(p.getUUID());
            if (e == null || e.tipo != tipo) {
                e = new EstadoAura(tipo);
                e.anunciar = anunciar.remove(p.getUUID());
                e.corpo.copiar(p, primeiraPessoa);
                estados.put(p.getUUID(), e);
            }
            e.corpo.copiar(p, primeiraPessoa);
            // Longe = menos particulas
            float lod = dist < 16 ? 1f : dist < 32 ? 0.6f : 0.35f;
            e.densidade = cfg.densidade * lod;
            e.tick();
            vistos.add(p.getUUID());
        }
        Iterator<UUID> it = estados.keySet().iterator();
        while (it.hasNext()) {
            if (!vistos.contains(it.next())) it.remove();
        }
        // Quem nao estava a vista perde a animacao de ativacao
        anunciar.clear();
    }

    private void pedirMenu(Minecraft mc) {
        if (mc.player == null) return;
        if (Plataforma.cliente().servidorAceita(Pacotes.PedirMenu.TIPO)) {
            Plataforma.cliente().enviarAoServidor(Pacotes.PedirMenu.INSTANCIA);
        } else {
            mc.player.displayClientMessage(Component.translatable("msg.auras.servidor_sem_mod")
                    .withStyle(ChatFormatting.YELLOW), false);
            Auras.LOG.info("Servidor sem o mod Auras: menu indisponivel");
        }
    }

    private AurasCliente() {
    }
}
