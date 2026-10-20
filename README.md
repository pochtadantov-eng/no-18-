# PampGram for Android

Мод для официального [Telegram-Android](https://github.com/DrKLO/Telegram) (клиент DrKLO).
Форк собирается отдельно, здесь хранятся патч (`mod/telegram-android.patch`) и новые
модули под `mod/components/`. Логически повторяет [PampGram для iOS](https://github.com/pochtadantov-eng/PampGram):
те же функции, та же схема «base-patch + отдельные модули + anchor-checked
post-patch», только на стеке Android (Java/Kotlin, Gradle).

## Статус

Этап 0 — только скелет и CI, который собирает чистый APK Telegram-Android. Патч
пустой, модули — пустые пакеты. Этапы 1-6 добавляют функции (см. [FEATURES-RU.md](FEATURES-RU.md)).

## Сборка

APK собирается в GitHub Actions из workflow `.github/workflows/build-apk.yml`
вручную (Actions → Build APK → Run workflow). Нужны репозиторные секреты
`TELEGRAM_API_ID` и `TELEGRAM_API_HASH` ([my.telegram.org/apps](https://my.telegram.org/apps)).
Готовый APK подписан самоподписанным debug-ключом, который генерируется в самом
workflow — сторонние сервисы вроде Sideloadly здесь не нужны, APK ставится на
Android напрямую (с включённой опцией «Установка из неизвестных источников»).
