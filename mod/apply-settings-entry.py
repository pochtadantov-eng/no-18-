#!/usr/bin/env python3
"""Anchor-checked integration of the PampGram settings entry into ProfileActivity.

Why anchor-based and not a unified-diff patch: ProfileActivity.java is ~14k
lines and churns upstream. A line-numbered patch would break every few weeks.
Anchor edits key off stable code fragments (specific field declarations and
if-chains) and fail loudly when the shape changes, so a mismatch surfaces here
instead of mid-build or, worse, as silently-missing UI.

Every edit is idempotent — re-running on an already-patched tree is a no-op.
Any anchor missing is a hard failure, no silent skip.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path


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


def _apply_once(src: str, marker: str, anchor: str, replacement: str, where: str) -> str:
    """Insert `replacement` once at `anchor`; raise if the anchor doesn't match."""
    if marker in src:
        return src  # already applied
    hits = src.count(anchor)
    if hits == 0:
        raise SystemExit(
            f"apply-settings-entry: anchor missing at {where}: {anchor!r}. "
            "Upstream ProfileActivity.java moved; update this script."
        )
    if hits > 1:
        raise SystemExit(
            f"apply-settings-entry: anchor matches {hits} times at {where}: {anchor!r}. "
            "The anchor is no longer unique; tighten it."
        )
    return src.replace(anchor, replacement, 1)


def _apply_regex(src: str, marker: str, pattern: str, replacement: str, where: str) -> str:
    if marker in src:
        return src
    new_src, n = re.subn(pattern, replacement, src, count=1)
    if n == 0:
        raise SystemExit(
            f"apply-settings-entry: regex anchor missing at {where}: {pattern!r}. "
            "Upstream ProfileActivity.java moved; update this script."
        )
    return new_src


def main() -> int:
    if not PROFILE_ACTIVITY.exists():
        print(f"FAIL: {PROFILE_ACTIVITY} not found; is the working directory telegram-android?")
        return 1

    s = PROFILE_ACTIVITY.read_text()

    # 1. Field declaration. Marker: the token "pampgramRow" lets us detect the
    #    patch idempotently; we add it right after liteModeRow which has been
    #    stable in that block for a long time.
    s = _apply_once(
        s,
        marker="private int pampgramRow;",
        anchor="    private int liteModeRow;\n",
        replacement="    private int liteModeRow;\n    private int pampgramRow;\n",
        where="field declaration block",
    )

    # 2. Row reset in updateRowsIds() (set to -1 before any assignment).
    s = _apply_once(
        s,
        marker="pampgramRow = -1;",
        anchor="        liteModeRow = -1;\n",
        replacement="        liteModeRow = -1;\n        pampgramRow = -1;\n",
        where="row reset block",
    )

    # 3. Row assignment: insert right after languageRow in the settings-section
    #    builder. The exact lines "liteModeRow = rowCount++;" and
    #    "languageRow = rowCount++;" are inside the same block in that order.
    #    We hook after languageRow so our row appears last in the settings list,
    #    just above "Devices" — least invasive placement.
    s = _apply_once(
        s,
        marker="pampgramRow = rowCount++;",
        anchor="                languageRow = rowCount++;\n",
        replacement="                languageRow = rowCount++;\n                pampgramRow = rowCount++;\n",
        where="settings section rowCount block",
    )

    # 4. isEnabled(): add pampgramRow to the long OR-chain. Match the tail
    #    "position == linkedCommunityRow;" of the current chain.
    s = _apply_regex(
        s,
        marker="position == pampgramRow",  # once added, won't rematch
        pattern=r"(position == linkedCommunityRow;)",
        replacement=r"position == pampgramRow || \1",
        where="isEnabled() OR-chain",
    )

    # 5. getItemViewType(): add pampgramRow to the VIEW_TYPE_TEXT chain. The
    #    chain's unique tail is "position == tonRow\n            )".
    s = _apply_regex(
        s,
        marker="position == pampgramRow\n",  # distinct from the isEnabled form above (semicolon vs \n)
        pattern=r"(position == tonRow\n\s+\))",
        replacement=r"position == tonRow || position == pampgramRow\n            )",
        where="getItemViewType() VIEW_TYPE_TEXT chain",
    )

    # 6. onBindViewHolder rendering: add a textCell.setTextAndIcon call. Hook
    #    right after the liteModeRow render so the row title/icon are
    #    consistent in style. msg2_discussion is a safe placeholder icon; we
    #    swap it for a dedicated drawable in stage 2 when assets land.
    s = _apply_once(
        s,
        marker='textCell.setTextAndIcon("PampGram"',
        anchor='                    } else if (position == liteModeRow) {\n'
               '                        textCell.setTextAndIcon(LocaleController.getString(R.string.PowerUsage), R.drawable.msg2_battery, true);\n',
        replacement='                    } else if (position == liteModeRow) {\n'
                    '                        textCell.setTextAndIcon(LocaleController.getString(R.string.PowerUsage), R.drawable.msg2_battery, true);\n'
                    '                    } else if (position == pampgramRow) {\n'
                    '                        textCell.setTextAndIcon("PampGram", R.drawable.msg2_discussion, true);\n',
        where="onBindViewHolder render block",
    )

    # 7. onItemClick: open PampGramSettingsActivity when the row is tapped.
    s = _apply_once(
        s,
        marker="new org.telegram.pampgram.settings.PampGramSettingsActivity()",
        anchor="            } else if (position == liteModeRow) {\n"
               "                presentFragment(new LiteModeSettingsActivity());\n",
        replacement="            } else if (position == liteModeRow) {\n"
                    "                presentFragment(new LiteModeSettingsActivity());\n"
                    "            } else if (position == pampgramRow) {\n"
                    "                presentFragment(new org.telegram.pampgram.settings.PampGramSettingsActivity());\n",
        where="onItemClick dispatch block",
    )

    PROFILE_ACTIVITY.write_text(s)
    print("apply-settings-entry: 7 anchor edits applied (or already in place).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
