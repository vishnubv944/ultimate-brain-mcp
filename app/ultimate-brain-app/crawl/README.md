# Ultimate Brain Android — Screen Crawler

A headless uiautomator + adb crawler that drives the running app on a connected device, captures a screenshot after every meaningful state change, and hash-dedupes so you end up with one PNG per unique screen.

## What it does

- Force-stops and cold-starts the app (`com.aistudio.ultimatebrain.ptvz/com.example.MainActivity`).
- Dumps the uiautomator XML, parses every clickable node.
- Taps each safe candidate (skipping destructive labels, EditText fields, unsafe FAB regions).
- Screencaps + hashes after each tap. Stops when no new state appears or `--max-screens` is reached.
- Writes a `manifest.json` with provenance (which taps led to which screen).

## Quickstart

```bash
# Plug in an Android device with the app installed, then:
python crawl.py --max-screens 10 --verbose   # smoke test
python crawl.py                                # full run, default 80 screens
```

Output lives in `./screenshots/` (relative to this script) by default. Override with `--out DIR`.

## CLI flags

| Flag | Default | Purpose |
|---|---|---|
| `--adb-path PATH` | `C:\Android\Sdk\platform-tools\adb.exe` | Path to adb.exe |
| `--serial SERIAL` | auto-pick first online | Target a specific device |
| `--out DIR` | `./screenshots` (relative to script) | Where to write PNGs + manifest |
| `--max-screens N` | `80` | Cap on distinct captures |
| `--start-screen NAME` | (cold start) | Reserved |
| `--verbose` | off | Extra log output |

## Output

```
screenshots/
├── 001_my_day.png
├── 002_tasks.png
├── 003_my_day.png          ← skipped if hash matches a prior capture
├── ...
├── manifest.json            ← index, file, hash, first text nodes, tap path
└── crawl.log                ← timestamped progress + every tap result
```

## Safety

The crawler **will not tap**:

- Nodes whose text/desc contains `delete`, `archive`, `remove`, `logout`, `cancel`, `trash`, `destroy`, `drop`
- Nodes inside `android.widget.EditText` (no typing)
- FABs in the bottom-right corner unless labeled `add`, `new`, `search`, `create`, `compose`, or `filter`

If the app loses focus (e.g. the tap launched another activity), the crawler force-stops + relaunches automatically.

## Re-running

The crawler never overwrites an existing file. Re-runs are safe; identical states are skipped via hash.

## Requirements

- Python 3.10+ (uses `match`-style features; tested on 3.13)
- ADB on the host (default path hardcoded for Windows; override with `--adb-path`)
- A connected Android device (USB or wireless ADB) with the Ultimate Brain app installed

## Notes

- The crawler lives outside `app/src/`, so Gradle never sees it.
- Screenshots are gitignored — only the script + README are tracked.
- Notion data is whatever the connected workspace has; empty workspaces will render empty states, which is fine for visual inspection.
