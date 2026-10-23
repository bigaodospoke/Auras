package com.auras.cliente.efeito;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/** Onde esta o dono da aura: posicao dos pes, tamanho e direcao do corpo. */
public final class Corpo {
    public double x, y, z;
    public double xo, yo, zo;
    public float altura = 1.8f;
    public float largura = 0.6f;
    /** Direcao do corpo em graus (igual ao yBodyRot do Minecraft). */
    public float yaw, yawO;
    public boolean noChao = true;
    public boolean primeiraPessoa;
    /** Prevista do menu: sem mundo, o corpo fica parado na origem. */
    public boolean gui;

    public void copiar(Player p, boolean primeiraPessoa) {
        xo = x;
        yo = y;
        zo = z;
        x = p.getX();
        y = p.getY();
        z = p.getZ();
        if (p.tickCount <= 1) {
            xo = x;
            yo = y;
            zo = z;
        }
        altura = p.getPose() == Pose.SLEEPING ? 0.4f : p.getBbHeight();
        largura = p.getBbWidth();
        yawO = yaw;
        yaw = p.yBodyRot;
        noChao = p.onGround();
        this.primeiraPessoa = primeiraPessoa;
        gui = false;
    }

    public void parado(float yawCorpo) {
        x = y = z = xo = yo = zo = 0;
        altura = 1.8f;
        largura = 0.6f;
        yaw = yawO = yawCorpo;
        noChao = true;
        primeiraPessoa = false;
        gui = true;
    }

    public double x(float parcial) {
        return Mth.lerp(parcial, xo, x);
    }

    public double y(float parcial) {
        return Mth.lerp(parcial, yo, y);
    }

    public double z(float parcial) {
        return Mth.lerp(parcial, zo, z);
    }

    public float yaw(float parcial) {
        return Mth.rotLerp(parcial, yawO, yaw);
    }

    /** Quanto andou no ultimo tick (blocos). */
    public double velocidade() {
        double dx = x - xo, dz = z - zo;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
