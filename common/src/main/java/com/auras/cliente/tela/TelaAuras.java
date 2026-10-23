package com.auras.cliente.tela;

import com.auras.Plataforma;
import com.auras.TipoAura;
import com.auras.cliente.ConfigCliente;
import com.auras.cliente.efeito.EstadoAura;
import com.auras.cliente.efeito.Sprite;
import com.auras.cliente.efeito.Sprites;
import com.auras.cliente.render.RenderAuras;
import com.auras.cliente.render.TiposRender;
import com.auras.rede.Pacotes;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Menu das auras: previa 3D do jogador com a aura, lista para escolher e as opcoes do cliente. */
public final class TelaAuras extends Screen {
    private static final float[] QUALIDADES = {0.5f, 1f, 1.5f, 2f};
    private static final int COLUNAS = 2;

    @Nullable
    private TipoAura atual;
    private TipoAura selecionada;
    private final Set<TipoAura> liberadas = EnumSet.noneOf(TipoAura.class);
    private EstadoAura previa;

    private int px, py, pw, ph;
    private int boxX, boxY, boxW, boxH;
    private int gradeX, gradeY, cartaoW, cartaoH;
    private float giro;
    private boolean arrastando;
    private Button botaoEquipar, botaoDesligar;

    public TelaAuras(@Nullable TipoAura atual, List<String> liberadas) {
        super(Component.translatable("gui.auras.titulo"));
        this.atual = atual;
        for (String id : liberadas) {
            TipoAura t = TipoAura.porId(id);
            if (t != null) this.liberadas.add(t);
        }
        this.selecionada = atual != null ? atual : TipoAura.values()[0];
        trocarPrevia(false);
    }

    private void trocarPrevia(boolean anunciar) {
        previa = new EstadoAura(selecionada);
        previa.anunciar = anunciar;
        previa.corpo.parado(180f);
    }

    @Override
    protected void init() {
        pw = Math.min(460, width - 16);
        ph = Math.min(262, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        int esquerda = (int) (pw * 0.4f);
        boxX = px + 8;
        boxY = py + 22;
        boxW = esquerda - 8;
        boxH = ph - 22 - 52;

        gradeX = px + esquerda + 8;
        gradeY = py + 22;
        int gradeW = pw - esquerda - 16;
        cartaoW = (gradeW - 4) / COLUNAS;
        int linhas = (TipoAura.values().length + COLUNAS - 1) / COLUNAS;
        cartaoH = Mth.clamp((ph - 22 - 56) / linhas - 4, 16, 28);

        int by = gradeY + linhas * (cartaoH + 4) + 4;
        int bw = (gradeW - 4) / 2;
        botaoEquipar = addRenderableWidget(Button.builder(Component.empty(), b -> equipar())
                .bounds(gradeX, by, bw, 20).build());
        botaoDesligar = addRenderableWidget(Button.builder(Component.translatable("gui.auras.desligar"), b -> desligar())
                .bounds(gradeX + bw + 4, by, bw, 20).build());

        int cy = by + 24;
        int cw = (gradeW - 8) / 3;
        addRenderableWidget(Button.builder(textoQualidade(), b -> {
            ConfigCliente cfg = ConfigCliente.get();
            cfg.densidade = QUALIDADES[(indiceQualidade() + 1) % QUALIDADES.length];
            cfg.salvar();
            b.setMessage(textoQualidade());
        }).bounds(gradeX, cy, cw, 20).build());
        addRenderableWidget(Button.builder(textoOutros(), b -> {
            ConfigCliente cfg = ConfigCliente.get();
            cfg.mostrarOutros = !cfg.mostrarOutros;
            cfg.salvar();
            b.setMessage(textoOutros());
        }).bounds(gradeX + cw + 4, cy, cw, 20).build());
        addRenderableWidget(Button.builder(textoSons(), b -> {
            ConfigCliente cfg = ConfigCliente.get();
            cfg.sons = !cfg.sons;
            cfg.salvar();
            b.setMessage(textoSons());
        }).bounds(gradeX + 2 * (cw + 4), cy, cw, 20).build());
        atualizarBotoes();
    }

    private static int indiceQualidade() {
        float d = ConfigCliente.get().densidade;
        int melhor = 0;
        for (int i = 1; i < QUALIDADES.length; i++) {
            if (Math.abs(QUALIDADES[i] - d) < Math.abs(QUALIDADES[melhor] - d)) melhor = i;
        }
        return melhor;
    }

    private static Component textoQualidade() {
        return Component.translatable("gui.auras.qualidade", Component.translatable("gui.auras.qualidade." + indiceQualidade()));
    }

    private static Component simNao(boolean v) {
        return Component.translatable(v ? "gui.auras.sim" : "gui.auras.nao");
    }

    private static Component textoOutros() {
        return Component.translatable("gui.auras.outros", simNao(ConfigCliente.get().mostrarOutros));
    }

    private static Component textoSons() {
        return Component.translatable("gui.auras.sons", simNao(ConfigCliente.get().sons));
    }

    private void atualizarBotoes() {
        boolean pode = liberadas.contains(selecionada);
        boolean equipada = selecionada == atual;
        botaoEquipar.setMessage(Component.translatable(equipada ? "gui.auras.equipada" : "gui.auras.equipar"));
        botaoEquipar.active = pode && !equipada;
        botaoEquipar.setTooltip(pode ? null : Tooltip.create(Component.translatable("gui.auras.bloqueada_dica")));
        botaoDesligar.active = atual != null;
    }

    private void equipar() {
        if (!liberadas.contains(selecionada)) return;
        Plataforma.cliente().enviarAoServidor(new Pacotes.Escolher(selecionada.id));
        atual = selecionada;
        atualizarBotoes();
    }

    private void desligar() {
        Plataforma.cliente().enviarAoServidor(new Pacotes.Escolher(""));
        atual = null;
        atualizarBotoes();
    }

    @Override
    public void tick() {
        previa.densidade = ConfigCliente.get().densidade;
        previa.corpo.parado(180f);
        previa.tick();
        if (!arrastando) giro += 0.6f;
    }

    @Nullable
    private TipoAura cartaoEm(double mx, double my) {
        TipoAura[] tipos = TipoAura.values();
        for (int i = 0; i < tipos.length; i++) {
            int x = gradeX + (i % COLUNAS) * (cartaoW + 4), y = gradeY + (i / COLUNAS) * (cartaoH + 4);
            if (mx >= x && mx < x + cartaoW && my >= y && my < y + cartaoH) return tipos[i];
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int botao) {
        if (botao == 0) {
            TipoAura t = cartaoEm(mx, my);
            if (t != null) {
                if (t != selecionada) {
                    selecionada = t;
                    trocarPrevia(true);
                    atualizarBotoes();
                }
                playClick();
                return true;
            }
            if (mx >= boxX && mx < boxX + boxW && my >= boxY && my < boxY + boxH) {
                arrastando = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, botao);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int botao, double dx, double dy) {
        if (arrastando) {
            giro += (float) dx * 1.5f;
            return true;
        }
        return super.mouseDragged(mx, my, botao, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int botao) {
        arrastando = false;
        return super.mouseReleased(mx, my, botao);
    }

    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
        super.renderBackground(g, mx, my, parcial);
        // Painel
        g.fill(px, py, px + pw, py + ph, 0xD00C0C14);
        g.renderOutline(px, py, pw, ph, 0xFF2A2A3A);
        g.drawString(font, Component.literal("✦ ").withStyle(s -> s.withColor(selecionada.cor)).append(title),
                px + 8, py + 8, 0xFFFFFFFF, true);

        // Caixa da previa
        int cor = selecionada.cor;
        g.fillGradient(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF05050A, 0xFF000000 | escurecer(cor, 0.18f));
        g.renderOutline(boxX, boxY, boxW, boxH, 0xFF000000 | escurecer(cor, 0.55f));
        desenharPrevia(g, parcial);
        g.drawString(font, Component.translatable("gui.auras.dica"), boxX + 4, boxY + boxH - 11, 0x88FFFFFF, false);

        // Nome e descricao da aura selecionada
        int ty = boxY + boxH + 5;
        g.drawString(font, selecionada.nome().copy().withStyle(ChatFormatting.BOLD), boxX, ty, 0xFFFFFFFF, true);
        List<FormattedCharSequence> linhas = font.split(selecionada.descricao(), boxW);
        for (int i = 0; i < Math.min(3, linhas.size()); i++) {
            g.drawString(font, linhas.get(i), boxX, ty + 12 + i * 10, 0xFFB8B8C8, false);
        }

        // Cartoes
        TipoAura[] tipos = TipoAura.values();
        TipoAura sobre = cartaoEm(mx, my);
        for (int i = 0; i < tipos.length; i++) {
            TipoAura t = tipos[i];
            int x = gradeX + (i % COLUNAS) * (cartaoW + 4), y = gradeY + (i / COLUNAS) * (cartaoH + 4);
            boolean livre = liberadas.contains(t);
            int fundo = t == selecionada ? 0xFF000000 | escurecer(t.cor, 0.3f) : t == sobre ? 0xFF1E1E2C : 0xFF14141E;
            g.fill(x, y, x + cartaoW, y + cartaoH, fundo);
            g.renderOutline(x, y, cartaoW, cartaoH, t == selecionada ? 0xFF000000 | t.cor : 0xFF2E2E40);
            int icone = cartaoH - 6;
            if (t == TipoAura.POKEBOLA && livre) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                blitSprite(g, Sprites.POKEBOLAS[0], x + 3, y + 3, icone);
                RenderSystem.disableBlend();
            } else {
                icone(g, x + 3, y + 3, icone, livre ? t.cor : 0x555555);
            }
            int textoX = x + icone + 7;
            int textoY = y + (cartaoH - 8) / 2 - (t == atual || !livre ? 4 : 0);
            g.drawString(font, t.nome(), textoX, textoY, livre ? 0xFFFFFFFF : 0xFF707070, true);
            if (t == atual) {
                g.drawString(font, Component.translatable("gui.auras.equipada"), textoX, textoY + 9, 0xFF6BE070, false);
            } else if (!livre) {
                g.drawString(font, Component.translatable("gui.auras.bloqueada"), textoX, textoY + 9, 0xFFC05050, false);
            }
        }
    }

    /** Bolinha brilhante da cor da aura, usando o atlas de efeitos. */
    private static void icone(GuiGraphics g, int x, int y, int tam, int rgb) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, 1f);
        blitSprite(g, Sprites.BRILHO, x - tam / 4, y - tam / 4, tam + tam / 2);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        blitSprite(g, Sprites.NUCLEO, x + tam / 4, y + tam / 4, tam / 2);
        RenderSystem.disableBlend();
    }

    private static void blitSprite(GuiGraphics g, Sprite s, int x, int y, int tam) {
        int lw = Sprite.LARGURA, lh = Sprite.ALTURA;
        int u = Math.round(s.u0() * lw), v = Math.round(s.v0() * lh);
        int w = Math.round((s.u1() - s.u0()) * lw), h = Math.round((s.v1() - s.v0()) * lh);
        g.blit(TiposRender.ATLAS, x, y, tam, tam, u, v, w, h, lw, lh);
    }

    private static int escurecer(int rgb, float f) {
        int r = (int) ((rgb >> 16 & 255) * f), gg = (int) ((rgb >> 8 & 255) * f), b = (int) ((rgb & 255) * f);
        return r << 16 | gg << 8 | b;
    }

    private void desenharPrevia(GuiGraphics g, float parcial) {
        LocalPlayer p = minecraft == null ? null : minecraft.player;
        float escala = Math.min(boxW / 3.4f, boxH / 3.6f);
        float x = boxX + boxW / 2f, y = boxY + boxH * 0.72f;
        float ang = (giro + (arrastando ? 0 : parcial * 0.6f)) * Mth.DEG_TO_RAD;
        Quaternionf rot = new Quaternionf().rotateZ(Mth.PI).rotateX(-0.32f).rotateY(ang);

        g.enableScissor(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + boxH - 1);
        if (p != null) {
            float corpo = p.yBodyRot, corpoO = p.yBodyRotO, yaw = p.getYRot(), pitch = p.getXRot();
            float cabeca = p.yHeadRot, cabecaO = p.yHeadRotO;
            p.yBodyRot = p.yBodyRotO = 180f;
            p.setYRot(180f);
            p.setXRot(0f);
            p.yHeadRot = p.yHeadRotO = 180f;
            InventoryScreen.renderEntityInInventory(g, x, y, escala, new Vector3f(), rot, null, p);
            p.yBodyRot = corpo;
            p.yBodyRotO = corpoO;
            p.setYRot(yaw);
            p.setXRot(pitch);
            p.yHeadRot = cabeca;
            p.yHeadRotO = cabecaO;
        }
        RenderAuras.gui(g, previa, x, y, escala, rot, parcial);
        g.disableScissor();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
