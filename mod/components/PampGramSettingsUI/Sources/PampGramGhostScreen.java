package org.telegram.pampgram.settings;

import androidx.annotation.NonNull;

import java.util.List;

// Ghost section — maps to PampGramGhostScreen.swift. Master toggle gates
// everything else; the feature toggles persist independently so turning the
// master back on restores the previous choices.
public final class PampGramGhostScreen extends PampGramPreferenceFragment {

    public static final String KEY_MASTER                = "ghost.mode_enabled";
    public static final String KEY_HIDE_READ_RECEIPTS    = "ghost.hide_read_receipts";
    public static final String KEY_HIDE_STORY_VIEWS      = "ghost.hide_story_views";
    public static final String KEY_HIDE_ONLINE           = "ghost.hide_online";
    public static final String KEY_HIDE_TYPING           = "ghost.hide_typing";
    public static final String KEY_AUTO_OFFLINE          = "ghost.auto_offline";
    public static final String KEY_READ_ON_ACTION        = "ghost.read_on_action";
    public static final String KEY_EXCLUDE_ALL_CHANNELS  = "ghost.exclude_all_channels";
    public static final String KEY_EXCLUDE_ALL_GROUPS    = "ghost.exclude_all_groups";

    @Override
    protected String screenTitle() {
        return "Ghost";
    }

    @Override
    protected void onCreateRows(List<Row> out) {
        out.add(Row.header("Режим"));
        out.add(Row.toggle(KEY_MASTER, false, "Режим призрака"));
        out.add(Row.info("Главный переключатель. Пока он выключен, остальные опции сохраняются, но не действуют."));

        out.add(Row.header("Что скрыть"));
        out.add(Row.toggle(KEY_HIDE_READ_RECEIPTS, true,  "Не читать сообщения"));
        out.add(Row.toggle(KEY_HIDE_STORY_VIEWS,   true,  "Не читать истории"));
        out.add(Row.toggle(KEY_HIDE_ONLINE,        true,  "Не отправлять «онлайн»"));
        out.add(Row.toggle(KEY_HIDE_TYPING,        true,  "Не отправлять «печатает»"));
        out.add(Row.toggle(KEY_AUTO_OFFLINE,       false, "Автоматический «офлайн»"));
        out.add(Row.toggle(KEY_READ_ON_ACTION,     false, "Читать при действиях"));
        out.add(Row.info("«Читать при действиях» принудительно отправляет прочтение, когда вы отвечаете или ставите реакцию, даже если прочтения в целом скрыты."));

        out.add(Row.header("Исключения"));
        out.add(Row.toggle(KEY_EXCLUDE_ALL_CHANNELS, false, "Все каналы"));
        out.add(Row.toggle(KEY_EXCLUDE_ALL_GROUPS,   false, "Все группы"));
        out.add(Row.value("Папки",  "0", () -> showComingSoon("Папки-исключения")));
        out.add(Row.value("Чаты",   "0", () -> showComingSoon("Чаты-исключения")));
        out.add(Row.info("В исключениях Ghost ведёт себя как обычный клиент: читает, показывает онлайн и печатает."));
    }

    @Override
    protected void onToggleChanged(@NonNull String key, boolean newValue) {
        if (KEY_MASTER.equals(key)) {
            // The master toggle doesn't visibly change children, but future
            // behavior hooks will read master && feature, so a UI redraw isn't
            // needed yet.
        }
    }
}
