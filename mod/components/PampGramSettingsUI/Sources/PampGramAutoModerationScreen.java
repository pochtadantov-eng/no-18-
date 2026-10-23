package org.telegram.pampgram.settings;

import org.telegram.pampgram.core.PampGramStorage;

import java.util.List;

// Автомодерация section — toggles for the on-device content filter.
// Implementation of the filter itself (TFLite classifier or hook into
// Telegram's own media download pipeline) is a later stage; this screen
// controls what the filter WOULD do once wired.
public final class PampGramAutoModerationScreen extends PampGramPreferenceFragment {

    public static final String KEY_ENABLED       = "automod.enabled";
    public static final String KEY_FILTER_18      = "automod.filter_18plus";
    public static final String KEY_FILTER_VIOLENCE = "automod.filter_violence";
    public static final String KEY_FILTER_SHOCK   = "automod.filter_shock";
    public static final String KEY_BLUR_INSTEAD  = "automod.blur_instead";
    public static final String KEY_THRESHOLD      = "automod.threshold"; // 1..100

    @Override
    protected String screenTitle() {
        return "Автомодерация";
    }

    @Override
    protected void onCreateRows(List<Row> out) {
        PampGramStorage s = PampGramStorage.getInstance();
        long threshold = s.getKvLong(KEY_THRESHOLD, 70);

        out.add(Row.header("Включение"));
        out.add(Row.toggle(KEY_ENABLED, false, "Автомодерация"));
        out.add(Row.info("ИИ-модель прогоняет входящие изображения и видео на устройстве. Ничего не отправляется наружу."));

        out.add(Row.header("Что фильтровать"));
        out.add(Row.toggle(KEY_FILTER_18,       true, "18+ контент"));
        out.add(Row.toggle(KEY_FILTER_VIOLENCE, true, "Жестокость"));
        out.add(Row.toggle(KEY_FILTER_SHOCK,    false, "Шок-контент"));

        out.add(Row.header("Как реагировать"));
        out.add(Row.toggle(KEY_BLUR_INSTEAD, true, "Размывать вместо скрытия"));
        out.add(Row.value("Порог срабатывания", threshold + "%",
                () -> showComingSoon("Порог срабатывания")));
        out.add(Row.info("При включённом размытии контент не удаляется — закрывается плёнкой с кнопкой «показать»."));

        out.add(Row.header("Диагностика"));
        out.add(Row.action("Журнал срабатываний", () -> showComingSoon("Журнал")));
    }
}
