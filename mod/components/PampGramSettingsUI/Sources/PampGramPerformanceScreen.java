package org.telegram.pampgram.settings;

import java.util.List;

// Плавность и заряд section — perf/battery toggles that would be read by
// Telegram-Android's own LiteMode-adjacent code paths. The actual read sites
// are per-feature anchor patches in a later stage.
public final class PampGramPerformanceScreen extends PampGramPreferenceFragment {

    public static final String KEY_LITE_ANIMATIONS = "perf.lite_animations";
    public static final String KEY_LITE_STICKERS   = "perf.lite_stickers";
    public static final String KEY_LITE_AVATARS    = "perf.lite_avatars";
    public static final String KEY_LITE_CHAT_BG    = "perf.lite_chat_bg";
    public static final String KEY_LITE_BLUR       = "perf.lite_blur";
    public static final String KEY_LOW_POWER       = "perf.low_power";
    public static final String KEY_PRELOAD_OFF     = "perf.disable_media_preload";

    @Override
    protected String screenTitle() {
        return "Плавность и заряд";
    }

    @Override
    protected void onCreateRows(List<Row> out) {
        out.add(Row.header("Экономия заряда"));
        out.add(Row.toggle(KEY_LOW_POWER,     false, "Режим энергосбережения"));
        out.add(Row.toggle(KEY_PRELOAD_OFF,   false, "Не подгружать медиа заранее"));
        out.add(Row.info("В режиме энергосбережения эффекты, анимации и фоновые задачи отключаются вместе."));

        out.add(Row.header("Плавность"));
        out.add(Row.toggle(KEY_LITE_ANIMATIONS, false, "Упростить анимации"));
        out.add(Row.toggle(KEY_LITE_STICKERS,   false, "Статичные стикеры и эмодзи"));
        out.add(Row.toggle(KEY_LITE_AVATARS,    false, "Статичные аватарки"));
        out.add(Row.toggle(KEY_LITE_CHAT_BG,    false, "Статичный фон чата"));
        out.add(Row.toggle(KEY_LITE_BLUR,       false, "Отключить размытие"));
        out.add(Row.info("Опции аналогичны штатному Power Saving Mode Telegram, но управляются отдельно от него и остаются включёнными при любом уровне заряда."));
    }
}
