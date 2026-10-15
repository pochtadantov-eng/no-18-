import asyncio
import io
import logging
import time
from datetime import timedelta

from aiogram import Bot, Dispatcher, F, Router
from aiogram.filters import Command
from aiogram.types import (
    CallbackQuery, ChatMemberUpdated, ChatPermissions, InlineKeyboardButton,
    InlineKeyboardMarkup, Message,
)
from aiogram.exceptions import TelegramBadRequest, TelegramForbiddenError

from . import antispam, db, vision
from .config import BOT_TOKEN, OWNER_ID

log = logging.getLogger("oleg")
router = Router()
GROUPS = {"group", "supergroup"}
_admin_cache: dict[tuple[int, int], tuple[float, bool]] = {}


# ───────────────────────── меню настройки (только в личке, только владелец) ─────────────────────────

def _on(v: bool) -> str:
    return "✅ вкл" if v else "❌ выкл"


def chats_kb() -> InlineKeyboardMarkup:
    rows = [[InlineKeyboardButton(text=c.title or str(c.chat_id), callback_data=f"c:{c.chat_id}")]
            for c in db.list_chats()]
    rows.append([InlineKeyboardButton(text="🔄 Обновить", callback_data="list")])
    return InlineKeyboardMarkup(inline_keyboard=rows)


def panel(chat_id: int) -> tuple[str, InlineKeyboardMarkup]:
    c = db.get_chat(chat_id)
    act = "бан навсегда" if c.spam_action == "ban" else f"мут на {c.mute_minutes} мин"
    text = (
        f"🛡 <b>{c.title}</b>\n\n"
        f"Фильтр медиа (18+, расчленёнка): {_on(c.media_on)}\n"
        f"Антиспам: {_on(c.spam_on)}\n"
        f"Порог спама: {c.spam_limit} сообщений за {c.spam_window} сек\n"
        f"Наказание за спам: {act}\n\n"
        "За запрещённое медиа — удаление сообщения и бан."
    )
    kb = InlineKeyboardMarkup(inline_keyboard=[
        [InlineKeyboardButton(text=f"Медиа-фильтр: {_on(c.media_on)}", callback_data=f"t:{chat_id}:media_on")],
        [InlineKeyboardButton(text=f"Антиспам: {_on(c.spam_on)}", callback_data=f"t:{chat_id}:spam_on")],
        [InlineKeyboardButton(text="➖", callback_data=f"n:{chat_id}:-"),
         InlineKeyboardButton(text=f"Порог: {c.spam_limit}", callback_data="noop"),
         InlineKeyboardButton(text="➕", callback_data=f"n:{chat_id}:+")],
        [InlineKeyboardButton(text=f"Наказание: {act}", callback_data=f"a:{chat_id}")],
        [InlineKeyboardButton(text="⬅️ К чатам", callback_data="list")],
    ])
    return text, kb


@router.message(Command("start"), F.chat.type == "private")
async def cmd_start(m: Message):
    if m.from_user.id != OWNER_ID:
        await m.answer("Я Олег Лямкин, охраняю порядок в чатах. Настраивать меня может только владелец.")
        return
    await m.answer(
        "Здравствуйте, я Олег Лямкин, отвечаю за безопасность.\n"
        "Добавьте меня в группу администратором (права: удалять сообщения и банить), "
        "затем выберите чат ниже.",
        reply_markup=chats_kb(),
    )


@router.callback_query(F.from_user.id != OWNER_ID)
async def not_owner(cb: CallbackQuery):
    await cb.answer("Только владелец может это настраивать", show_alert=True)


@router.callback_query(F.data == "list")
async def cb_list(cb: CallbackQuery):
    await cb.message.edit_text("Выберите чат:", reply_markup=chats_kb())
    await cb.answer()


@router.callback_query(F.data == "noop")
async def cb_noop(cb: CallbackQuery):
    await cb.answer()


async def _refresh(cb: CallbackQuery, chat_id: int):
    text, kb = panel(chat_id)
    try:
        await cb.message.edit_text(text, reply_markup=kb, parse_mode="HTML")
    except TelegramBadRequest:
        pass
    await cb.answer()


@router.callback_query(F.data.startswith("c:"))
async def cb_chat(cb: CallbackQuery):
    await _refresh(cb, int(cb.data.split(":")[1]))


@router.callback_query(F.data.startswith("t:"))
async def cb_toggle(cb: CallbackQuery, bot: Bot):
    _, cid, field = cb.data.split(":")
    cid = int(cid)
    c = db.get_chat(cid)
    new = not getattr(c, field)
    if new:
        me = await bot.get_chat_member(cid, bot.id)
        if me.status != "administrator" or not getattr(me, "can_delete_messages", False) \
                or not getattr(me, "can_restrict_members", False):
            await cb.answer("Сделайте меня админом с правами «Удалять сообщения» и «Блокировать пользователей»",
                            show_alert=True)
            return
    db.set_field(cid, field, int(new))
    await _refresh(cb, cid)


@router.callback_query(F.data.startswith("n:"))
async def cb_num(cb: CallbackQuery):
    _, cid, sign = cb.data.split(":")
    cid = int(cid)
    c = db.get_chat(cid)
    db.set_field(cid, "spam_limit", max(3, min(20, c.spam_limit + (1 if sign == "+" else -1))))
    await _refresh(cb, cid)


@router.callback_query(F.data.startswith("a:"))
async def cb_action(cb: CallbackQuery):
    cid = int(cb.data.split(":")[1])
    c = db.get_chat(cid)
    db.set_field(cid, "spam_action", "ban" if c.spam_action == "mute" else "mute")
    await _refresh(cb, cid)


# ───────────────────────── учёт чатов ─────────────────────────

@router.my_chat_member()
async def on_my_status(u: ChatMemberUpdated, bot: Bot):
    if u.chat.type not in GROUPS:
        return
    present = u.new_chat_member.status in {"member", "administrator"}
    db.upsert_chat(u.chat.id, u.chat.title or str(u.chat.id), present)
    if present:
        try:
            await bot.send_message(
                OWNER_ID,
                f"Добавлен в «{u.chat.title}». Сделайте меня админом и включите защиту: /start")
        except TelegramForbiddenError:
            pass


# ───────────────────────── модерация в группах ─────────────────────────

async def is_admin(bot: Bot, chat_id: int, user_id: int) -> bool:
    key = (chat_id, user_id)
    hit = _admin_cache.get(key)
    if hit and time.monotonic() - hit[0] < 60:
        return hit[1]
    mem = await bot.get_chat_member(chat_id, user_id)
    res = mem.status in {"creator", "administrator"}
    _admin_cache[key] = (time.monotonic(), res)
    return res


def media_file(m: Message):
    """(file_id, описание) картинки, которую можно проверить: само фото или превью видео/стикера."""
    if m.photo:
        return m.photo[-1].file_id
    for obj in (m.video, m.animation, m.video_note, m.sticker):
        if obj:
            if m.sticker and not (m.sticker.is_animated or m.sticker.is_video):
                return m.sticker.file_id
            if obj.thumbnail:
                return obj.thumbnail.file_id
            return None
    if m.document and m.document.mime_type and m.document.mime_type.startswith("image/"):
        return m.document.file_id
    if m.document and m.document.thumbnail:
        return m.document.thumbnail.file_id
    return None


async def punish_media(bot: Bot, m: Message, category: str):
    user = m.from_user
    await m.delete()
    await bot.ban_chat_member(m.chat.id, user.id)
    label = "18+ контент" if category == "nsfw" else "жестокий контент"
    await bot.send_message(
        m.chat.id,
        f"🚫 Олег Лямкин: {user.full_name} забанен за {label}. Сообщение удалено.")


async def punish_spam(bot: Bot, m: Message, c: db.Chat, ids: list[int]):
    user = m.from_user
    try:
        await bot.delete_messages(m.chat.id, ids)
    except TelegramBadRequest:
        await m.delete()
    if c.spam_action == "ban":
        await bot.ban_chat_member(m.chat.id, user.id)
        verdict = "забанен"
    else:
        until = timedelta(minutes=c.mute_minutes)
        await bot.restrict_chat_member(
            m.chat.id, user.id, permissions=ChatPermissions(can_send_messages=False),
            until_date=until)
        verdict = f"замучен на {c.mute_minutes} мин"
    await bot.send_message(m.chat.id, f"🚫 Олег Лямкин: {user.full_name} {verdict} за спам.")


@router.message(F.chat.type.in_(GROUPS), ~F.from_user.is_bot)
async def on_group_message(m: Message, bot: Bot):
    if m.from_user is None or m.sender_chat:
        return
    c = db.get_chat(m.chat.id)
    if c is None:
        db.upsert_chat(m.chat.id, m.chat.title or str(m.chat.id))
        return
    if not (c.media_on or c.spam_on):
        return
    try:
        if await is_admin(bot, m.chat.id, m.from_user.id):
            return

        if c.spam_on:
            ids = antispam.register(m.chat.id, m.from_user.id, m.message_id, c.spam_limit, c.spam_window)
            if ids:
                await punish_spam(bot, m, c, ids)
                return

        if c.media_on:
            fid = media_file(m)
            if fid:
                buf = io.BytesIO()
                await bot.download(fid, destination=buf)
                bad, category = await vision.check_image(buf.getvalue())
                if bad:
                    await punish_media(bot, m, category)
    except (TelegramBadRequest, TelegramForbiddenError) as e:
        log.warning("не хватает прав в чате %s: %s", m.chat.id, e)


async def main():
    logging.basicConfig(level=logging.INFO)
    bot = Bot(BOT_TOKEN)
    dp = Dispatcher()
    dp.include_router(router)
    await dp.start_polling(bot, allowed_updates=dp.resolve_used_update_types())


def run():
    asyncio.run(main())
