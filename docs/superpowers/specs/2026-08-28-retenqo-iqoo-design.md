# Retenqo — iQOO City Battle design

**Date:** 2026-08-28
**Track:** 04 Productivity (Pune 5–6 Sep; Finale-eligible)
**Lead:** Priyanshu Chawda
**Status:** Frozen for Round 1 + 30h (features may be added later; demo spine does not change)

## 1. Product

**Retenqo** is a **private on-device commitment helper**, not a general assistant and not a phone agent.

It stores **cards** (`due | promise | waiting | fact | ask_for_you`) captured from the phone, indexes them with **embeddings**, and answers **only from retrieved cards** (RAG). Background work is **notification ingest + optional Pulse/Focus**, not 24/7 screenshots.

**One sentence:** The iQOO compiles what you saw and heard into owe/promise/waiting/due cards, then brief / Ask / Due — airplane-capable.

**Primary user for the pitch:** working professional (Calendar + WhatsApp + meetings). The same loop works for students (LMS instead of Calendar). Demo names are **scripted** (e.g. Priya) — not real contacts.

## 2. Non-goals

- GUI agent (tap WhatsApp, auto-send, auto-pay)
- Cloud as the core brain (OpenRouter is a parachute only)
- Magical full Google account dump without capture
- Shipping the existing Redmi APK as the **event entry** (libs OK; 30h must evolve/rebuild on the loaner)
- Fake Hexagon tok/s recorded on a mid-range phone

## 3. Information flow

```
WA notifs + media, Calendar/Gmail/LMS notifs, Pulse, Focus (screen+mic)
        → rules extract (cheap) → rare local LLM
        → Room card + embedding
        → Catch-up brief | Ask (vector top-k + cite) | Due
```

**Today (prototype `com.mneme.app`):** Pulse, WA notifs/media, notification listener (including Calendar **notifications** if the system posts them), Ask with HashEmbed + optional Qwen, due digest.

**Not required for Round 1:** Google Calendar REST API, in-app WhatsApp, person-graph UI, draft-to-send.

**Person merge** means: same display name on two **notifications** → two cards that Ask can cite together. No WhatsApp client inside Retenqo.

**Ephemeral Pulse** means: screenshot processed → **text card kept** → **image deleted**. Gallery must be empty of that capture.

## 4. Demo spine (do not change)

Scripted seed: Calendar-style notif “Priya 4pm” + WA-style “Priya 5pm” + one bill/timetable image.

| Step | Beat | Why |
|------|------|-----|
| 1 | Catch-up (since last open) | Usefulness in 2s |
| 2 | Ask “When is Priya?” → **conflict** (two sources) | Novelty; not a chatbot |
| 3 | Image → due card (local) | Phone + vision/OCR |
| 4 | Airplane + “what do I owe?” + citation | On-device / privacy |
| 5 | Pulse sensitive dummy screen → gallery empty | Ephemeral pixels |
| 6 | Optional: two notifs, same person | Merge-by-name |
| 7 | Focus 10–15s live, or 40s clip **4x** with caption *real-time, sped up* | iQOO NPU chapter |
| 8 | Mark one card done | Keep using it (30%) |

**Scale-only (slides, not the 3-minute speech):** waiting > 3 days, draft-you-send, quiet hours, spoken standup.

**Video (75–90s):** cold-open airplane Ask → steps 1–5 → Focus 4x. Captions on. No IDE.

**Round 1:** film on daily phone; label *no Hexagon*. **Floor:** same script on iQOO 15.

## 5. Scoring map

| Weight | Dimension | Evidence |
|--------|-----------|----------|
| 30% | Product | Airplane Ask, cards, mark done |
| 20% | Novelty | Conflict + ephemeral Pulse + Focus, not an agent |
| 15% | HackTracker phone | MediaProjection/Pulse, mic optional, **on-device** model |
| 15% | Depth | Rules→LLM, RAG, citations, `AiBackend` QNN/GGUF |
| 10% | Office Kit | Windows: push GGUF/LiteRT in Green; Red Light via Kit |
| 10% | Pitch | 3–5 min **on the phone** |

## 6. Architecture (30h)

- **Capture:** NotificationListener (allowlist), Pulse (MediaProjection + ML Kit), Focus (sampled frames + optional Whisper), media watcher for WA images/PDFs.
- **Extract:** `RulesExtract` first; local Qwen/Gemma JSON when rules miss.
- **Store:** Room notes + embedding column (HashEmbed now; Qwen3-Embedding / EmbeddingGemma on iQOO).
- **Ask:** intent shortcuts → else retrieve top-k → short complete with cite guard → extractive fallback.
- **AiBackend:** llama.cpp GGUF parachute; LiteRT-QNN on Hexagon when it loads. Never show fake HUD numbers.
- **Office Kit:** model files laptop → phone; Red Light input through Kit.

## 7. Error handling

- Ask with no hits → refuse: *I don't have that in your memory.*
- LLM OOM / airplane → extractive titles from top hit.
- Focus too slow on loaner → Pulse + photo-due + airplane still win the round.
- QNN fail → GGUF on 8 Elite CPU; HUD says CPU/GPU honestly.

## 8. Testing

- Unit: card ask intents, catch-up brief, extract rules (`./scripts/test-mneme.sh`).
- Demo rehearsal: seed script + seven stills + airplane.
- Event: same tests on iQOO after port.

## 9. Round 1 assets

- 14-slide 16:9 deck (All-Knighters density): problem photo, USP chain, 7-proof strip, 10-block architecture, eval table, 30h delta, links.
- Unlisted 90s video.
- GitHub `priyanshuchawda/mneme` as experiment log, labeled prototype.
- Form blurb: private RAG over commitments; Pulse + chat/notifs; Focus on NPU; airplane.

## 10. 30h delta (only)

Live short Focus, real NPU path if possible, WA/gallery image latency, Office Kit model push, done/snooze if missing. Do not build watchers, AR, Calendar REST, or an agent loop.
