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
- `apply-profile-visuals.py` — anchor-checked post-patch для
  `ProfileActivity.java`. Запускается после базового патча; падает, если якорь
  не найден (значит, upstream разъехался и правку надо обновить).

Пока все три компонента — пустые пакеты с `package-info.java` и BUILD-маркером,
чтобы Gradle видел их как источники. Код добавится в этапах 1-6.
