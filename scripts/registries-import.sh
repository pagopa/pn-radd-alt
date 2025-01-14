#!/bin/bash

CSV_PATH="test.csv"                                       # Sostituire con il path del file CSV
API_BASE_URL="https://api.radd.dev.notifichedigitali.it"  # sostituire con il base url per l'Api di import massivo di radd-alt
JWT="JWT"                                                 # Sostituire con il token JWT
CX_UID="UID"                                              # Sostituire con il valore del campo uid

calculate_sha256() {
    local file=$1
    sha256sum "$file" | awk '{print $1}' | xxd -r -p | base64
}

if [ ! -f "$CSV_PATH" ]; then
    echo "Errore: il file $CSV_PATH non esiste."
    exit 1
fi

CHECKSUM=$(calculate_sha256 "$CSV_PATH")
echo "Checksum calcolato: $CHECKSUM"

RESPONSE=$(curl -v POST "$API_BASE_URL/radd-net/api/v1/registry/import/upload" \
           -H "uid: $CX_UID" \
           -H "Authorization: Bearer $JWT" \
           -H "Content-Type: application/json" \
           -d '{"checksum": "'"$CHECKSUM"'"}'
           )

URL=$(echo "$RESPONSE" | jq -r '.url')
SECRET=$(echo "$RESPONSE" | jq -r '.secret')

if [ -z "$URL" ] || [ -z "$SECRET" ]; then
    echo "Errore: Risposta incompleta, URL o secret mancanti."
    exit 1
fi

curl -X PUT "$URL" \
    -H "Content-Type: text/csv" \
    -H "x-amz-meta-secret: $SECRET" \
    -H "x-amz-checksum-sha256: $CHECKSUM" \
    --data-binary "@$CSV_PATH"

echo "Richiesta di import massivo completata."
