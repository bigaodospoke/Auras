import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Gera o atlas de efeitos das auras (1024x2048) e o icone do mod.
 * Rodar da raiz do projeto:  java ferramentas/GerarTexturas.java
 *
 * Indices das celulas precisam bater com common/.../cliente/efeito/Sprites.java.
 * Celulas pequenas: 64px numa grade 16x16. Grandes: 256px (4x4 celulas) a partir da linha 4.
 * As texturas sao brancas (a cor vem do vertice) exceto lava e mapa estelar, que ja vem coloridas.
 */
public final class GerarTexturas {
    static final int TAM = 1024, ALT = 2048, CEL = 64;
    static final float[] R = new float[TAM * ALT], G = new float[TAM * ALT], B = new float[TAM * ALT], A = new float[TAM * ALT];

    interface Fn {
        /** x, y de -1 a 1 (y para cima). Escreve rgba (alfa reto) em out. */
        void f(float x, float y, float[] out);
    }

    public static void main(String[] args) throws Exception {
        String raiz = args.length > 0 ? args[0] : ".";
        File atlas = new File(raiz, "common/src/main/resources/assets/auras/textures/efeitos/atlas.png");
        File icone = new File(raiz, "common/src/main/resources/assets/auras/icon.png");

        celula(0, (x, y, o) -> branco(o, brilho(len(x, y))));
        celula(1, (x, y, o) -> {
            float r = len(x, y);
            branco(o, borda(r) * Math.max(gauss(r, 0.16f), 0.55f * gauss(r, 0.45f)));
        });
        celula(2, (x, y, o) -> branco(o, estrela4(x, y)));
        for (int i = 0; i < 4; i++) {
            final int v = i;
            celula(3 + i, (x, y, o) -> branco(o, chama(x, y, v)));
        }
        celula(7, (x, y, o) -> branco(o, fumaca(x, y, 1.3f)));
        celula(8, (x, y, o) -> branco(o, fumaca(x, y, 7.9f)));
        celula(20, (x, y, o) -> branco(o, fumaca(x, y, 15.1f)));
        celula(9, (x, y, o) -> {
            float r = len(x, y);
            branco(o, borda(r) * clamp(gauss(r - 0.72f, 0.07f) + 0.35f * gauss(r - 0.72f, 0.2f)));
        });
        celula(10, (x, y, o) -> branco(o, floco(x, y)));
        celula(11, (x, y, o) -> folha(x, y, o, false));
        celula(19, (x, y, o) -> folha(x, y, o, true));
        celula(12, (x, y, o) -> petala(x, y, o));
        celula(13, (x, y, o) -> {
            float r = len(x, y);
            float a = gauss(r - 0.68f, 0.07f) + 0.15f * (r < 0.68f ? 1 : 0) + gauss(len(x + 0.3f, y - 0.3f), 0.1f);
            branco(o, borda(r) * clamp(a));
        });
        celula(14, (x, y, o) -> cristal(x, y, o));
        celula(15, (x, y, o) -> {
            float k = Math.max(0, 1 - x * x);
            branco(o, clamp(gauss(y, 0.09f) * (float) Math.pow(k, 1.5) + 0.3f * gauss(y, 0.3f) * k * k) * borda(Math.abs(y) * 0.95f));
        });
        celula(16, (x, y, o) -> {
            float ay = Math.abs(y);
            branco(o, clamp(gauss(y, 0.13f) + 0.5f * gauss(y, 0.42f)) * (1 - suave(0.82f, 0.98f, ay)));
        });
        celula(17, (x, y, o) -> pena(x, y, o));
        celula(18, (x, y, o) -> {
            float r = len(x, y);
            branco(o, borda(r) * clamp(1 - suave(0.3f, 0.42f, r) + 0.3f * gauss(r, 0.5f)));
        });
        celula(21, (x, y, o) -> {
            float a = estrela4(x, y);
            float xr = (x + y) * 0.7071f, yr = (y - x) * 0.7071f;
            branco(o, clamp(a + 0.5f * estrela4(xr * 1.4f, yr * 1.4f)));
        });
        celula(22, (x, y, o) -> {
            float r = len(x, y);
            branco(o, borda(r) * clamp(gauss(r - 0.8f, 0.035f) + 0.4f * gauss(r - 0.8f, 0.12f)));
        });

        grande(0, circuloRunas());
        grande(1, circuloGelo());
        grandeFn(2, GerarTexturas::lava);
        grandeFn(3, GerarTexturas::vortice);
        grande(4, circuloEstrelas());
        grande(5, circuloNatureza());
        grande(6, asa());
        grandeFn(7, (x, y, o) -> {
            float r = len(x, y);
            branco(o, (1 - suave(0.85f, 0.99f, r)) * clamp(0.6f * gauss(r - 0.72f, 0.15f) + 0.3f * (1 - suave(0f, 0.9f, r))));
        });
        grande(8, circuloRaios());
        grande(9, circuloFogo());

        // Pokebolas (coloridas): poke, great, ultra, master
        for (int i = 0; i < 4; i++) {
            final int tipo = i;
            celula(23 + i, (x, y, o) -> pokebola(x, y, tipo, o));
        }
        grande(10, emblemaPokebola());

        // Morcego astral batendo as asas (4 quadros) e lua crescente
        float[] batidas = {-30f, -4f, 22f, -4f};
        for (int i = 0; i < 4; i++) celulaImagem(27 + i, comBrilho(morcego(128, batidas[i]), 3f, 0.8f));
        celula(31, (x, y, o) -> {
            float lua = (1 - suave(0.72f, 0.75f, len(x, y))) * suave(0.6f, 0.63f, len(x - 0.28f, y - 0.16f));
            branco(o, clamp(lua + 0.25f * gauss(len(x, y) - 0.6f, 0.15f) * borda(len(x, y))));
        });
        grande(11, rachadura());
        grande(12, seloVampiro());
        grande(13, circuloNecro());

        atlas.getParentFile().mkdirs();
        ImageIO.write(paraImagem(R, G, B, A, TAM, ALT), "png", atlas);
        ImageIO.write(icone(), "png", icone);
        System.out.println("Atlas salvo em " + atlas.getAbsolutePath());
        System.out.println("Icone salvo em " + icone.getAbsolutePath());
    }

    // ------------------------------------------------------------ utilidades

    static float clamp(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    static float len(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    static float gauss(float d, float s) {
        return (float) Math.exp(-(d * d) / (2 * s * s));
    }

    static float suave(float a, float b, float v) {
        float t = clamp((v - a) / (b - a));
        return t * t * (3 - 2 * t);
    }

    /** Zera perto da borda da celula para o filtro linear nao vazar no vizinho. */
    static float borda(float r) {
        return 1 - suave(0.86f, 0.98f, r);
    }

    static float brilho(float r) {
        return borda(r) * (float) Math.exp(-r * r * 5.0);
    }

    static void branco(float[] o, float a) {
        o[0] = o[1] = o[2] = 1;
        o[3] = clamp(a);
    }

    static float hash(int x, int y, int s) {
        int h = x * 374761393 + y * 668265263 + s * 982451653;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    static float ruido(float x, float y, int s) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float fx = x - xi, fy = y - yi;
        float u = fx * fx * (3 - 2 * fx), v = fy * fy * (3 - 2 * fy);
        float a = hash(xi, yi, s), b = hash(xi + 1, yi, s), c = hash(xi, yi + 1, s), d = hash(xi + 1, yi + 1, s);
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
    }

    static float fbm(float x, float y, int s) {
        float soma = 0, amp = 0.5f, f = 1;
        for (int i = 0; i < 5; i++) {
            soma += amp * ruido(x * f, y * f, s + i * 17);
            f *= 2;
            amp *= 0.5f;
        }
        return soma;
    }

    /** Distancia do ponto ao segmento. */
    static float segmento(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay;
        float t = clamp(((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy + 1e-9f));
        return len(px - ax - dx * t, py - ay - dy * t);
    }

    // ------------------------------------------------------------ formas pequenas

    static float estrela4(float x, float y) {
        float ax = Math.abs(x), ay = Math.abs(y);
        float raioH = (float) Math.exp(-ay * 28) * (float) Math.pow(Math.max(0, 1 - ax), 2.2);
        float raioV = (float) Math.exp(-ax * 28) * (float) Math.pow(Math.max(0, 1 - ay), 2.2);
        float r = len(x, y);
        return borda(r * 0.97f) * clamp(raioH + raioV + gauss(r, 0.12f) + 0.25f * gauss(r, 0.35f));
    }

    static float chama(float x, float y, int v) {
        float seed = v * 1.7f;
        // a ponta balanca de um jeito diferente em cada variacao
        float s = clamp((y + 0.6f) / 1.5f);
        x += (float) Math.sin(y * 3.2f + seed) * 0.13f * s * s + (v - 1.5f) * 0.05f * s * s;
        float sx = 0.42f * (1 - 0.72f * suave(-0.35f, 0.95f, y));
        float yy = y + 0.3f;
        float sy = yy < 0 ? 0.42f : 1.05f;
        float a = (float) Math.exp(-((x / sx) * (x / sx) + (yy / sy) * (yy / sy)) * 2.4f);
        // linguinhas secundarias
        float lx = x + (v % 2 == 0 ? 0.26f : -0.26f), ly = y + 0.05f;
        float a2 = (float) Math.exp(-((lx / 0.16f) * (lx / 0.16f) + ((ly + 0.1f) / 0.45f) * ((ly + 0.1f) / 0.45f)) * 2.4f);
        a = Math.max(a, 0.65f * a2);
        a *= 0.85f + 0.15f * fbm(x * 3 + seed, y * 3 - seed, 3 + v);
        return clamp(a * 1.25f) * borda(len(x * 0.9f, y));
    }

    static float fumaca(float x, float y, float seed) {
        float r = len(x, y);
        float n = fbm(x * 2.2f + seed, y * 2.2f - seed, (int) (seed * 10));
        float forma = 1 - suave(0.35f + 0.35f * n, 0.95f, r);
        return borda(r) * clamp(forma * (0.45f + 0.55f * n)) * 0.9f;
    }

    static float floco(float x, float y) {
        float d = 10;
        for (int k = 0; k < 6; k++) {
            double ang = Math.PI / 2 + k * Math.PI / 3;
            float cx = (float) Math.cos(ang), cy = (float) Math.sin(ang);
            d = Math.min(d, segmento(x, y, 0, 0, cx * 0.86f, cy * 0.86f));
            for (float pos : new float[]{0.42f, 0.64f}) {
                float bx = cx * pos, by = cy * pos;
                float comp = pos < 0.5f ? 0.24f : 0.17f;
                for (int lado = -1; lado <= 1; lado += 2) {
                    double a2 = ang + lado * Math.PI / 4;
                    d = Math.min(d, segmento(x, y, bx, by, bx + (float) Math.cos(a2) * comp, by + (float) Math.sin(a2) * comp));
                }
            }
        }
        float linha = 1 - suave(0.035f, 0.065f, d);
        float centro = 1 - suave(0.1f, 0.14f, len(x, y));
        return clamp(Math.max(linha, centro) + 0.35f * gauss(d, 0.08f)) * borda(len(x, y));
    }

    static void folha(float x, float y, float[] o, boolean redonda) {
        // folha na diagonal para caber maior na celula
        float u = (x - y) * 0.7071f, v = (x + y) * 0.7071f;
        float yy = v / 0.95f;
        float w = redonda
                ? 0.5f * (float) Math.pow(Math.max(0, 1 - yy * yy), 0.6) * (yy > 0 ? 1 - 0.25f * yy : 1)
                : 0.36f * (float) Math.pow(Math.max(0, 1 - yy * yy), 0.85);
        float dentro = 1 - suave(w - 0.03f, w + 0.01f, Math.abs(u));
        float cabo = (yy < -0.85f && yy > -1.05f && Math.abs(u) < 0.025f) ? 1 : 0;
        float nervura = 1 - 0.3f * (1 - suave(0.01f, 0.035f, Math.abs(u)));
        float veia = 1 - 0.15f * (1 - suave(0.0f, 0.03f, Math.abs(((Math.abs(u) * 1.2f - yy) * 6) % 1 - 0.5f) / 6));
        float luz = (0.82f + 0.18f * (u / Math.max(w, 0.01f))) * nervura * veia;
        o[0] = o[1] = o[2] = clamp(luz);
        o[3] = clamp(Math.max(dentro, cabo));
    }

    static void petala(float x, float y, float[] o) {
        float yy = (y - 0.05f) / 0.88f;
        float w = 0.48f * (float) Math.sqrt(Math.max(0, 1 - yy * yy)) * suave(-1.05f, -0.2f, yy);
        // entalhe em cima
        float entalhe = yy > 0.75f ? suave(0.0f, 0.12f, Math.abs(x)) : 1;
        float dentro = (1 - suave(w - 0.03f, w + 0.01f, Math.abs(x))) * entalhe;
        float luz = 0.8f + 0.2f * suave(-0.8f, 0.6f, yy);
        o[0] = o[1] = o[2] = luz;
        o[3] = clamp(dentro);
    }

    static void cristal(float x, float y, float[] o) {
        float ax = Math.abs(x), ay = Math.abs(y);
        // prisma hexagonal visto de lado: corpo reto e pontas
        float meia = 0.3f * (ay < 0.5f ? 1 : Math.max(0, 1 - (ay - 0.5f) / 0.45f));
        float d = meia - ax;
        float dentro = suave(-0.01f, 0.02f, d);
        float aresta = gauss(d, 0.025f) * (meia > 0.01f ? 1 : 0) + gauss(x, 0.02f) * 0.5f * (meia > 0 ? 1 : 0);
        float face = x < 0 ? 0.65f : 0.4f;
        float a = clamp(dentro * face + aresta);
        o[0] = o[1] = o[2] = 1;
        o[3] = a * borda(len(x * 1.5f, y) * 0.95f);
    }

    static void pena(float x, float y, float[] o) {
        float xx = x + 0.12f * y * y - 0.05f;
        float yy = y;
        float haste = (1 - suave(0.015f, 0.035f, Math.abs(xx))) * (yy > -0.95f && yy < 0.9f ? 1 : 0);
        float t = (yy + 0.75f) / 1.65f;
        float w = 0.32f * (float) Math.sin(Math.PI * clamp(t)) * (t > 0 && t < 1 ? 1 : 0);
        float dentro = 1 - suave(w - 0.03f, w + 0.005f, Math.abs(xx));
        float barbas = 0.75f + 0.25f * (float) Math.sin((yy + Math.abs(xx) * 0.9f) * 46);
        o[0] = o[1] = o[2] = 1;
        o[3] = clamp(Math.max(haste, dentro * barbas * 0.9f));
    }

    static void cor(float[] o, int rgb) {
        o[0] = (rgb >> 16 & 255) / 255f;
        o[1] = (rgb >> 8 & 255) / 255f;
        o[2] = (rgb & 255) / 255f;
    }

    /** Pokebola vista de frente, com sombreado de esfera. tipo: 0 poke, 1 great, 2 ultra, 3 master. */
    static void pokebola(float x, float y, int tipo, float[] o) {
        float r = len(x, y), R = 0.86f;
        o[3] = 1 - suave(R - 0.012f, R + 0.012f, r);
        if (o[3] <= 0) {
            o[0] = o[1] = o[2] = 1;
            return;
        }
        int preto = 0x1C1C22;
        int c;
        if (r > R - 0.065f || Math.abs(y) < 0.065f) {
            c = preto;
        } else if (y < 0) {
            c = 0xF2F2F2;
        } else {
            c = switch (tipo) {
                case 1 -> 0x2F6BE0;
                case 2 -> 0x2E2E34;
                case 3 -> 0x7A3FC9;
                default -> 0xE8383A;
            };
            float ax = Math.abs(x);
            if (tipo == 1) {
                // listras vermelhas inclinadas da great ball
                float u = ax - 0.2f * y;
                if (u > 0.3f && u < 0.52f && y > 0.16f) c = 0xE8383A;
            } else if (tipo == 2) {
                // faixas amarelas da ultra ball
                if (ax > 0.27f && ax < 0.5f && y < 0.66f) c = 0xF5C518;
            } else if (tipo == 3) {
                // bolinhas rosa e o M da master ball
                if (len(ax - 0.44f, y - 0.4f) < 0.13f) c = 0xF062C0;
                float m = Math.min(Math.min(segmento(x, y, -0.15f, 0.3f, -0.15f, 0.58f), segmento(x, y, -0.15f, 0.58f, 0f, 0.42f)),
                        Math.min(segmento(x, y, 0f, 0.42f, 0.15f, 0.58f), segmento(x, y, 0.15f, 0.58f, 0.15f, 0.3f)));
                if (m < 0.035f) c = 0xFFFFFF;
            }
        }
        // botao do meio
        if (r < 0.25f) c = preto;
        if (r < 0.18f) c = 0xF5F5F5;
        if (r < 0.13f && r > 0.11f) c = 0xC8C8CC;
        cor(o, c);
        // sombreado de esfera e brilho especular
        float luz = 0.7f + 0.35f * clamp(1 - len(x + 0.35f, y - 0.35f) / 1.35f);
        float spec = 0.55f * gauss(len(x + 0.32f, y - 0.42f), 0.09f);
        for (int k = 0; k < 3; k++) o[k] = clamp(o[k] * luz + spec);
    }

    /** Morcego de frente com as asas abertas; batida = altura da ponta da asa (negativo = para cima). */
    static BufferedImage morcego(int tam, float batida) {
        BufferedImage img = nova(tam);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.scale(tam / 128.0, tam / 128.0);
        g.setColor(new Color(1f, 1f, 1f, 0.95f));
        for (int lado = -1; lado <= 1; lado += 2) {
            double ox = 64, ombroY = 56, pontaY = 50 + batida;
            double px = ox + lado * 54;
            GeneralPath asa = new GeneralPath();
            asa.moveTo(ox + lado * 5, ombroY);
            asa.quadTo(ox + lado * 30, pontaY - 16, px, pontaY);
            // borda de tras recortada (os "dedos" da asa)
            double[][] dedos = {{0.3, 10}, {0.62, 12}, {1.0, 18}};
            double ax = px, ay = pontaY;
            for (double[] d : dedos) {
                double bx = px + (ox + lado * 5 - px) * d[0];
                double by = pontaY + (ombroY + 20 - pontaY) * d[0] + 6;
                double cx = (ax + bx) / 2, cy = (ay + by) / 2 - d[1];
                asa.quadTo(cx, cy, bx, by);
                ax = bx;
                ay = by;
            }
            asa.closePath();
            g.fill(asa);
        }
        g.fill(new Ellipse2D.Double(56, 50, 16, 30));
        g.fill(new Ellipse2D.Double(57, 41, 14, 14));
        for (int lado = -1; lado <= 1; lado += 2) {
            GeneralPath orelha = new GeneralPath();
            orelha.moveTo(64 + lado * 2, 44);
            orelha.lineTo(64 + lado * 7, 32);
            orelha.lineTo(64 + lado * 7, 46);
            orelha.closePath();
            g.fill(orelha);
        }
        g.dispose();
        return img;
    }

    /** Teia de rachaduras saindo do centro (impacto do kokusen). */
    static BufferedImage rachadura() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        Random rnd = new Random(1337);
        int raios = 15;
        double[][][] fim = new double[raios][][];
        for (int k = 0; k < raios; k++) {
            double a = k * Math.PI * 2 / raios + (rnd.nextDouble() - 0.5) * 0.25;
            double r = 6, x = Math.cos(a) * r, y = Math.sin(a) * r;
            Path2D.Double p = new Path2D.Double();
            p.moveTo(x, y);
            double[][] pontos = new double[12][];
            int n = 0;
            while (r < 118) {
                r += 7 + rnd.nextInt(9);
                a += (rnd.nextDouble() - 0.5) * 0.22;
                x = Math.cos(a) * Math.min(r, 118);
                y = Math.sin(a) * Math.min(r, 118);
                p.lineTo(x, y);
                if (n < pontos.length) pontos[n++] = new double[]{x, y, a, r};
            }
            float esp = 3.2f;
            g.setStroke(new BasicStroke(esp, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Color.WHITE);
            g.draw(p);
            // galhos
            for (int i = 2; i < n; i += 3) {
                double[] o = pontos[i];
                double ga = o[2] + (rnd.nextBoolean() ? 0.6 : -0.6);
                double gl = 12 + rnd.nextInt(18);
                linha(g, o[0], o[1], o[0] + Math.cos(ga) * gl, o[1] + Math.sin(ga) * gl, 1.6f, 0.9f);
            }
            fim[k] = pontos;
        }
        // aneis tortos ligando as rachaduras (teia)
        for (int anel = 0; anel < 3; anel++) {
            int idx = 2 + anel * 3;
            Path2D.Double p = new Path2D.Double();
            boolean comecou = false;
            for (int k = 0; k <= raios; k++) {
                double[] o = fim[k % raios][Math.min(idx, 11)];
                if (o == null) continue;
                if (!comecou) {
                    p.moveTo(o[0], o[1]);
                    comecou = true;
                } else if (rnd.nextFloat() < 0.75f) {
                    p.lineTo(o[0], o[1]);
                } else {
                    p.moveTo(o[0], o[1]);
                }
            }
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(1f, 1f, 1f, 0.85f));
            g.draw(p);
        }
        // cratera
        g.setPaint(new java.awt.RadialGradientPaint(new java.awt.geom.Point2D.Float(0, 0), 26f, new float[]{0f, 1f},
                new Color[]{new Color(1f, 1f, 1f, 0.9f), new Color(1f, 1f, 1f, 0f)}));
        g.fill(new Ellipse2D.Float(-26, -26, 52, 52));
        g.dispose();
        return comBrilho(img, 5f, 0.9f);
    }

    static BufferedImage seloVampiro() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        anel(g, 117, 2.5f, 1f);
        anel(g, 108, 1.2f, 0.8f);
        // espinhos goticos entre os aneis
        for (int i = 0; i < 32; i++) {
            AffineTransform antes = g.getTransform();
            g.rotate(i * Math.PI * 2 / 32);
            GeneralPath esp = new GeneralPath();
            boolean grande = i % 4 == 0;
            esp.moveTo(-3, -108);
            esp.lineTo(0, grande ? -126 : -116);
            esp.lineTo(3, -108);
            esp.closePath();
            g.setColor(Color.WHITE);
            g.fill(esp);
            g.setTransform(antes);
        }
        // quatro morcegos nos pontos cardeais
        BufferedImage bat = morcego(48, -10f);
        for (int k = 0; k < 4; k++) {
            AffineTransform antes = g.getTransform();
            g.rotate(k * Math.PI / 2 + Math.PI / 4);
            g.drawImage(bat, -24, -110, null);
            g.setTransform(antes);
        }
        anel(g, 70, 2.2f, 0.9f);
        anel(g, 64, 1f, 0.6f);
        // estrela de espinhos e lua no meio
        for (int i = 0; i < 8; i++) {
            AffineTransform antes = g.getTransform();
            g.rotate(i * Math.PI / 4);
            GeneralPath esp = new GeneralPath();
            esp.moveTo(-5, -26);
            esp.lineTo(0, -62);
            esp.lineTo(5, -26);
            esp.closePath();
            g.setColor(new Color(1f, 1f, 1f, 0.85f));
            g.fill(esp);
            g.setTransform(antes);
        }
        java.awt.geom.Area lua = new java.awt.geom.Area(new Ellipse2D.Float(-22, -22, 44, 44));
        lua.subtract(new java.awt.geom.Area(new Ellipse2D.Float(-12, -26, 40, 40)));
        g.setColor(Color.WHITE);
        g.fill(lua);
        g.dispose();
        return comBrilho(img, 5f, 0.8f);
    }

    static void cranio(Graphics2D g, double x, double y, double r) {
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r * 0.95));
        g.fill(new java.awt.geom.RoundRectangle2D.Double(x - r * 0.55, y + r * 0.5, r * 1.1, r * 0.7, r * 0.3, r * 0.3));
        java.awt.Composite antes = g.getComposite();
        g.setComposite(java.awt.AlphaComposite.Clear);
        g.fill(new Ellipse2D.Double(x - r * 0.62, y - r * 0.2, r * 0.5, r * 0.55));
        g.fill(new Ellipse2D.Double(x + r * 0.12, y - r * 0.2, r * 0.5, r * 0.55));
        GeneralPath nariz = new GeneralPath();
        nariz.moveTo(x, y + r * 0.3);
        nariz.lineTo(x - r * 0.12, y + r * 0.52);
        nariz.lineTo(x + r * 0.12, y + r * 0.52);
        nariz.closePath();
        g.fill(nariz);
        for (int d = -1; d <= 1; d++) g.fill(new java.awt.geom.Rectangle2D.Double(x + d * r * 0.3 - r * 0.04, y + r * 0.85, r * 0.08, r * 0.35));
        g.setComposite(antes);
    }

    static BufferedImage circuloNecro() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        Random rnd = new Random(666);
        anel(g, 118, 2.4f, 1f);
        anel(g, 104, 1.6f, 0.9f);
        // runas na faixa externa
        for (int i = 0; i < 30; i++) {
            AffineTransform antes = g.getTransform();
            g.rotate(i * Math.PI * 2 / 30);
            g.translate(0, -111);
            for (int k = 0; k < 3; k++) {
                double x0 = rnd.nextInt(3) * 2.5 - 2.5, y0 = rnd.nextInt(3) * 2.5 - 2.5;
                double x1 = rnd.nextInt(3) * 2.5 - 2.5, y1 = rnd.nextInt(3) * 2.5 - 2.5;
                linha(g, x0, y0, x1, y1, 1.2f, 0.9f);
            }
            g.setTransform(antes);
        }
        anel(g, 84, 2f, 0.9f);
        // pentagrama
        Path2D.Double estrela = new Path2D.Double();
        for (int k = 0; k <= 5; k++) {
            double a = -Math.PI / 2 + (k * 2 % 5) * Math.PI * 2 / 5;
            double px = Math.cos(a) * 84, py = Math.sin(a) * 84;
            if (k == 0) estrela.moveTo(px, py);
            else estrela.lineTo(px, py);
        }
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(estrela);
        // cranios nas pontas e no centro
        for (int k = 0; k < 5; k++) {
            double a = -Math.PI / 2 + k * Math.PI * 2 / 5;
            cranio(g, Math.cos(a) * 94, Math.sin(a) * 94, 9);
        }
        cranio(g, 0, -4, 18);
        anel(g, 40, 1.4f, 0.7f);
        g.dispose();
        return comBrilho(img, 5f, 0.85f);
    }

    /** Coloca uma imagem (de qualquer tamanho) numa celula de 64px, reduzindo com media. */
    static void celulaImagem(int indice, BufferedImage img) {
        int ox = (indice % 16) * CEL, oy = (indice / 16) * CEL;
        int f = img.getWidth() / CEL;
        for (int y = 0; y < CEL; y++) {
            for (int x = 0; x < CEL; x++) {
                float sa = 0, sr = 0, sg = 0, sb = 0;
                for (int j = 0; j < f; j++) {
                    for (int i = 0; i < f; i++) {
                        int c = img.getRGB(x * f + i, y * f + j);
                        float a = (c >>> 24) / 255f;
                        sa += a;
                        sr += (c >> 16 & 255) / 255f * a;
                        sg += (c >> 8 & 255) / 255f * a;
                        sb += (c & 255) / 255f * a;
                    }
                }
                int idx = (oy + y) * TAM + ox + x;
                boolean margem = x < 1 || y < 1 || x >= CEL - 1 || y >= CEL - 1;
                A[idx] = margem ? 0 : sa / (f * f);
                if (sa > 1e-5f) {
                    R[idx] = sr / sa;
                    G[idx] = sg / sa;
                    B[idx] = sb / sa;
                } else {
                    R[idx] = G[idx] = B[idx] = 1;
                }
            }
        }
    }

    static BufferedImage emblemaPokebola() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        // preenchimento fraquinho: metade de cima e metade de baixo
        g.setColor(new Color(1f, 1f, 1f, 0.1f));
        g.fill(new Ellipse2D.Float(-106, -106, 212, 212));
        anel(g, 118, 4f, 1f);
        anel(g, 106, 1.5f, 0.8f);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24 + Math.PI / 24;
            ponto(g, Math.cos(a) * 112, Math.sin(a) * 112, 1.6f, Color.WHITE);
        }
        linha(g, -106, 0, -36, 0, 5f, 1f);
        linha(g, 36, 0, 106, 0, 5f, 1f);
        anel(g, 34, 4.5f, 1f);
        anel(g, 22, 2f, 0.9f);
        ponto(g, 0, 0, 8f, Color.WHITE);
        g.dispose();
        // metade de cima vermelha (neon), metade de baixo branca
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 256; x++) {
                int c = img.getRGB(x, y);
                int a = c >>> 24;
                if (a == 0) continue;
                img.setRGB(x, y, a << 24 | 0xFF4040);
            }
        }
        return comBrilho(img, 6f, 0.9f);
    }

    // ------------------------------------------------------------ formas grandes (pixel a pixel)

    static float[] pontosLava;

    static void lava(float x, float y, float[] o) {
        if (pontosLava == null) {
            Random rnd = new Random(42);
            pontosLava = new float[90];
            for (int i = 0; i < 45; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * 1.05;
                pontosLava[i * 2] = (float) (Math.cos(a) * r);
                pontosLava[i * 2 + 1] = (float) (Math.sin(a) * r);
            }
        }
        float r = len(x, y);
        // distorce para as rachaduras nao ficarem retas
        float dx = x + (fbm(x * 3, y * 3, 5) - 0.5f) * 0.18f;
        float dy = y + (fbm(x * 3 + 9, y * 3 - 4, 6) - 0.5f) * 0.18f;
        float f1 = 9, f2 = 9;
        for (int i = 0; i < pontosLava.length; i += 2) {
            float d = len(dx - pontosLava[i], dy - pontosLava[i + 1]);
            if (d < f1) {
                f2 = f1;
                f1 = d;
            } else if (d < f2) f2 = d;
        }
        float borda = f2 - f1;
        float largura = 0.012f + 0.02f * (1 - r);
        float racha = gauss(borda, largura);
        float brilhoRacha = gauss(borda, largura * 0.45f);
        float miolo = 0.22f * (1 - suave(0f, 0.3f, f1)) * (0.6f + 0.4f * fbm(x * 6, y * 6, 9));
        float fade = 1 - suave(0.65f, 0.97f, r);
        // rachadura: laranja por fora, amarelo-branco no centro
        float cr = 1f, cg = 0.32f + 0.5f * brilhoRacha + 0.1f * racha, cb = 0.04f + 0.4f * brilhoRacha * brilhoRacha;
        float a = Math.max(racha, miolo);
        if (miolo > racha) {
            cr = 0.85f;
            cg = 0.18f;
            cb = 0.02f;
        }
        o[0] = clamp(cr);
        o[1] = clamp(cg);
        o[2] = clamp(cb);
        o[3] = clamp(a * fade);
    }

    static void vortice(float x, float y, float[] o) {
        float r = len(x, y);
        float ang = (float) Math.atan2(y, x);
        float espiral = (float) Math.cos(4 * (ang + 2.6f * Math.log(r + 0.06f)));
        float braco = (float) Math.pow(0.5f + 0.5f * espiral, 3);
        float n = fbm(x * 4, y * 4, 21);
        float radial = suave(0.08f, 0.35f, r) * (1 - suave(0.7f, 0.97f, r));
        float a = braco * radial * (0.6f + 0.6f * n) + 0.5f * gauss(r - 0.9f, 0.03f) + 0.25f * gauss(r - 0.9f, 0.09f);
        branco(o, a * (1 - suave(0.95f, 0.995f, r)));
    }

    // ------------------------------------------------------------ formas grandes (Java2D)

    static BufferedImage nova(int tam) {
        return new BufferedImage(tam, tam, BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D pincel(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.translate(128, 128);
        return g;
    }

    static void anel(Graphics2D g, float r, float esp, float alfa) {
        g.setStroke(new BasicStroke(esp));
        g.setColor(new Color(1f, 1f, 1f, alfa));
        g.draw(new Ellipse2D.Float(-r, -r, 2 * r, 2 * r));
    }

    static void linha(Graphics2D g, double x0, double y0, double x1, double y1, float esp, float alfa) {
        g.setStroke(new BasicStroke(esp, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(1f, 1f, 1f, alfa));
        g.draw(new java.awt.geom.Line2D.Double(x0, y0, x1, y1));
    }

    static void ponto(Graphics2D g, double x, double y, float r, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
    }

    static BufferedImage circuloRunas() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        Random rnd = new Random(7);
        anel(g, 118, 2.2f, 1f);
        anel(g, 106, 1.4f, 0.9f);
        // runas na faixa entre os dois aneis externos
        int runas = 28;
        for (int i = 0; i < runas; i++) {
            AffineTransform antes = g.getTransform();
            g.rotate(i * Math.PI * 2 / runas);
            g.translate(0, -112);
            for (int k = 0; k < 3; k++) {
                double x0 = rnd.nextInt(3) * 3 - 3, y0 = rnd.nextInt(3) * 2.5 - 2.5;
                double x1 = rnd.nextInt(3) * 3 - 3, y1 = rnd.nextInt(3) * 2.5 - 2.5;
                linha(g, x0, y0, x1, y1, 1.3f, 0.95f);
            }
            g.setTransform(antes);
        }
        anel(g, 82, 3f, 0.9f);
        anel(g, 76, 1f, 0.6f);
        // hexagrama
        for (int t = 0; t < 2; t++) {
            Path2D.Double p = new Path2D.Double();
            for (int k = 0; k < 3; k++) {
                double a = -Math.PI / 2 + t * Math.PI / 3 + k * Math.PI * 2 / 3;
                double px = Math.cos(a) * 80, py = Math.sin(a) * 80;
                if (k == 0) p.moveTo(px, py);
                else p.lineTo(px, py);
            }
            p.closePath();
            g.setStroke(new BasicStroke(1.8f));
            g.setColor(Color.WHITE);
            g.draw(p);
        }
        anel(g, 40, 1.8f, 0.9f);
        anel(g, 22, 1.2f, 0.7f);
        for (int k = 0; k < 6; k++) {
            double a = -Math.PI / 2 + k * Math.PI / 3;
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(Color.WHITE);
            g.draw(new Ellipse2D.Double(Math.cos(a) * 80 - 6, Math.sin(a) * 80 - 6, 12, 12));
            ponto(g, Math.cos(a + Math.PI / 6) * 94, Math.sin(a + Math.PI / 6) * 94, 2.2f, Color.WHITE);
        }
        g.dispose();
        return comBrilho(img, 5f, 0.7f);
    }

    static BufferedImage circuloGelo() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        anel(g, 116, 1.8f, 0.9f);
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            double r0 = i % 2 == 0 ? 104 : 109;
            linha(g, Math.cos(a) * r0, Math.sin(a) * r0, Math.cos(a) * 116, Math.sin(a) * 116, 1.2f, 0.8f);
        }
        // floco grande com galhos
        for (int k = 0; k < 6; k++) {
            AffineTransform antes = g.getTransform();
            g.rotate(k * Math.PI / 3);
            linha(g, 0, 0, 0, -98, 3f, 1f);
            for (double[] galho : new double[][]{{-34, 26}, {-58, 22}, {-80, 14}}) {
                double y = galho[0], c = galho[1];
                linha(g, 0, y, -c * 0.8, y - c, 2f, 1f);
                linha(g, 0, y, c * 0.8, y - c, 2f, 1f);
                linha(g, -c * 0.5, y - c * 0.62, -c * 0.5 - 6, y - c * 0.62 + 4, 1.2f, 0.9f);
                linha(g, c * 0.5, y - c * 0.62, c * 0.5 + 6, y - c * 0.62 + 4, 1.2f, 0.9f);
            }
            g.setTransform(antes);
        }
        Path2D.Double hex = new Path2D.Double();
        for (int k = 0; k < 6; k++) {
            double a = Math.PI / 6 + k * Math.PI / 3;
            if (k == 0) hex.moveTo(Math.cos(a) * 24, Math.sin(a) * 24);
            else hex.lineTo(Math.cos(a) * 24, Math.sin(a) * 24);
        }
        hex.closePath();
        g.setStroke(new BasicStroke(2f));
        g.setColor(Color.WHITE);
        g.draw(hex);
        g.dispose();
        return comBrilho(img, 6f, 0.8f);
    }

    static BufferedImage circuloEstrelas() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        Random rnd = new Random(99);
        // anel com degrade de cor
        int seg = 180;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1.2) * Math.PI * 2 / seg;
            float h = 0.6f + 0.25f * (float) Math.sin(a0 * 2);
            Color c = Color.getHSBColor(h, 0.55f, 1f);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(c);
            g.draw(new java.awt.geom.Line2D.Double(Math.cos(a0) * 116, Math.sin(a0) * 116, Math.cos(a1) * 116, Math.sin(a1) * 116));
        }
        for (int i = 0; i < 72; i++) {
            double a = i * Math.PI * 2 / 72;
            ponto(g, Math.cos(a) * 102, Math.sin(a) * 102, 0.9f, new Color(1f, 1f, 1f, 0.7f));
        }
        // estrelas e constelacoes
        double[][] estrelas = new double[46][];
        for (int i = 0; i < estrelas.length; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * 92;
            estrelas[i] = new double[]{Math.cos(a) * r, Math.sin(a) * r, 0.6 + rnd.nextDouble() * 1.8};
        }
        g.setStroke(new BasicStroke(0.8f));
        for (int i = 0; i < estrelas.length - 1; i++) {
            if (i % 5 == 4) continue;
            double[] a = estrelas[i], b = estrelas[i + 1];
            if (Math.hypot(a[0] - b[0], a[1] - b[1]) > 45) continue;
            g.setColor(new Color(0.75f, 0.8f, 1f, 0.45f));
            g.draw(new java.awt.geom.Line2D.Double(a[0], a[1], b[0], b[1]));
        }
        for (double[] e : estrelas) {
            Color c = Color.getHSBColor(0.55f + rnd.nextFloat() * 0.35f, rnd.nextFloat() * 0.4f, 1f);
            ponto(g, e[0], e[1], (float) e[2], c);
        }
        anel(g, 58, 0.8f, 0.35f);
        g.dispose();
        return comBrilho(img, 4f, 0.9f);
    }

    static BufferedImage circuloNatureza() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        // cipo ondulado
        Path2D.Double cipo = new Path2D.Double();
        for (int i = 0; i <= 360; i++) {
            double a = Math.toRadians(i);
            double r = 100 + 5 * Math.sin(a * 9);
            if (i == 0) cipo.moveTo(Math.cos(a) * r, Math.sin(a) * r);
            else cipo.lineTo(Math.cos(a) * r, Math.sin(a) * r);
        }
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(cipo);
        // folhas ao longo do cipo
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI * 2 / 36;
            double r = 100 + 5 * Math.sin(a * 9);
            AffineTransform antes = g.getTransform();
            g.translate(Math.cos(a) * r, Math.sin(a) * r);
            g.rotate(a + (i % 2 == 0 ? 0.6 : Math.PI - 0.6));
            GeneralPath folha = new GeneralPath();
            folha.moveTo(0, 0);
            folha.quadTo(6, -5, 16, 0);
            folha.quadTo(6, 5, 0, 0);
            g.setColor(new Color(1f, 1f, 1f, 0.9f));
            g.fill(folha);
            g.setTransform(antes);
        }
        anel(g, 64, 1.3f, 0.7f);
        // florzinhas de 5 petalas
        for (int k = 0; k < 6; k++) {
            double a = -Math.PI / 2 + k * Math.PI / 3;
            double fx = Math.cos(a) * 64, fy = Math.sin(a) * 64;
            for (int p = 0; p < 5; p++) {
                double pa = p * Math.PI * 2 / 5;
                g.setColor(new Color(1f, 1f, 1f, 0.85f));
                g.fill(new Ellipse2D.Double(fx + Math.cos(pa) * 5 - 3.5, fy + Math.sin(pa) * 5 - 3.5, 7, 7));
            }
            ponto(g, fx, fy, 2.5f, Color.WHITE);
        }
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6;
            linha(g, Math.cos(a) * 30, Math.sin(a) * 30, Math.cos(a + 0.3) * 50, Math.sin(a + 0.3) * 50, 1.2f, 0.6f);
        }
        anel(g, 28, 1.4f, 0.7f);
        g.dispose();
        return comBrilho(img, 5f, 0.6f);
    }

    static BufferedImage asa() {
        BufferedImage img = nova(256);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // borda de ataque: do ombro (esquerda) arqueando ate a ponta (direita)
        double[] p0 = {12, 100}, p1 = {80, 40}, p2 = {170, 30}, p3 = {242, 58};
        int penas = 16;
        // primarias (longas, por baixo), depois secundarias e as coberteiras (curtas, por cima)
        for (int camada = 0; camada < 3; camada++) {
            int n = camada == 2 ? 12 : penas;
            for (int i = 0; i < n; i++) {
                double t = (i + 0.5) / n;
                double[] p = bezier(p0, p1, p2, p3, t);
                double comp, larg, ang;
                float alfa;
                if (camada == 0) {
                    comp = 64 + 118 * Math.pow(t, 1.2);
                    larg = 32 - 8 * t;
                    ang = Math.toRadians(98 - 40 * t);
                    alfa = 0.42f;
                } else if (camada == 1) {
                    comp = 46 + 52 * t;
                    larg = 30;
                    ang = Math.toRadians(100 - 32 * t);
                    alfa = 0.5f;
                } else {
                    comp = 26 + 16 * t;
                    larg = 32;
                    ang = Math.toRadians(104 - 30 * t);
                    alfa = 0.7f;
                }
                desenharPena(g, p[0], p[1], ang, comp, larg, alfa);
            }
        }
        // braco da asa
        Path2D.Double braco = new Path2D.Double();
        braco.moveTo(p0[0], p0[1]);
        braco.curveTo(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(braco);
        g.dispose();
        // apaga o que encostou na borda
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                int m = Math.min(Math.min(x, y), Math.min(255 - x, 255 - y));
                if (m < 3) img.setRGB(x, y, 0);
            }
        }
        return comBrilho(img, 4f, 0.5f);
    }

    static double[] bezier(double[] a, double[] b, double[] c, double[] d, double t) {
        double u = 1 - t;
        return new double[]{
                u * u * u * a[0] + 3 * u * u * t * b[0] + 3 * u * t * t * c[0] + t * t * t * d[0],
                u * u * u * a[1] + 3 * u * u * t * b[1] + 3 * u * t * t * c[1] + t * t * t * d[1]};
    }

    static void desenharPena(Graphics2D g, double x, double y, double ang, double comp, double larg, float alfa) {
        AffineTransform antes = g.getTransform();
        g.translate(x, y);
        g.rotate(ang - Math.PI / 2);
        GeneralPath p = new GeneralPath();
        p.moveTo(-larg * 0.35, 0);
        p.curveTo(-larg * 0.6, comp * 0.35, -larg * 0.5, comp * 0.85, 0, comp);
        p.curveTo(larg * 0.5, comp * 0.85, larg * 0.6, comp * 0.35, larg * 0.35, 0);
        p.closePath();
        g.setColor(new Color(1f, 1f, 1f, alfa));
        g.fill(p);
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(new Color(1f, 1f, 1f, 1f));
        g.draw(p);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(1f, 1f, 1f, 0.9f));
        g.draw(new java.awt.geom.Line2D.Double(0, 2, 0, comp * 0.9));
        g.setTransform(antes);
    }

    static BufferedImage circuloRaios() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        Random rnd = new Random(3);
        anel(g, 116, 1.6f, 0.9f);
        anel(g, 96, 1.2f, 0.7f);
        // zigue-zague entre os aneis
        Path2D.Double zz = new Path2D.Double();
        int n = 64;
        for (int i = 0; i <= n; i++) {
            double a = i * Math.PI * 2 / n;
            double r = i % 2 == 0 ? 100 : 112;
            if (i == 0) zz.moveTo(Math.cos(a) * r, Math.sin(a) * r);
            else zz.lineTo(Math.cos(a) * r, Math.sin(a) * r);
        }
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
        g.setColor(Color.WHITE);
        g.draw(zz);
        // raios saindo do centro, tortos
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6 + 0.1;
            double r = 34;
            double x = Math.cos(a) * r, y = Math.sin(a) * r;
            Path2D.Double raio = new Path2D.Double();
            raio.moveTo(x, y);
            while (r < 94) {
                r += 9 + rnd.nextInt(8);
                double aa = a + (rnd.nextDouble() - 0.5) * 0.18;
                raio.lineTo(Math.cos(aa) * Math.min(r, 94), Math.sin(aa) * Math.min(r, 94));
            }
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(raio);
            ponto(g, Math.cos(a) * 94, Math.sin(a) * 94, 2.6f, Color.WHITE);
        }
        Path2D.Double hex = new Path2D.Double();
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            if (k == 0) hex.moveTo(Math.cos(a) * 34, Math.sin(a) * 34);
            else hex.lineTo(Math.cos(a) * 34, Math.sin(a) * 34);
        }
        hex.closePath();
        g.setStroke(new BasicStroke(2f));
        g.draw(hex);
        anel(g, 18, 1.4f, 0.8f);
        g.dispose();
        return comBrilho(img, 6f, 0.9f);
    }

    static BufferedImage circuloFogo() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        anel(g, 64, 2.2f, 0.9f);
        anel(g, 54, 1f, 0.6f);
        int linguas = 18;
        for (int i = 0; i < linguas; i++) {
            AffineTransform antes = g.getTransform();
            g.rotate(i * Math.PI * 2 / linguas);
            double comp = i % 2 == 0 ? 54 : 38;
            GeneralPath p = new GeneralPath();
            p.moveTo(-9, -62);
            p.curveTo(-12, -62 - comp * 0.4, 6, -62 - comp * 0.7, 3, -62 - comp);
            p.curveTo(12, -62 - comp * 0.6, 11, -62 - comp * 0.3, 9, -62);
            p.closePath();
            g.setColor(new Color(1f, 1f, 1f, 0.75f));
            g.fill(p);
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(Color.WHITE);
            g.draw(p);
            g.setTransform(antes);
        }
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI / 12;
            ponto(g, Math.cos(a) * 44, Math.sin(a) * 44, 1.6f, Color.WHITE);
        }
        anel(g, 30, 1.6f, 0.7f);
        g.dispose();
        return comBrilho(img, 6f, 0.9f);
    }

    /** Soma uma copia borrada por cima: da o brilho em volta das linhas. */
    static BufferedImage comBrilho(BufferedImage img, float raio, float forca) {
        int w = img.getWidth(), h = img.getHeight();
        float[] r = new float[w * h], g = new float[w * h], b = new float[w * h], a = new float[w * h];
        for (int i = 0; i < w * h; i++) {
            int c = img.getRGB(i % w, i / w);
            float al = (c >>> 24) / 255f;
            a[i] = al;
            r[i] = (c >> 16 & 255) / 255f * al;
            g[i] = (c >> 8 & 255) / 255f * al;
            b[i] = (c & 255) / 255f * al;
        }
        float[][] canais = {r, g, b, a};
        float[][] borrados = new float[4][];
        for (int k = 0; k < 4; k++) borrados[k] = borrar(canais[k], w, h, raio);
        for (int i = 0; i < w * h; i++) {
            for (int k = 0; k < 4; k++) canais[k][i] = Math.min(1, canais[k][i] + borrados[k][i] * forca);
        }
        // de volta para alfa reto
        for (int i = 0; i < w * h; i++) {
            if (a[i] > 1e-4f) {
                r[i] = Math.min(1, r[i] / a[i]);
                g[i] = Math.min(1, g[i] / a[i]);
                b[i] = Math.min(1, b[i] / a[i]);
            } else {
                r[i] = g[i] = b[i] = 1;
            }
        }
        return paraImagem(r, g, b, a, w, h);
    }

    static float[] borrar(float[] src, int w, int h, float sigma) {
        int raio = (int) Math.ceil(sigma * 3);
        float[] k = new float[raio * 2 + 1];
        float soma = 0;
        for (int i = -raio; i <= raio; i++) soma += k[i + raio] = (float) Math.exp(-i * i / (2 * sigma * sigma));
        for (int i = 0; i < k.length; i++) k[i] /= soma;
        float[] tmp = new float[w * h], out = new float[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float s = 0;
                for (int i = -raio; i <= raio; i++) {
                    int xx = Math.min(w - 1, Math.max(0, x + i));
                    s += src[y * w + xx] * k[i + raio];
                }
                tmp[y * w + x] = s;
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float s = 0;
                for (int i = -raio; i <= raio; i++) {
                    int yy = Math.min(h - 1, Math.max(0, y + i));
                    s += tmp[yy * w + x] * k[i + raio];
                }
                out[y * w + x] = s;
            }
        }
        return out;
    }

    // ------------------------------------------------------------ montagem do atlas

    static void celula(int indice, Fn f) {
        int cx = (indice % 16) * CEL, cy = (indice / 16) * CEL;
        preencher(cx, cy, CEL, f);
    }

    static void grandeFn(int b, Fn f) {
        int cx = (b % 4) * 4 * CEL, cy = (4 + (b / 4) * 4) * CEL;
        preencher(cx, cy, CEL * 4, f);
    }

    /** Avalia a funcao com 3x3 amostras por pixel (borda lisa). */
    static void preencher(int ox, int oy, int tam, Fn f) {
        float[] o = new float[4];
        int ss = 3;
        for (int py = 0; py < tam; py++) {
            for (int px = 0; px < tam; px++) {
                float sr = 0, sg = 0, sb = 0, sa = 0;
                for (int j = 0; j < ss; j++) {
                    for (int i = 0; i < ss; i++) {
                        float x = ((px + (i + 0.5f) / ss) / tam) * 2 - 1;
                        float y = 1 - ((py + (j + 0.5f) / ss) / tam) * 2;
                        f.f(x, y, o);
                        sa += o[3];
                        sr += o[0] * o[3];
                        sg += o[1] * o[3];
                        sb += o[2] * o[3];
                    }
                }
                int idx = (oy + py) * TAM + ox + px;
                float n = ss * ss;
                A[idx] = sa / n;
                if (sa > 1e-5f) {
                    R[idx] = sr / sa;
                    G[idx] = sg / sa;
                    B[idx] = sb / sa;
                } else {
                    R[idx] = G[idx] = B[idx] = 1;
                }
            }
        }
    }

    static void grande(int b, BufferedImage img) {
        int ox = (b % 4) * 4 * CEL, oy = (4 + (b / 4) * 4) * CEL;
        int tam = CEL * 4;
        for (int y = 0; y < tam; y++) {
            for (int x = 0; x < tam; x++) {
                int c = img.getRGB(x * img.getWidth() / tam, y * img.getHeight() / tam);
                int idx = (oy + y) * TAM + ox + x;
                // margem de 2px sem nada
                boolean margem = x < 2 || y < 2 || x >= tam - 2 || y >= tam - 2;
                A[idx] = margem ? 0 : (c >>> 24) / 255f;
                R[idx] = (c >> 16 & 255) / 255f;
                G[idx] = (c >> 8 & 255) / 255f;
                B[idx] = (c & 255) / 255f;
            }
        }
    }

    static BufferedImage paraImagem(float[] r, float[] g, float[] b, float[] a, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < w * h; i++) {
            int ai = Math.round(clamp(a[i]) * 255), ri = Math.round(clamp(r[i]) * 255);
            int gi = Math.round(clamp(g[i]) * 255), bi = Math.round(clamp(b[i]) * 255);
            img.setRGB(i % w, i / w, ai << 24 | ri << 16 | gi << 8 | bi);
        }
        return img;
    }

    // ------------------------------------------------------------ icone

    static BufferedImage icone() {
        int t = 128;
        float[] r = new float[t * t], g = new float[t * t], b = new float[t * t], a = new float[t * t];
        float[][] cores = {{0.36f, 0.86f, 1f}, {1f, 0.48f, 0.1f}, {0.7f, 0.35f, 1f}};
        float[] incl = {1.1f, -1.0f, 0.25f};
        for (int py = 0; py < t; py++) {
            for (int px = 0; px < t; px++) {
                float x = (px + 0.5f) / t * 2 - 1, y = 1 - (py + 0.5f) / t * 2;
                int i = py * t + px;
                // fundo: quadrado arredondado escuro com degrade
                float qx = Math.max(Math.abs(x) - 0.72f, 0), qy = Math.max(Math.abs(y) - 0.72f, 0);
                float dq = len(qx, qy) - 0.24f;
                float fundo = 1 - suave(-0.02f, 0.01f, dq);
                float cr = 0.05f + 0.06f * (1 - y), cg = 0.04f + 0.02f * (1 - y), cb = 0.1f + 0.1f * (1 - y);
                // tres aneis orbitais
                for (int k = 0; k < 3; k++) {
                    float c = (float) Math.cos(incl[k] + k), s = (float) Math.sin(incl[k] + k);
                    float rx = x * c + y * s, ry = -x * s + y * c;
                    float ex = rx / 0.62f, ey = ry / (0.22f + 0.08f * k);
                    float d = Math.abs(len(ex, ey) - 1) * 0.2f;
                    float l = gauss(d, 0.012f) + 0.35f * gauss(d, 0.04f);
                    cr += cores[k][0] * l;
                    cg += cores[k][1] * l;
                    cb += cores[k][2] * l;
                }
                float rc = len(x, y);
                float nucleo = gauss(rc, 0.08f) + 0.4f * gauss(rc, 0.22f);
                cr += nucleo;
                cg += nucleo * 0.95f;
                cb += nucleo;
                r[i] = clamp(cr);
                g[i] = clamp(cg);
                b[i] = clamp(cb);
                a[i] = fundo;
            }
        }
        return paraImagem(r, g, b, a, t, t);
    }
}
