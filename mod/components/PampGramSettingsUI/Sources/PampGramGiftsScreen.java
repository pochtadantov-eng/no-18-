package org.telegram.pampgram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.pampgram.core.PampGramStorage;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// PampGram Gifts hub. First functional subscreen of the mod — ports what the
// iOS PampGram shows when a user opens the "Подарки" section:
//   * Three local-only balance tiles (Stars / TON / Rubles) that read and
//     write PampGramStorage; tap to edit, long-press clears to zero.
//   * Collection count row (how many visual gifts are currently saved to the
//     own profile) + "в разработке" placeholders for the receive-gift flow.
//   * History row that opens the ledger of local transactions.
//
// What this screen does NOT do yet (next stages):
//   * The "receive gift" compose flow (pick type/counterparty/price → create a
//     PhantomGift record).
//   * Collection grid with per-item actions (pin / hide / wear / transfer /
//     sell / list on market).
//   * Local market screen.
//   * Rendering phantom gifts in Telegram's own profile grid and chat.
public final class PampGramGiftsScreen extends BaseFragment {

    private static final int ROW_HEADER_BALANCES = 0;
    private static final int ROW_BALANCE_STARS   = 1;
    private static final int ROW_BALANCE_TON     = 2;
    private static final int ROW_BALANCE_RUB     = 3;
    private static final int ROW_HEADER_ACTIONS  = 4;
    private static final int ROW_RECEIVE_SELF    = 5;
    private static final int ROW_RECEIVE_PEER    = 6;
    private static final int ROW_PORTALS         = 7;
    private static final int ROW_MARKET          = 8;
    private static final int ROW_HISTORY         = 9;
    private static final int ROW_HEADER_COLLECTION = 10;
    private static final int ROW_COLLECTION_INFO = 11;
    private static final int ROW_FOOTER          = 12;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT   = 1;
    private static final int VIEW_TYPE_INFO   = 2;

    private PampGramStorage.PampGramPreferences prefs;
    private int collectionCount;
    private final List<Row> rows = new ArrayList<>();

    private RecyclerListView listView;
    private ListAdapter adapter;

    private static final class Row {
        final int viewType;
        final int id;
        final String title;
        final String subtitleOrValue;
        final int iconRes;
        final int iconBg;

        Row(int viewType, int id, String title, String subtitleOrValue, int iconRes, int iconBg) {
            this.viewType = viewType;
            this.id = id;
            this.title = title;
            this.subtitleOrValue = subtitleOrValue;
            this.iconRes = iconRes;
            this.iconBg = iconBg;
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Подарки");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        FrameLayout frame = new FrameLayout(context);
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frame;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter());
        DefaultItemAnimator anim = new DefaultItemAnimator();
        anim.setDurations(350);
        anim.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        listView.setItemAnimator(anim);
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= rows.size()) return;
            onRowClick(rows.get(position));
        });

        reload();
        return fragmentView;
    }

    @Override
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        // The compose screen pushes on top of this one and pops back when it's
        // done — we re-read balances and the collection count here so the UI
        // reflects whatever the user just created.
        if (fragmentView != null) {
            reload();
        }
    }

    private void reload() {
        PampGramStorage storage = PampGramStorage.getInstance();
        prefs = storage.getPreferences();
        collectionCount = storage.countPhantomGiftsForSelf(getUserConfig().getClientUserId());
        rebuildRows();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void rebuildRows() {
        rows.clear();
        rows.add(new Row(VIEW_TYPE_HEADER, ROW_HEADER_BALANCES, "Балансы", null, 0, 0));
        rows.add(new Row(VIEW_TYPE_TEXT, ROW_BALANCE_STARS, "Stars",
                formatStars(prefs.fakeStarsBalance),
                R.drawable.settings_stars, 0xFFE77512));
        rows.add(new Row(VIEW_TYPE_TEXT, ROW_BALANCE_TON, "TON",
                formatTon(prefs.fakeTonBalanceNanos),
                R.drawable.filled_gram_earnings, 0xFF1488E1));
        rows.add(new Row(VIEW_TYPE_TEXT, ROW_BALANCE_RUB, "Рубли",
                formatRubles(prefs.localRublesKopecks),
                R.drawable.settings_data, 0xFF4CAF50));

        rows.add(new Row(VIEW_TYPE_HEADER, ROW_HEADER_ACTIONS, "Действия", null, 0, 0));
        rows.add(new Row(VIEW_TYPE_TEXT, ROW_RECEIVE_SELF, "Подарок мне",
                "Добавить визуальный подарок в свою коллекцию",
                R.drawable.settings_gift, 0xFF8E44EC));
        rows.add(new Row(VIEW_TYPE_TEXT, ROW_RECEIVE_PEER, "Подарок ему",
                "Отправить визуальный подарок получателю",
                R.drawable.msg_customize, 0xFF8E44EC));
        rows.add(new Row(VIEW_TYPE_TEXT, ROW_PORTALS, "Portals",
                "Витрина уникальных подарков с ценой в TON",
                R.drawable.filled_gram_earnings, 0xFF1AC9FF));
        rows.add(new Row(VIEW_TYPE_TEXT, ROW_MARKET, "Маркет",
                "Локальная витрина выставленных подарков",
                R.drawable.msg_colors, 0xFFFF9500));
        rows.add(new Row(VIEW_TYPE_TEXT, ROW_HISTORY, "История операций",
                "Все покупки, продажи и переводы",
                R.drawable.msg_recent, 0xFF8E8E93));

        rows.add(new Row(VIEW_TYPE_HEADER, ROW_HEADER_COLLECTION, "Коллекция", null, 0, 0));
        String collSubtitle = collectionCount == 0
                ? "Пока пусто — тапните «Подарок мне», чтобы добавить."
                : collectionCount + " " + pluralGifts(collectionCount) + " в вашем профиле.";
        rows.add(new Row(VIEW_TYPE_INFO, ROW_COLLECTION_INFO, null, collSubtitle, 0, 0));
        rows.add(new Row(VIEW_TYPE_INFO, ROW_FOOTER, null,
                "Балансы и подарки хранятся локально, Telegram их не видит и не передаёт.",
                0, 0));
    }

    private void onRowClick(Row row) {
        switch (row.id) {
            case ROW_BALANCE_STARS:
                promptEditBalance("Stars", prefs.fakeStarsBalance, newVal -> {
                    PampGramStorage.getInstance().updatePreferences(p -> p.fakeStarsBalance = newVal);
                    PampGramStorage.getInstance().addHistoryEntry(
                            PampGramStorage.HISTORY_ADJUSTMENT, PampGramStorage.CURRENCY_STARS,
                            newVal - prefs.fakeStarsBalance, null, "Ручная коррекция через PampGram");
                    reload();
                });
                break;
            case ROW_BALANCE_TON:
                promptEditBalance("TON (в нано, 1 TON = 1 000 000 000)",
                        prefs.fakeTonBalanceNanos, newVal -> {
                    PampGramStorage.getInstance().updatePreferences(p -> p.fakeTonBalanceNanos = newVal);
                    PampGramStorage.getInstance().addHistoryEntry(
                            PampGramStorage.HISTORY_ADJUSTMENT, PampGramStorage.CURRENCY_TON,
                            newVal - prefs.fakeTonBalanceNanos, null, "Ручная коррекция через PampGram");
                    reload();
                });
                break;
            case ROW_BALANCE_RUB:
                promptEditBalance("Рубли (в копейках, 1 ₽ = 100)",
                        prefs.localRublesKopecks, newVal -> {
                    PampGramStorage.getInstance().updatePreferences(p -> p.localRublesKopecks = newVal);
                    PampGramStorage.getInstance().addHistoryEntry(
                            PampGramStorage.HISTORY_ADJUSTMENT, PampGramStorage.CURRENCY_RUB,
                            newVal - prefs.localRublesKopecks, null, "Ручная коррекция через PampGram");
                    reload();
                });
                break;
            case ROW_HISTORY:
                presentFragment(new PampGramLedgerScreen());
                break;
            case ROW_RECEIVE_SELF:
                presentFragment(new PampGramGiftComposeScreen(PampGramGiftComposeScreen.MODE_SELF));
                break;
            case ROW_RECEIVE_PEER:
                presentFragment(new PampGramGiftComposeScreen(PampGramGiftComposeScreen.MODE_PEER));
                break;
            case ROW_PORTALS:
                presentFragment(new PampGramPortalsScreen());
                break;
            case ROW_MARKET:
                showComingSoon(row.title);
                break;
            default:
                break;
        }
    }

    private void promptEditBalance(String label, long current, BalanceCallback cb) {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(label);

        EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setSelection(input.getText().length());
        input.setPadding(dp(24), dp(16), dp(24), dp(16));
        b.setView(input);

        b.setPositiveButton("Сохранить", (dialog, which) -> {
            try {
                long parsed = Long.parseLong(input.getText().toString().trim());
                cb.onChosen(Math.max(0, parsed));
            } catch (NumberFormatException ignore) {
                // user typed non-digits; keep old value
            }
        });
        b.setNegativeButton("Отмена", null);
        showDialog(b.create());
    }

    private interface BalanceCallback {
        void onChosen(long newValue);
    }

    private void showComingSoon(String name) {
        Drawable icon = getContext().getResources().getDrawable(R.drawable.msg_info).mutate();
        BulletinFactory.of(this).createSimpleBulletin(icon, name + " — в разработке").show();
    }

    // ----- Formatting ------------------------------------------------------

    private static final NumberFormat GROUPED;
    static {
        GROUPED = NumberFormat.getIntegerInstance(new Locale("ru"));
        GROUPED.setGroupingUsed(true);
    }

    static String formatStars(long stars) {
        return "⭐ " + GROUPED.format(stars);
    }

    static String formatTon(long nanos) {
        double ton = nanos / 1_000_000_000.0;
        return String.format(Locale.ROOT, "💎 %,.4f TON", ton).replace(',', ' ');
    }

    static String formatRubles(long kopecks) {
        return String.format(Locale.ROOT, "₽ %,.2f", kopecks / 100.0).replace(',', ' ');
    }

    private static String pluralGifts(int n) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 14) return "подарков";
        if (mod10 == 1) return "подарок";
        if (mod10 >= 2 && mod10 <= 4) return "подарка";
        return "подарков";
    }

    // ----- Icon helper (copy of hub's style) --------------------------------

    private static Drawable createRoundedIcon(Context ctx, int bgColor, int iconRes) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(10));
        bg.setColor(bgColor);
        Drawable icon = ctx.getResources().getDrawable(iconRes).mutate();
        icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        LayerDrawable layered = new LayerDrawable(new Drawable[]{bg, icon});
        int inset = dp(6);
        layered.setLayerInset(1, inset, inset, inset, inset);
        return layered;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context ctx = parent.getContext();
            View v;
            if (viewType == VIEW_TYPE_HEADER) {
                v = new HeaderCell(ctx);
            } else if (viewType == VIEW_TYPE_INFO) {
                v = new TextInfoPrivacyCell(ctx);
            } else {
                TextCell cell = new TextCell(ctx);
                cell.heightDp = 60;
                v = cell;
            }
            return new RecyclerListView.Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            if (row.viewType == VIEW_TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText(row.title);
            } else if (row.viewType == VIEW_TYPE_INFO) {
                ((TextInfoPrivacyCell) holder.itemView).setText(row.subtitleOrValue);
            } else {
                TextCell cell = (TextCell) holder.itemView;
                Drawable icon = createRoundedIcon(cell.getContext(), row.iconBg, row.iconRes);
                boolean divider = position + 1 < rows.size()
                        && rows.get(position + 1).viewType == VIEW_TYPE_TEXT;
                cell.setTextAndIcon(row.title, icon, divider);
                cell.setSubtitle(row.subtitleOrValue);
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).viewType;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == VIEW_TYPE_TEXT;
        }
    }
}
