# iQOO Hackathon 2026 — Reference Guide

> City Battles · Aug–Oct 2026 · Phone-first · On-device AI · Office Kit  
> Last updated: 2026-08-22

---

## Table of contents

1. [Track 04 — Productivity](#track-04--productivity)
2. [Track 05 — Smart Living](#track-05--smart-living)
3. [Track 06 — Developer Tools](#track-06--developer-tools)
4. [iQOO 15 loaner — full hardware spec](#iqoo-15-loaner--full-hardware-spec)
5. [Snapdragon 8 Elite Gen 5 — CPU / GPU / NPU](#snapdragon-8-elite-gen-5--cpu--gpu--npu)
6. [Office Kit & HackTracker](#office-kit--hacktracker)
7. [Red Light / Green Light format](#red-light--green-light-format)
8. [Scoring rubric](#scoring-rubric)
9. [What to build with (stacks & AI)](#what-to-build-with-stacks--ai)
10. [Your dev phone vs event phone](#your-dev-phone-vs-event-phone)
11. [Pre-event checklist](#pre-event-checklist)

---

## Track 04 — Productivity

**Available in:** City battles + Grand Finale  
**Bucket:** Students OR working professionals (team cannot mix)

### What the track means

Build something that helps people **get more done with less friction** — using AI as the core, not a sidebar feature.

This is the broadest “work & life admin” track. Think tools for:

| Area | Example ideas |
|------|----------------|
| **Task & time** | Smart to-do, calendar blocking, meeting prep, deadline reminders |
| **Information overload** | Summarize emails/docs, extract action items, prioritize inbox |
| **Repetitive work** | Auto-fill forms, template generation, batch file renaming/processing |
| **Knowledge work** | Note-taking with AI, search across your docs, Q&A over personal files |
| **Workflow glue** | Connect phone actions → laptop work via Office Kit (copy snippet → paste in IDE) |
| **Focus & habits** | Distraction blocking with context-aware AI, daily standup generator |

### What judges likely care about

- Does it **save real time** on a task people do daily?
- Is AI **essential** (not “ChatGPT button in a wrapper”)?
- Does it run and demo **on the iQOO phone**?
- **On-device** inference (local/open-source model on NPU) = brownie points
- Creative use of **camera** (scan doc → structured tasks), **voice** (dictate → organized output)

### Good fit for phone-first

- Capture on phone (photo/voice) → process on NPU or bridge to laptop for heavy steps
- Quick actions from notification / widget
- Offline-capable productivity (on-device model, no cloud required)

### Avoid

- Pre-built todo app with API call bolted on
- Desktop-only workflow that barely runs on phone
- Generic “AI assistant” with no clear productivity problem

---

## Track 05 — Smart Living

**Available in:** City battles + Grand Finale  
**Bucket:** Students OR working professionals (team cannot mix)

### What the track means

AI for ** everyday life at home and on the go** — convenience, comfort, connected devices, and “smart” routines.

Not limited to literal smart-home hardware. Valid angles:

| Area | Example ideas |
|------|----------------|
| **Smart home** | Voice/scene control, device orchestration, energy usage insights |
| **IoT** | Sensor dashboards, alerts (door, temp, motion), MQTT/HTTP device bridges |
| **Daily convenience** | Grocery/meal planning, commute helper, bill reminders, package tracking |
| **Health-adjacent living** | Sleep routine, hydration, home air quality tips (not full HealthTech — that’s Track 03) |
| **Accessibility** | Voice-controlled home tasks, large-text summaries for elderly family |
| **Local / edge AI** | On-device vision (what’s in the fridge?), on-device voice commands without cloud |

### What judges likely care about

- **Real-world convenience** — would someone use this weekly?
- Phone as **control surface** and **demo surface**
- Integration story: phone ↔ laptop via Office Kit for config/dashboard on big screen
- IoT demos can use **simulated devices** or phone sensors if no physical hardware

### Good fit for phone-first

- Phone as remote control + notification hub
- Camera for visual tasks (read meter, identify appliance error code)
- Voice agent for hands-free home commands
- PWA or native app that feels native on OriginOS 6

### Avoid

- Home automation that only works on laptop
- Cloud-only voice assistant clone
- Ideas that belong clearly in HealthTech (Track 03) or FinTech (Track 01) without a “living” angle

---

## Track 06 — Developer Tools

**Available in:** City battles + Grand Finale  
**Bucket:** Students OR working professionals (team cannot mix)

### What the track means

Build **tools for builders** — anything that helps developers (or technical users) **create, test, deploy, or collaborate faster** with AI in the loop.

This track aligns naturally with the hackathon format (phone + laptop + Office Kit).

| Area | Example ideas |
|------|----------------|
| **Code assist** | Mobile-friendly code review, explain diff, generate tests from phone |
| **Debug & logs** | Paste stack trace → structured fix suggestions; log summarizer |
| **API / backend** | Mock server controller, request builder, webhook tester from phone |
| **Deploy & CI** | Trigger builds, read CI status, approve PRs with AI summary |
| **Collaboration** | Standup bot, PR description generator, doc sync phone ↔ laptop |
| **On-device dev** | Run small models on phone for offline codegen hints; Termux-adjacent workflows |
| **Mobile-first IDE helpers** | Snippet library, regex tester, JSON/YAML validator with AI fix |

### What judges likely care about

- **Technical depth** (15% of score) — architecture, code quality, real hardware use
- Would **you** use it during the hackathon or in real dev work?
- Office Kit usage: mirror phone logs on laptop, file transfer for configs
- On-device model for sensitive code (no sending proprietary snippets to cloud)

### Good fit for phone-first

- Phone captures error screenshot → OCR + AI diagnosis
- Voice: “explain this function” while mirroring laptop via Office Kit
- Quick deploy trigger + notification on phone
- Developer tool that **demos on phone** even if build happens on laptop during Green Light

### Avoid

- Full VS Code clone (too big for 30 hours)
- Tool that only works on desktop
- Generic ChatGPT wrapper without dev-specific workflow

---

## Track comparison (quick pick)

| Track | Best if you… | Phone superpower |
|-------|----------------|------------------|
| **04 Productivity** | Care about tasks, time, docs, workflows | Camera scan, voice capture, notifications |
| **05 Smart Living** | Like IoT, home, daily convenience | Sensors, voice control, visual checks |
| **06 Developer Tools** | Are devs building for devs | Office Kit bridge, log/error capture, mobile dev UX |

All three run in **city battles and Grand Finale** (unlike FinTech/Education/Health which are city-only).

---

## iQOO 15 loaner — full hardware spec

Device provided at check-in (one per person). Property of iQOO — return before exit.

| Category | Specification |
|----------|----------------|
| **Model** | iQOO 15 |
| **OS** | OriginOS 6 (Android 16-based) |
| **Pre-installed** | HackTracker, Office Kit (pre-paired) |

### Display
| Spec | Value |
|------|--------|
| Panel | Samsung 2K M14 LEAD OLED |
| Size | 6.85" |
| Resolution | **3168 × 1440** (~508 PPI) |
| Refresh rate | **144 Hz** |
| Peak brightness | Up to 6000 nits (local), 2600 nits global |
| Fingerprint | 3D Ultrasonic |

### Memory & storage
| Spec | Value |
|------|--------|
| RAM | **12 GB or 16 GB** LPDDR5X Ultra / Ultra Pro |
| Storage | **256 GB / 512 GB / 1 TB** UFS 4.1 |
| RAM speed | Up to 9600 Mbps (16GB+1TB variant: LPDDR5X Ultra Pro) |

### Battery & cooling
| Spec | Value |
|------|--------|
| Battery | **7000 mAh** silicon-anode |
| Wired charge | 100W FlashCharge |
| Wireless | 40W |
| Cooling | **8000 mm²** vapor chamber (8K VC), graphene + dual graphite |
| Endurance (lab) | ~10.5 h gaming, ~32 h video |

### Cameras
| Camera | Spec |
|--------|------|
| Main | 50 MP Sony IMX921, 1/1.56", OIS |
| Periscope telephoto | 50 MP Sony IMX882, 3× optical, up to 100× digital |
| Ultrawide | 50 MP |
| Front | 32 MP |
| ISP | Qualcomm Spectra triple 20-bit AI ISP |

### Connectivity
| Spec | Value |
|------|--------|
| 5G modem | Snapdragon X85 (up to 12.5 Gbps DL) |
| Wi-Fi | **Wi-Fi 7** (FastConnect 7900) |
| Bluetooth | **Bluetooth 6.0** |
| Antennas | 23-antenna design |

### Extra silicon
| Chip | Role |
|------|------|
| **SuperComputing Chip Q3** | Rendering, ray tracing, AI game optimization; helps sustained performance |

---

## Snapdragon 8 Elite Gen 5 — CPU / GPU / NPU

Main SoC inside iQOO 15. Target for on-device AI.

### CPU — Qualcomm Oryon (3rd gen)
| Spec | Value |
|------|--------|
| Process | **3 nm** (TSMC) |
| Cores | 8-core |
| Prime | 2× @ **4.6 GHz** |
| Performance | 6× @ **3.62 GHz** |
| Notes | Built-in matrix acceleration for lighter AI workloads |
| Benchmark | ~4.1M+ AnTuTu on iQOO 15 (reference) |

### GPU — Adreno 840
| Spec | Value |
|------|--------|
| Clock | Up to ~1.2 GHz |
| Memory | 18 MB Adreno HPM |
| Use | UI, vision preview, GPU ML fallback |
| vs NPU | Prefer **NPU** for LLM inference (lower power, higher throughput) |

### NPU — Hexagon (primary for hackathon AI)
| Spec | Value |
|------|--------|
| Generation | New Hexagon NPU on SD 8 Elite Gen 5 |
| vs previous gen | **~37% faster** |
| LLM throughput | **220+ tokens/sec** (Qualcomm claim, on-device LLM) |
| Precision | INT2, INT4, INT8, INT16, FP8, FP16 |
| Power | ~70% better perf/watt vs prior gen |

### Practical on-device model sizes (realistic on phone)
| Model class | Feasibility |
|-------------|-------------|
| 1B–3B quantized (INT4) | Good target for demo |
| 4B quantized | Possible with 12–16 GB RAM, careful memory mgmt |
| Embeddings, Whisper-small, vision small | Strong fit |
| 7B+ full on-device | Unlikely for smooth demo |
| Cloud API | Allowed (free credits) but **judges favor on-device** |

### SDK / runtime paths for NPU
| Tool | Purpose |
|------|---------|
| **Qualcomm AI Runtime (QAIRT)** | Primary runtime |
| **QNN SDK** (AI Engine Direct) | Model conversion & NPU execution |
| **ONNX Runtime + QNN EP** | Cross-platform inference on Hexagon |
| **TFLite** | Some models with delegates |
| **llama.cpp** | CPU/GPU fallback; NPU via QNN conversion |

### Recommended open-source models to explore
- Llama 3.2 1B/3B (quantized)
- Qwen2.5 0.5B–3B (quantized)
- Gemma 2B
- Whisper (speech-to-text)
- EmbeddingGemma / small embedding models for RAG

---

## Office Kit & HackTracker

### Office Kit (software bridge — NOT hardware)
| Feature | Description |
|---------|-------------|
| Screen mirror | Phone UI on laptop |
| Shared clipboard | Copy/paste across devices |
| File transfer | Drag-drop, no cable |
| Remote control | Laptop keyboard drives phone |

**Laptop requirements:** Windows 10+ or macOS 10.14.6+  
**Download:** https://pc.vivoglobal.com (V6.0.0+)  
**Scoring:** **10%** of rubric (HackTracker telemetry)

**Red Light:** Laptop closed as build machine — Office Kit is the **only** link.  
**Green Light:** Both devices; laptop for heavy compute, phone stays in loop.

### HackTracker (pre-installed)
| Tracks | Weight |
|--------|--------|
| Creative phone use (camera, voice, on-device AI) | **15%** |
| Office Kit usage (duration/counts) | **10%** |

**Privacy:** Counts and durations only — no keystrokes, screenshots, or browsing.

---

## Red Light / Green Light format

| Phase | ~Share of build time | Rules |
|-------|----------------------|--------|
| **Red Light** | ~55% | **Phone only** — build via Office Kit bridge |
| **Green Light** | ~45% | Phone + laptop both allowed |

Clock: Saturday 10:00 start, active hacking from 11:00, Sunday awards.

---

## Scoring rubric

| Dimension | Weight | Source |
|-----------|--------|--------|
| End product quality | 30% | Jury |
| Novelty & impact | 20% | Jury |
| HackTracker · creative phone use | 15% | Device data |
| Technical depth | 15% | Jury |
| HackTracker · Office Kit usage | 10% | Device data |
| Demo & presentation | 10% | Jury |

**Brownie points:** Local or open-source model at the core, phone in loop via Office Kit.

---

## What to build with (stacks & AI)

### Allowed app stacks
- Native Android (Kotlin / Jetpack Compose)
- Flutter
- React Native
- PWA (must run well on phone)

### AI strategy (recommended)
1. **Primary:** Small quantized model on **Hexagon NPU** (QNN/ONNX)
2. **Fallback:** Cloud API with free weekend **AI credits**
3. **Hybrid:** NPU for inference, laptop (Green Light) for convert/train/compile

### Free resources at event
- iQOO 15 loaner per person
- Office Kit pre-paired
- AI credits for cloud LLMs (weekend)
- 10–15 mentors, meals included

---

## Your dev phone vs event phone

Use **Redmi 13 5G** for daily prototyping until the event.

| Spec | Redmi 13 5G (dev) | iQOO 15 (event) |
|------|-------------------|-----------------|
| ADB serial | `1431df87` | loaner at check-in |
| Android | 16 (HyperOS 3.0) | 16 (OriginOS 6) |
| SoC | Snapdragon 4 Gen 2 | Snapdragon 8 Elite Gen 5 |
| RAM | 6 GB | 12–16 GB |
| NPU | Weak | Hexagon ~220 tok/s |
| Storage free | ~35 GB | Fresh device |
| Office Kit | No | Yes |
| HackTracker | No | Yes |

**Pipeline:** Build UI + flow on Redmi 13 5G → validate NPU + Office Kit on iQOO 15 at event.

---

## Pre-event checklist

- [ ] Pick track: 04 Productivity / 05 Smart Living / 06 Developer Tools
- [ ] Pick city battle + register at https://iqoo.reskilll.com/
- [ ] Install Office Kit on laptop from pc.vivoglobal.com
- [ ] Prototype app on Redmi 13 5G (`adb -s 1431df87 install …`)
- [ ] Test one small on-device model (ONNX/TFLite) on dev phone (best-effort)
- [ ] Design **phone-first**: demo, camera/voice, Red Light workflow
- [ ] Plan Office Kit into architecture (mirror, clipboard, file transfer)
- [ ] Prepare 3–5 min pitch that runs **live on phone**
- [ ] Team bucket: all students OR all working professionals (max 3)

---

## Useful links

| Resource | URL |
|----------|-----|
| Hackathon portal | https://iqoo.reskilll.com/ |
| Office Kit download | https://pc.vivoglobal.com |
| iQOO 15 specs | https://www.iqoo.com/in/products/param/iqoo-15 |
| Organizer contact | sameera@reskilll.com |

---

## Grand Finale note

Tracks 04, 05, 06 all continue to **Grand Finale** (Oct 9–11, 2026, Bengaluru, 48 hours).  
Top 6 teams per city (3 student + 3 professional) advance.
