# mod/ — PampGram patch and modules for Telegram-Android

Структура повторяет iOS-мод:

- `telegram-android.patch` — единый патч против официального
  [DrKLO/Telegram](https://github.com/DrKLO/Telegram). Трогает только файлы,
  которые уже существуют в upstream. Этап 0 — пустой (`git apply` не делает
  ничего).
- `components/` — новые модули PampGram, которые просто копируются внутрь
  `TMessagesProj/src/main/java/org/telegram/pampgram/` при сборке. Отдельно
  от патча, чтобы правка собственного кода не требовала перегенерации патча.
  - `PampGramCore` — локальная БД, модели, бизнес-логика.
  - `PampGramSettingsUI` — экраны настроек PampGram.
  - `PhantomGiftKit` — локальные подарки, визуальные балансы, casino bridge.
- `apply-settings-entry.py` — anchor-checked post-patch, который добавляет
  строку «PampGram» в экран настроек (`ProfileActivity.java`) и подключает к
  ней `PampGramSettingsActivity`. Все 7 правок (field, reset, rowCount,
  onItemClick, render, isEnabled, getItemViewType) идемпотентны и падают
  громко, если якорь не найден (значит, upstream разъехался).
- `apply-profile-visuals.py` — такой же anchor-checked post-patch, но для
  визуальных правок самого профиля (`+888`, рейтинг). Пустая заглушка в
  этапе 1; будет заполнен в этапе 3.

Этап 1: заполнены `PampGramCore` (локальная БД на `SQLiteOpenHelper`:
балансы, история, визуальный профиль) и `PampGramSettingsUI` (hub-экран с
placeholder-разделами для будущих этапов). `PhantomGiftKit` всё ещё пустой —
наполнится в этапе 2.
