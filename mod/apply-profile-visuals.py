#!/usr/bin/env python3
"""Anchor-checked post-patch for Telegram-Android's ProfileActivity.

Mirror of mod/apply-profile-visuals.py from the iOS repo. In later stages this
edits TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java to
inject the visual +888 and visual rating hooks right where the real number and
rating are rendered.

Stage 0: no-op. The script runs cleanly but prints that nothing is wired yet,
so the CI step stays in place and later stages only need to flip the TODO.
"""

from __future__ import annotations

import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
TELEGRAM_ROOT = Path.cwd()  # the workflow cds into telegram-android first
PROFILE_ACTIVITY = (
    TELEGRAM_ROOT
    / "TMessagesProj"
    / "src"
    / "main"
    / "java"
    / "org"
    / "telegram"
    / "ui"
    / "ProfileActivity.java"
)


def main() -> int:
    if not PROFILE_ACTIVITY.exists():
        print(f"FAIL: {PROFILE_ACTIVITY} not found; is the working directory telegram-android?")
        return 1
    print(f"apply-profile-visuals.py: nothing to inject yet (stage 0); {PROFILE_ACTIVITY.name} is present.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
