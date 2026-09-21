#!/usr/bin/env python3
"""
Estrattore di CAP e località dalle righe di log che contengono:
"Checking coverage for cap: 06012 and locality: CITTA' DI CASTELLO"

Uso:
  python3 scripts/extract_caps_localities.py path/to/logfile.log
  cat logfile.log | python3 scripts/extract_caps_localities.py

Opzioni principali:
  --unique      : stampa solo coppie cap,locality uniche
  --out FILE    : salva l'output CSV in FILE invece di stdout
  --no-header   : non stampare l'intestazione CSV
  --caps-only   : stampare solo i CAP (una per riga)
  --localities-only : stampare solo le località (una per riga)

Il formato di output di default è CSV con intestazione: cap,locality
"""

import sys
import re
import argparse
import csv
from typing import Iterable, Tuple, Set, List

PATTERN = re.compile(r"Checking\s+coverage\s+for\s+cap:\s*([0-9]{2,6})\s+and\s+locality:\s*(.+?)(?:\s*$|\"\s*$)", re.IGNORECASE)
LOCALITY_STOP_PATTERN = re.compile(r'(?=(?:\s+with\s+search\s+mode\s*:|\"\s*,|\",|,\s*\"(?:level|timestamp|logger|thread|message)\"|,\s*(?:INFO|DEBUG|WARN|ERROR)\b|\s+-\s+|\s{2,}))', re.IGNORECASE)


def normalize_locality(value: str) -> str:
    locality = value.strip().strip('"').strip()
    stop_match = LOCALITY_STOP_PATTERN.search(locality)
    if stop_match:
        locality = locality[:stop_match.start()]
    locality = locality.rstrip(' .;,')
    return locality


def parse_lines(lines: Iterable[str]) -> List[Tuple[str, str]]:
    """Estrae tutte le coppie (cap, locality) dalle righe fornite.
    La locality viene normalizzata e il parsing ignora il resto del log.
    """
    results: List[Tuple[str, str]] = []
    for ln in lines:
        if not ln:
            continue
        m = PATTERN.search(ln)
        if m:
            cap = m.group(1).zfill(5)
            locality = normalize_locality(m.group(2))
            if locality:
                results.append((cap, locality))
    return results


def read_input(path: str = None) -> Iterable[str]:
    if path and path != "-":
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                yield line
    else:
        for line in sys.stdin:
            yield line


def main(argv=None):
    p = argparse.ArgumentParser(description="Estrai CAP e località da file di log (messaggi 'Checking coverage for cap: ... and locality: ...')")
    p.add_argument("file", nargs="?", default="-", help="File di log da leggere. Usa '-' o ometti per leggere da stdin")
    p.add_argument("--unique", action="store_true", help="Mostra solo coppie uniche")
    p.add_argument("--out", help="File di output CSV (default stdout)")
    p.add_argument("--no-header", action="store_true", help="Non stampare l'intestazione CSV")
    p.add_argument("--caps-only", action="store_true", help="Stampare solo i CAP, una per riga")
    p.add_argument("--localities-only", action="store_true", help="Stampare solo le località, una per riga")
    args = p.parse_args(argv)

    lines = read_input(args.file)
    pairs = parse_lines(lines)

    if args.unique:
        seen: Set[Tuple[str,str]] = set()
        unique_pairs: List[Tuple[str,str]] = []
        for cap, loc in pairs:
            if (cap, loc) not in seen:
                seen.add((cap, loc))
                unique_pairs.append((cap, loc))
        pairs = unique_pairs

    pairs.sort(key=lambda item: (item[0], item[1]))

    out_lines: List[str] = []
    if not args.caps_only and not args.localities_only:
        if not args.no_header:
            out_lines.append("cap,locality")
        for cap, loc in pairs:
            import io
            temp = io.StringIO()
            writer = csv.writer(temp, lineterminator="")
            writer.writerow([cap, loc])
            out_lines.append(temp.getvalue())
    elif args.caps_only:
        seen_caps: Set[str] = set()
        for cap, _ in pairs:
            if args.unique:
                if cap in seen_caps:
                    continue
                seen_caps.add(cap)
            out_lines.append(cap)
    elif args.localities_only:
        seen_locs: Set[str] = set()
        for _, loc in pairs:
            if args.unique:
                if loc in seen_locs:
                    continue
                seen_locs.add(loc)
            out_lines.append(loc)

    output = "\n".join(out_lines) + ("\n" if out_lines and not out_lines[-1].endswith("\n") else "")

    if args.out:
        with open(args.out, "w", encoding="utf-8", newline="") as f:
            f.write(output)
    else:
        sys.stdout.write(output)


if __name__ == '__main__':
    main()
