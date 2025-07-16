const fs = require('fs');
const path = require('path');
const csv = require('csv-parser');
const createCsvWriter = require('csv-writer').createObjectCsvWriter;
const axios = require('axios');
const API_BASE_URL = process.env.API_BASE_URL;

if (process.argv.length < 5) {
  console.error('Uso: node index.js <username> <password> <csvFilePath>');
  process.exit(1);
}

// TODO verificare sicurezza nel passare credenziali via riga di comando
const username = process.argv[2];
const password = process.argv[3];

const csvFilePath  = process.argv[4];
const partnerId = path.basename(csvFilePath).replace('.csv', '');

let JWT_TOKEN;
let csvRegistries = [];
let apiRegistries = [];

const headers = {
  Authorization: `Bearer ${JWT_TOKEN}`,
  'Content-Type': 'application/json',
};

// Autenticazione dell'operatore. L'autenticazione verrà fatta tramite Cognito e restituirà un token JWT
function authenticateUser(username, password) {
  return "";
}

async function readCsv(filePath) {
  return new Promise((resolve, reject) => {
    const results = [];
    fs.createReadStream(filePath)
      .pipe(csv())
      .on('data', data => results.push(data))
      .on('end', () => resolve(results))
      .on('error', reject);
  });
}

function convertToRegistryRequest(csvRow) {
  return {
    partnerId: csvRow.partnerId,
    locationId: csvRow.locationId,
    description: csvRow.description,
    phoneNumbers: csvRow.phoneNumbers ? csvRow.phoneNumbers.split('|') : [],
    email: csvRow.email || null,
    openingTime: csvRow.openingTime,
    startValidity: csvRow.startValidity,
    endValidity: csvRow.endValidity,
    externalCodes: csvRow.externalCodes ? csvRow.externalCodes.split('|') : [],
    appointmentRequired: csvRow.appointmentRequired === 'true',
    website: csvRow.website || null,
    partnerType: csvRow.partnerType,
    address: {
      addressRow: csvRow.addressRow.replace(/^"|"$/g, ''),
      cap: csvRow.cap,
      city: csvRow.city,
      province: csvRow.province,
      country: csvRow.country
    }
  };
}

async function getRegistriesByPartnerId() {
  try {
    const registries = [];
    let lastKey = null;
    do {
      const res = await axios.get(`${API_BASE_URL}/radd-bo/api/v2/registry/${partnerId}`, {
        headers,
        params: {
          limit: 100,
          ...(lastKey && { lastKey }) // aggiunge lastKey solo se esiste
        }
      });

      const { items, lastKey: newLastKey } = res.data;
      if (items && items.length > 0) {
        registries.push(...items);
      }

      lastKey = newLastKey;
    } while (lastKey);

    return registries;

  } catch (err) {
    console.error('Errore lettura sedi da API:', err.message);
    return [];
  }
}

async function deleteRegistry(locationId) {
  try {
    await axios.delete(`${API_BASE_URL}/radd-bo/api/v2/registry/${partnerId}/${locationId}`, { headers });
    console.log(`✅ Eliminata sede con locationId: ${locationId}`);
  } catch (err) {
    console.error(`❌ Errore eliminazione sede ${locationId}:`, err.message);
  }
}

async function createRegistry(registry) {
  try {
    const res = await axios.post(`${API_BASE_URL}/radd-bo/api/v2/registry/${partnerId}`, registry, { headers });
    console.log(`➕ Aggiunta sede ${registry.locationId}`);
    return { ...registry, status: 'OK', result: JSON.stringify(res.data) };
  } catch (err) {
    const reason = err.response?.data?.reason || err.message;
    console.error(`⚠️ Inserimento KO: ${registry.locationId} - ${reason}`);
    return { ...registry, status: 'KO', error: reason };
  }
}

function findlocationId(apiRegistry, csvRegistry) {
  return apiRegistry.find(s =>
    s.locationId === csvRegistry.locationId
  )?.locationId;
}

async function main() {
  // Autenticazione operatore
  JWT_TOKEN = authenticateUser(username, password);

  // Lettura CSV e reperimento sedi associate all'ente
  csvRegistries = await readCsv(csvFilePath).map(convertToRegistryRequest);
  apiRegistries = await getRegistriesByPartnerId();

  const usedLocationIds = new Set();
  const report = [];

  // Scorrimento CSV
  for (const registry of csvRegistries) {
    const locationId = findlocationId(apiRegistries, registry);

    // Se la sede esiste già, la cancello
    if (locationId) {
      // TODO Gestire caso in cui la DELETE va in errore
      await deleteRegistry(locationId);
      usedLocationIds.add(locationId);
    }

    // Creo una nuova sede e la aggiungo al report finale
    const risultato = await createRegistry(registry);
    report.push(risultato);
  }

  // Rimozione sedi rimaste in API (non presenti nel CSV)
  for (const registry of apiRegistries) {
    if (!usedLocationIds.has(registry.locationId)) {
      await deleteRegistry(registry.locationId);
    }
  }

  // Scrittura report finale
  const csvWriter = createCsvWriter({
    path: `report-${partnerId}-${Date.now()}.csv`,
    header: [
      ...Object.keys(report[0]).filter(k => !['status', 'error', 'result'].includes(k)).map(k => ({ id: k, title: k })),
      { id: 'status', title: 'Stato' },
      { id: 'error', title: 'Errore' },
      { id: 'result', title: 'Risultato' }
    ]
  });

  await csvWriter.writeRecords(report);
  console.log('📄 Report generato con esito delle operazioni.');
}

main();