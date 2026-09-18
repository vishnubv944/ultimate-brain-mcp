#!/usr/bin/env python3
"""Aggregate today's voice activity log into a latency + cost report.

Runs locally on hermes (or anywhere with read access to the JSONL log).

Usage:
    python cost-report.py                 # today
    python cost-report.py 2026-09-18      # specific date
    python cost-report.py --json          # machine-readable output
"""
from __future__ import annotations

import argparse
import json
import statistics
import sys
from datetime import datetime, timezone
from pathlib import Path

LOG_DIR = Path(
    __import__("os").environ.get("HERMES_HOME", "/home/vishnubv944/.hermes")
) / "voice-live-notes"

# Pricing (USD) — update if providers change.
DEEPGRAM_NOVA3_PER_MIN = 0.0043      # https://deepgram.com/pricing
CARTESIA_SONIC3_PER_CHAR = 0.000035  # https://cartesia.ai/pricing
OPENAI_GPT4O_MINI_IN = 0.15 / 1e6    # input tokens
OPENAI_GPT4O_MINI_OUT = 0.60 / 1e6   # output tokens


def load_events(date: str) -> list[dict]:
    path = LOG_DIR / f"{date}.jsonl"
    if not path.exists():
        return []
    events = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            continue
        try:
            events.append(json.loads(line))
        except json.JSONDecodeError:
            continue
    return events


def estimate_costs(events: list[dict]) -> dict:
    """Estimate Deepgram/Cartesia cost from event text length.

    Deepgram: audio seconds not logged separately, so we approximate from
    user_speech text length (assume 150 wpm ≈ 12.5 chars/sec).
    Cartesia: use agent_speech text length (one char at a time).
    """
    user_chars = sum(len(e.get("text", "")) for e in events if e.get("event") == "user_speech")
    agent_chars = sum(len(e.get("text", "")) for e in events if e.get("event") == "agent_speech")
    user_audio_sec = user_chars / 12.5
    return {
        "deepgram_usd": round(user_audio_sec / 60 * DEEPGRAM_NOVA3_PER_MIN, 4),
        "cartesia_usd": round(agent_chars * CARTESIA_SONIC3_PER_CHAR, 4),
        "openai_input_tokens_est": int(user_chars / 4),
        "openai_output_tokens_est": int(agent_chars / 4),
        "openai_usd_est": round(
            (user_chars / 4) * OPENAI_GPT4O_MINI_IN
            + (agent_chars / 4) * OPENAI_GPT4O_MINI_OUT,
            4,
        ),
        "total_usd_est": 0.0,
    }


def summarize_latency(events: list[dict]) -> dict:
    samples: dict[str, list[float]] = {
        "stt_ms": [],
        "llm_ttft_ms": [],
        "tts_ttfb_ms": [],
    }
    for ev in events:
        if ev.get("event") != "agent_speech":
            continue
        for key in samples:
            v = ev.get(key)
            if isinstance(v, (int, float)):
                samples[key].append(v)
    out: dict[str, dict] = {}
    for key, vals in samples.items():
        if not vals:
            out[key] = None
            continue
        out[key] = {
            "p50": round(statistics.median(vals), 1),
            "p95": (
                round(sorted(vals)[int(len(vals) * 0.95) - 1], 1)
                if len(vals) >= 20
                else round(max(vals), 1)
            ),
            "n": len(vals),
        }
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("date", nargs="?", default=datetime.now(timezone.utc).strftime("%Y-%m-%d"))
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    events = load_events(args.date)
    costs = estimate_costs(events)
    costs["total_usd_est"] = round(
        costs["deepgram_usd"] + costs["cartesia_usd"] + costs["openai_usd_est"], 4
    )
    latency = summarize_latency(events)
    sessions = sum(1 for e in events if e.get("event") == "session_start")
    turns = sum(1 for e in events if e.get("event") == "agent_speech")
    errors = sum(1 for e in events if e.get("event") == "error")

    report = {
        "date": args.date,
        "sessions": sessions,
        "turns": turns,
        "errors": errors,
        "costs": costs,
        "latency": latency,
    }

    if args.json:
        print(json.dumps(report, indent=2))
        return 0

    print(f"=== Voice mode report for {args.date} ===")
    print(f"  Sessions: {sessions}")
    print(f"  Turns:    {turns}")
    print(f"  Errors:   {errors}")
    print()
    print("  Latency (ms):")
    for stage, stats in latency.items():
        if stats is None:
            print(f"    {stage}: no samples")
            continue
        print(f"    {stage}: p50={stats['p50']}  p95={stats['p95']}  n={stats['n']}")
    print()
    print("  Cost estimate (USD):")
    print(f"    Deepgram Nova-3: ${costs['deepgram_usd']:.4f}")
    print(f"    Cartesia Sonic-3: ${costs['cartesia_usd']:.4f}")
    print(f"    OpenAI (est):     ${costs['openai_usd_est']:.4f}")
    print(f"    Total:            ${costs['total_usd_est']:.4f}")

    if latency.get("llm_ttft_ms") and latency["llm_ttft_ms"]["p95"] > 1500:
        print()
        print("  ⚠️  LLM TTFT p95 above 1500ms — investigate gateway latency.")
    if costs["total_usd_est"] > 5.0:
        print()
        print(f"  ⚠️  Estimated daily cost ${costs['total_usd_est']:.2f} > $5 — review usage.")

    return 0


if __name__ == "__main__":
    sys.exit(main())
