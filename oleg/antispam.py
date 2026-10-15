import time
from collections import defaultdict, deque

# (chat_id, user_id) -> deque[(timestamp, message_id)]
_history: dict[tuple[int, int], deque] = defaultdict(deque)


def register(chat_id: int, user_id: int, message_id: int, limit: int, window: int) -> list[int] | None:
    """Записывает сообщение. Если за `window` секунд набралось `limit` сообщений
    подряд — возвращает id всех сообщений всплеска (их надо удалить), иначе None."""
    now = time.monotonic()
    q = _history[(chat_id, user_id)]
    q.append((now, message_id))
    while q and now - q[0][0] > window:
        q.popleft()
    if len(q) >= limit:
        ids = [m for _, m in q]
        q.clear()
        return ids
    return None
