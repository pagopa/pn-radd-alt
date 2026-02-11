const fs = require('fs');
const readline = require('readline');

async function extractCapLocality(inputFile, outputFile) {
  const uniquePairs = new Set();
  const capLocalityRegex = /cap:\s*(\d+)\s+and\s+locality:\s+([A-Z\s]+?)\s+with\s+search/i;

  const fileStream = fs.createReadStream(inputFile);
  const rl = readline.createInterface({
    input: fileStream,
    crlfDelay: Infinity
  });

  console.log('Elaborazione del file in corso...');

  for await (const line of rl) {
    const match = line.match(capLocalityRegex);
    if (match) {
      const cap = match[1];
      const locality = match[2].trim();
      const pair = `${cap},${locality}`;
      uniquePairs.add(pair);
    }
  }

  console.log(`Trovate ${uniquePairs.size} coppie uniche CAP-LOCALITY`);

  // Ordina le coppie e scrive nel file CSV
  const sortedPairs = Array.from(uniquePairs).sort((a, b) => {
    const [capA] = a.split(',');
    const [capB] = b.split(',');
    return capA.localeCompare(capB);
  });

  // Scrivi il file CSV con header
  const csvContent = 'CAP,LOCALITY\n' + sortedPairs.join('\n');
  fs.writeFileSync(outputFile, csvContent, 'utf8');

  console.log(`File CSV creato: ${outputFile}`);
  console.log(`Prime 10 righe:`);
  sortedPairs.slice(0, 10).forEach(pair => console.log(pair));
}

// Esegui l'estrazione
const inputFile = process.argv[2] || '/Users/alessandro.masci/Downloads/logs_cloudwatch.csv';
const outputFile = process.argv[3] || '/Users/alessandro.masci/Desktop/Script creazione CAP coverage/data/cap-locality.csv';

extractCapLocality(inputFile, outputFile)
  .then(() => console.log('\nEstrazione completata con successo!'))
  .catch(err => console.error('Errore:', err));
