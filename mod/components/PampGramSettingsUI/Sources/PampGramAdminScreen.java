package org.telegram.pampgram.settings;

import java.util.List;

// Админ-панель section — maps to PampGramAdminScreen.swift. On iOS this
// shows subscription status pulled from a server; on Android we display the
// local admin toggles and leave the server-backed subscription view as a
// placeholder until the pampgram-subs-worker is wired up (same server as iOS).
public final class PampGramAdminScreen extends PampGramPreferenceFragment {

    public static final String KEY_ADMIN_ENABLED      = "admin.enabled";
    public static final String KEY_ADMIN_AUTO_RENEW   = "admin.auto_renew";
    public static final String KEY_ADMIN_NOTIFICATIONS = "admin.notifications";

    @Override
    protected String screenTitle() {
        return "Админ-панель";
    }

    @Override
    protected void onCreateRows(List<Row> out) {
        out.add(Row.header("Подписка"));
        out.add(Row.value("Статус", "Проверка…", () -> showComingSoon("Статус подписки")));
        out.add(Row.value("Срок действия", "—", () -> showComingSoon("Срок действия")));
        out.add(Row.value("План", "Стандартный", () -> showComingSoon("Выбор плана")));
        out.add(Row.info("Данные подписки подтягиваются с pampgram-subs-worker. На этой сборке подключение к серверу ещё не настроено."));

        out.add(Row.header("Настройки"));
        out.add(Row.toggle(KEY_ADMIN_ENABLED, true, "Админ-панель включена"));
        out.add(Row.toggle(KEY_ADMIN_AUTO_RENEW, false, "Автопродление"));
        out.add(Row.toggle(KEY_ADMIN_NOTIFICATIONS, true, "Уведомления о подписке"));
        out.add(Row.info("Уведомления приходят только локально — не выходят за пределы устройства."));

        out.add(Row.header("Управление"));
        out.add(Row.action("Активировать ключ", () -> showComingSoon("Активация ключа")));
        out.add(Row.action("Проверить подписку", () -> showComingSoon("Проверка подписки")));
        out.add(Row.action("Выйти", () -> showComingSoon("Выход")));
    }
}
