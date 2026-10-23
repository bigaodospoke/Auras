package com.auras.cliente.efeito;

/** Um pedaco do atlas de efeitos (textures/efeitos/atlas.png, gerado por ferramentas/GerarTexturas.java). */
public record Sprite(float u0, float v0, float u1, float v1) {
    public static final int LARGURA = 1024;
    public static final int ALTURA = 2048;
    private static final int CEL = 64;

    /** Celula pequena (64px) no indice i da grade de 16 colunas (linhas 0 a 3). */
    static Sprite celula(int i) {
        int c = i % 16, l = i / 16;
        return area(c * CEL, l * CEL, CEL, CEL);
    }

    /** Sprite grande (256px). Os grandes ocupam blocos 4x4 a partir da linha 4, quatro por faixa. */
    static Sprite grande(int b) {
        int c = (b % 4) * 4, l = 4 + (b / 4) * 4;
        return area(c * CEL, l * CEL, CEL * 4, CEL * 4);
    }

    private static Sprite area(int x, int y, int w, int h) {
        return new Sprite((x + 0.5f) / LARGURA, (y + 0.5f) / ALTURA, (x + w - 0.5f) / LARGURA, (y + h - 0.5f) / ALTURA);
    }
}
