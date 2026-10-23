package org.telegram.pampgram.settings;

import org.telegram.pampgram.core.PampGramStorage;

import java.util.List;

// Дополнительно section — maps to PampGramAdditionalSettingsScreen.swift.
// Collects the misc toggles that don't fit into any other section.
public final class PampGramAdditionalScreen extends PampGramPreferenceFragment {

    public static final String KEY_INFINITE_PINS   = "advanced.infinite_pins";
    public static final String KEY_LEGAL_PREMIUM   = "advanced.legal_premium";
    public static final String KEY_MASTER_ENABLED  = "advanced.master_enabled";
    public static final String KEY_FAKE_LOCATION   = "advanced.fake_location_enabled";
    public static final String KEY_FAKE_LAT        = "advanced.fake_location_lat";
    public static final String KEY_FAKE_LNG        = "advanced.fake_location_lng";
    public static final String KEY_CHAT_LOCK       = "advanced.chat_lock_enabled";
    public static final String KEY_UPLOAD_SPEED    = "advanced.upload_speed";   // "default"|"fast"|"turbo"
    public static final String KEY_DOWNLOAD_SPEED  = "advanced.download_speed";

    @Override
    protected String screenTitle() {
        return "Дополнительно";
    }

    @Override
    protected void onCreateRows(List<Row> out) {
        PampGramStorage s = PampGramStorage.getInstance();

        out.add(Row.header("Модификации"));
        out.add(Row.toggle(KEY_MASTER_ENABLED, true,  "PampGram включён"));
        out.add(Row.info("Главный выключатель мода. При выключении все локальные фичи PampGram неактивны, Telegram работает как обычно."));

        out.add(Row.header("Скорость"));
        out.add(Row.value("Ускорение загрузки",
                speedDisplayName(s.getKvString(KEY_UPLOAD_SPEED, "default")),
                () -> showComingSoon("Ускорение загрузки")));
        out.add(Row.value("Ускорение скачивания",
                speedDisplayName(s.getKvString(KEY_DOWNLOAD_SPEED, "default")),
                () -> showComingSoon("Ускорение скачивания")));
        out.add(Row.info("Поднимает число параллельных потоков передачи. При очень слабой сети выключите, если видите артефакты."));

        out.add(Row.header("Чаты"));
        out.add(Row.toggle(KEY_INFINITE_PINS, false, "Закрепить чаты ∞"));
        out.add(Row.toggle(KEY_LEGAL_PREMIUM, false, "Легальный премиум"));
        out.add(Row.info("Снимает лимит на число закреплённых чатов и локально помечает аккаунт как Premium (только для вашей клиентской UI)."));

        out.add(Row.header("Геолокация"));
        out.add(Row.toggle(KEY_FAKE_LOCATION, false, "Фейковая геолокация"));
        out.add(Row.value("Координаты", formatCoords(s), () -> showComingSoon("Выбор координат")));
        out.add(Row.info("При включении Telegram будет использовать указанные координаты вместо реальных для «Live Location» и «Поблизости»."));

        out.add(Row.header("Приватность"));
        out.add(Row.toggle(KEY_CHAT_LOCK, false, "Блокировка чатов"));
        out.add(Row.value("Выбор чатов", "—", () -> showComingSoon("Выбор чатов для блокировки")));
        out.add(Row.info("Выбранные чаты скроются из основного списка и будут требовать PIN."));
    }

    private static String speedDisplayName(String mode) {
        if (mode == null) return "По умолчанию";
        switch (mode) {
            case "fast":  return "Быстро";
            case "turbo": return "Турбо";
            default:       return "По умолчанию";
        }
    }

    private static String formatCoords(PampGramStorage s) {
        if (!s.getKvBool(KEY_FAKE_LOCATION, false)) return "Выключено";
        long lat100k = s.getKvLong(KEY_FAKE_LAT, Long.MIN_VALUE);
        long lng100k = s.getKvLong(KEY_FAKE_LNG, Long.MIN_VALUE);
        if (lat100k == Long.MIN_VALUE || lng100k == Long.MIN_VALUE) return "Не задано";
        return String.format(java.util.Locale.ROOT, "%.5f, %.5f",
                lat100k / 100000.0, lng100k / 100000.0);
    }
}
