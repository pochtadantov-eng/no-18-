package org.telegram.pampgram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.R;
import org.telegram.pampgram.core.PampGramStorage;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.util.Locale;

// Portals item detail. Matches the portals-market mini-app's item sheet:
//   - centered square hero card with the signature background and
//     centerpiece glyph, name + "#number" underneath, and a green
//     "Доступно для крафта" ribbon when the item has it;
//   - attributes table with 3 rows (Model / Symbol / Backdrop), each
//     carrying a blue rarity-% pill and the item's GRAM price on the
//     right, plus a "Мин. Цена" summary row;
//   - "Вознаграждение за покупку N ✦" row showing stars that will be
//     credited on buy;
//   - two secondary buttons ("Сделать оффер", "Lucky Buy") and a sticky
//     primary "Купить подарок N GRAM" button.
//
// Only the two buy actions are functional — both touch the local
// PampGramStorage (fakeTonBalanceNanos for debit, fakeStarsBalance for
// reward credit, HISTORY_PURCHASE entry, and a PhantomGift for the
// current user's collection). "Сделать оффер" opens an informational
// dialog; a real offer flow is a later stage.
public final class PampGramPortalsDetailScreen extends BaseFragment {

    private static final int SURFACE          = 0xFF0B0B0F;
    private static final int SURFACE_ELEVATED = 0xFF14141A;
    private static final int SURFACE_INNER    = 0xFF101016;
    private static final int ACCENT_PRIMARY   = 0xFF1AC9FF; // GRAM blue
    private static final int ACCENT_LUCKY     = 0xFF2A5A2E; // deep green
    private static final int ACCENT_STARS     = 0xFFFFB400;
    private static final int ACCENT_CRAFT     = 0xFF2DD17B;
    private static final int PILL_RARITY_BG   = 0xFF1E415A;
    private static final int PILL_RARITY_FG   = 0xFF1AC9FF;

    private final String itemId;
    private PampGramPortalsCatalog.Item item;

    public PampGramPortalsDetailScreen(String itemId) {
        this.itemId = itemId;
    }

    @Override
    public boolean onFragmentCreate() {
        item = PampGramPortalsCatalog.byId(itemId);
        return item != null && super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(item.collection);
        actionBar.setSubtitle("#" + item.number);
        actionBar.setBackgroundColor(SURFACE);
        actionBar.setItemsColor(0xFFFFFFFF, false);
        actionBar.setItemsBackgroundColor(0x33FFFFFF, false);
        actionBar.setTitleColor(0xFFFFFFFF);
        actionBar.setSubtitleColor(0x99FFFFFF);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(SURFACE);
        fragmentView = root;

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        root.addView(scroll, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(16), dp(16), dp(120)); // room for sticky bar
        scroll.addView(col, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Centered square hero.
        FrameLayout heroWrap = new FrameLayout(context);
        heroWrap.setPadding(dp(24), dp(8), dp(24), 0);
        heroWrap.addView(buildHero(context), LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        col.addView(heroWrap, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 320));

        // Attributes table.
        col.addView(buildAttributesTable(context), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 20, 0, 0));

        // Reward line + two secondary buttons.
        col.addView(buildRewardRow(context), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 20, 0, 10));
        col.addView(buildOfferAndLuckyRow(context), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Primary buy button (not sticky — it sits in the scrollable column,
        // same as the real Portals sheet, which scrolls as one piece).
        col.addView(buildPrimaryBuyButton(context), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));

        return fragmentView;
    }

    // ---- Hero -------------------------------------------------------------

    private View buildHero(Context ctx) {
        FrameLayout outer = new FrameLayout(ctx);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{item.gradientTop, item.gradientBottom});
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(28));
        bg.setStroke(dp(1), 0x33FFFFFF);
        outer.setBackground(bg);

        TextView glyph = new TextView(ctx);
        glyph.setText(item.emoji);
        glyph.setTextSize(120);
        glyph.setGravity(Gravity.CENTER);
        outer.addView(glyph, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Title + "#number" + craft ribbon at the bottom of the card.
        LinearLayout bottom = new LinearLayout(ctx);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER_HORIZONTAL);
        bottom.setPadding(dp(12), dp(8), dp(12), dp(16));
        TextView title = new TextView(ctx);
        title.setText(item.collection + "  ›");
        title.setTextSize(22);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        bottom.addView(title);

        TextView number = new TextView(ctx);
        number.setText("#" + item.number);
        number.setTextSize(14);
        number.setTextColor(0x99FFFFFF);
        number.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        numLp.topMargin = dp(2);
        numLp.gravity = Gravity.CENTER_HORIZONTAL;
        bottom.addView(number, numLp);

        if (item.craftable) {
            TextView craft = new TextView(ctx);
            craft.setText("⚒  Доступно для крафта");
            craft.setTextSize(12);
            craft.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            craft.setTextColor(ACCENT_CRAFT);
            craft.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams craftLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            craftLp.topMargin = dp(8);
            craftLp.gravity = Gravity.CENTER_HORIZONTAL;
            bottom.addView(craft, craftLp);
        }

        outer.addView(bottom, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));

        return outer;
    }

    // ---- Attributes -------------------------------------------------------

    private View buildAttributesTable(Context ctx) {
        LinearLayout table = new LinearLayout(ctx);
        table.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(18));
        bg.setColor(SURFACE_ELEVATED);
        bg.setStroke(dp(1), 0x14FFFFFF);
        table.setBackground(bg);
        table.setPadding(dp(14), dp(6), dp(14), dp(6));

        table.addView(buildAttrRow(ctx, "Model",    item.model,    item.modelPct10,    item.priceNanoGram));
        table.addView(buildThinDivider(ctx));
        table.addView(buildAttrRow(ctx, "Symbol",   item.symbol,   item.symbolPct10,   item.priceNanoGram));
        table.addView(buildThinDivider(ctx));
        table.addView(buildAttrRow(ctx, "Backdrop", item.backdrop, item.backdropPct10, item.priceNanoGram));
        table.addView(buildThinDivider(ctx));
        table.addView(buildFloorRow(ctx));

        return table;
    }

    private View buildAttrRow(Context ctx, String label, String value, int rarityPct10, long priceNanoGram) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        TextView l = new TextView(ctx);
        l.setText(label);
        l.setTextSize(14);
        l.setTextColor(0x99FFFFFF);
        row.addView(l, new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout middle = new LinearLayout(ctx);
        middle.setOrientation(LinearLayout.HORIZONTAL);
        middle.setGravity(Gravity.CENTER_VERTICAL);
        TextView v = new TextView(ctx);
        v.setText(value);
        v.setTextSize(15);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        v.setTextColor(0xFF1AC9FF);
        middle.addView(v);
        TextView pct = new TextView(ctx);
        pct.setText(formatPct(rarityPct10));
        pct.setTextSize(11);
        pct.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        pct.setTextColor(PILL_RARITY_FG);
        pct.setPadding(dp(7), dp(2), dp(7), dp(2));
        GradientDrawable pctBg = new GradientDrawable();
        pctBg.setShape(GradientDrawable.RECTANGLE);
        pctBg.setCornerRadius(dp(10));
        pctBg.setColor(PILL_RARITY_BG);
        pct.setBackground(pctBg);
        LinearLayout.LayoutParams pctLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pctLp.leftMargin = dp(8);
        middle.addView(pct, pctLp);
        row.addView(middle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView right = new TextView(ctx);
        right.setText(PampGramPortalsCatalog.formatGram(priceNanoGram) + " GRAM");
        right.setTextSize(14);
        right.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        right.setTextColor(0xFFFFFFFF);
        row.addView(right, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return row;
    }

    private View buildFloorRow(Context ctx) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, dp(10));
        TextView l = new TextView(ctx);
        l.setText("Мин. Цена");
        l.setTextSize(14);
        l.setTextColor(0x99FFFFFF);
        row.addView(l, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = new TextView(ctx);
        v.setText(PampGramPortalsCatalog.formatGram(item.priceNanoGram) + " GRAM");
        v.setTextSize(15);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        v.setTextColor(0xFFFFFFFF);
        row.addView(v);
        return row;
    }

    private View buildThinDivider(Context ctx) {
        View d = new View(ctx);
        d.setBackgroundColor(0x11FFFFFF);
        return d;
    }

    // ---- Reward + secondary buttons ---------------------------------------

    private View buildRewardRow(Context ctx) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView l = new TextView(ctx);
        l.setText("Вознаграждение за покупку");
        l.setTextSize(14);
        l.setTextColor(0x99FFFFFF);
        row.addView(l, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView stars = new TextView(ctx);
        stars.setText(PampGramPortalsCatalog.formatStars(item.rewardStars()) + "  ✨");
        stars.setTextSize(16);
        stars.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        stars.setTextColor(ACCENT_STARS);
        row.addView(stars);
        return row;
    }

    private View buildOfferAndLuckyRow(Context ctx) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.addView(buildTwoLineButton(ctx,
                "Сделать оффер",
                "0.01 GRAM",
                SURFACE_INNER,
                0xFFFFFFFF,
                0x99FFFFFF,
                false,
                v -> showComingSoon()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        View spacer = new View(ctx);
        row.addView(spacer, new LinearLayout.LayoutParams(dp(10), 1));

        long luckyPrice = item.priceNanoGram / 2;
        row.addView(buildTwoLineButton(ctx,
                "Lucky Buy  🍀",
                "За " + PampGramPortalsCatalog.formatGram(luckyPrice) + " GRAM",
                ACCENT_LUCKY,
                0xFFFFFFFF,
                0xCCFFFFFF,
                true,
                v -> onLuckyBuyTap()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return row;
    }

    private View buildTwoLineButton(Context ctx, String top, String bottom,
                                    int bg, int topColor, int bottomColor, boolean stroked,
                                    View.OnClickListener onClick) {
        LinearLayout btn = new LinearLayout(ctx);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(10), dp(12), dp(10), dp(12));
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(20));
        d.setColor(bg);
        if (stroked) d.setStroke(dp(1), 0x4D34C759);
        btn.setBackground(d);
        TextView tv1 = new TextView(ctx);
        tv1.setText(top);
        tv1.setTextSize(15);
        tv1.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv1.setTextColor(topColor);
        tv1.setGravity(Gravity.CENTER);
        btn.addView(tv1);
        TextView tv2 = new TextView(ctx);
        tv2.setText(bottom);
        tv2.setTextSize(12);
        tv2.setTextColor(bottomColor);
        tv2.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tv2Lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tv2Lp.topMargin = dp(2);
        tv2Lp.gravity = Gravity.CENTER_HORIZONTAL;
        btn.addView(tv2, tv2Lp);
        btn.setOnClickListener(onClick);
        return btn;
    }

    private View buildPrimaryBuyButton(Context ctx) {
        LinearLayout btn = new LinearLayout(ctx);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(22));
        d.setColor(ACCENT_PRIMARY);
        btn.setBackground(d);

        TextView title = new TextView(ctx);
        title.setText("Купить подарок");
        title.setTextSize(16);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setTextColor(0xFF000000);
        title.setGravity(Gravity.CENTER);
        btn.addView(title);

        TextView price = new TextView(ctx);
        price.setText(PampGramPortalsCatalog.formatGram(item.priceNanoGram) + " GRAM");
        price.setTextSize(13);
        price.setTextColor(0xCC000000);
        price.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pLp.topMargin = dp(2);
        pLp.gravity = Gravity.CENTER_HORIZONTAL;
        btn.addView(price, pLp);

        btn.setOnClickListener(v -> onPrimaryBuyTap());
        return btn;
    }

    // ---- Buy actions ------------------------------------------------------

    private void onPrimaryBuyTap() {
        confirmPurchase(item.priceNanoGram, item.rewardStars(), "Купить подарок");
    }

    private void onLuckyBuyTap() {
        long luckyPrice = item.priceNanoGram / 2;
        long luckyReward = item.rewardStars() / 2;
        confirmPurchase(luckyPrice, luckyReward, "Lucky Buy");
    }

    private void confirmPurchase(long priceNanoGram, long rewardStars, String action) {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        PampGramStorage storage = PampGramStorage.getInstance();
        PampGramStorage.PampGramPreferences prefs = storage.getPreferences();

        String priceStr = PampGramPortalsCatalog.formatGram(priceNanoGram) + " GRAM";

        if (prefs.fakeTonBalanceNanos < priceNanoGram) {
            String haveStr = PampGramPortalsCatalog.formatGram(prefs.fakeTonBalanceNanos) + " GRAM";
            AlertDialog.Builder b = new AlertDialog.Builder(ctx);
            b.setTitle("Недостаточно GRAM");
            b.setMessage("Нужно " + priceStr + ", у вас " + haveStr + ".\n\n"
                    + "Пополнить баланс можно, тапнув на пилюлю GRAM в правом верхнем "
                    + "углу Portals Market.");
            b.setPositiveButton("OK", null);
            showDialog(b.create());
            return;
        }

        String rewardStr = PampGramPortalsCatalog.formatStars(rewardStars) + " ✦";
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(action + ": " + item.collection + " #" + item.number);
        b.setMessage("Будет списано " + priceStr + ". Вы получите " + rewardStr
                + " в баланс звёзд, а подарок появится в вашей коллекции.");
        b.setPositiveButton("Купить", (dialog, which) -> commitPurchase(priceNanoGram, rewardStars));
        b.setNegativeButton("Отмена", null);
        showDialog(b.create());
    }

    private void commitPurchase(long priceNanoGram, long rewardStars) {
        PampGramStorage storage = PampGramStorage.getInstance();
        storage.updatePreferences(p -> {
            p.fakeTonBalanceNanos = Math.max(0L, p.fakeTonBalanceNanos - priceNanoGram);
            p.fakeStarsBalance = Math.max(0L, p.fakeStarsBalance + rewardStars);
        });
        storage.addHistoryEntry(
                PampGramStorage.HISTORY_PURCHASE,
                PampGramStorage.CURRENCY_TON,
                priceNanoGram,
                "Portals Market",
                item.collection + " #" + item.number);
        long selfId = getUserConfig().getClientUserId();
        storage.addPhantomGift(
                selfId,
                item.collection + " #" + item.number,
                null,
                "unique",
                item.number,
                PampGramStorage.CURRENCY_TON,
                priceNanoGram,
                true /* received — the item lands in the collection, debit already applied above */);

        Drawable icon = getContext().getResources()
                .getDrawable(R.drawable.msg_info).mutate();
        BulletinFactory.of(this)
                .createSimpleBulletin(icon,
                        item.collection + " #" + item.number + " у вас в коллекции")
                .show();
        finishFragment();
    }

    private void showComingSoon() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Сделать оффер");
        b.setMessage("Оффер‑флоу появится в следующих версиях PampGram.");
        b.setPositiveButton("OK", null);
        showDialog(b.create());
    }

    // ---- Helpers ----------------------------------------------------------

    private static String formatPct(int pct10) {
        double pct = pct10 / 10.0;
        return String.format(Locale.ROOT, "%.1f%%", pct).replace(',', '.');
    }
}
