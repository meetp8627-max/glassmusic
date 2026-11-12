#!/usr/bin/env python3
"""Builds the public release description for a GlassMusic release.

Order of preference for the "what changed" part:
  1. the matching section of CHANGELOG.md (hand-written, best quality)
  2. the commit messages since the previous release, filtered and grouped
"""
import argparse
import pathlib
import re
import subprocess

# Lines that must never appear in the public description.
HIDDEN = re.compile(r"(?i)glas+mio")

# Commit subjects that are build plumbing, not user-facing changes.
NOISE = re.compile(
    r"(?i)(apply uploaded zip|^merge\b|^wip\b|^revert\b|\bgradle\b|\bagp\b|\bkotlin\b|\bsdk\b|"
    r"\bworkflows?\b|\bci\b|\bimports?\b|\bsetters?\b|client id|restore project|version bump|"
    r"\bzip\b|\bproguard\b|\bkeystore\b|\bsigning\b|\breadme\b|\bchangelog\b|\bnotices?\b|"
    r"\btypo\b|\bgitignore\b|\bdependenc)"
)

PREFIX = re.compile(r"^(feat|fix|perf|refactor|chore|docs|style|build|ci|test)(\([^)]*\))?!?:\s*", re.I)
ROUND = re.compile(r"^round\s*\d+\s*[:\-]\s*", re.I)


def run(*args):
    return subprocess.run(args, capture_output=True, text=True, check=False).stdout.strip()


def curated(version):
    path = pathlib.Path("CHANGELOG.md")
    if not path.exists():
        return None
    text = path.read_text(encoding="utf-8")
    m = re.search(r"^##\s*\[?%s\]?[^\n]*\n(.*?)(?=^##\s|\Z)" % re.escape(version), text, re.S | re.M)
    return m.group(1).strip() if m else None


def classify(subject):
    kind = None
    m = PREFIX.match(subject)
    if m:
        kind = m.group(1).lower()
        subject = subject[m.end():]
        if kind in ("chore", "docs", "build", "ci", "test"):
            return None, subject
    subject = ROUND.sub("", subject).strip().rstrip(".")
    low = subject.lower()
    if kind == "fix" or re.match(r"^(fix|fixed|hotfix|resolve|repair)\b", low):
        return "fixes", subject
    if kind == "feat" or re.match(r"^(add|added|new|introduce|support|implement|create)\b", low):
        return "new", subject
    if kind in ("perf", "refactor", "style") or re.search(
        r"\b(perf|optimi[sz]\w*|faster|smooth\w*|speed\w*|lag|lite|lighter|improv\w*|polish\w*|refine\w*|"
        r"tweak\w*|animation\w*|redesign\w*|update\w*)\b", low
    ):
        return "improvements", subject
    if re.search(r"\b(crash\w*|bug\w*|broken)\b", low):
        return "fixes", subject
    return "other", subject


def from_commits(prev):
    rng = [f"{prev}..HEAD"] if prev else ["-n", "60", "HEAD"]
    raw = run("git", "log", "--no-merges", "--pretty=format:%s", *rng)
    groups = {"new": [], "improvements": [], "fixes": [], "other": []}
    seen = set()
    for subject in raw.splitlines():
        subject = subject.strip()
        if not subject or HIDDEN.search(subject) or NOISE.search(subject):
            continue
        kind, text = classify(subject)
        if not kind or not text:
            continue
        text = text[0].upper() + text[1:]
        if text.lower() in seen:
            continue
        seen.add(text.lower())
        groups[kind].append(text)
    parts = []
    titles = (("new", "New"), ("improvements", "Improvements"), ("fixes", "Fixes"), ("other", "Other changes"))
    for key, title in titles:
        if groups[key]:
            parts.append(f"### {title}\n" + "\n".join(f"- {t}" for t in groups[key]))
    if not parts:
        parts.append("### Improvements\n- Stability and performance improvements.")
    return "\n\n".join(parts)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", required=True)
    ap.add_argument("--tag", required=True)
    ap.add_argument("--repo", required=True)
    ap.add_argument("--prev", default="")
    ap.add_argument("--out", required=True)
    a = ap.parse_args()

    body = curated(a.version) or from_commits(a.prev)
    body = "\n".join(line for line in body.splitlines() if not HIDDEN.search(line)).strip()
    if not re.search(r"^###\s", body, re.M) and not body.startswith("###"):
        body = "### What's changed\n" + body

    base = f"https://github.com/{a.repo}"
    lines = [
        f"## GlassMusic {a.version}",
        "",
        body,
        "",
        "### Download",
        "| File | For |",
        "|---|---|",
        "| **GlassMusic.apk** | Android 13 or newer (recommended) |",
        "| GlassMusic-Classic.apk | Android 8.0 to 12 |",
        "| SHA256SUMS.txt | Checksums to verify your download |",
        "",
        "### Install",
        "1. Download **GlassMusic.apk** on your phone.",
        "2. Open it and allow installs from your browser or file manager if Android asks.",
        "3. Updating? Install it over the previous version.",
        "",
    ]
    if a.prev:
        lines.append(f"**Full changelog:** {base}/compare/{a.prev}...{a.tag}")
        lines.append("")
    lines.append(f"**Licenses and credits:** {base}/blob/{a.tag}/THIRD_PARTY_NOTICES.md")
    lines.append("")

    text = "\n".join(lines)
    text = "\n".join(line for line in text.splitlines() if not HIDDEN.search(line)) + "\n"
    pathlib.Path(a.out).write_text(text, encoding="utf-8")


if __name__ == "__main__":
    main()
