package org.telegram.pampgram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.widget.EditText;

import org.telegram.messenger.R;
import org.telegram.pampgram.core.PampGramStorage;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;

import java.util.List;

// Compose form for "Подарок мне" / "Подарок ему". A thin BaseFragment built
// on top of PampGramPreferenceFragment: each form field is a value row that
// opens an inline AlertDialog to edit. On submit the gift is written to the
// phantom_gift table; for the peer-mode the balance is debited and a matching
// history entry is logged, mirroring the iOS PampGramPhantomGiftManager flow.
//
// The peer id for "Подарок ему" is derived from a hash of the display name
// the user types. A real contact/chat picker (ContactsActivity in Telegram-
// Android) is a later stage — the hash gives us a stable per-name peer id so
// multiple gifts to the same name land on the same visual peer.
public final class PampGramGiftComposeScreen extends PampGramPreferenceFragment {

    public static final int MODE_SELF = 0;
    public static final int MODE_PEER = 1;

    private final int mode;

    private String giftTitle = "Подарок";
    private String currency = PampGramStorage.CURRENCY_STARS;
    private long priceAmount = 100;
    private String peerName = "";

    public PampGramGiftComposeScreen(int mode) {
        this.mode = mode;
    }

    @Override
    protected String screenTitle() {
        return mode == MODE_SELF ? "Подарок мне" : "Подарок ему";
    }

    @Override
    protected void onCreateRows(List<Row> out) {
        out.add(Row.header("Детали подарка"));
        out.add(Row.value("Название", giftTitle, this::editTitle));
        out.add(Row.value("Валюта", currencyDisplay(currency), this::pickCurrency));
        out.add(Row.value("Цена", formatPrice(currency, priceAmount), this::editPrice));
        if (mode == MODE_PEER) {
            out.add(Row.value("Получатель",
                    peerName.isEmpty() ? "Введите имя или @username" : peerName,
                    this::editPeer));
            out.add(Row.info("Пока список контактов Telegram не подключён. Введите любое имя — подарок привяжется к нему локально."));
        } else {
            out.add(Row.info("Подарок добавится в вашу коллекцию. Баланс не изменится — это «подарок себе»."));
        }

        out.add(Row.action("Создать подарок", this::submit));
    }

    private void editTitle() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Название подарка");
        EditText input = new EditText(getParentActivity());
        input.setText(giftTitle);
        input.setSelection(input.getText().length());
        input.setPadding(dp(24), dp(16), dp(24), dp(16));
        b.setView(input);
        b.setPositiveButton("Сохранить", (dlg, which) -> {
            String v = input.getText().toString().trim();
            if (!v.isEmpty()) {
                giftTitle = v;
                rebuildRows();
            }
        });
        b.setNegativeButton("Отмена", null);
        showDialog(b.create());
    }

    private void editPrice() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(priceLabelFor(currency));
        EditText input = new EditText(getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(priceAmount));
        input.setSelection(input.getText().length());
        input.setPadding(dp(24), dp(16), dp(24), dp(16));
        b.setView(input);
        b.setPositiveButton("Сохранить", (dlg, which) -> {
            try {
                long parsed = Long.parseLong(input.getText().toString().trim());
                priceAmount = Math.max(0, parsed);
                rebuildRows();
            } catch (NumberFormatException ignore) {}
        });
        b.setNegativeButton("Отмена", null);
        showDialog(b.create());
    }

    private void editPeer() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Получатель");
        EditText input = new EditText(getParentActivity());
        input.setText(peerName);
        input.setSelection(input.getText().length());
        input.setPadding(dp(24), dp(16), dp(24), dp(16));
        b.setView(input);
        b.setPositiveButton("Сохранить", (dlg, which) -> {
            peerName = input.getText().toString().trim();
            rebuildRows();
        });
        b.setNegativeButton("Отмена", null);
        showDialog(b.create());
    }

    private void pickCurrency() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Валюта");
        CharSequence[] items = new CharSequence[]{
                "⭐ Stars",
                "💎 TON (нано)",
                "₽ Рубли (копейки)"
        };
        b.setItems(items, (dlg, which) -> {
            if (which == 0) currency = PampGramStorage.CURRENCY_STARS;
            else if (which == 1) currency = PampGramStorage.CURRENCY_TON;
            else if (which == 2) currency = PampGramStorage.CURRENCY_RUB;
            rebuildRows();
        });
        showDialog(b.create());
    }

    private void submit() {
        if (giftTitle.isEmpty()) {
            flash("Укажите название подарка");
            return;
        }
        if (priceAmount < 0) {
            flash("Цена не может быть отрицательной");
            return;
        }
        PampGramStorage s = PampGramStorage.getInstance();
        long targetPeerId;
        String counterpartyLabel;
        if (mode == MODE_SELF) {
            targetPeerId = getUserConfig().getClientUserId();
            counterpartyLabel = "Себе";
        } else {
            if (peerName.isEmpty()) {
                flash("Выберите получателя");
                return;
            }
            // Stable, name-derived pseudo peer id — real Telegram peer picking
            // comes in a later stage.
            targetPeerId = Math.abs((long) peerName.hashCode());
            counterpartyLabel = peerName;
        }

        if (mode == MODE_PEER && priceAmount > 0) {
            if (!canCharge(s, currency, priceAmount)) {
                flash("Недостаточно средств на балансе " + currencyShortLabel(currency));
                return;
            }
            s.updatePreferences(p -> {
                if (PampGramStorage.CURRENCY_STARS.equals(currency))      p.fakeStarsBalance    -= priceAmount;
                else if (PampGramStorage.CURRENCY_TON.equals(currency))   p.fakeTonBalanceNanos -= priceAmount;
                else if (PampGramStorage.CURRENCY_RUB.equals(currency))   p.localRublesKopecks  -= priceAmount;
            });
            s.addHistoryEntry(PampGramStorage.HISTORY_PURCHASE, currency, -priceAmount,
                    counterpartyLabel, "Подарок «" + giftTitle + "»");
        } else if (mode == MODE_SELF && priceAmount > 0) {
            // No debit for a self-received gift; log it as an incoming transfer
            // so the ledger shows where visual gifts came from.
            s.addHistoryEntry(PampGramStorage.HISTORY_TRANSFER_IN, currency, priceAmount,
                    "Себе", "Подарок «" + giftTitle + "»");
        }

        s.addPhantomGift(targetPeerId, giftTitle, null, "generic", null,
                currency, priceAmount, mode == MODE_SELF);

        Drawable ok = getContext().getResources().getDrawable(R.drawable.msg_info).mutate();
        BulletinFactory.of(this)
                .createSimpleBulletin(ok, "Подарок «" + giftTitle + "» создан")
                .show();
        finishFragment();
    }

    private static boolean canCharge(PampGramStorage s, String currency, long amount) {
        PampGramStorage.PampGramPreferences p = s.getPreferences();
        if (PampGramStorage.CURRENCY_STARS.equals(currency))    return p.fakeStarsBalance    >= amount;
        if (PampGramStorage.CURRENCY_TON.equals(currency))      return p.fakeTonBalanceNanos >= amount;
        if (PampGramStorage.CURRENCY_RUB.equals(currency))      return p.localRublesKopecks  >= amount;
        return false;
    }

    private void flash(String text) {
        Drawable icon = getContext().getResources().getDrawable(R.drawable.msg_info).mutate();
        BulletinFactory.of(this).createSimpleBulletin(icon, text).show();
    }

    // ----- Labels ----------------------------------------------------------

    private static String currencyDisplay(String currency) {
        if (PampGramStorage.CURRENCY_STARS.equals(currency)) return "⭐ Stars";
        if (PampGramStorage.CURRENCY_TON.equals(currency))   return "💎 TON";
        if (PampGramStorage.CURRENCY_RUB.equals(currency))   return "₽ Рубли";
        return currency;
    }

    private static String currencyShortLabel(String currency) {
        if (PampGramStorage.CURRENCY_STARS.equals(currency)) return "Stars";
        if (PampGramStorage.CURRENCY_TON.equals(currency))   return "TON";
        if (PampGramStorage.CURRENCY_RUB.equals(currency))   return "Рубли";
        return currency;
    }

    private static String priceLabelFor(String currency) {
        if (PampGramStorage.CURRENCY_TON.equals(currency))   return "Цена в TON-нано (1 TON = 1 000 000 000)";
        if (PampGramStorage.CURRENCY_RUB.equals(currency))   return "Цена в копейках (1 ₽ = 100)";
        return "Цена в Stars";
    }

    private static String formatPrice(String currency, long amount) {
        if (PampGramStorage.CURRENCY_STARS.equals(currency)) return PampGramGiftsScreen.formatStars(amount);
        if (PampGramStorage.CURRENCY_TON.equals(currency))   return PampGramGiftsScreen.formatTon(amount);
        if (PampGramStorage.CURRENCY_RUB.equals(currency))   return PampGramGiftsScreen.formatRubles(amount);
        return String.valueOf(amount);
    }
}
