package org.telegram.pampgram.settings;

import java.util.List;

// Запись звонков section — toggles plus a stub list for recordings.
// Actual recording on Android requires AccessibilityService or Audio
// APIs, which is OS-permission-sensitive; those hooks come in a later
// stage. This screen just owns the preferences for them.
public final class PampGramCallRecordScreen extends PampGramPreferenceFragment {

    public static final String KEY_ENABLED        = "callrec.enabled";
    public static final String KEY_RECORD_SELF    = "callrec.record_self";
    public static final String KEY_RECORD_PEER    = "callrec.record_peer";
    public static final String KEY_NOTIFY_PEER    = "callrec.notify_peer"; // off by default — legal gray area
    public static final String KEY_AUTO_START     = "callrec.auto_start";

    @Override
    protected String screenTitle() {
        return "Запись звонков";
    }

    @Override
    protected void onCreateRows(List<Row> out) {
        out.add(Row.header("Включение"));
        out.add(Row.toggle(KEY_ENABLED, false, "Запись звонков"));
        out.add(Row.toggle(KEY_AUTO_START, false, "Записывать все звонки"));
        out.add(Row.info("При включённом «записывать все» запись начинается автоматически при подключении."));

        out.add(Row.header("Что записывать"));
        out.add(Row.toggle(KEY_RECORD_SELF, true,  "Свой голос"));
        out.add(Row.toggle(KEY_RECORD_PEER, true,  "Голос собеседника"));
        out.add(Row.info("На Android запись звонков требует отдельного разрешения. При первом включении система покажет диалог доступа к микрофону."));

        out.add(Row.header("Уведомление"));
        out.add(Row.toggle(KEY_NOTIFY_PEER, false, "Сообщать собеседнику о записи"));
        out.add(Row.info("В ряде стран запись без уведомления собеседника незаконна. Проверьте местное законодательство."));

        out.add(Row.header("Записи"));
        out.add(Row.action("Открыть список записей", () -> showComingSoon("Список записей")));
        out.add(Row.action("Очистить все записи",    () -> showComingSoon("Очистка записей")));
    }
}
