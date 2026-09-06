#!/usr/bin/env python3
"""
Headless UI crawler for the Ultimate Brain Android app.

Drives the running app on a connected device through a BFS over the uiautomator
tree. After each tap it screencaps and hash-dedupes. Resets between destructive
taps via force-stop + relaunch.

Output: ./screenshots/NNN_<slug>.png + manifest.json + crawl.log

Usage:
    python crawl.py [--adb-path PATH] [--serial SERIAL] [--out DIR]
                    [--max-screens N] [--start-screen NAME] [--verbose]

Stdlib only. No third-party deps.
"""

import argparse
import hashlib
import json
import logging
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

PACKAGE = "com.aistudio.ultimatebrain.ptvz"
ACTIVITY = "com.example.MainActivity"
DEFAULT_ADB = r"C:\Android\Sdk\platform-tools\adb.exe"

# Tap-safety: words that signal a destructive action. Skip these taps
# to avoid confirm-dialog loops and accidental data loss.
DESTRUCTIVE_TOKENS = (
    "delete", "archive", "remove", "logout", "cancel",
    "trash", "destroy", "drop",
)

# FAB safe-list: known-OK FAB actions. Anything outside this in the
# bottom-right region is skipped by default (the FAB region is too
# likely to fire an irreversible action).
FAB_SAFE_TOKENS = ("add", "new", "search", "create", "compose", "filter")

# After-settle delay constants.
AFTER_SCREENCAP = 0.8
AFTER_TAP = 0.5
AFTER_LAUNCH = 2.0

# Slug cap.
SLUG_MAX_LEN = 30

# Status bar / system insets — skip clicks in this top band.
# Phones report the status bar in roughly the top 3% of the screen.
# Using a fixed pixel band is more reliable across devices than a ratio
# because we don't know the device resolution until we parse the UI dump.
STATUS_BAR_PX = 96

# Output filenames inside the out dir.
LOG_FILENAME = "crawl.log"
MANIFEST_FILENAME = "manifest.json"


# ---------------------------------------------------------------------------
# ADB helpers
# ---------------------------------------------------------------------------

@dataclass
class AdbSession:
    adb_path: str
    serial: Optional[str] = None

    def _adb(self, *args: str, check: bool = True) -> subprocess.CompletedProcess:
        cmd = [self.adb_path]
        if self.serial:
            cmd += ["-s", self.serial]
        cmd += list(args)
        return subprocess.run(
            cmd,
            capture_output=True,
            text=check is False,
            check=check,
            timeout=30,
        )

    def devices(self) -> list[tuple[str, str]]:
        out = self._adb("devices", check=False).stdout or ""
        rows = []
        for line in out.splitlines()[1:]:
            line = line.strip()
            if not line or "List of devices" in line:
                continue
            parts = line.split()
            if len(parts) >= 2:
                rows.append((parts[0], parts[1]))
        return rows

    def pick_serial(self) -> str:
        rows = self.devices()
        online = [r for r in rows if r[1] == "device"]
        if not online:
            raise SystemExit(
                "No online Android devices found. Connect a device and retry.\n"
                "Run 'adb devices' manually to debug."
            )
        if len(online) == 1:
            self.serial = online[0][0]
            return self.serial
        # Multiple devices: pick the first online one.
        self.serial = online[0][0]
        return self.serial

    def shell(self, cmd: str, check: bool = True) -> str:
        cp = self._adb("shell", cmd, check=check)
        return cp.stdout or ""

    def screencap(self, dst: Path) -> bytes:
        # Windows-safe: exec-out + binary stdout. Avoids `adb shell screencap` \r\n corruption.
        cmd = [self.adb_path]
        if self.serial:
            cmd += ["-s", self.serial]
        cmd += ["exec-out", "screencap", "-p"]
        with open(dst, "wb") as f:
            cp = subprocess.run(cmd, stdout=f, timeout=30)
            if cp.returncode != 0:
                raise RuntimeError(f"screencap failed: rc={cp.returncode}")
        return dst.read_bytes()

    def dump_ui(self, dst: Path) -> Optional[bytes]:
        # Force the dump to /sdcard, then pull.
        self._adb("shell", "uiautomator dump /sdcard/crawl_ui.xml", check=False)
        cp = self._adb("pull", "/sdcard/crawl_ui.xml", str(dst), check=False)
        if cp.returncode != 0:
            return None
        if not dst.exists():
            return None
        return dst.read_bytes()

    def tap(self, x: int, y: int) -> None:
        self._adb("shell", f"input tap {x} {y}", check=False)

    def keyevent(self, code: str) -> None:
        self._adb("shell", f"input keyevent {code}", check=False)

    def force_stop(self) -> None:
        self._adb("shell", f"am force-stop {PACKAGE}", check=False)

    def start_app(self) -> None:
        self._adb(
            "shell",
            f"am start -n {PACKAGE}/{ACTIVITY}",
            check=False,
        )

    def current_focus(self) -> str:
        out = self.shell("dumpsys window 2>/dev/null | grep mCurrentFocus", check=False)
        return out.strip()


# ---------------------------------------------------------------------------
# UI tree parsing
# ---------------------------------------------------------------------------

@dataclass
class Candidate:
    x: int
    y: int
    text: str
    content_desc: str
    resource_id: str
    cls: str
    is_test_tag: bool
    in_edit_text: bool
    in_fab_region: bool
    destructive: bool
    bounds: str = ""

    @property
    def label(self) -> str:
        # Prefer human-readable label.
        for s in (self.text, self.content_desc, self.resource_id):
            if s:
                return s
        return self.cls


_BOUNDS_RE = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


def parse_bounds(bounds: str) -> Optional[tuple[int, int, int, int]]:
    m = _BOUNDS_RE.match(bounds)
    if not m:
        return None
    return tuple(int(g) for g in m.groups())  # x1,y1,x2,y2


def parse_ui(xml_bytes: bytes, screen_h: int = 2400, screen_w: int = 1080) -> list[Candidate]:
    """Return clickable nodes ordered top-to-bottom-left-to-right.

    Flags destructive / testTag / editText / fab-region candidates so the
    caller can filter them.
    """
    try:
        root = ET.fromstring(xml_bytes)
    except ET.ParseError:
        return []

    def collect_text(n: ET.Element) -> str:
        """Compose's clickable ancestor rarely carries `text` directly —
        the label lives on a child Text composable. Walk descendants and
        concatenate non-empty `text` attrs."""
        parts: list[str] = []
        for sub in n.iter("node"):
            t = (sub.get("text") or "").strip()
            if t:
                parts.append(t)
            d = (sub.get("content-desc") or "").strip()
            if d and d != t:
                parts.append(d)
        return " ".join(parts[:6])

    # First pass: collect all clickable nodes with their text/desc/resource-id/class.
    raw: list[tuple[int, int, Candidate]] = []
    for node in root.iter("node"):
        if node.get("clickable") != "true":
            continue
        if node.get("enabled") == "false":
            continue
        bounds = node.get("bounds") or ""
        b = parse_bounds(bounds)
        if b is None:
            continue
        x1, y1, x2, y2 = b
        if x2 <= x1 or y2 <= y1:
            continue
        # Skip status-bar region — those taps bounce to the launcher.
        if y1 < STATUS_BAR_PX:
            continue
        cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
        text = (node.get("text") or "").strip()
        desc = (node.get("content-desc") or "").strip()
        rid = (node.get("resource-id") or "").strip()
        cls = (node.get("class") or "").strip()
        # If neither ancestor nor descendants carry text/desc, fall back
        # to descendant walk for the slug-derivation pass later.
        full_text = text or desc or collect_text(node)
        # FAB region: bottom-right corner. Heuristic: bottom 18% vertically
        # AND right 22% horizontally.
        in_fab = (y2 >= screen_h * 0.82) and (x1 >= screen_w * 0.78)
        is_test_tag = "testTag" in rid or "/test_tag/" in rid
        raw.append((cy, cx, Candidate(
            x=cx, y=cy, text=full_text, content_desc=desc,
            resource_id=rid, cls=cls,
            is_test_tag=is_test_tag,
            in_edit_text=cls.endswith("EditText"),
            in_fab_region=in_fab,
            destructive=False,
            bounds=bounds,
        )))

    # Sort by (y, x) so top-down left-right traversal is the default order.
    raw.sort(key=lambda t: (t[0], t[1]))
    candidates = [c for _, _, c in raw]

    # Mark destructive: any node whose text/desc contains a destructive token.
    for c in candidates:
        haystack = (c.text + " " + c.content_desc).lower()
        c.destructive = any(tok in haystack for tok in DESTRUCTIVE_TOKENS)
    return candidates


# ---------------------------------------------------------------------------
# Slug + hashing
# ---------------------------------------------------------------------------

_SLUG_STRIP = re.compile(r"[^a-z0-9]+")


def make_slug(texts: list[str]) -> str:
    parts: list[str] = []
    for t in texts:
        t = t.strip()
        if not t:
            continue
        # Strip newlines and collapse.
        t = t.replace("\n", " ")
        # Split into words, take first chunk.
        for chunk in re.split(r"\s+", t):
            chunk = chunk.lower()
            chunk = _SLUG_STRIP.sub("_", chunk).strip("_")
            if not chunk:
                continue
            parts.append(chunk)
            if len(parts) >= 4:
                break
        if len(parts) >= 4:
            break
    slug = "_".join(parts)[:SLUG_MAX_LEN].strip("_")
    return slug or "screen"


def md5_bytes(b: bytes) -> str:
    return hashlib.md5(b).hexdigest()


# ---------------------------------------------------------------------------
# Crawler core
# ---------------------------------------------------------------------------

@dataclass
class CaptureRecord:
    index: int
    file: str
    hash: str
    first_text_nodes: list[str]
    tap_path: list[dict] = field(default_factory=list)


def setup_logging(out_dir: Path, verbose: bool) -> logging.Logger:
    log = logging.getLogger("crawl")
    log.setLevel(logging.DEBUG if verbose else logging.INFO)
    # File handler.
    fh = logging.FileHandler(out_dir / LOG_FILENAME, mode="w", encoding="utf-8")
    fh.setFormatter(logging.Formatter("%(asctime)s %(levelname)s %(message)s"))
    log.addHandler(fh)
    # Console handler.
    ch = logging.StreamHandler(sys.stderr)
    ch.setFormatter(logging.Formatter("%(asctime)s %(levelname)s %(message)s"))
    log.addHandler(ch)
    return log


def wait_for_focus(adb: AdbSession, want: str = PACKAGE, timeout_s: float = 5.0) -> bool:
    """Block until mCurrentFocus contains `want`, or timeout."""
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        focus = adb.current_focus()
        if want in focus:
            return True
        time.sleep(0.2)
    return False


def safe_to_tap(c: Candidate) -> tuple[bool, str]:
    """Filter logic for tap candidates. Returns (ok, reason)."""
    if c.destructive:
        return False, "destructive"
    if c.in_edit_text:
        return False, "in_edit_text"
    if c.in_fab_region:
        haystack = (c.text + " " + c.content_desc).lower()
        if not any(tok in haystack for tok in FAB_SAFE_TOKENS):
            return False, "fab_region_unsafe"
    return True, "ok"


def run_crawl(args: argparse.Namespace) -> int:
    out_dir: Path = Path(args.out).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)
    shots_dir = out_dir / "screenshots"
    shots_dir.mkdir(exist_ok=True)

    log = setup_logging(out_dir, args.verbose)
    log.info("output dir: %s", out_dir)

    # Pick a serial.
    adb = AdbSession(adb_path=args.adb_path, serial=args.serial)
    if adb.serial is None:
        adb.pick_serial()
    log.info("device serial: %s", adb.serial)

    # Verify focus is on the app; relaunch if not.
    if PACKAGE not in adb.current_focus():
        log.info("focus not on app; force-stop + relaunch")
        adb.force_stop()
        time.sleep(0.5)
        adb.start_app()
        time.sleep(AFTER_LAUNCH)
    wait_for_focus(adb)
    log.info("focus confirmed on %s", PACKAGE)

    # BFS state.
    visited_hashes: set[str] = set()
    manifest: list[CaptureRecord] = []
    tap_path: list[dict] = []
    max_screens = args.max_screens
    counter = 0

    # Helper: capture + record + parse UI for the current state.
    def capture_current(reason: str) -> tuple[Optional[bytes], list[Candidate], list[str]]:
        """Returns (png_bytes, candidates, first_text_nodes)."""
        tmp = out_dir / "_tmp_screen.png"
        try:
            png = adb.screencap(tmp)
        except Exception as exc:
            log.error("screencap failed: %s", exc)
            return None, [], []
        time.sleep(AFTER_SCREENCAP)
        ui_tmp = out_dir / "_tmp_ui.xml"
        ui_bytes = adb.dump_ui(ui_tmp)
        if ui_bytes is None:
            log.warning("uiautomator dump failed after %s", reason)
            return png, [], []
        cands = parse_ui(ui_bytes)
        # First 1-4 text-bearing nodes for slug derivation. Prefer
        # candidates' text (already enriched with descendant walk), but
        # fall back to a top-down scan of every node so we always get
        # something meaningful — even if the screen has zero clickables.
        texts: list[str] = []
        seen: set[str] = set()
        for c in cands:
            for piece in c.text.split():
                if piece and piece not in seen:
                    texts.append(piece)
                    seen.add(piece)
                    if len(texts) >= 4:
                        break
            if len(texts) >= 4:
                break
        if len(texts) < 4:
            # Scan the whole tree top-down for any visible text.
            try:
                tree = ET.fromstring(ui_bytes)
                for n in tree.iter("node"):
                    t = (n.get("text") or "").strip()
                    if not t or t in seen:
                        continue
                    seen.add(t)
                    texts.append(t)
                    if len(texts) >= 4:
                        break
            except ET.ParseError:
                pass
        return png, cands, texts

    # Capture initial state.
    log.info("capturing initial state (cold start)")
    png, cands, texts = capture_current("cold_start")
    if png is None:
        log.error("initial capture failed; aborting")
        return 1

    # BFS: pull candidates off the queue, tap, capture, enqueue new candidates.
    queue: list[Candidate] = list(cands)
    queue_index = 0  # monotonic pointer so we never revisit a candidate we already tried

    while counter < max_screens:
        # 1. Save current state.
        h = md5_bytes(png)
        if h not in visited_hashes:
            visited_hashes.add(h)
            counter += 1
            # Slug sources: page text + the last tap's label (so each state
            # is distinguishable even when the same My Day sub-section
            # is reached via different chip taps).
            last_tap = tap_path[-1]["label"] if tap_path else ""
            slug_sources: list[str] = []
            if last_tap:
                slug_sources.append(last_tap)
            slug_sources.extend(texts)
            slug = make_slug(slug_sources)
            fname = f"{counter:03d}_{slug}.png"
            (shots_dir / fname).write_bytes(png)
            manifest.append(CaptureRecord(
                index=counter,
                file=fname,
                hash=h,
                first_text_nodes=texts[:4],
                tap_path=list(tap_path),
            ))
            log.info("[%d/%d] captured %s (texts=%s)", counter, max_screens, fname, texts[:2])
        else:
            log.debug("state hash %s already visited; skipping capture", h[:8])

        # 2. Pop next candidate.
        if queue_index >= len(queue):
            log.info("queue exhausted after %d captures", counter)
            break
        c = queue[queue_index]
        queue_index += 1

        ok, reason = safe_to_tap(c)
        if not ok:
            log.debug("skip tap at (%d,%d) — %s (%s)", c.x, c.y, reason, c.label[:30])
            continue

        # 3. Tap.
        log.debug("tap (%d,%d) — %s", c.x, c.y, c.label[:40])
        adb.tap(c.x, c.y)
        time.sleep(AFTER_TAP)

        # 4. Check focus; relaunch if needed.
        focus = adb.current_focus()
        if PACKAGE not in focus:
            log.warning("focus lost after tap (%s); relaunching", focus)
            adb.force_stop()
            time.sleep(0.3)
            adb.start_app()
            time.sleep(AFTER_LAUNCH)
            wait_for_focus(adb)
            tap_path.clear()
            # After relaunch, rebuild queue from current screen.
            png, cands, texts = capture_current("after_relaunch")
            queue = list(cands)
            queue_index = 0
            continue

        # 5. Forward-only: capture the new state, extend the queue with any
        # newly discovered clickable nodes, don't auto-back. Auto-back was
        # bouncing us out of My Day to the launcher because routine taps
        # (e.g. ritual-jump-bar segments) don't change the screen and the
        # BACK keystroke actually pops the navigation stack.
        tap_path.append({"x": c.x, "y": c.y, "label": c.label[:40], "text": c.text[:40]})
        post_png, post_cands, post_texts = capture_current("after_tap")
        if post_png is not None:
            png = post_png
            cands = post_cands
            texts = post_texts
        seen_coords = {(q.x, q.y) for q in queue}
        for nc in cands:
            if (nc.x, nc.y) in seen_coords:
                continue
            ok, _ = safe_to_tap(nc)
            if not ok:
                continue
            queue.append(nc)
            seen_coords.add((nc.x, nc.y))

    # Write manifest.
    manifest_data = [
        {
            "index": r.index,
            "file": r.file,
            "hash": r.hash,
            "first_text_nodes": r.first_text_nodes,
            "tap_path": r.tap_path,
        }
        for r in manifest
    ]
    (out_dir / MANIFEST_FILENAME).write_text(
        json.dumps(manifest_data, indent=2, ensure_ascii=False),
        encoding="utf-8",
    )
    log.info("wrote manifest with %d entries", len(manifest_data))
    log.info("done. %d unique screens captured.", len(manifest_data))
    return 0


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args(argv: list[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Headless Android screen crawler.")
    p.add_argument("--adb-path", default=DEFAULT_ADB, help="Path to adb.exe (default: %(default)s)")
    p.add_argument("--serial", default=None, help="Device serial (default: auto-pick first online)")
    p.add_argument("--out", default=str(Path(__file__).parent / "out"),
                   help="Output directory (default: ./out relative to this script)")
    p.add_argument("--max-screens", type=int, default=80,
                   help="Cap on distinct screen captures (default: %(default)s)")
    p.add_argument("--start-screen", default=None,
                   help="Reserved for future use; currently the app always cold-starts")
    p.add_argument("--verbose", action="store_true", help="Verbose log output")
    return p.parse_args(argv)


def main(argv: Optional[list[str]] = None) -> int:
    args = parse_args(argv if argv is not None else sys.argv[1:])
    try:
        return run_crawl(args)
    except KeyboardInterrupt:
        print("\naborted by user", file=sys.stderr)
        return 130


if __name__ == "__main__":
    raise SystemExit(main())
