#!/usr/bin/env bash
# Monta um GIF por aura a partir dos quadros gravados pelo autoteste.
# Uso: ferramentas/fazer-gifs.sh [pasta-do-run]   (padrao: fabric/run/autoteste)
# Precisa do ffmpeg no PATH. Os GIFs vao para gifs/ na raiz do projeto.
set -e
RUN="${1:-fabric/run/autoteste}"
mkdir -p gifs
for pasta in "$RUN"/quadros/*/; do
    nome=$(basename "$pasta")
    ffmpeg -y -loglevel error -framerate 20 -i "$pasta/%03d.png" \
        -vf "fps=20,scale=640:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=256:stats_mode=full[p];[b][p]paletteuse=dither=sierra2_4a" \
        -loop 0 "gifs/aura_$nome.gif"
    echo "gifs/aura_$nome.gif"
done
