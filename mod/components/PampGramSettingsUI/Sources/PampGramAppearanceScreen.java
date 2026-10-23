package org.telegram.pampgram.settings;

import org.telegram.pampgram.core.PampGramStorage;

import java.util.List;

// Внешний вид section — maps to PampGramAppearanceScreen.swift. Preset picker,
// badge/UI toggles and icon picker. The real icon switch on Android uses
// activity-aliases in the manifest; that wiring is a later stage.
public final class PampGramAppearanceScreen extends PampGramPreferenceFragment {

    public static final String KEY_PRESET            = "appearance.preset"; // "standard"|"glass"|"compact"
    public static final String KEY_REDUCE_ANIMATIONS = "appearance.reduce_animations";
    public static final String KEY_OLED              = "appearance.oled";
    public static final String KEY_COMPACT_HUB       = "appearance.compact_hub";
    public static final String KEY_MONOCHROME        = "appearance.monochrome";
    public static final String KEY_GLASS_CARDS       = "appearance.glass_cards";
    public static final String KEY_MINIMAL           = "appearance.minimal";
    public static final String KEY_CHAT_PREVIEW      = "appearance.chat_preview";
    public static final String KEY_CHAT_DATE         = "appearance.chat_date";
    public static final String KEY_SELECTED_ICON     = "appearance.selected_icon";

    @Override
    protected String screenTitle() {
        return "Внешний вид";
    }

    @Override
    protected void onCreateRows(List<Row> out) {
        PampGramStorage s = PampGramStorage.getInstance();
        String preset = s.getKvString(KEY_PRESET, "standard");
        String presetLabel = presetDisplayName(preset);

        out.add(Row.header("Пресет"));
        out.add(Row.value("Внешний вид", presetLabel, () -> showComingSoon("Выбор пресета")));
        out.add(Row.info("Пресеты задают стиль пузырей, фон и плотность. Отдельные опции ниже перекрывают пресет."));

        out.add(Row.header("Чат"));
        out.add(Row.value("Радиус пузыря",   "—", () -> showComingSoon("Радиус пузыря")));
        out.add(Row.value("Прозрачность пузыря", "—", () -> showComingSoon("Прозрачность пузыря")));
        out.add(Row.value("Размытие фона",   "—", () -> showComingSoon("Размытие фона")));
        out.add(Row.value("Плотность",       "—", () -> showComingSoon("Плотность чата")));
        out.add(Row.toggle(KEY_CHAT_PREVIEW, true,  "Превью сообщений"));
        out.add(Row.toggle(KEY_CHAT_DATE,    true,  "Дата над сообщениями"));
        out.add(Row.value("Размер аватарок", "—", () -> showComingSoon("Размер аватарок")));

        out.add(Row.header("Интерфейс"));
        out.add(Row.toggle(KEY_GLASS_CARDS,  false, "Стеклянные карточки"));
        out.add(Row.toggle(KEY_COMPACT_HUB,  false, "Компактный хаб PampGram"));
        out.add(Row.toggle(KEY_MONOCHROME,   false, "Монохромные иконки"));
        out.add(Row.toggle(KEY_MINIMAL,      false, "Минимализм"));
        out.add(Row.toggle(KEY_OLED,         false, "OLED-чёрный"));
        out.add(Row.value("Размытие профиля","—", () -> showComingSoon("Размытие профиля")));
        out.add(Row.toggle(KEY_REDUCE_ANIMATIONS, false, "Уменьшить анимации"));

        out.add(Row.header("Иконка приложения"));
        out.add(Row.value("Выбрать иконку", "По умолчанию", () -> showComingSoon("Выбор иконки")));
        out.add(Row.info("Смена иконки на Android делается через activity-alias — функция появится в следующей сборке."));

        out.add(Row.action("Сбросить внешний вид", () -> {
            PampGramStorage st = PampGramStorage.getInstance();
            st.setKvString(KEY_PRESET, null);
            st.setKvString(KEY_REDUCE_ANIMATIONS, null);
            st.setKvString(KEY_OLED, null);
            st.setKvString(KEY_COMPACT_HUB, null);
            st.setKvString(KEY_MONOCHROME, null);
            st.setKvString(KEY_GLASS_CARDS, null);
            st.setKvString(KEY_MINIMAL, null);
            st.setKvString(KEY_CHAT_PREVIEW, null);
            st.setKvString(KEY_CHAT_DATE, null);
            st.setKvString(KEY_SELECTED_ICON, null);
            rebuildRows();
        }));
        out.add(Row.info("Сброс возвращает стандартный пресет и значения по умолчанию."));
    }

    private static String presetDisplayName(String preset) {
        if (preset == null) return "Standard";
        switch (preset) {
            case "glass":   return "Glass";
            case "compact": return "Compact";
            default:         return "Standard";
        }
    }
}
