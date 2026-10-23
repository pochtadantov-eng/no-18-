package org.telegram.pampgram.settings;

import java.util.List;

// Чаты section — maps to PampGramMessagesSettingsScreen.swift. Toggles persist
// to kv_prefs; the actual behavior hooks (anti-delete capture, visual edit UI,
// voice changer) are separate anchor patches that each later stage adds.
public final class PampGramMessagesScreen extends PampGramPreferenceFragment {

    public static final String KEY_ANTI_DELETE   = "chats.anti_delete";
    public static final String KEY_VISUAL_EDIT   = "chats.visual_edit";
    public static final String KEY_VOICE_CHANGER = "chats.voice_changer_messages";
    public static final String KEY_FROM_HIM_GIFTS = "chats.from_him_gifts";

    @Override
    protected String screenTitle() {
        return "Чаты";
    }

    @Override
    protected void onCreateRows(List<Row> out) {
        out.add(Row.header("Удалённые сообщения"));
        out.add(Row.toggle(KEY_ANTI_DELETE, false, "Сохранять удалённые"));
        out.add(Row.value("Исключения", "Выбор чатов", () -> showComingSoon("Исключения")));
        out.add(Row.info("Удалённые сообщения локально сохраняются и остаются видны в чате."));

        out.add(Row.header("Визуальный редактор"));
        out.add(Row.toggle(KEY_VISUAL_EDIT, false, "Изменить визуально"));
        out.add(Row.info("Локально меняет отображение текста ваших сообщений в чате. Собеседник видит оригинал."));

        out.add(Row.header("Голос"));
        out.add(Row.toggle(KEY_VOICE_CHANGER, false, "Изменять голос в сообщениях"));
        out.add(Row.value("Голосовой пресет", "По умолчанию", () -> showComingSoon("Голосовой пресет")));
        out.add(Row.info("Голос применяется к записываемым голосовым и видео-сообщениям."));

        out.add(Row.header("Подарки"));
        out.add(Row.toggle(KEY_FROM_HIM_GIFTS, false, "«От него» в коллекции"));
        out.add(Row.info("Разрешить добавлять визуальные подарки «от собеседника» в свою коллекцию."));
    }
}
