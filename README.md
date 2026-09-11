# Retenqo

Personal phone assistant and memory for Android. Capture context from screens, notifications, WhatsApp media, voice, and files — then ask in plain text or by voice. Gemini handles generation; OCR, Whisper, storage, and retrieval remain on-device.

**Project root:** `~/retenqo` (GitHub repo: [mneme](https://github.com/priyanshuchawda/mneme))

**Author:** [Priyanshu Chawda](https://github.com/priyanshuchawda)

## Daily loop (text-first)

```
Pulse (screen) / WA notif / WA media → structured note + embed
Ask / Due → grounded memory answers or Gemini help for general phone questions
Due dates → morning reminder notification
```

### On-device stack (Phone 1 — Redmi 13 5G)

| Job | Engine |
|-----|--------|
| OCR | ML Kit |
| Extract + Ask | Gemini 3.5 Flash Lite |
| Embeddings | Lightweight hashed vectors |
| Due reminders | AlarmManager + notifications |

Voice (Whisper), notification access, screen capture, and optional accessibility context require explicit Android permissions.

## App

```
android/mneme/       # product app
android/mneme-test/  # model lab harness
```

### Build & install

```bash
./scripts/build-mneme.sh
./scripts/install-mneme.sh
./scripts/test-mneme.sh
```

JDK **21** for Gradle (`~/.local/jdk-21`). App bytecode Java **17**.

### GitHub

```bash
git checkout -b feature/your-change
# stage product files …
./scripts/commit-and-pr.sh "Why this change" "PR title"
gh pr merge <n> --merge
```

Repo: https://github.com/priyanshuchawda/mneme  

Internal Android package stays `com.mneme.app` (safe upgrades). Brand name is **Retenqo**.

## Docs

- [idea.md](./idea.md) — product vision  
- [docs/ROADMAP.md](./docs/ROADMAP.md) — GitHub issues & build order
- [MODELS.md](./MODELS.md) — model catalog  
- [DEV_DEVICE_REDMi13.md](./DEV_DEVICE_REDMi13.md) — Phone 1 notes  
- [phone2.md](./phone2.md) — second device notes  
- [eval-results/REPORT.md](./eval-results/REPORT.md) — measured evals  

## Status

- Pulse → OCR → note → embed  
- WhatsApp media watcher (battery-aware)  
- WhatsApp notification text ingest  
- Ask with refuse / citation guard  
- Due digest + morning reminders  
- Unit tests: `./scripts/test-mneme.sh`
