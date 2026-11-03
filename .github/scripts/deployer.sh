#!/usr/bin/env bash
# Met en ligne le launcher sur majestycraft.com (FTP chiffré), appelé par le workflow
# « Déployer le launcher ». Rien n'est envoyé si un fichier n'est pas signé par la clé de
# MajestyCraft. Chaque fichier part sous un nom temporaire puis est renommé : un joueur ne
# télécharge jamais un fichier à moitié envoyé. Le jar passe toujours avant son .json.
#
# Variables : FTP_HOST, FTP_USER, FTP_PASSWORD, FTP_DIR (dossier qui contient bootstrap/,
# relatif à la connexion FTP), SITE (adresse web de ce dossier), BOOTSTRAP et VERSIONS
# (true/false). FTP_CACERT : certificat à accepter, pour les essais seulement.
set -euo pipefail

SITE="${SITE:-https://majestycraft.com/minecraft}"
BOOTSTRAP="${BOOTSTRAP:-false}"
VERSIONS="${VERSIONS:-false}"
CLE="deploy/cle-publique.pem"

erreur() { echo "::error::$*"; exit 1; }

for v in FTP_HOST FTP_USER FTP_PASSWORD FTP_DIR; do
    [ -n "${!v:-}" ] || erreur "Secret $v manquant (Settings > Secrets and variables > Actions)."
done
FTP_DIR="${FTP_DIR%/}"

# --- 1. Signatures, avant toute connexion -------------------------------------------
verifier_signature() {
    local jar=$1 json=$2 md5 sig
    [ -f "$jar" ] && [ -f "$json" ] || erreur "$jar ou $json absent du dépôt."
    # Format écrit par signature-launcher.ps1 : {"md5": "...", "sig": "..."}
    md5=$(sed -n 's/.*"md5": *"\([0-9a-fA-F]*\)".*/\1/p' "$json")
    sig=$(sed -n 's/.*"sig": *"\([A-Za-z0-9+\/=]*\)".*/\1/p' "$json")
    [ -n "$sig" ] || erreur "$json sans signature : lance build.ps1 (il signe le jar) puis pousse le jar ET son .json."
    base64 -d <<< "$sig" > signature.bin
    [ "$(md5sum "$jar" | cut -c1-32)" = "$md5" ] || erreur "$json ne correspond pas à $jar (MD5) : pousse les deux fichiers du même build."
    openssl dgst -sha256 -verify "$CLE" -signature signature.bin "$jar" >/dev/null \
        || erreur "Signature invalide pour $jar : il n'est pas signé par la clé de MajestyCraft."
    rm -f signature.bin
    echo "Signature valide : $jar (md5 $md5)"
}
verifier_signature target/MajestyCraft.jar target/launcher.json
if [ "$BOOTSTRAP" = "true" ]; then
    verifier_signature deploy/bootstrap/bootstrap.jar deploy/bootstrap/bootstrap.json
fi

# --- 2. Connexion FTP ----------------------------------------------------------------
NETRC=$(mktemp)
trap 'rm -f "$NETRC"' EXIT
chmod 600 "$NETRC"
printf 'machine %s login %s password %s\n' "${FTP_HOST%%:*}" "$FTP_USER" "$FTP_PASSWORD" > "$NETRC"
CURL=(curl --ssl-reqd --netrc-file "$NETRC" --fail --silent --show-error --connect-timeout 20 --retry 2)
if [ -n "${FTP_CACERT:-}" ]; then CURL+=(--cacert "$FTP_CACERT" --ssl-no-revoke); fi
BASE="ftp://$FTP_HOST/$FTP_DIR"

# Vrai si <dossier>/bootstrap/ contient MajestyCraft.jar (chemin depuis le dossier de connexion,
# ou absolu s'il commence par /)
contient_launcher() {
    local l
    l=$("${CURL[@]}" --list-only "ftp://$FTP_HOST/$1/bootstrap/" 2>/dev/null) || return 1
    grep -qx "MajestyCraft.jar" <<< "$(tr -d '\r' <<< "$l")"
}

if ! contient_launcher "$FTP_DIR"; then
    # Identifiants ou dossier ? Le dossier de connexion doit au moins être lisible.
    code=0
    "${CURL[@]}" --list-only "ftp://$FTP_HOST/" > /dev/null 2> erreur-ftp.txt || code=$?
    case $code in
        0) ;;
        67) erreur "Identifiant ou mot de passe FTP refusé : vérifie FTP_USER et FTP_PASSWORD." ;;
        *) erreur "Serveur FTP injoignable ou FTP chiffré refusé (vérifie FTP_HOST) : $(cat erreur-ftp.txt)" ;;
    esac
    # Connexion OK : on cherche le dossier du launcher aux emplacements habituels
    for d in . httpdocs public_html www htdocs web majestycraft.com majestycraft.com/httpdocs \
             majestycraft.com/public_html domains/majestycraft.com/public_html; do
        for candidat in "$d/minecraft" "$d"; do
            if contient_launcher "$candidat"; then
                erreur "Connexion FTP OK, mais FTP_DIR ne désigne pas le bon dossier. Le launcher est dans « ${candidat#./}/bootstrap/ » : mets « ${candidat#./} » dans le secret FTP_DIR, puis relance."
            fi
        done
    done
    erreur "Connexion FTP OK, mais dossier bootstrap/ introuvable. Dans FileZilla, ouvre le dossier qui contient MajestyCraft.jar et launcher.json : FTP_DIR est le chemin affiché au-dessus (« Site distant »), sans /bootstrap à la fin, par exemple /httpdocs/minecraft."
fi
echo "Connexion FTP chiffrée OK : $FTP_DIR/bootstrap/"

# --- 3. Envoi ------------------------------------------------------------------------
md5_en_ligne() {
    curl -fsSL "$SITE/$1?maj=$(date +%s%N)" | md5sum | cut -c1-32
}

# envoyer <fichier local> <chemin sous FTP_DIR>
envoyer() {
    local src=$1 dest=$2 attendu
    "${CURL[@]}" --ftp-create-dirs -T "$src" "$BASE/$dest.envoi" || erreur "Envoi de $dest impossible."
    # Chemins depuis le dossier de connexion (commandes envoyées avant tout changement de dossier)
    if ! "${CURL[@]}" -o /dev/null -Q "RNFR $FTP_DIR/$dest.envoi" -Q "RNTO $FTP_DIR/$dest" "$BASE/"; then
        # Serveur qui refuse de renommer par-dessus un fichier existant : il est supprimé juste avant
        "${CURL[@]}" -o /dev/null -Q "DELE $FTP_DIR/$dest" -Q "RNFR $FTP_DIR/$dest.envoi" -Q "RNTO $FTP_DIR/$dest" "$BASE/" \
            || erreur "Renommage de $dest.envoi en $dest impossible."
    fi
    attendu=$(md5sum "$src" | cut -c1-32)
    for essai in 1 2 3 4 5 6; do
        [ "$(md5_en_ligne "$dest")" = "$attendu" ] && { echo "En ligne : $SITE/$dest"; return 0; }
        sleep 5
    done
    erreur "$SITE/$dest ne renvoie pas le fichier envoyé : FTP_DIR ne correspond pas à $SITE ?"
}

envoyer target/MajestyCraft.jar bootstrap/MajestyCraft.jar
envoyer target/launcher.json bootstrap/launcher.json

if [ "$BOOTSTRAP" = "true" ]; then
    envoyer deploy/bootstrap/bootstrap.jar bootstrap/bootstrap.jar
    envoyer deploy/bootstrap/bootstrap.json bootstrap/bootstrap.json
fi

if [ "$VERSIONS" = "true" ]; then
    while IFS= read -r f; do
        envoyer "deploy/versions/$f" "$f"
    done < <(cd deploy/versions && find . -type f -name '*.json' | sed 's#^\./##' | sort)
fi

echo "Déploiement terminé."
