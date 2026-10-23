package com.auras.cliente.efeito;

/** Indices aqui precisam bater com ferramentas/GerarTexturas.java. */
public final class Sprites {
    public static final Sprite BRILHO = Sprite.celula(0);
    public static final Sprite NUCLEO = Sprite.celula(1);
    public static final Sprite ESTRELA = Sprite.celula(2);
    public static final Sprite[] CHAMAS = {Sprite.celula(3), Sprite.celula(4), Sprite.celula(5), Sprite.celula(6)};
    public static final Sprite[] FUMACAS = {Sprite.celula(7), Sprite.celula(8), Sprite.celula(20)};
    public static final Sprite ANEL = Sprite.celula(9);
    public static final Sprite FLOCO = Sprite.celula(10);
    public static final Sprite[] FOLHAS = {Sprite.celula(11), Sprite.celula(19)};
    public static final Sprite PETALA = Sprite.celula(12);
    public static final Sprite BOLHA = Sprite.celula(13);
    public static final Sprite CRISTAL = Sprite.celula(14);
    public static final Sprite FAISCA = Sprite.celula(15);
    public static final Sprite FEIXE = Sprite.celula(16);
    public static final Sprite PENA = Sprite.celula(17);
    public static final Sprite PONTO = Sprite.celula(18);
    public static final Sprite ESTRELA_SUAVE = Sprite.celula(21);
    public static final Sprite ANEL_FINO = Sprite.celula(22);
    /** Coloridas: poke, great, ultra, master. */
    public static final Sprite[] POKEBOLAS = {Sprite.celula(23), Sprite.celula(24), Sprite.celula(25), Sprite.celula(26)};

    public static final Sprite CIRCULO_RUNAS = Sprite.grande(0);
    public static final Sprite CIRCULO_GELO = Sprite.grande(1);
    public static final Sprite LAVA = Sprite.grande(2);
    public static final Sprite VORTICE = Sprite.grande(3);
    public static final Sprite CIRCULO_ESTRELAS = Sprite.grande(4);
    public static final Sprite CIRCULO_NATUREZA = Sprite.grande(5);
    public static final Sprite ASA = Sprite.grande(6);
    public static final Sprite ANEL_SUAVE = Sprite.grande(7);
    public static final Sprite CIRCULO_RAIOS = Sprite.grande(8);
    public static final Sprite CIRCULO_FOGO = Sprite.grande(9);
    public static final Sprite EMBLEMA_POKEBOLA = Sprite.grande(10);
    /** Morcego batendo as asas: cima, meio, baixo, meio. */
    public static final Sprite[] MORCEGO = {Sprite.celula(27), Sprite.celula(28), Sprite.celula(29), Sprite.celula(30)};
    public static final Sprite LUA = Sprite.celula(31);
    public static final Sprite RACHADURA = Sprite.grande(11);
    public static final Sprite SELO_VAMPIRO = Sprite.grande(12);
    public static final Sprite CIRCULO_NECRO = Sprite.grande(13);

    private Sprites() {
    }
}
