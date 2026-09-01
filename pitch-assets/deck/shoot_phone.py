#!/usr/bin/env python3
"""Wake the phone, open each pitch frontend shot, screencap into deck/out."""
from pathlib import Path
import subprocess
import time

SERIAL = "1431df87"
PKG = "com.mneme.app.debug"
ACTIVITY = f"{PKG}/com.mneme.app.MainActivity"
OUT = Path("/home/priyanshuchawda/retenqo/pitch-assets/deck/out")
OUT.mkdir(exist_ok=True)

SHOTS = [
    "messy",
    "cards",
    "brief",
    "owe",
    "conflict",
    "refuse",
    "pulse",
    "focus",
    "gallery",
]


def adb(*args: str, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["adb", "-s", SERIAL, *args],
        check=check,
        capture_output=True,
    )


def main() -> None:
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    adb("shell", "wm", "dismiss-keyguard", check=False)
    time.sleep(0.4)

    for shot in SHOTS:
        adb(
            "shell",
            "am",
            "start",
            "-n",
            ACTIVITY,
            "-a",
            "android.intent.action.MAIN",
            "--es",
            "pitch",
            shot,
        )
        time.sleep(1.6)
        dest = OUT / f"phone-{shot}.png"
        cap = adb("exec-out", "screencap", "-p")
        dest.write_bytes(cap.stdout)
        print(f"wrote {dest} ({len(cap.stdout)} bytes)")

    print("done")


if __name__ == "__main__":
    main()
