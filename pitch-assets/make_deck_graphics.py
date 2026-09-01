#!/usr/bin/env python3
"""Dark 16:9 graphics for the Round-1 deck: eval table + 10-block architecture."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path("/home/priyanshuchawda/retenqo/pitch-assets")
W, H = 1920, 1080
BG = (18, 18, 20)
ACCENT = (196, 163, 90)
WHITE = (245, 240, 232)
MUTED = (168, 164, 156)
CARD = (32, 34, 32)
LINE = (55, 58, 52)
GREEN = (140, 184, 156)


def font(size, bold=False):
    candidates = [
        "/usr/share/fonts/google-noto-vf/NotoSerif[wght].ttf",
        "/usr/share/fonts/google-droid-sans-fonts/DroidSans-Bold.ttf" if bold else "/usr/share/fonts/google-droid-sans-fonts/DroidSans.ttf",
        "/usr/share/fonts/dejavu-sans-fonts/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/dejavu-sans-fonts/DejaVuSans.ttf",
    ]
    for p in candidates:
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def rounded(draw, box, r, fill):
    draw.rounded_rectangle(box, radius=r, fill=fill)


def wrap(draw, text, fnt, max_w):
    words = text.split()
    lines, cur = [], ""
    for w in words:
        trial = (cur + " " + w).strip()
        if draw.textlength(trial, font=fnt) <= max_w:
            cur = trial
        else:
            if cur:
                lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines or [text]


def architecture():
    im = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(im)
    title = font(42, bold=True)
    small = font(22)
    tiny = font(18)
    d.text((64, 36), "On-device path — 10 blocks, no cloud brain", font=title, fill=WHITE)
    d.text((64, 92), "Rules first. LLM only when rules miss. Ask cites cards or refuses.", font=small, fill=MUTED)

    blocks = [
        ("1", "WA / chat\nnotifications", "Allowlisted listener"),
        ("2", "Calendar &\nGmail notifs", "If the OS posts them"),
        ("3", "Pulse\nscreenshot", "MediaProjection + OCR"),
        ("4", "Focus\nscreen + mic", "iQOO Hexagon chapter"),
        ("5", "RulesExtract", "Cheap; no GPU"),
        ("6", "Local Qwen\nJSON", "Only if rules miss"),
        ("7", "Room card\n+ embedding", "due · promise · waiting"),
        ("8", "Catch-up\nbrief", "Since last open"),
        ("9", "Ask +\ncite / refuse", "Vector top-k"),
        ("10", "AiBackend\nGGUF / QNN", "Honest HUD"),
    ]

    cols, rows = 5, 2
    gap_x, gap_y = 24, 28
    left, top = 64, 160
    bw = (W - left * 2 - gap_x * (cols - 1)) // cols
    bh = 340

    for i, (num, name, sub) in enumerate(blocks):
        c, r = i % cols, i // cols
        x = left + c * (bw + gap_x)
        y = top + r * (bh + gap_y)
        rounded(d, (x, y, x + bw, y + bh), 22, CARD)
        d.ellipse((x + 18, y + 18, x + 70, y + 70), fill=ACCENT)
        d.text((x + 32, y + 24), num, font=font(26, bold=True), fill=BG)
        ny = y + 96
        for line in name.split("\n"):
            d.text((x + 20, ny), line, font=font(26, bold=True), fill=WHITE)
            ny += 36
        d.text((x + 20, y + bh - 56), sub, font=tiny, fill=MUTED)

    # flow arrows between row 1
    for c in range(cols - 1):
        x = left + (c + 1) * (bw + gap_x) - gap_x + 2
        y = top + bh // 2
        d.polygon([(x, y - 8), (x + 16, y), (x, y + 8)], fill=ACCENT)

    d.text(
        (64, H - 56),
        "Office Kit (not a block in the phone loop): push GGUF/LiteRT from Windows. Red Light input via Kit on the floor.",
        font=tiny,
        fill=MUTED,
    )
    path = OUT / "architecture-10.png"
    im.save(path, "PNG")
    print("wrote", path)


def eval_table():
    im = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(im)
    d.text((64, 36), "Fedora eval — Qwen3.5-4B Ask (fixed CLI)", font=font(40, bold=True), fill=WHITE)
    d.text(
        (64, 90),
        "Shared context: Uber ₹620 · Netflix ₹1,299 · Priya Thursday 4pm · doctor meds.  Host llama-completion, closed <think>.",
        font=font(20),
        fill=MUTED,
    )

    headers = ["Prompt", "Model output", "Score"]
    rows = [
        ["How much was my airport Uber?", "Your airport Uber cost ₹620 [1].", "Excellent"],
        ["When/where meeting with Priya?", "Meeting with Priya Thursday 4pm, cafe near gate 2 [3].", "Excellent"],
        ["What did Netflix charge?", "Netflix charged INR 1,299 [2].", "Excellent"],
        ["Passport number? (missing)", "I don't have that in your memory.", "Excellent"],
        ["What do I have on Thursday?", "Meeting at 4pm at a cafe near gate 2. [3]", "Good"],
        ["Pulse JSON (clean OCR)", "title + due_date structured", "Good"],
        ["Pulse JSON (dirty OCR)", "JSON valid; body inherits OCR junk", "OK — OCR dominates"],
        ["Whisper base (JFK)", "Classic line, ~3.5s laptop CPU", "Keep"],
        ["Qwen3-VL-2B receipt (CPU)", "~2 min / image — not v1 Pulse", "Defer"],
        ["Qwen3 / Qwen2.5 on this host", "Garbage decode", "Avoid"],
    ]

    left, top = 64, 150
    col_w = [520, 980, 280]
    row_h = 72
    # header
    x = left
    for i, h in enumerate(headers):
        rounded(d, (x, top, x + col_w[i] - 8, top + 52), 10, ACCENT)
        d.text((x + 16, top + 12), h, font=font(22, bold=True), fill=BG)
        x += col_w[i]

    y = top + 62
    for ri, row in enumerate(rows):
        x = left
        bg = CARD if ri % 2 == 0 else (26, 28, 26)
        for i, cell in enumerate(row):
            rounded(d, (x, y, x + col_w[i] - 8, y + row_h - 8), 8, bg)
            color = GREEN if i == 2 and cell in ("Excellent", "Good", "Keep") else WHITE
            if cell.startswith("OK") or cell in ("Defer", "Avoid"):
                color = ACCENT
            d.text((x + 14, y + 18), cell, font=font(20), fill=color)
            x += col_w[i]
        y += row_h

    d.text(
        (64, H - 48),
        "Redmi daily: Qwen3.5-2B.  iQOO / quality: same pipeline, swap 4B.  Never fake Hexagon tok/s from this phone.",
        font=font(20),
        fill=MUTED,
    )
    path = OUT / "eval-table.png"
    im.save(path, "PNG")
    print("wrote", path)


def usp_strip():
    im = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(im)
    d.text((64, 40), "USP — capture → card → brief / Ask / due", font=font(40, bold=True), fill=WHITE)
    steps = [
        ("See", "WA · Calendar\nnotif · Pulse"),
        ("Extract", "Rules, then\nrare local LLM"),
        ("Card", "due · promise\nwaiting · fact"),
        ("Brief", "Since last\nopen, 2 seconds"),
        ("Ask", "Cite or refuse\nairplane-capable"),
        ("Focus", "Live NPU on\niQOO 15 only"),
    ]
    n = len(steps)
    left, top = 48, 200
    bw = (W - 96 - 20 * (n - 1)) // n
    for i, (h, b) in enumerate(steps):
        x = left + i * (bw + 20)
        rounded(d, (x, top, x + bw, top + 620), 24, CARD)
        d.ellipse((x + bw // 2 - 36, top + 36, x + bw // 2 + 36, top + 108), fill=ACCENT)
        tw = d.textlength(str(i + 1), font=font(32, bold=True))
        d.text((x + bw // 2 - tw / 2, top + 52), str(i + 1), font=font(32, bold=True), fill=BG)
        d.text((x + 24, top + 140), h, font=font(34, bold=True), fill=WHITE)
        yy = top + 210
        for line in b.split("\n"):
            d.text((x + 24, yy), line, font=font(24), fill=MUTED)
            yy += 40
        if i < n - 1:
            ax = x + bw + 2
            d.polygon([(ax, top + 310), (ax + 16, top + 322), (ax, top + 334)], fill=ACCENT)
    d.text((64, H - 70), "Not an agent: no auto-send, no tapping WhatsApp, no 24/7 screenshots.", font=font(24), fill=ACCENT)
    path = OUT / "usp-pipeline.png"
    im.save(path, "PNG")
    print("wrote", path)


if __name__ == "__main__":
    architecture()
    eval_table()
    usp_strip()
