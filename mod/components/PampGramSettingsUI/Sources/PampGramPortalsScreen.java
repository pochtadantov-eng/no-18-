package org.telegram.pampgram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.R;
import org.telegram.pampgram.core.PampGramStorage;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

import java.util.List;
import java.util.Locale;

// Portals Market — visual-only mini-market inside PampGram. Rebuilt to
// match the real portals-market mini-app layout: a Market header with a
// stars-balance pill on the left and a GRAM-balance pill on the right, a
// Halloween event banner with a live-updating countdown, four big section
// tiles (Все / Коллекции / Звезды / Крафт), a filter chip strip, and a
// vertical list of items. Taps on an item row open
// PampGramPortalsDetailScreen for the full-price buy flow; taps on the
// GRAM pill open the balance editor so a user can top up without leaving
// Portals.
//
// Wiring is done entirely in code (no new XML, no new drawables, no new
// strings in strings.xml) so the mod stays a diff that only adds files
// under org.telegram.pampgram.
public final class PampGramPortalsScreen extends BaseFragment {

    // Portals dark palette. These stay fixed across Telegram themes — the
    // real Portals is always dark, so a Market opened from a light theme
    // keeps the same look.
    private static final int SURFACE          = 0xFF0B0B0F;
    private static final int SURFACE_ELEVATED = 0xFF14141A;
    private static final int SURFACE_CHIP     = 0xFF1E1E26;
    private static final int ACCENT_PRIMARY   = 0xFF1AC9FF; // GRAM blue
    private static final int ACCENT_STARS     = 0xFFFFB400; // amber ✦
    private static final int ACCENT_SUCCESS   = 0xFF34C759;

    private String selectedCollection; // null = "Все"
    private PampGramStorage.PampGramPreferences prefs;

    // Views held as fields because we repaint them from several places:
    // reload() rebuilds the item list, the collection picker, the balance
    // pill updates, and the live countdown ticker.
    private TextView starsPill;
    private TextView gramPill;
    private LinearLayout itemsColumn;
    private TextView collectionChip;
    private TextView filterCountChip;
    private View filterClearChip;

    // Halloween countdown. End time is captured once per fragment create so
    // the ticker counts smoothly during the session; it resets if the user
    // reopens Portals, which is close enough to the real mini-app's feel.
    private long eventEndMs;
    private TextView countdownView;
    private Runnable countdownTick;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Portals Market");
        actionBar.setBackgroundColor(SURFACE);
        actionBar.setItemsColor(0xFFFFFFFF, false);
        actionBar.setItemsBackgroundColor(0x33FFFFFF, false);
        actionBar.setTitleColor(0xFFFFFFFF);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(SURFACE);
        fragmentView = root;

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 0, dp(24));
        scroll.addView(col, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        col.addView(buildBalancePills(context), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        col.addView(buildEventBanner(context), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 110, 0, 16, 12, 16, 16));
        col.addView(buildSectionTiles(context), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 12, 4));
        col.addView(buildFilterChips(context), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 4, 8));

        itemsColumn = new LinearLayout(context);
        itemsColumn.setOrientation(LinearLayout.VERTICAL);
        itemsColumn.setPadding(dp(12), dp(4), dp(12), dp(4));
        col.addView(itemsColumn, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        reload();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Detail screen pops back here on a successful buy — pick up the
        // new balance so both pills refresh.
        if (fragmentView != null) {
            reload();
        }
        startCountdown();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopCountdown();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        stopCountdown();
    }

    private void reload() {
        prefs = PampGramStorage.getInstance().getPreferences();
        updateBalancePills();
        rebuildItemsColumn();
        updateFilterChipState();
    }

    // ---- Top balance row --------------------------------------------------

    private View buildBalancePills(Context ctx) {
        FrameLayout wrap = new FrameLayout(ctx);
        wrap.setPadding(dp(16), dp(10), dp(16), dp(2));

        starsPill = new TextView(ctx);
        starsPill.setTextSize(14);
        starsPill.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        starsPill.setTextColor(0xFFFFFFFF);
        starsPill.setPadding(dp(12), dp(7), dp(14), dp(7));
        GradientDrawable sb = new GradientDrawable();
        sb.setShape(GradientDrawable.RECTANGLE);
        sb.setCornerRadius(dp(18));
        sb.setColor(0xFF2A1E0A);
        sb.setStroke(dp(1), 0x66FFB400);
        starsPill.setBackground(sb);
        starsPill.setOnClickListener(v -> promptEditBalance(
                "Звёзды PampGram",
                prefs.fakeStarsBalance,
                newVal -> {
                    PampGramStorage.getInstance().updatePreferences(p -> p.fakeStarsBalance = newVal);
                    reload();
                }));
        wrap.addView(starsPill, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL));

        gramPill = new TextView(ctx);
        gramPill.setTextSize(14);
        gramPill.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        gramPill.setTextColor(0xFFFFFFFF);
        gramPill.setPadding(dp(12), dp(7), dp(14), dp(7));
        GradientDrawable gb = new GradientDrawable();
        gb.setShape(GradientDrawable.RECTANGLE);
        gb.setCornerRadius(dp(18));
        gb.setColor(0xFF08263A);
        gb.setStroke(dp(1), 0x661AC9FF);
        gramPill.setBackground(gb);
        gramPill.setOnClickListener(v -> promptEditBalance(
                "GRAM (в нано, 1 GRAM = 1 000 000 000)",
                prefs.fakeTonBalanceNanos,
                newVal -> {
                    PampGramStorage.getInstance().updatePreferences(p -> p.fakeTonBalanceNanos = newVal);
                    reload();
                }));
        wrap.addView(gramPill, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL));

        return wrap;
    }

    private void updateBalancePills() {
        starsPill.setText("✨ " + PampGramPortalsCatalog.formatCompactSupply((int)
                Math.min(Integer.MAX_VALUE, prefs.fakeStarsBalance)));
        gramPill.setText("💎 " + PampGramPortalsCatalog.formatGram(prefs.fakeTonBalanceNanos) + " GRAM");
    }

    // ---- Event banner (Halloween, with live countdown) --------------------

    private View buildEventBanner(Context ctx) {
        if (eventEndMs == 0L) {
            // 7 days from first open — matches the real Halloween tournament tile.
            eventEndMs = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000L;
        }

        FrameLayout banner = new FrameLayout(ctx);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF4A1E62, 0xFF2A0E3A, 0xFF1A0620});
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0x33FFFFFF);
        banner.setBackground(bg);
        banner.setPadding(dp(18), dp(14), dp(18), dp(14));

        LinearLayout left = new LinearLayout(ctx);
        left.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(ctx);
        title.setText("Halloween турнир");
        title.setTextSize(20);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setTextColor(0xFFFFFFFF);
        left.addView(title);

        countdownView = new TextView(ctx);
        countdownView.setTextSize(13);
        countdownView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        countdownView.setTextColor(0xFFFFFFFF);
        countdownView.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable cdBg = new GradientDrawable();
        cdBg.setShape(GradientDrawable.RECTANGLE);
        cdBg.setCornerRadius(dp(12));
        cdBg.setColor(0x44000000);
        cdBg.setStroke(dp(1), 0x33FFFFFF);
        countdownView.setBackground(cdBg);
        LinearLayout.LayoutParams cdLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cdLp.topMargin = dp(10);
        left.addView(countdownView, cdLp);
        banner.addView(left, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL));

        // Pumpkin trio on the right for flavour.
        TextView pumpkins = new TextView(ctx);
        pumpkins.setText("🎃🎃🎃");
        pumpkins.setTextSize(40);
        banner.addView(pumpkins, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL));

        updateCountdownLabel();
        return banner;
    }

    private void startCountdown() {
        if (countdownView == null || countdownTick != null) return;
        countdownTick = new Runnable() {
            @Override public void run() {
                updateCountdownLabel();
                if (countdownView != null) {
                    countdownView.postDelayed(this, 1000);
                }
            }
        };
        countdownView.postDelayed(countdownTick, 1000);
    }

    private void stopCountdown() {
        if (countdownView != null && countdownTick != null) {
            countdownView.removeCallbacks(countdownTick);
        }
        countdownTick = null;
    }

    private void updateCountdownLabel() {
        if (countdownView == null) return;
        long left = Math.max(0L, eventEndMs - System.currentTimeMillis());
        long days = left / (24 * 60 * 60 * 1000L);
        long hours = (left / (60 * 60 * 1000L)) % 24;
        long mins = (left / (60 * 1000L)) % 60;
        long secs = (left / 1000L) % 60;
        countdownView.setText(String.format(Locale.ROOT,
                "%d:%02d:%02d:%02d", days, hours, mins, secs));
    }

    // ---- Four big section tiles row ---------------------------------------

    private View buildSectionTiles(Context ctx) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), 0, dp(16), 0);
        row.setGravity(Gravity.CENTER);
        row.addView(makeTile(ctx, "🎁", "Все",      true,  v -> {
            selectedCollection = null;
            reload();
        }), tileLp());
        row.addView(makeTile(ctx, "👤", "Коллекции", false, v -> openCollectionPicker()), tileLp());
        row.addView(makeTile(ctx, "⭐",       "Звезды",    false, v -> showComingSoon("Звёзды")), tileLp());
        row.addView(makeTile(ctx, "⛏️", "Крафт",     false, v -> showComingSoon("Крафт")), tileLp());
        return row;
    }

    private LinearLayout.LayoutParams tileLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        return lp;
    }

    private View makeTile(Context ctx, String emoji, String label, boolean active,
                          View.OnClickListener onClick) {
        LinearLayout tile = new LinearLayout(ctx);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(0, 0, 0, dp(4));

        TextView iconHolder = new TextView(ctx);
        iconHolder.setText(emoji);
        iconHolder.setTextSize(30);
        iconHolder.setGravity(Gravity.CENTER);
        iconHolder.setMinimumWidth(dp(70));
        iconHolder.setMinimumHeight(dp(70));
        iconHolder.setPadding(0, dp(10), 0, dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(22));
        bg.setColor(active ? 0xFF1E1E26 : 0xFF13131A);
        bg.setStroke(dp(1), active ? 0x55FFFFFF : 0x1AFFFFFF);
        iconHolder.setBackground(bg);
        if (!active) {
            iconHolder.setAlpha(0.55f);
        }
        iconHolder.setOnClickListener(onClick);
        tile.addView(iconHolder, new LinearLayout.LayoutParams(dp(70), dp(70)));

        TextView name = new TextView(ctx);
        name.setText(label);
        name.setTextSize(13);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setTextColor(active ? 0xFFFFFFFF : 0x99FFFFFF);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nameLp.topMargin = dp(6);
        tile.addView(name, nameLp);

        return tile;
    }

    // ---- Filter chip strip ------------------------------------------------

    private View buildFilterChips(Context ctx) {
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(16), 0, dp(16), 0);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        filterCountChip = makeIconChip(ctx, "⚙", v -> showComingSoon("Фильтры"));
        bar.addView(filterCountChip, chipLp(dp(8)));

        bar.addView(makeIconChip(ctx, "⇅", v -> showComingSoon("Сортировка")), chipLp(dp(8)));

        View divider = new View(ctx);
        divider.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(dp(1), dp(22));
        divLp.setMargins(dp(2), 0, dp(10), 0);
        bar.addView(divider, divLp);

        filterClearChip = makeIconChip(ctx, "✕", v -> {
            selectedCollection = null;
            reload();
        });
        filterClearChip.setVisibility(View.GONE);
        bar.addView(filterClearChip, chipLp(dp(8)));

        collectionChip = makeDropChip(ctx, "Коллекция", v -> openCollectionPicker());
        bar.addView(collectionChip, chipLp(dp(8)));

        TextView modelChip = makeDropChip(ctx, "Модель", v -> showComingSoon("Фильтр модели"));
        bar.addView(modelChip, chipLp(0));

        return bar;
    }

    private LinearLayout.LayoutParams chipLp(int rightMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = rightMargin;
        return lp;
    }

    private View makeIconChip(Context ctx, String glyph, View.OnClickListener onClick) {
        TextView t = new TextView(ctx);
        t.setText(glyph);
        t.setTextSize(16);
        t.setGravity(Gravity.CENTER);
        t.setMinimumWidth(dp(40));
        t.setMinimumHeight(dp(34));
        t.setTextColor(0xFFFFFFFF);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(17));
        bg.setColor(SURFACE_CHIP);
        t.setBackground(bg);
        t.setOnClickListener(onClick);
        return t;
    }

    private TextView makeDropChip(Context ctx, String label, View.OnClickListener onClick) {
        TextView t = new TextView(ctx);
        t.setText(label + "  ▾");
        t.setTextSize(13);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setTextColor(0xFFFFFFFF);
        t.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(17));
        bg.setColor(SURFACE_CHIP);
        t.setBackground(bg);
        t.setOnClickListener(onClick);
        return t;
    }

    private void updateFilterChipState() {
        if (collectionChip != null) {
            if (selectedCollection == null) {
                collectionChip.setText("Коллекция  ▾");
            } else {
                collectionChip.setText("Коллекция 1  ▾");
            }
        }
        if (filterClearChip != null) {
            filterClearChip.setVisibility(selectedCollection == null ? View.GONE : View.VISIBLE);
        }
    }

    private void openCollectionPicker() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        List<PampGramPortalsCatalog.Collection> cols = PampGramPortalsCatalog.collections();

        CharSequence[] labels = new CharSequence[cols.size() + 1];
        labels[0] = selectedCollection == null
                ? "✓  Все (без фильтра)"
                : "   Все (без фильтра)";
        for (int i = 0; i < cols.size(); i++) {
            PampGramPortalsCatalog.Collection c = cols.get(i);
            String floor = PampGramPortalsCatalog.formatGram(c.floorNanoGram);
            String prefix = TextUtils.equals(selectedCollection, c.name) ? "✓  " : "   ";
            labels[i + 1] = prefix + c.emoji + "  " + c.name + " — " + floor + " GRAM"
                    + (c.isNew ? "  • NEW" : "");
        }

        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Коллекция");
        b.setItems(labels, (dialog, which) -> {
            selectedCollection = which == 0 ? null : cols.get(which - 1).name;
            reload();
        });
        b.setNegativeButton("Отмена", null);
        showDialog(b.create());
    }

    // ---- Items list -------------------------------------------------------

    private void rebuildItemsColumn() {
        if (itemsColumn == null) return;
        itemsColumn.removeAllViews();

        List<PampGramPortalsCatalog.Item> visible = PampGramPortalsCatalog.filter(selectedCollection);
        if (visible.isEmpty()) {
            TextView empty = new TextView(itemsColumn.getContext());
            empty.setText("В этой коллекции пока пусто.");
            empty.setTextColor(0x99FFFFFF);
            empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(16), dp(32), dp(16), dp(32));
            itemsColumn.addView(empty, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        } else {
            for (PampGramPortalsCatalog.Item item : visible) {
                itemsColumn.addView(buildItemRow(itemsColumn.getContext(), item),
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                                LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
            }
        }

        // Footer hint — nothing in this screen touches the network.
        TextView footer = new TextView(itemsColumn.getContext());
        footer.setText("Portals Market визуально. Покупка списывает фейковый GRAM "
                + "и добавляет уникальный подарок в вашу локальную коллекцию.");
        footer.setTextColor(0x66FFFFFF);
        footer.setTextSize(12);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(16), dp(18), dp(16), dp(6));
        itemsColumn.addView(footer, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private View buildItemRow(Context ctx, @NonNull PampGramPortalsCatalog.Item item) {
        FrameLayout row = new FrameLayout(ctx);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setShape(GradientDrawable.RECTANGLE);
        rowBg.setCornerRadius(dp(18));
        rowBg.setColor(SURFACE_ELEVATED);
        row.setBackground(rowBg);
        row.setOnClickListener(v -> presentFragment(new PampGramPortalsDetailScreen(item.id)));

        // Left: 56dp colored square with emoji centerpiece.
        FrameLayout iconWrap = new FrameLayout(ctx);
        GradientDrawable iconBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{item.gradientTop, item.gradientBottom});
        iconBg.setShape(GradientDrawable.RECTANGLE);
        iconBg.setCornerRadius(dp(14));
        iconBg.setStroke(dp(1), 0x22FFFFFF);
        iconWrap.setBackground(iconBg);
        TextView iconText = new TextView(ctx);
        iconText.setText(item.emoji);
        iconText.setTextSize(28);
        iconText.setGravity(Gravity.CENTER);
        iconWrap.addView(iconText, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        FrameLayout.LayoutParams iconLp = LayoutHelper.createFrame(56, 56,
                Gravity.START | Gravity.CENTER_VERTICAL);
        row.addView(iconWrap, iconLp);

        // Middle: name + "#number".
        LinearLayout labels = new LinearLayout(ctx);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(72), 0, dp(130), 0);
        TextView name = new TextView(ctx);
        name.setText(item.collection);
        name.setTextSize(16);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        name.setTextColor(0xFFFFFFFF);
        name.setMaxLines(2);
        labels.addView(name);
        TextView number = new TextView(ctx);
        number.setText("#" + item.number);
        number.setTextSize(13);
        number.setTextColor(0x80FFFFFF);
        LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        numLp.topMargin = dp(2);
        labels.addView(number, numLp);
        row.addView(labels, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        // Right: blue price pill + round basket.
        LinearLayout rightCluster = new LinearLayout(ctx);
        rightCluster.setOrientation(LinearLayout.HORIZONTAL);
        rightCluster.setGravity(Gravity.CENTER_VERTICAL);

        TextView price = new TextView(ctx);
        price.setText(PampGramPortalsCatalog.formatGram(item.priceNanoGram) + "  💎");
        price.setTextSize(14);
        price.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        price.setTextColor(0xFFFFFFFF);
        price.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable priceBg = new GradientDrawable();
        priceBg.setShape(GradientDrawable.RECTANGLE);
        priceBg.setCornerRadius(dp(18));
        priceBg.setColor(ACCENT_PRIMARY);
        price.setBackground(priceBg);
        LinearLayout.LayoutParams priceLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        priceLp.rightMargin = dp(8);
        rightCluster.addView(price, priceLp);

        TextView basket = new TextView(ctx);
        basket.setText("🛒"); // 🛒
        basket.setTextSize(16);
        basket.setGravity(Gravity.CENTER);
        basket.setMinimumWidth(dp(40));
        basket.setMinimumHeight(dp(40));
        GradientDrawable basketBg = new GradientDrawable();
        basketBg.setShape(GradientDrawable.OVAL);
        basketBg.setColor(SURFACE_CHIP);
        basket.setBackground(basketBg);
        basket.setOnClickListener(v -> presentFragment(new PampGramPortalsDetailScreen(item.id)));
        rightCluster.addView(basket, new LinearLayout.LayoutParams(dp(40), dp(40)));

        row.addView(rightCluster, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL));

        return row;
    }

    // ---- Shared helpers ---------------------------------------------------

    private interface BalanceCallback { void onChosen(long newValue); }

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

    private void showComingSoon(String name) {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(name);
        b.setMessage("Этот раздел будет в следующих версиях PampGram.");
        b.setPositiveButton("OK", null);
        showDialog(b.create());
    }
}
