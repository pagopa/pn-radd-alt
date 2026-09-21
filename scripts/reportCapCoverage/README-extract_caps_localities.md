extract_caps_localities.py
==========================

Script per estrarre CAP e località dalle righe di log del tipo:

  Checking coverage for cap: 06012 and locality: CITTA' DI CASTELLO

Esempio d'uso:

1) Leggere da file e stampare CSV su stdout:

   python3 scripts/extract_caps_localities.py /path/to/logfile.log

2) Leggere da stdin e salvare in file:

   cat /path/to/logfile.log | python3 scripts/extract_caps_localities.py --out out.csv

3) Stampare solo coppie uniche:

   python3 scripts/extract_caps_localities.py /path/to/logfile.log --unique

4) Stampare solo i CAP (uno per riga):

   python3 scripts/extract_caps_localities.py /path/to/logfile.log --caps-only --unique

Opzioni:
  --unique        Mostra solo coppie cap,locality uniche
  --out FILE      Salva l'output CSV in FILE
  --no-header     Non stampare l'intestazione CSV
  --caps-only     Stampare solo i CAP
  --localities-only Stampare solo le località

Nota: il pattern è case-insensitive e normalizza i CAP a 5 cifre con zfill.

