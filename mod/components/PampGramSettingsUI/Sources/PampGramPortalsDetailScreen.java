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

// Detail view for one Portals catalog item. Shows the big hero card, three
// attribute rows (Model / Backdrop / Symbol with rarity percent, same shape
// as portals-market.com), an owner line, and a sticky "Купить" button that
// commits the fake purchase against PampGramStorage.
public final class PampGramPortalsDetailScreen extends BaseFragment {

    private static final int SURFACE          = 0xFF0B0B0F;
    private static final int SURFACE_ELEVATED = 0xFF14141A;
    private static final int ACCENT_BUY       = 0xFF1AC9FF;

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
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(SURFACE);
        fragmentView = root;

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        root.addView(scroll, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(16), dp(16), dp(16), dp(96)); // room for sticky bottom bar
        scroll.addView(column, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        column.addView(buildHero(context), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 320));

        TextView attributesHeader = new TextView(context);
        attributesHeader.setText("Attributes");
        attributesHeader.setTextSize(13);
        attributesHeader.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        attributesHeader.setTextColor(0x99FFFFFF);
        attributesHeader.setAllCaps(true);
        attributesHeader.setLetterSpacing(0.1f);
        attributesHeader.setPadding(0, dp(18), 0, dp(10));
        column.addView(attributesHeader);

        column.addView(buildAttrRow(context, "Model", item.model, item.modelRarityPct10),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
        column.addView(buildAttrRow(context, "Backdrop", item.backdrop, item.backdropRarityPct10),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
        column.addView(buildAttrRow(context, "Symbol", item.symbol, item.symbolRarityPct10),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        TextView ownerHeader = new TextView(context);
        ownerHeader.setText("Owner");
        ownerHeader.setTextSize(13);
        ownerHeader.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        ownerHeader.setTextColor(0x99FFFFFF);
        ownerHeader.setAllCaps(true);
        ownerHeader.setLetterSpacing(0.1f);
        ownerHeader.setPadding(0, dp(18), 0, dp(10));
        column.addView(ownerHeader);

        TextView ownerRow = new TextView(context);
        ownerRow.setText("@" + item.owner);
        ownerRow.setTextSize(16);
        ownerRow.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        ownerRow.setTextColor(0xFFFFFFFF);
        ownerRow.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable ownerBg = new GradientDrawable();
        ownerBg.setShape(GradientDrawable.RECTANGLE);
        ownerBg.setCornerRadius(dp(14));
        ownerBg.setColor(SURFACE_ELEVATED);
        ownerRow.setBackground(ownerBg);
        column.addView(ownerRow, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Sticky buy bar at bottom.
        FrameLayout buyBar = new FrameLayout(context);
        buyBar.setBackgroundColor(SURFACE);
        buyBar.setPadding(dp(16), dp(10), dp(16), dp(16));

        TextView buyButton = new TextView(context);
        double ton = item.priceNanos / 1_000_000_000.0;
        buyButton.setText(String.format(Locale.ROOT, "Купить за %,.0f TON", ton)
                .replace(',', ' '));
        buyButton.setTextSize(16);
        buyButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        buyButton.setTextColor(0xFF000000);
        buyButton.setGravity(Gravity.CENTER);
        GradientDrawable buyBg = new GradientDrawable();
        buyBg.setShape(GradientDrawable.RECTANGLE);
        buyBg.setCornerRadius(dp(14));
        buyBg.setColor(ACCENT_BUY);
        buyButton.setBackground(buyBg);
        buyButton.setPadding(dp(20), dp(16), dp(20), dp(16));
        buyButton.setOnClickListener(v -> onBuyTap());
        buyBar.addView(buyButton, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        root.addView(buyBar, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));

        return fragmentView;
    }

    private View buildHero(Context ctx) {
        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{item.gradientTop, item.gradientBottom});
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), 0x22FFFFFF);
        hero.setBackground(bg);

        TextView emoji = new TextView(ctx);
        emoji.setText(item.emoji);
        emoji.setTextSize(120);
        emoji.setGravity(Gravity.CENTER);
        hero.addView(emoji, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout topBand = new LinearLayout(ctx);
        topBand.setOrientation(LinearLayout.VERTICAL);
        topBand.setPadding(dp(18), dp(18), dp(18), 0);
        TextView collection = new TextView(ctx);
        collection.setText(item.collection);
        collection.setTextSize(13);
        collection.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        collection.setTextColor(0xCCFFFFFF);
        collection.setAllCaps(true);
        collection.setLetterSpacing(0.05f);
        topBand.addView(collection);
        TextView number = new TextView(ctx);
        number.setText("#" + item.number);
        number.setTextSize(20);
        number.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        number.setTextColor(0xFFFFFFFF);
        topBand.addView(number);
        hero.addView(topBand, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START));

        return hero;
    }

    private View buildAttrRow(Context ctx, String label, String value, int rarityPct10) {
        FrameLayout row = new FrameLayout(ctx);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(14));
        bg.setColor(SURFACE_ELEVATED);
        row.setBackground(bg);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout left = new LinearLayout(ctx);
        left.setOrientation(LinearLayout.VERTICAL);
        TextView labelView = new TextView(ctx);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(0x99FFFFFF);
        labelView.setAllCaps(true);
        labelView.setLetterSpacing(0.05f);
        left.addView(labelView);
        TextView valueView = new TextView(ctx);
        valueView.setText(value);
        valueView.setTextSize(16);
        valueView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        valueView.setTextColor(0xFFFFFFFF);
        valueView.setPadding(0, dp(2), 0, 0);
        left.addView(valueView);
        row.addView(left, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL));

        TextView rarity = new TextView(ctx);
        double pct = rarityPct10 / 10.0;
        rarity.setText(String.format(Locale.ROOT, "%.1f%%", pct).replace(',', '.'));
        rarity.setTextSize(13);
        rarity.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        rarity.setTextColor(0xFFFFFFFF);
        rarity.setPadding(dp(10), dp(5), dp(10), dp(5));
        GradientDrawable rarityBg = new GradientDrawable();
        rarityBg.setShape(GradientDrawable.RECTANGLE);
        rarityBg.setCornerRadius(dp(12));
        rarityBg.setColor(0x22FFFFFF);
        rarity.setBackground(rarityBg);
        row.addView(rarity, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL));

        return row;
    }

    private void onBuyTap() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        PampGramStorage storage = PampGramStorage.getInstance();
        PampGramStorage.PampGramPreferences prefs = storage.getPreferences();

        double ton = item.priceNanos / 1_000_000_000.0;
        String priceStr = String.format(Locale.ROOT, "%,.0f TON", ton).replace(',', ' ');

        if (prefs.fakeTonBalanceNanos < item.priceNanos) {
            double have = prefs.fakeTonBalanceNanos / 1_000_000_000.0;
            String haveStr = String.format(Locale.ROOT, "%,.2f TON", have).replace(',', ' ');
            AlertDialog.Builder b = new AlertDialog.Builder(ctx);
            b.setTitle("Недостаточно TON");
            b.setMessage("Нужно " + priceStr + ", у вас " + haveStr + ".\n\n"
                    + "Положить фейковый TON можно в разделе Подарки → Балансы → TON.");
            b.setPositiveButton("OK", null);
            showDialog(b.create());
            return;
        }

        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Купить " + item.collection + " #" + item.number + "?");
        b.setMessage("Будет списано " + priceStr + " с визуального баланса. "
                + "Подарок появится в вашей коллекции.");
        b.setPositiveButton("Купить", (dialog, which) -> commitPurchase());
        b.setNegativeButton("Отмена", null);
        showDialog(b.create());
    }

    private void commitPurchase() {
        PampGramStorage storage = PampGramStorage.getInstance();
        storage.updatePreferences(p ->
                p.fakeTonBalanceNanos = Math.max(0L, p.fakeTonBalanceNanos - item.priceNanos));
        storage.addHistoryEntry(
                PampGramStorage.HISTORY_PURCHASE,
                PampGramStorage.CURRENCY_TON,
                item.priceNanos,
                "Portals",
                item.collection + " #" + item.number);
        long selfId = getUserConfig().getClientUserId();
        storage.addPhantomGift(
                selfId,
                item.collection + " #" + item.number,
                null,
                "unique",
                item.number,
                PampGramStorage.CURRENCY_TON,
                item.priceNanos,
                true /* isReceived — item lands in collection without debiting again */);

        Drawable icon = getContext().getResources()
                .getDrawable(R.drawable.msg_info).mutate();
        BulletinFactory.of(this)
                .createSimpleBulletin(icon,
                        item.collection + " #" + item.number + " в вашей коллекции")
                .show();
        finishFragment();
    }
}
