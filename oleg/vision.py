"""Проверка медиа на 18+ / расчленёнку / жестокость через Claude (vision)."""
import base64
import io
import json
import logging

from anthropic import AsyncAnthropic
from PIL import Image

from .config import ANTHROPIC_API_KEY, VISION_MODEL

log = logging.getLogger(__name__)
_client = AsyncAnthropic(api_key=ANTHROPIC_API_KEY) if ANTHROPIC_API_KEY else None

PROMPT = (
    "Ты — модератор контента в Telegram-чате. Оцени изображение. "
    "Нарушением считается: порнография и обнажёнка (nsfw), расчленёнка, трупы, "
    "кровавое насилие, жестокость над людьми или животными (gore), самоповреждение, "
    "сексуализация несовершеннолетних (nsfw). Обычные фото, мемы, одежда, купальники "
    "без эротики, спорт, еда, пейзажи — НЕ нарушение. "
    'Ответь ТОЛЬКО JSON: {"violation": true|false, "category": "nsfw"|"gore"|"none", '
    '"confidence": 0.0-1.0}'
)


def _to_jpeg(data: bytes) -> bytes:
    img = Image.open(io.BytesIO(data))
    img.seek(0)
    img = img.convert("RGB")
    img.thumbnail((1024, 1024))
    out = io.BytesIO()
    img.save(out, "JPEG", quality=85)
    return out.getvalue()


async def check_image(data: bytes) -> tuple[bool, str]:
    """Возвращает (нарушение, категория). При ошибке — (False, 'error')."""
    if _client is None:
        return False, "no_api_key"
    try:
        jpeg = _to_jpeg(data)
        resp = await _client.messages.create(
            model=VISION_MODEL,
            max_tokens=100,
            messages=[{
                "role": "user",
                "content": [
                    {"type": "image", "source": {
                        "type": "base64", "media_type": "image/jpeg",
                        "data": base64.b64encode(jpeg).decode()}},
                    {"type": "text", "text": PROMPT},
                ],
            }],
        )
        text = resp.content[0].text
        verdict = json.loads(text[text.index("{"): text.rindex("}") + 1])
        bad = bool(verdict.get("violation")) and float(verdict.get("confidence", 1)) >= 0.6
        return bad, str(verdict.get("category", "none"))
    except Exception:
        log.exception("vision check failed")
        return False, "error"
