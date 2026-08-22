# On-Device Model Catalog — Mneme (2026)

**Author:** [Priyanshu Chawda](https://github.com/priyanshuchawda) (`priyanshuchawda`)  
**Updated:** 2026-08-22 (selected stacks locked for Phone 1 + Phone 2)  
**Project:** Mneme — offline personal memory assistant (iQOO Hackathon 2026)  
**Devices:** Phone 1 (Redmi 13 5G, 6 GB) · Phone 2 (Note 11 Pro+ 5G, 8 GB) · iQOO 15 loaner (event)

Related docs: [idea.md](./idea.md) · [DEV_DEVICE_REDMi13.md](./DEV_DEVICE_REDMi13.md) · [phone2.md](./phone2.md) · [HACKATHON_REFERENCE.md](./HACKATHON_REFERENCE.md)

---

## Architecture (3 layers)

```
Pulse (screenshot) → OCR or tiny VLM → structured note
Notes → embedding model → vector store (RAG)
Voice → Whisper (batch STT) → LLM + RAG → answer + citation + tools (alarm)
```

**Runtime options:** `llama.cpp` (GGUF, Q4_K_M) · **LiteRT / MediaPipe** (Google) · **ONNX Runtime Mobile**

Neither dev phone has Hexagon NPU — CPU/GPU inference now; **iQOO 15 Hexagon + LiteRT-QNN** at hackathon.

> **Rule for both phones:** Load **one large model at a time** (LLM **or** VLM). Embeddings + Whisper can stay resident. Target **≤3.6 GB** model RAM on Phone 1, **≤5 GB** on Phone 2.

---

## ✅ Selected stacks — USE THESE (Mneme v1)

**Status:** Phone 1 (`1431df87`) = primary build target · Phone 2 (`3e68912fdff2`) = regression + bigger models  
**Last locked:** 2026-08-22

### Phone 1 — Redmi 13 5G (6 GB) — primary dev

| # | Mneme job | **Use this model** | Fallback | Runtime | File / quant | On-disk | RAM (run) | Phase |
|---|-----------|-------------------|----------|---------|--------------|---------|-----------|-------|
| 1 | **Pulse → text** (screenshot) | **ML Kit Text Recognition** | — | Google ML Kit | bundled SDK | 0 | ~0 | **P0 now** |
| 2 | **Pulse → structured note** (JSON) | **Qwen3.5-2B-Instruct** | Qwen2.5-1.5B-Instruct (LiteRT) | llama.cpp JNI | `Q4_K_M` GGUF | ~1.5 GB | ~2.2 GB | **P0 now** |
| 3 | **Voice Q&A + citations** | **Qwen3.5-2B-Instruct** *(same file as #2)* | — | llama.cpp JNI | shared weights | — | shared | **P0 now** |
| 4 | **Intent** (`REMEMBER` / `QUERY` / `ALARM`) | **Keyword + regex rules** | Qwen3.5-0.8B-Instruct | rules → llama.cpp | `Q4_K_M` | ~0.5 GB | ~0.8 GB | P0 rules / P1 model |
| 5 | **RAG embeddings** | **EmbeddingGemma-300M** | Qwen3-Embedding-0.6B | LiteRT or ONNX | INT4 / Q4 | ~0.2 GB | ~0.3 GB | **P0 now** |
| 6 | **RAG search** | **Cosine top-5** on embed vectors | — | SQLite + numpy | — | 0 | ~0 | **P0 now** |
| 7 | **Voice input (STT)** | **Whisper base** | Whisper tiny | whisper.cpp JNI | `base-q5_1.gguf` | ~60 MB | ~0.1 GB | **P0 now** |
| 8 | **Voice output (TTS)** | **Android TextToSpeech** | — | system API | — | 0 | 0 | **P0 now** |
| 9 | **Notification capture** | **NotificationListenerService** | — | Android | — | 0 | 0 | **P0 now** |
| 10 | **Notification filter** | **Keyword rules** (deadline, date, `@`) | TFLite micro-classifier | rules | train later | ~1 MB | ~0 | P0 / P2 |
| 11 | **Clipboard / share capture** | **Android APIs** | — | Android | — | 0 | 0 | **P0 now** |
| 12 | **Alarm / reminder tool** | **AlarmManager** | Calendar intent | Android | — | 0 | 0 | **P0 now** |
| 13 | **Pulse vision** (screenshot without OCR) | *defer* | Qwen3.5-2B multimodal image | llama.cpp | `Q4_K_M` | included in #2 | swap with #2 | **P1** |
| 14 | **Reranker** | *skip* — not enough RAM for 3 models | — | — | — | — | — | — |

**Phone 1 totals (P0):** ~**1.8 GB** on disk · ~**2.6 GB** peak RAM (LLM + embed + Whisper, one at a time for LLM)

**Phone 1 one-line stack:**
```
ML Kit → Qwen3.5-2B (extract + Q&A) + EmbeddingGemma + Whisper base + Android TTS/alarms
```

---

### Phone 2 — Redmi Note 11 Pro+ 5G (8 GB) — secondary / stress test

| # | Mneme job | **Use this model** | Fallback | Runtime | File / quant | On-disk | RAM (run) | Phase |
|---|-----------|-------------------|----------|---------|--------------|---------|-----------|-------|
| 1 | **Pulse → text** | **ML Kit Text Recognition** | — | ML Kit | bundled | 0 | ~0 | **P0 now** |
| 2 | **Pulse → structured note** | **Qwen3.5-4B-Instruct** | Qwen3.5-2B-Instruct | llama.cpp JNI | `Q4_K_M` GGUF | ~2.5 GB | ~3.5 GB | **P0–P1** |
| 3 | **Voice Q&A + citations** | **Qwen3.5-4B-Instruct** *(shared)* | Qwen3.5-2B | llama.cpp JNI | shared | — | shared | **P0–P1** |
| 4 | **Intent routing** | **Qwen3.5-0.8B-Instruct** | rules | llama.cpp | `Q4_K_M` | ~0.5 GB | ~0.8 GB | P1 |
| 5 | **RAG embeddings** | **Qwen3-Embedding-0.6B** | EmbeddingGemma-300M | ONNX / LiteRT | INT8 | ~0.5 GB | ~0.7 GB | **P0 now** |
| 6 | **RAG rerank** | **Qwen3-Reranker-0.6B** | cosine only | ONNX | INT8 | ~0.5 GB | ~0.7 GB | **P1** |
| 7 | **Voice input (STT)** | **Whisper base** | Whisper small | whisper.cpp | `base-q5_1.gguf` | ~60 MB | ~0.1 GB | **P0 now** |
| 8 | **Voice output (TTS)** | **Android TextToSpeech** | — | system | — | 0 | 0 | **P0 now** |
| 9 | **Capture + tools** | Same as Phone 1 | — | Android | — | 0 | 0 | **P0 now** |
| 10 | **Pulse vision (full VLM)** | **MiniCPM-V 4.6-Instruct** | Qwen3.5-4B MM | llama.cpp + mmproj | `Q4_K_M` + f16 | ~1.6 GB | ~2.5 GB | **P1** |
| 11 | **Screen agent experiments** | **Qwen3-VL-2B-Instruct** | MiniCPM-V 4.6 | llama.cpp | `Q4_K_M` | ~1.4 GB | ~2 GB | **P2** |
| 12 | **Google stack trial** | **Gemma 4 E2B-it QAT mobile** | — | LiteRT | mobile QAT | ~1.5 GB | ~2.5 GB | P2 (iQOO prep) |

**Phone 2 totals (P0–P1):** ~**3.5–4 GB** on disk · ~**4.5 GB** peak RAM (4B LLM + embed; unload LLM before VLM)

**Phone 2 one-line stack:**
```
ML Kit → Qwen3.5-4B (extract + Q&A) + Qwen3-Embed-0.6B + Qwen3-Reranker + Whisper base
```

---

### Side-by-side — what we use per phone

| Mneme job | Phone 1 (6 GB) | Phone 2 (8 GB) |
|-----------|----------------|----------------|
| Pulse OCR | ML Kit | ML Kit |
| Note extract + Q&A LLM | **Qwen3.5-2B** | **Qwen3.5-4B** |
| Embeddings | **EmbeddingGemma-300M** | **Qwen3-Embedding-0.6B** |
| Reranker | skip | **Qwen3-Reranker-0.6B** |
| Voice STT | **Whisper base Q5** | **Whisper base Q5** |
| Voice TTS | Android TTS | Android TTS |
| Pulse VLM | P1: share Qwen3.5-2B | P1: **MiniCPM-V 4.6** |
| Intent | rules → P1: Qwen3.5-0.8B | **Qwen3.5-0.8B** |
| Runtime | llama.cpp + LiteRT embed | llama.cpp + ONNX embed |
| ADB serial | `1431df87` | `3e68912fdff2` |

---

### Download manifest (lock these HF / Ollama IDs)

| Model | Hugging Face / source | Quant | Phone |
|-------|----------------------|-------|-------|
| Qwen3.5-2B-Instruct | `Qwen/Qwen3.5-2B-Instruct` → GGUF via bartowski/unsloth | `Q4_K_M` | Phone 1 |
| Qwen3.5-4B-Instruct | `Qwen/Qwen3.5-4B-Instruct` → GGUF | `Q4_K_M` | Phone 2 |
| Qwen3.5-0.8B-Instruct | `Qwen/Qwen3.5-0.8B-Instruct` → GGUF | `Q4_K_M` | Phone 2 / P1 Phone 1 |
| Qwen2.5-1.5B fallback | `litert-community/Qwen2.5-1.5B-Instruct` | dynamic INT8 | Phone 1 fallback |
| EmbeddingGemma | `google/embeddinggemma-300m` | Q4 / LiteRT | Phone 1 |
| Qwen3-Embed-0.6B | `Qwen/Qwen3-Embedding-0.6B` | ONNX INT8 | Phone 2 |
| Qwen3-Reranker-0.6B | `Qwen/Qwen3-Reranker-0.6B` | ONNX INT8 | Phone 2 |
| MiniCPM-V 4.6 | `openbmb/MiniCPM-V-4.6` → GGUF + mmproj | `Q4_K_M` | Phone 2 P1 |
| Qwen3-VL-2B | `Qwen/Qwen3-VL-2B-Instruct` → GGUF | `Q4_K_M` | Phone 2 P2 |
| Whisper base | `ggerganov/whisper.cpp` → `ggml-base-q5_1.bin` | Q5 | both |
| Gemma 4 E2B mobile | `google/gemma-4-E2B-it-qat-mobile-transformers` | mobile QAT | iQOO prep |

**Test on laptop before phone:**
```bash
ollama pull qwen3.5:2b    # Phone 1 brain
ollama pull qwen3.5:4b    # Phone 2 brain
ollama pull embeddinggemma
```

---

### Build phases (both phones)

| Phase | What ships | Phone 1 | Phone 2 |
|-------|------------|---------|---------|
| **P0** | Pulse (ML Kit) + SQLite notes + notifications + Whisper + rules intent + embed search + alarm | ✅ all | ✅ all |
| **P1** | Qwen3.5 LLM Q&A with citations + JSON extract; optional VLM Pulse | 2B | 4B + MiniCPM-V 4.6 |
| **P2** | Reranker, Qwen3-VL screen agent, fine-tuned intents | 0.8B intent | full stack |
| **Event** | iQOO: Qwen3.5-9B or Gemma 4 E4B on Hexagon | — | — |

---

### Memory load order (avoid OOM)

1. Start app → load **EmbeddingGemma** or **Qwen3-Embed** (small, keep resident)
2. User taps Pulse → run **ML Kit** (no model load)
3. Need AI extract → load **Qwen3.5-2B/4B** → run → **keep warm** for Q&A session
4. User speaks → **Whisper base** (can run while LLM loaded if total < budget; else unload LLM briefly)
5. VLM Pulse (P1) → **unload LLM** → load VLM → extract → unload VLM → reload LLM

---


## Phone fit summary

| | Phone 1 — Redmi 13 5G | Phone 2 — Note 11 Pro+ 5G | iQOO 15 (loaner) |
|--|----------------------|---------------------------|------------------|
| RAM | 6 GB | 8 GB | 12–16 GB |
| GPU | Adreno 613 | Adreno 619 | Adreno flagship |
| NPU | None | None | Hexagon ~220 tok/s |
| **Model budget (Q4)** | ~2–2.5 GB | ~3–4 GB | ~5–9 GB |
| **Primary dev** | ✅ App + Android 16 | Secondary tester | Final AI demo |

---

## 2026 headline releases

| Family | Released | Sizes | Why it matters |
|--------|----------|-------|----------------|
| **Qwen 3.5 Small** | Mar 2026 | 0.8B, 2B, 4B, 9B | Natively multimodal (text/image/video), 262K context, Apache 2.0, Gated DeltaNet |
| **Gemma 4** | Early 2026 | E2B, E4B, 12B, 26B MoE, 31B | Mobile-first: vision + audio + thinking + function calling; QAT mobile builds |
| **Ministral 3** | Dec 2025 | 3B, 8B, 14B | Apache 2.0, built-in vision, 256K context, base/instruct/reasoning |
| **MiniCPM-V 4.x** | 2026 | 4.0 (4.1B), **4.6 (1.3B)** | Open Android/iOS demo apps, llama.cpp GGUF |
| **LFM2.5** | 2026 | 230M–2.6B | Liquid neural nets, <1 GB, fast CPU/NPU inference |
| **Qwen3-VL** | Oct 2025 | 2B, 4B, 8B, 32B, MoE | Mobile GUI agent, OSWorld-class screen understanding |
| **EmbeddingGemma** | 2025 | 300M | On-device RAG embeddings, <200 MB quantized |
| **Qwen3-Embedding** | 2025 | 0.6B, 4B, 8B | MTEB-leading retrieval + reranker family |

### References

- Qwen 3.5 Small: [MarkTechPost](https://www.marktechpost.com/2026/03/02/alibaba-just-released-qwen-3-5-small-models-a-family-of-0-8b-to-9b-parameters-built-for-on-device-applications/) · [Awesome Agents](https://awesomeagents.ai/news/qwen-3-5-small-models-series/)
- Gemma 4: [Google Blog](https://blog.google/innovation-and-ai/technology/developers-tools/gemma-4/) · [Model card](https://ai.google.dev/gemma/docs/core/model_card_4)
- Ministral 3: [Mistral AI](https://mistral.ai/news/mistral-3/) · [HF 3B](https://huggingface.co/mistralai/Ministral-3-3B-Base-2512)
- MiniCPM-V Apps: [GitHub](https://github.com/OpenBMB/MiniCPM-V-Apps)
- LFM2.5: [Liquid AI blog](https://www.liquid.ai/blog/lfm2-5-1-2b-thinking-on-device-reasoning-under-1gb)
- Qwen3-VL: [GitHub](https://github.com/QwenLM/Qwen3-VL) · [Ollama](https://ollama.com/library/qwen3-vl)
- EmbeddingGemma: [Google Developers Blog](https://developers.googleblog.com/introducing-embeddinggemma/)
- Qwen3-Embedding: [GitHub](https://github.com/QwenLM/Qwen3-Embedding)

---

## 1. Text LLM — Q&A, JSON extract, tools

| Tier | Model | Q4 size (~) | Phone 1 | Phone 2 | Standout |
|------|--------|-------------|---------|---------|----------|
| 🔥 NEW | **Qwen3.5-0.8B** | ~0.5 GB | ✅ | ✅ | Fastest; 262K ctx; multimodal |
| 🔥 NEW | **Qwen3.5-2B** | ~1.5 GB | ✅ **Best** | ✅ | Sweet spot for 6 GB |
| 🔥 NEW | **Gemma 4 E2B** (QAT) | ~1.5–4 GB | ✅ | ✅ | Thinking + tools + vision |
| 🔥 NEW | **LFM2.5-1.2B-Thinking** | ~0.9 GB | ✅ | ✅ | Reasoning under 1 GB |
| ⭐ | **Qwen3-1.7B** | ~1 GB | ✅ | ✅ | Thinking variant available |
| ⭐ | **Qwen3-3B** | ~2 GB | ⚠️ | ✅ | Strong general benchmarks |
| ⭐ | **Phi-4-mini** (3.8B) | ~2.2 GB | ⚠️ | ✅ | Best math/reasoning per GB |
| ⭐ | **Ministral 3 3B Instruct** | ~2 GB | ⚠️ | ✅ | Vision + 256K, Apache 2.0 |
| ⭐ | **SmolLM3-3B** | ~2 GB | ⚠️ | ✅ | Fully open training pipeline |
| ⭐ | **Llama 3.2 3B** | ~2 GB | ⚠️ | ✅ | Best tool-use at 3B |
| ⭐ | **Qwen2.5-1.5B-Instruct** | ~1.0 GB | ✅ | ✅ | Proven LiteRT Android builds |
| ⭐ | **Qwen2.5-0.5B-Instruct** | ~0.4 GB | ✅ | ✅ | Ultra-fast dev/debug |
| 🔥 NEW | **Qwen3.5-4B** | ~2.5 GB | ⚠️ tight | ✅ | Multimodal agent, one model |
| 🔥 NEW | **Gemma 4 E4B** (QAT) | ~2.5–5 GB | ❌ | ✅ | Strongest Google edge |
| 🔥 NEW | **Qwen3.5-9B** | ~5.7 GB | ❌ | ⚠️ | Beats much larger models |
| 🧪 | **LFM2.5-2.6B** | ~1.6 GB | ⚠️ | ✅ | Agent planning, very fast |
| 🧪 | **Jamba Reasoning 3B** | ~2 GB | ⚠️ | ✅ | Mamba hybrid, 256K ctx |
| 🧪 | **DeepSeek-R1-Distill-Qwen-1.5B** | ~1 GB | ✅ | ✅ | CoT on tiny hardware |
| 🧪 | **BitNet b1.58 2B** | ~1.1 GB | ✅ | ✅ | Ternary, CPU-first |
| 🧪 | **Granite 4.1 3B** | ~2 GB | ⚠️ | ✅ | IBM, strong tool calling |
| 🧪 | **Danube 3 500M** | ~0.4 GB | ✅ | ✅ | Ultra-tiny routing |
| ❌ iQOO | **Qwen3.5-9B**, **Ministral 3 8B** | 5–6 GB+ | — | — | Hackathon demo tier |

### Qwen 3.5 Small — detail (Mar 2026)

| Model | 4-bit VRAM | Context | Multimodal | License |
|-------|------------|---------|------------|---------|
| **0.8B** | ~0.5 GB | 262K | ✅ text/image/video | Apache 2.0 |
| **2B** | ~1.5 GB | 262K | ✅ | Apache 2.0 |
| **4B** | ~2.5 GB | 262K | ✅ | Apache 2.0 |
| **9B** | ~5.7 GB | 262K (1M ext.) | ✅ | Apache 2.0 |

Architecture: Gated DeltaNet hybrid attention + multi-token prediction. No separate VL adapter — one weight file for Pulse + Q&A.

**Ollama:** `qwen3.5:0.8b` · `qwen3.5:2b` · `qwen3.5:4b` · `qwen3.5:9b`

### Gemma 4 edge — detail

| Model | Effective params | Total (with PLE) | Modalities | Context |
|-------|------------------|------------------|------------|---------|
| **E2B** | 2.3B | 5.1B | text, image, **audio** | 128K |
| **E4B** | 4.5B | 8B | text, image, **audio** | 128K |

Features: thinking mode, native function calling, mobile QAT (`wNa8o8`), co-designed with Qualcomm/MediaTek.

**Ollama:** `gemma4:e2b` · `gemma4:e4b` (use QAT variants on phone)

### Legacy / still valid (2024–2025)

| Model | Params | Q4 size | Notes |
|-------|--------|---------|-------|
| Llama 3.2 1B | 1B | ~0.8 GB | Routing, autocomplete |
| Llama 3.2 3B | 3B | ~2 GB | PocketLLM #1 ranked SLM 2026 |
| Phi-3.5 Mini | 3.8B | ~2.4 GB | MIT, strong structured tasks |
| Gemma 3 4B | 4B | ~3 GB | 140+ languages, multimodal |
| Gemma 3n E2B / E4B | 2B / 4B eff. | 2–3 GB | Predecessor to Gemma 4 |
| Qwen2.5 3B | 3B | ~1.9 GB | 128K context |
| Mistral 7B | 7B | ~4.1 GB | Too big for Phone 1 |

---

## 2. Vision / Pulse — screenshot → structured note

| Tier | Model | Size (Q4) | Phone 1 | Phone 2 | Best for Mneme |
|------|--------|-----------|---------|---------|----------------|
| MVP | **ML Kit Text Recognition** | — | ✅ | ✅ | Fastest Pulse; no LLM vision |
| 🔥 NEW | **Qwen3.5-2B** (native MM) | ~1.5 GB | ✅ | ✅ | One model: Pulse + Q&A |
| 🔥 NEW | **Qwen3.5-4B** | ~2.5 GB | ⚠️ | ✅ | UI/doc agent Pulse |
| 🔥 NEW | **Gemma 4 E2B** | ~1.5 GB eff. | ✅ | ✅ | Vision + audio + thinking |
| ⭐ | **MiniCPM-V 4.6** | ~1.6 GB total | ✅ | ✅ | Smallest strong VLM; Android demo |
| ⭐ | **MiniCPM-V 4.0** | ~2.9 GB | ⚠️ | ✅ | 4.1B, GPT-4V-mini class |
| ⭐ | **Qwen3-VL-2B** | ~1.4 GB INT4 | ✅ | ✅ | Mobile GUI agent cookbook |
| ⭐ | **Qwen3-VL-4B** | ~3 GB INT4 | ⚠️ | ✅ | Balanced VLM |
| ⭐ | **Ministral 3 3B** | ~2 GB | ⚠️ | ✅ | Vision + text, Apache 2.0 |
| ⭐ | **SmolVLM2-500M** | ~0.8 GB | ✅ | ✅ | Smallest real VLM |
| ⭐ | **SmolVLM2-2.2B** | ~1.5 GB | ⚠️ | ✅ | Better OCR/doc quality |
| OCR | **GLM-OCR** | ~0.9B / ~1 GB | ✅ | ✅ | Doc/table specialist |
| OCR | **LightOnOCR-2-1B** | ~1 GB | ✅ | ✅ | Strong tiny OCR |
| OCR | **dots.ocr** | ~3B / ~2 GB | ⚠️ | ✅ | Layout-aware parsing leader |
| Legacy | **Qwen2.5-VL-3B** | ~2 GB INT4 | ⚠️ | ✅ | Proven doc understanding |
| Legacy | **Phi-4-multimodal** | ~3 GB | ❌ | ⚠️ | 5.6B, strong benchmarks |
| 🧪 | **MagicVL-2B** | ~2 GB | ⚠️ | ✅ | Phone-optimized, OCRBench |
| 🧪 | **TinyGPT-V** | 2.8B | ❌ | ⚠️ | EVA + Phi-2 legacy |

### MiniCPM-V download sizes (GGUF Q4_K_M + mmproj)

| Model | LLM Q4 | mmproj | Total | Min RAM |
|-------|--------|--------|-------|---------|
| MiniCPM-V 4.6 | ~0.5 GB | ~1.1 GB | ~1.6 GB | ≥ 6 GB |
| MiniCPM-V 4.0 | ~2.0 GB | ~0.9 GB | ~2.9 GB | ≥ 6 GB |
| MiniCPM-V 2.6 | ~4.4 GB | ~1.0 GB | ~5.4 GB | ≥ 8 GB |

Android demo APK: [MiniCPM-V-Apps](https://github.com/OpenBMB/MiniCPM-V-Apps) (models downloaded on first launch).

### OCR benchmark reference (2026)

| Model | Params | OCRBench-class score | Notes |
|-------|--------|----------------------|-------|
| dots.ocr-1.5 | ~3B | ~82% | Best layout parser |
| GLM-OCR | 0.9B | ~76% | Best value per param |
| LightOnOCR-2-1B | 1B | ~75% | Strong small OCR |
| Qwen3.5-4B | 4B | ~79% | General VLM, surprisingly good |
| Qwen3.5-2B | 2B | ~72% | General purpose |
| Qwen3.5-0.8B | 0.8B | ~58% | Not enough for full docs |

---

## 3. Embeddings + RAG retrieval

| Tier | Model | Size | Dims | Context | License |
|------|--------|------|------|---------|---------|
| 🔥 | **EmbeddingGemma-300M** | <200 MB quant | 768→128 MRL | 2K | Gemma |
| 🔥 | **Qwen3-Embedding-0.6B** | ~0.6 GB | 1024 MRL | 32K | Apache 2.0 |
| ⭐ | **Qwen3-Reranker-0.6B** | ~0.6 GB | — | 32K | Pair with embedder |
| ⭐ | **jina-embeddings-v5-text-small** | <1 GB | — | 32K | Retrieval SOTA class |
| ⭐ | **jina-reranker-v3** | 0.6B | — | 8K | RAG reranking |
| Classic | **all-MiniLM-L6-v2** (ONNX) | ~87 MB | 384 | 512 | Apache 2.0 |
| Classic | **BGE-small-en** | ~130 MB | 384 | 512 | MIT |
| Classic | **BGE-M3** | ~2 GB | hybrid | 8K | Dense+sparse+multi-vector |
| Server | **Qwen3-Embedding-8B** | large | 4096 | 32K | Best quality, not for phone |

**EmbeddingGemma prompts:** prepend task strings (`task: search result | query: {text}`).

**HF:** [EmbeddingGemma](https://huggingface.co/google/embeddinggemma) · [Qwen3-Embedding-0.6B](https://huggingface.co/Qwen/Qwen3-Embedding-0.6B)

---

## 4. Speech — voice input (STT)

| Model | Size | Mode | Speed (typical) | Notes |
|-------|------|------|-----------------|-------|
| **Whisper tiny Q8** | ~42 MB | Batch | ~15× realtime | Best for Phone 1 MVP |
| **Whisper base Q5** | ~60 MB | Batch | ~8× realtime | Better accents/noise |
| **Whisper small Q5** | ~190 MB | Batch | ~3× realtime | Phone 2 if quality needed |
| **Android SpeechRecognizer** | 0 | Online | Fast | Dev only — not for airplane demo |
| **Gemma 4 E2B audio** | bundled | — | — | iQOO + Google stack later |

**Note:** Live streaming Whisper on mobile CPU is often 5× slower than realtime — use **tap-to-talk batch** for Mneme.

**Models:** [whisper.cpp HuggingFace](https://huggingface.co/ggerganov/whisper.cpp)

---

## 5. Speech output (TTS) — optional

| Model | Size | Notes |
|-------|------|-------|
| **Android TTS** | 0 | MVP voice replies |
| **VoxCPM2** | ~2.8 GB | On-device TTS in MiniCPM ecosystem |
| **Piper / Coqui** | small | Lightweight offline TTS alternatives |

---

## 6. Cool / experimental 2026 models

| Model | Params | Size | Why cool | Mneme use case |
|-------|--------|------|----------|----------------|
| **LFM2.5-230M / 350M** | 230M–350M | <0.5 GB | QAD 4-bit, Raspberry Pi class | Intent router |
| **LFM2.5-1.2B-Thinking** | 1.2B | ~0.9 GB | 70+ tok/s phone CPU | Fast reasoning |
| **LFM2.5-2.6B** | 2.6B | ~1.6 GB | Agent harness on phone | Tool planning |
| **BitNet b1.58 2B4T** | 2B | ~1.1 GB | Native ternary, bitnet.cpp | Battery saver |
| **Jamba Reasoning 3B** | 3B | ~2 GB | Mamba+Transformer hybrid | Long context Q&A |
| **ZAYA1-8B MoE** | 8B (0.76B active) | ~4.8 GB Q4 | MoE speed trick | iQOO experiment |
| **DiffusionGemma** | 25B (3.8B active) | large | Parallel decode | Research only |
| **North Mini Code 30B** | 30B (3B active) | ~19 GB | Coding MoE | Not for Mneme v1 |
| **Qwen3.6-35B-A3B** | 35B (3B active) | ~22 GB | 1M context MoE | Server fine-tune only |
| **Bonsai 27B** | 27B binary | ~3.9 GB | Extreme quant research | Not practical v1 |
| **MiniCPM5-1B** | 1B | ~0.5 GB | Text-only ultra small | Notification summary |
| **DeepSeek-R1-Distill** variants | 1.5B–8B | varies | Reasoning distill | Math/deadline logic |

**LFM2.5 HF:** [LFM2.5-1.2B-Thinking](https://huggingface.co/LiquidAI/LFM2.5-1.2B-Thinking)

---

## 7. Quantization guide (mobile)

| Format | Quality vs FP16 | Size | When to use |
|--------|-----------------|------|-------------|
| **Q4_K_M** | ~5–8% loss | ~50% of FP16 | **Default** for llama.cpp mobile |
| **Q8_0** | ~2% loss | larger | Whisper, tiny models |
| **QAT mobile (Gemma 4)** | near BF16 | 40% smaller | Gemma 4 on 6–8 GB phones |
| **INT8 / dynamic_int8 (LiteRT)** | good | smallest | Qwen2.5 LiteRT builds |
| **IQ variants** | varies | smallest | Experimental |

**Rule of thumb:** model RAM ≈ **1.5× file size** (weights + KV cache + overhead).

- 4 GB phone: model budget ≈ **40% RAM** (~1.6 GB)
- 6 GB+ phone: model budget ≈ **60% RAM** (~3.6 GB on 6 GB)

---

## 8. Recommended stacks (Mneme v1)

> **Canonical stacks:** see **[Selected stacks — USE THESE](#-selected-stacks--use-these-mneme-v1)** at the top of this doc.

### iQOO 15 — hackathon demo (event only)

```
Hero LLM:   Qwen3.5-9B  OR  Gemma 4 E4B on Hexagon (LiteRT-QNN)
Pulse VLM:  Qwen3.5-4B native multimodal
Embed:      EmbeddingGemma on NPU
Voice:      Whisper small  OR  Gemma 4 audio path
Story:      Multi-model NPU pipeline, airplane mode, citations
```

---

## 9. Fine-tuning plan (when ready)

| Priority | Model | Task | Method |
|----------|--------|------|--------|
| P0 | **Qwen3.5-2B** (Phone 1) / **Qwen3.5-4B** (Phone 2) | Intents + JSON note schema | LoRA → GGUF |
| P1 | **EmbeddingGemma** or **Qwen3-Embed-0.6B** | Domain notes (courses, apps, deadlines) | Contrastive fine-tune |
| P2 | **GLM-OCR** or **Qwen3.5-2B** | Pulse: screenshot → structured fields | LoRA on screenshot pairs |
| P3 | TFLite classifier | Notification junk vs important filter | Train on laptop, deploy tiny |
| P4 | **Whisper base** | User accent (optional) | Lightweight adapt |

Train on laptop → export **GGUF** (llama.cpp) or **LiteRT** (Google) → bundle in APK or download on first launch.

---

## 10. Runtime & integration (Android)

| Backend | Best for | Models |
|---------|----------|--------|
| **llama.cpp** (JNI) | GGUF, VLMs, Whisper | Qwen, Gemma, SmolVLM, MiniCPM, LFM |
| **LiteRT / MediaPipe** | Google-first, INT8 | Qwen2.5, Gemma 3/4, EmbeddingGemma |
| **ONNX Runtime Mobile** | Embeddings | MiniLM, Qwen3-Embed |
| **ML Kit** | OCR only | Pulse MVP |
| **LiteRT-QNN** | iQOO Hexagon | Gemma 4, Qwen (event) |

**ADB deploy loop:**

```bash
export ANDROID_SERIAL=1431df87   # Phone 1
# export ANDROID_SERIAL=3e68912fdff2  # Phone 2
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Useful llama.cpp Android reference:** [SoundsguyZA/llama.cpp-android](https://github.com/SoundsguyZA/llama.cpp-android) · [Native-LLM-for-Android](https://github.com/DakeQQ/Native-LLM-for-Android)

---

## 11. Ollama pull commands (dev / test on laptop)

```bash
# Qwen 3.5 (2026 multimodal small)
ollama pull qwen3.5:0.8b
ollama pull qwen3.5:2b
ollama pull qwen3.5:4b
ollama pull qwen3.5:9b

# Gemma 4 edge
ollama pull gemma4:e2b
ollama pull gemma4:e4b

# Vision specialists
ollama pull qwen3-vl:2b
ollama pull qwen3-vl:4b

# Text classics
ollama pull phi4-mini
ollama pull llama3.2:3b
ollama pull ministral-3:3b

# Embeddings (where supported)
ollama pull embeddinggemma
```

---

## 12. Decision matrix (quick pick)

| Question | Answer |
|----------|--------|
| **Phone 1 stack (locked)?** | ML Kit + **Qwen3.5-2B** + **EmbeddingGemma** + **Whisper base** |
| **Phone 2 stack (locked)?** | ML Kit + **Qwen3.5-4B** + **Qwen3-Embed-0.6B** + **Qwen3-Reranker** + Whisper |
| Best Google / iQOO narrative? | **Gemma 4 E2B/E4B** + **EmbeddingGemma** on Hexagon |
| Phone 2 VLM (P1)? | **MiniCPM-V 4.6** |
| Phone 2 screen agent (P2)? | **Qwen3-VL-2B** |
| Hackathon hero model? | **Qwen3.5-9B** or **Gemma 4 E4B** on iQOO NPU |
| Full catalog below? | Sections 1–7, 9–13 |

---

## 13. What NOT to run on dev phones

| Model | Why skip on Phone 1/2 |
|-------|------------------------|
| Qwen3.5-9B (full ctx) | RAM; save for iQOO |
| Mistral 7B+ | Too large for 6 GB |
| Qwen2.5-VL-7B+ | VRAM |
| Phi-4 14B | Needs 12 GB class |
| North Mini Code 30B | 19 GB Q4 |
| 24/7 Focus VLM polling | Battery + thermal on budget SoC |

---

*Catalog compiled from public sources (Google, Alibaba/Qwen, Mistral, Hugging Face, Liquid AI, OpenBMB, community benchmarks) for Mneme hackathon planning. Model availability and GGUF support change frequently — verify on Hugging Face before locking APK downloads.*
