import sqlite3
from dataclasses import dataclass

from .config import DB_PATH

_conn = sqlite3.connect(DB_PATH, check_same_thread=False)
_conn.row_factory = sqlite3.Row
_conn.execute(
    """CREATE TABLE IF NOT EXISTS chats (
        chat_id INTEGER PRIMARY KEY,
        title TEXT NOT NULL DEFAULT '',
        media_on INTEGER NOT NULL DEFAULT 0,
        spam_on INTEGER NOT NULL DEFAULT 0,
        spam_limit INTEGER NOT NULL DEFAULT 6,
        spam_window INTEGER NOT NULL DEFAULT 8,
        spam_action TEXT NOT NULL DEFAULT 'mute',
        mute_minutes INTEGER NOT NULL DEFAULT 60,
        active INTEGER NOT NULL DEFAULT 1
    )"""
)
_conn.commit()


@dataclass
class Chat:
    chat_id: int
    title: str
    media_on: bool
    spam_on: bool
    spam_limit: int
    spam_window: int
    spam_action: str  # 'mute' | 'ban'
    mute_minutes: int
    active: bool


def _row(r: sqlite3.Row) -> Chat:
    return Chat(
        r["chat_id"], r["title"], bool(r["media_on"]), bool(r["spam_on"]),
        r["spam_limit"], r["spam_window"], r["spam_action"], r["mute_minutes"],
        bool(r["active"]),
    )


def upsert_chat(chat_id: int, title: str, active: bool = True) -> None:
    _conn.execute(
        "INSERT INTO chats(chat_id, title, active) VALUES(?,?,?) "
        "ON CONFLICT(chat_id) DO UPDATE SET title=excluded.title, active=excluded.active",
        (chat_id, title, int(active)),
    )
    _conn.commit()


def get_chat(chat_id: int) -> Chat | None:
    r = _conn.execute("SELECT * FROM chats WHERE chat_id=?", (chat_id,)).fetchone()
    return _row(r) if r else None


def list_chats() -> list[Chat]:
    rows = _conn.execute("SELECT * FROM chats WHERE active=1 ORDER BY title").fetchall()
    return [_row(r) for r in rows]


def set_field(chat_id: int, field: str, value) -> None:
    assert field in {"media_on", "spam_on", "spam_limit", "spam_window", "spam_action", "mute_minutes"}
    _conn.execute(f"UPDATE chats SET {field}=? WHERE chat_id=?", (value, chat_id))
    _conn.commit()
