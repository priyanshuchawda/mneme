# Mneme — offline personal memory

Private notes from your phone screen, notifications, and voice — answered offline with citations. Your context never leaves the device.

**Author:** [Priyanshu Chawda](https://github.com/priyanshuchawda)

## Daily product (this is the goal)

```
Pulse / notify / share → note in Room
Ask (text or voice) → RAG + Qwen → cite source → optional alarm
```

Validated on-device stack (Phone 1 Redmi 13 5G / Snapdragon arm64):

| Job | Model / API |
|-----|-------------|
| OCR | ML Kit |
| Extract + Q&A | Qwen3.5-2B (llama.cpp JNI) |
| Embeddings | EmbeddingGemma TFLite |
| STT | Whisper base (whisper.cpp JNI) |
| TTS / alarms | Android system |

## App

```
android/mneme/          # product app (Compose + Room + Pulse)
android/mneme-test/     # model lab harness
```

### Build & install (Phone 1)

```bash
./scripts/build-mneme.sh
./scripts/install-mneme.sh          # default serial 1431df87
./scripts/smoke-mneme-ui.sh
```

JDK **21** is pinned for Gradle (`~/.local/jdk-21`). App bytecode stays Java **17**.

### GitHub workflow

```bash
git checkout -b feature/your-change
# ... edit, stage ...
./scripts/commit-and-pr.sh "What changed and why" "PR title"
# review, then:
gh pr merge --squash
```

Issues: https://github.com/priyanshuchawda/mneme-iqoo-hackathon/issues

## Docs

- [idea.md](./idea.md) — product spec  
- [MODELS.md](./MODELS.md) — model catalog  
- [PHONE1_TESTING.md](./PHONE1_TESTING.md) — Phone 1 lab notes  
- [DEV_DEVICE_REDMi13.md](./DEV_DEVICE_REDMi13.md) · [phone2.md](./phone2.md) · [HACKATHON_REFERENCE.md](./HACKATHON_REFERENCE.md)

## Status

- Model JNI validation: **PASS** (OCR, LLM, Whisper, Embed)  
- Product scaffold: Pulse → OCR → Room timeline (in progress on `feature/mneme-product-scaffold`)
