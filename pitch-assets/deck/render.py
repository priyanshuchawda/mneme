#!/usr/bin/env python3
"""Render designed phones + full-bleed slides, assemble PPTX."""
from pathlib import Path

from playwright.sync_api import sync_playwright
from pptx import Presentation
from pptx.enum.shapes import MSO_SHAPE
from pptx.util import Inches

ROOT = Path("/home/priyanshuchawda/retenqo/pitch-assets/deck")
OUT = ROOT / "out"
OUT.mkdir(exist_ok=True)

PHONES = [
    "conflict",
    "airplane",
    "cards",
    "messy",
    "pulse",
    "gallery",
    "focus",
    "refuse",
]

PROOFS = [
    "hero-messy",
    "hero-cards",
    "p-capture",
    "p-extract",
    "p-card",
    "p-retrieve",
    "p-route",
    "p-cite",
    "p-refuse",
    "p-act",
    "p-delete",
    "f-sources",
    "f-schema",
    "f-conflict",
    "f-cite",
    "w-brief",
    "w-ask",
    "w-conflict",
    "w-refuse",
]


def render():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1600, "height": 1200}, device_scale_factor=2)

        page.goto((ROOT / "phones.html").as_uri(), wait_until="networkidle")
        for pid in PHONES:
            el = page.locator(f"#{pid}")
            el.screenshot(path=str(OUT / f"mock-{pid}.png"), type="png")
            print("phone", pid)

        page.goto((ROOT / "proofs.html").as_uri(), wait_until="networkidle")
        for pid in PROOFS:
            el = page.locator(f"#{pid}")
            el.screenshot(path=str(OUT / f"proof-{pid}.png"), type="png")
            print("proof", pid)

        page.goto((ROOT / "artifacts.html").as_uri(), wait_until="networkidle")
        for aid in ["native-wa", "privacy", "action"]:
            el = page.locator(f"#{aid}")
            el.screenshot(path=str(OUT / f"{aid}.png"), type="png")
            print("artifact", aid)

        page.set_viewport_size({"width": 1920, "height": 1080})
        page.goto((ROOT / "slides.html").as_uri(), wait_until="networkidle")
        for stale in OUT.glob("slide-*.jpg"):
            stale.unlink()
        n = page.locator("section.slide").count()
        for i in range(n):
            slide = page.locator("section.slide").nth(i)
            dest = OUT / f"slide-{i+1:02d}.jpg"
            slide.screenshot(path=str(dest), type="jpeg", quality=90)
            print("slide", dest.name)
        browser.close()


def assemble():
    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    blank = prs.slide_layouts[6]
    slides = sorted(OUT.glob("slide-*.jpg"))
    for img in slides:
        s = prs.slides.add_slide(blank)
        # kill default white by covering
        s.shapes.add_picture(str(img), 0, 0, prs.slide_width, prs.slide_height)
    dest = Path("/home/priyanshuchawda/retenqo/pitch-assets/Retenqo-iQOO-Round1.pptx")
    prs.save(str(dest))
    print("pptx", dest, dest.stat().st_size, "slides", len(slides))


if __name__ == "__main__":
    render()
    assemble()
