#!/bin/bash

# Useful links:
# - https://pagopa.atlassian.net/wiki/spaces/PN/pages/503972476/Ambienti+di+TEST#Utenti-di-test-Helpdesk
# - https://github.com/pagopa/pn-radd-alt/tree/develop/scripts/registryMassiveInsert

ENV=$1
CLIENTID=$2
PN_CONF_PATH=$3
CSV_FILES=$4 # Comment this variable means *.csv available in $CSV_PATH

# --------------------------

if [ $# -lt 3 ] || [ $# -gt 4 ] || [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    echo -e "\n    Usage: $0 <env> <clientid> <pn-configuration path> [<csv1,...,csvN>]"
    echo -e "\n    Note: If no csv list is provided, then all of them will be loaded"
    echo -e "\n    Autenticazione: SSO (login Google una sola volta sul portale Helpdesk)"
    echo -e "\n    Exit 0\n"
    exit 0
fi

# Calcolo URL Helpdesk in base all'ambiente (prod ha host senza il segmento env)
case "$ENV" in
    dev|test|uat|hotfix) HELPDESK_URL="https://helpdesk.${ENV}.notifichedigitali.it" ;;
    prod)                HELPDESK_URL="https://helpdesk.notifichedigitali.it" ;;
    *)
        echo -e "\n    Parametro <env> non valido. Valori ammessi: dev, test, uat, hotfix, prod\n"
        exit 1
        ;;
esac

CSV_PATH="${PN_CONF_PATH}/${ENV}/_conf/core/app_config/pn-radd-alt"


if [ $# -eq 3 ]; then
    echo -e "\nThis command will upload all csv available into the $CSV_PATH folder:"
    CSV_PATH_2=$(echo $CSV_PATH | sed -e 's|\/|\\\/|g')
    CSV_LIST=$(ls -1 ${CSV_PATH}/*.csv | sed -e "s/${CSV_PATH_2}\///g")
    echo -e "\n${CSV_LIST}\n"
    echo -e "Do you agree?\n"
    while [ "$ANSW" != "y" ] && [ "$ANSW" != "n" ]; do
        read -p "[y/n]: " ANSW
        if [ "$ANSW" == "n" ]; then
            exit 0
        fi
    done
else
    CSV_LIST=$(echo "$CSV_FILES" | tr ',' ' ')
    echo -e "\nChecking if provided files exists in $CSV_PATH folder...\n"
    for i in $CSV_LIST; do
        if [ ! -f ${CSV_PATH}/$i ]; then
            echo -e " - ${i}: Not Available -> Exit.\n"
            exit 0
        else
            echo -e " - ${i}: Available"
        fi
    done
    echo -e "\nThis command will upload the following csv files:"
    echo -e "\n$CSV_LIST\n"
    echo -e "Do you agree?\n"
    while [ "$ANSW" != "y" ] && [ "$ANSW" != "n" ]; do
        read -p "[y/n]: " ANSW
        if [ "$ANSW" == "n" ]; then
            exit 0
        fi
    done
fi

echo ""
export AWS_PROFILE=sso_pn-core-${ENV}
aws sso login --profile $AWS_PROFILE

# === LOGIN SSO UNA SOLA VOLTA - TOKEN SOLO IN MEMORIA (nessun file su disco) ===
# Richiamo direttamente il modulo condiviso usato anche da index.js per recuperare
# l'idToken dal portale Helpdesk (login Google una sola volta). Il token viene
# stampato su stdout e catturato nella variabile TOKEN; i log del modulo sono
# rediretti su stderr per non sporcare la cattura. index.js NON viene modificato.
echo -e "\n🔐 Accesso SSO in corso (login una sola volta) su ${HELPDESK_URL}..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOKEN=$(HELPDESK_URL="$HELPDESK_URL" SCRIPT_DIR="$SCRIPT_DIR" node -e '
  // I log del modulo vanno su stderr, sul terminale: solo il token finisce su stdout
  console.log = (...a) => process.stderr.write(a.join(" ") + "\n");
  const path = require("path");
  const { fetchHelpdeskIdToken } = require(path.resolve(process.env.SCRIPT_DIR, "../shared/helpdesk-token"));
  // playwright va caricato dalla node_modules locale e passato al modulo condiviso
  // (il modulo shared non ha playwright nella propria cartella)
  const playwright = require("playwright");
  fetchHelpdeskIdToken({ helpdeskUrl: process.env.HELPDESK_URL, playwright })
    .then(t => process.stdout.write(t))
    .catch(e => { console.error(e && e.message ? e.message : e); process.exit(1); });
')
LOGIN_RC=$?
if [ $LOGIN_RC -ne 0 ] || [ -z "$TOKEN" ]; then
    echo "❌ Errore durante l'accesso SSO"
    exit 1
fi
echo "✅ Accesso SSO completato (token mantenuto solo in memoria)"

RESULTS_NAME=${ENV}_$(date +%Y%m%d_%H%M%S)_radd
OUTPUT_FOlDER=${RESULTS_NAME}_results
OUTPUT_SCRIPT=./${OUTPUT_FOlDER}/${RESULTS_NAME}_output.txt
echo -e "\nGenerating ${OUTPUT_FOlDER} folder..."
mkdir ${OUTPUT_FOlDER}

for CSV_FILE in $CSV_LIST
do
    # Estrazione TAX_ID portabile (macOS/Linux), allineata a index.js: parte prima del '-'
    TAX_ID=$(basename "$CSV_FILE" .csv | cut -d'-' -f1)
    echo -e "\n - Uploading ${TAX_ID}.csv file..."
    node index.js --token "$TOKEN" $ENV $CLIENTID ${CSV_PATH}/${CSV_FILE} >> ${OUTPUT_SCRIPT}
    echo "   Return code: $?."
    mv report-${TAX_ID}-*.csv ${OUTPUT_FOlDER}
done

echo -e "\nReports and script output available into ./${OUTPUT_FOlDER} folder."

echo -e "\nCreating a .tar archive containing all generated reports..."
cd ${OUTPUT_FOlDER}
tar -cf ${OUTPUT_FOlDER}.tar *.csv
echo -e "\nRemoving duplicated .csv files..."
rm -f *.csv

echo -e "\nDone.\n"
