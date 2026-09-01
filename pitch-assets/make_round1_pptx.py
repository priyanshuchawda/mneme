#!/usr/bin/env python3
"""14-slide 16:9 Round-1 deck. Regenerates from pitch-assets stills + graphics."""
from pathlib import Path

from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN
from pptx.util import Inches, Pt

img = Path("/home/priyanshuchawda/retenqo/pitch-assets")
out = img / "Retenqo-iQOO-Round1-draft.pptx"

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
BLANK = prs.slide_layouts[6]
BG = RGBColor(0x12, 0x12, 0x14)
ACCENT = RGBColor(0xC4, 0xA3, 0x5A)
WHITE = RGBColor(0xF5, 0xF0, 0xE8)
MUTED = RGBColor(0xA8, 0xA4, 0x9C)
CARD = RGBColor(0x20, 0x22, 0x20)


def paint(slide):
    sh = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, prs.slide_height)
    sh.fill.solid()
    sh.fill.fore_color.rgb = BG
    sh.line.fill.background()
    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(0.12), prs.slide_height)
    bar.fill.solid()
    bar.fill.fore_color.rgb = ACCENT
    bar.line.fill.background()
    return slide


def add_text(slide, l, t, w, h, text, size=18, bold=False, color=WHITE, align=PP_ALIGN.LEFT):
    box = slide.shapes.add_textbox(Inches(l), Inches(t), Inches(w), Inches(h))
    tf = box.text_frame
    tf.word_wrap = True
    first = True
    for line in text.split("\n"):
        p = tf.paragraphs[0] if first else tf.add_paragraph()
        first = False
        p.text = line
        p.font.size = Pt(size)
        p.font.bold = bold
        p.font.color.rgb = color
        p.font.name = "Calibri"
        p.alignment = align
        p.space_after = Pt(4)
    return box


def pic(slide, name, l, t, w, h):
    p = img / name
    if p.exists():
        slide.shapes.add_picture(str(p), Inches(l), Inches(t), Inches(w), Inches(h))
        return True
    return False


def kicker(slide, text):
    add_text(slide, 0.5, 0.22, 12, 0.35, text, 13, True, ACCENT)


# 1 title
s = paint(prs.slides.add_slide(BLANK))
add_text(s, 0.55, 1.7, 12, 1.1, "Retenqo", 56, True)
add_text(
    s,
    0.55,
    2.9,
    12.2,
    1.6,
    "Private commitment memory on the iQOO 15.\nThe phone compiles what you saw and heard into cards you can Ask — airplane on.",
    22,
    False,
    MUTED,
)
add_text(
    s,
    0.55,
    5.5,
    12,
    1.3,
    "Track 04  ·  Productivity  ·  Pune City Battle 5–6 Sep  ·  Round 1 due 1 Sep 23:59 IST\nLead: Priyanshu Chawda  ·  Local / open-source model at the core  ·  Prototype on Redmi (no Hexagon)",
    15,
    False,
    ACCENT,
)

# 2 problem
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "Problem")
add_text(s, 0.55, 0.7, 12.2, 1.3, "People forget what they owe, promised, and are waiting on.", 28, True)
add_text(
    s,
    0.55,
    2.2,
    12.2,
    4.5,
    "WhatsApp, Calendar pings, LMS pages, meeting talk — the commitments live in five apps.\nRe-reading 400 chats is the work. Missing Friday is the cost.\n\nRetenqo is not another chatbot. It is a private store of those commitments on the phone.\n\nPrimary pitch user: working professional (Calendar + WhatsApp + meetings).\nSame loop for students (LMS instead of Calendar). Demo names are scripted (Priya).",
    18,
    False,
    MUTED,
)

# 3 USP pipeline graphic
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "USP — decision chain")
if not pic(s, "usp-pipeline.png", 0.25, 0.55, 12.8, 6.7):
    add_text(s, 0.55, 1.0, 12, 5, "life stream → cheap extract → cards + embeddings → brief / Ask / Due", 22, True)

# 4 not an agent
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "What we are not claiming")
add_text(s, 0.55, 0.7, 12.2, 0.6, "Not Hermes. Not Pocket Agent. Not cloud memory.", 26, True)
add_text(
    s,
    0.55,
    1.6,
    12.2,
    5.2,
    "• We do not auto-send WhatsApp or UPI, and we never tap other apps.\n"
    "• We do not record the screen 24/7. Pulse is user-started; Focus is timed.\n"
    "• We do not already “have” all of Google — only what was captured (notif / Pulse / Focus).\n"
    "• Redmi stills are the prototype. Hexagon tok/s come from the iQOO 15, never from this phone.\n"
    "• Pulse keeps a text card and deletes the screenshot (ephemeral pixels — gallery empty).\n"
    "• Unfair demo: Calendar 4pm vs WhatsApp 5pm → Ask “When is Priya?” → conflict + both sources.",
    18,
    False,
    MUTED,
)

# 5 demo spine
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "Demo spine — do not change")
add_text(s, 0.55, 0.65, 12, 0.5, "3 minutes on the phone. Scale-only ideas stay on slides.", 20, True)
beats = [
    ("1", "Catch-up brief", "Usefulness in 2 seconds"),
    ("2", "Ask “When is Priya?”", "Conflict: two sources"),
    ("3", "Image → due card", "Phone + OCR / Pulse"),
    ("4", "Airplane + “what do I owe?”", "On-device, cited"),
    ("5", "Pulse dummy → empty gallery", "Ephemeral pixels"),
    ("6", "Optional person-merge", "Same name, two notifs"),
    ("7", "Focus 10–15s or 40s @ 4×", "iQOO NPU chapter"),
    ("8", "Mark one card done", "Keep using it (30%)"),
]
for i, (n, title, sub) in enumerate(beats):
    col, row = i % 4, i // 4
    x, y = 0.45 + col * 3.2, 1.4 + row * 2.7
    sh = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(x), Inches(y), Inches(3.0), Inches(2.4))
    sh.fill.solid()
    sh.fill.fore_color.rgb = CARD
    sh.line.fill.background()
    add_text(s, x + 0.15, y + 0.2, 2.7, 0.4, n, 22, True, ACCENT)
    add_text(s, x + 0.15, y + 0.7, 2.7, 0.9, title, 16, True, WHITE)
    add_text(s, x + 0.15, y + 1.55, 2.7, 0.6, sub, 13, False, MUTED)

# 6 prototype stills row
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "Working prototype — daily phone, labeled no Hexagon")
for i, name in enumerate(["01-home.png", "08-ask-owe.png", "05-memory-timeline.png"]):
    pic(s, name, 0.35 + i * 4.3, 0.7, 4.05, 6.5)

# 7 seven-proof strip
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "Proof strip — real outputs, not mockups")
proofs = [
    ("01-home.png", "Brief"),
    ("08-ask-owe.png", "Ask + cite"),
    ("05-memory-timeline.png", "₹500 demo card"),
    ("13-marked-done.png", "Mark done"),
    ("04-settings-demo.png", "Demo ingest"),
    ("03-settings.png", "On-device setup"),
    ("02-ask.png", "Ask empty"),
]
for i, (name, cap) in enumerate(proofs):
    x = 0.28 + i * 1.86
    pic(s, name, x, 0.7, 1.75, 5.65)
    add_text(s, x, 6.45, 1.75, 0.55, cap, 12, True, ACCENT, PP_ALIGN.CENTER)
add_text(s, 0.45, 7.05, 12, 0.3, "Still needed on device (see SHOOTLIST.md): Pulse → empty gallery · airplane Ask · Focus 4×", 12, False, MUTED)

# 8 architecture graphic
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "Architecture — 10 blocks, no cloud brain")
if not pic(s, "architecture-10.png", 0.2, 0.5, 12.9, 6.8):
    add_text(s, 0.55, 1.0, 12, 5, "Missing architecture-10.png", 18)

# 9 eval table
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "Evals we actually ran (Fedora + honest limits)")
if not pic(s, "eval-table.png", 0.2, 0.5, 12.9, 6.8):
    add_text(s, 0.55, 1.0, 12, 5, "Missing eval-table.png", 18)

# 10 why iQOO
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "Why the iQOO 15")
add_text(s, 0.55, 0.7, 12, 0.7, "Live Focus is the chapter a mid-range phone cannot fake.", 24, True)
add_text(
    s,
    0.55,
    1.6,
    12.2,
    5.2,
    "Snapdragon 8 Elite Gen 5 — Hexagon + Adreno + CPU concurrent.\n"
    "HackTracker: camera/screen Pulse, mic, on-device model — not OpenRouter as the core.\n"
    "CPU VL on Fedora: ~2 min / image. That is why Focus is loaner-only, and why we never\n"
    "print fake Hexagon tok/s from the Redmi.\n\n"
    "Round 1 film: daily phone, caption “no Hexagon”.\n"
    "Floor: same eight-beat script on iQOO 15, Focus live 10–15s or 40s @ 4× with caption\n"
    "“real-time, sped up”.\n\n"
    "If QNN fails: GGUF on 8 Elite CPU. HUD says CPU/GPU honestly.",
    17,
    False,
    MUTED,
)

# 11 Office Kit + scoring
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "Scoring map + Office Kit")
rows = [
    ("30%", "Product", "Airplane Ask, cards, mark done — already on Redmi"),
    ("20%", "Novelty", "Conflict + ephemeral Pulse + Focus — not an agent"),
    ("15%", "Phone", "MediaProjection, mic optional, on-device model"),
    ("15%", "Depth", "Rules → LLM, RAG, citations, AiBackend QNN/GGUF"),
    ("10%", "Office Kit", "Windows: push GGUF/LiteRT in Green; Red Light via Kit"),
    ("10%", "Pitch", "3–5 min on the phone, same spine"),
]
for i, (w, name, ev) in enumerate(rows):
    y = 0.75 + i * 0.95
    sh = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.5), Inches(y), Inches(12.3), Inches(0.85))
    sh.fill.solid()
    sh.fill.fore_color.rgb = CARD
    sh.line.fill.background()
    add_text(s, 0.7, y + 0.18, 1.3, 0.5, w, 20, True, ACCENT)
    add_text(s, 2.1, y + 0.18, 2.4, 0.5, name, 18, True, WHITE)
    add_text(s, 4.6, y + 0.18, 7.9, 0.5, ev, 16, False, MUTED)

# 12 30h delta
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "What 30 hours add — same product")
add_text(
    s,
    0.55,
    0.75,
    12.2,
    5.8,
    "Weekend rebuild / evolve on the loaner. We do not sideload a finished store APK as the entry.\n\n"
    "• Short live Focus on iQOO 15 (video may show 40s at 4× with caption)\n"
    "• Real NPU path if QNN loads; honest CPU/GPU HUD if not\n"
    "• Image → due card faster on Hexagon (CPU VL was ~2 min / frame)\n"
    "• Office Kit model push from Windows\n"
    "• Done / snooze already in prototype — polish if missing on loaner\n\n"
    "Do not build: watchers, AR, Calendar REST, agent loop, fake NPU HUD.\n"
    "FAQ allows continuing a rudimentary prototype. That is this Redmi app.",
    18,
    False,
    MUTED,
)

# 13 feasibility / github
s = paint(prs.slides.add_slide(BLANK))
kicker(s, "Feasibility · impact · links")
add_text(
    s,
    0.55,
    0.75,
    12.2,
    6.0,
    "Someone keeps this on Monday: brief + “what do I owe?” + airplane.\n"
    "Scale = more sources on the same cards — not more tap-tools.\n"
    "Scale-only (slides, not the 3-min speech): waiting > 3 days, draft-you-send, quiet hours, spoken standup.\n\n"
    "GitHub (experiment log, labeled prototype):\n"
    "github.com/priyanshuchawda/mneme\n\n"
    "Spec: docs/superpowers/specs/2026-08-28-retenqo-iqoo-design.md\n"
    "Stills: pitch-assets/  ·  Evals: eval-results/REPORT.md\n"
    "Form blurb: pitch-assets/FORM_BLURB.md  ·  Human shoot: pitch-assets/SHOOTLIST.md\n\n"
    "Team: Priyanshu Chawda (lead). Members TBA before shortlist lock.",
    17,
    False,
    MUTED,
)

# 14 close / ask
s = paint(prs.slides.add_slide(BLANK))
add_text(s, 0.55, 1.8, 12, 1.0, "Cards you can Ask. Airplane on.", 36, True)
add_text(
    s,
    0.55,
    3.1,
    12.2,
    2.2,
    "Retenqo turns what you already saw into owe / promise / waiting / due —\n"
    "private RAG on the phone, Focus on Hexagon, Office Kit on Windows.",
    20,
    False,
    MUTED,
)
add_text(
    s,
    0.55,
    5.6,
    12,
    1.2,
    "github.com/priyanshuchawda/mneme   ·   Track 04 Productivity   ·   Pune 5–6 Sep",
    16,
    False,
    ACCENT,
)

prs.save(str(out))
print("Wrote", out, "slides", len(prs.slides), "bytes", out.stat().st_size)
