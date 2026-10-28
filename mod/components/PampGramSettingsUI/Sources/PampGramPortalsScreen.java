package org.telegram.pampgram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.pampgram.core.PampGramStorage;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Portals — visual-only mini-market styled after the Portals Telegram
// mini-app. A grid of fake "unique" gifts (see PampGramPortalsCatalog) that a
// user can "buy" with their local fake TON balance. Buying deducts nano-TON
// from PampGramStorage.PampGramPreferences.fakeTonBalanceNanos, writes a
// HISTORY_PURCHASE row, and adds a PhantomGift for the current user so the
// item lands in their collection — exactly like PampGramGiftComposeScreen
// MODE_SELF does for manual gifts, but with a pre-defined catalog and a
// Portals-style surface.
//
// Nothing here reaches the network; this is a visual mini-app, same as the
// rest of PampGram.
public final class PampGramPortalsScreen extends BaseFragment {

    private static final int SPAN_COUNT = 2;

    // Row types for the grid adapter. ITEM is half-width; the rest span 2.
    private static final int VIEW_TYPE_ITEM   = 0;
    private static final int VIEW_TYPE_FOOTER = 1;
    private static final int VIEW_TYPE_EMPTY  = 2;

    // Portals-signature dark background. Cards always sit on this surface, so
    // the look stays consistent across Telegram's light and dark themes.
    private static final int SURFACE          = 0xFF0B0B0F;
    private static final int SURFACE_ELEVATED = 0xFF14141A;
    private static final int CHIP_IDLE_BG     = 0xFF1E1E26;
    private static final int CHIP_SELECTED_BG = 0xFFFFFFFF;
    private static final int CHIP_IDLE_FG     = 0xFFBFBFC9;
    private static final int CHIP_SELECTED_FG = 0xFF000000;
    private static final int ACCENT_TON       = 0xFF1AC9FF;

    private String selectedCollection; // null = "All"
    private final List<PampGramPortalsCatalog.Item> visible = new ArrayList<>();

    private FrameLayout root;
    private LinearLayout chipStrip;
    private RecyclerListView gridView;
    private GridAdapter adapter;
    private TextView balancePill;
    private PampGramStorage.PampGramPreferences prefs;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Portals");
        actionBar.setSubtitle("Market");
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

        root = new FrameLayout(context);
        root.setBackgroundColor(SURFACE);
        fragmentView = root;

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        root.addView(column, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Balance pill header — mirrors the Portals mini-app's "N.NN TON"
        // chip in the top-right corner.
        FrameLayout balanceRow = new FrameLayout(context);
        balanceRow.setBackgroundColor(SURFACE);
        balanceRow.setPadding(dp(16), dp(10), dp(16), dp(10));
        balancePill = new TextView(context);
        balancePill.setTextSize(14);
        balancePill.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        balancePill.setTextColor(0xFFFFFFFF);
        balancePill.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(dp(20));
        pillBg.setColor(SURFACE_ELEVATED);
        pillBg.setStroke(dp(1), 0x33FFFFFF);
        balancePill.setBackground(pillBg);
        balanceRow.addView(balancePill, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL));

        TextView sectionTitle = new TextView(context);
        sectionTitle.setText("Market");
        sectionTitle.setTextSize(22);
        sectionTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        sectionTitle.setTextColor(0xFFFFFFFF);
        balanceRow.addView(sectionTitle, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL));
        column.addView(balanceRow, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Horizontal filter chip strip.
        HorizontalScrollView chipScroll = new HorizontalScrollView(context);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.setBackgroundColor(SURFACE);
        chipStrip = new LinearLayout(context);
        chipStrip.setOrientation(LinearLayout.HORIZONTAL);
        chipStrip.setPadding(dp(12), dp(4), dp(12), dp(10));
        chipScroll.addView(chipStrip, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        column.addView(chipScroll, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Grid of items.
        gridView = new RecyclerListView(context);
        gridView.setBackgroundColor(SURFACE);
        GridLayoutManager glm = new GridLayoutManager(context, SPAN_COUNT);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int type = adapter.getItemViewType(position);
                return (type == VIEW_TYPE_FOOTER || type == VIEW_TYPE_EMPTY) ? SPAN_COUNT : 1;
            }
        });
        gridView.setLayoutManager(glm);
        gridView.setVerticalScrollBarEnabled(false);
        gridView.setPadding(dp(8), 0, dp(8), dp(16));
        gridView.setClipToPadding(false);
        gridView.setAdapter(adapter = new GridAdapter());
        DefaultItemAnimator anim = new DefaultItemAnimator();
        anim.setDurations(260);
        anim.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        gridView.setItemAnimator(anim);
        column.addView(gridView, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 0, 1.0f));

        gridView.setOnItemClickListener((v, position) -> {
            if (position < 0 || position >= visible.size()) return;
            if (adapter.getItemViewType(position) != VIEW_TYPE_ITEM) return;
            presentFragment(new PampGramPortalsDetailScreen(visible.get(position).id));
        });

        rebuildChips();
        reload();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // After PampGramPortalsDetailScreen pops back from a successful
        // purchase, re-read the balance so the top-right pill updates.
        if (fragmentView != null) reload();
    }

    private void reload() {
        prefs = PampGramStorage.getInstance().getPreferences();
        visible.clear();
        visible.addAll(PampGramPortalsCatalog.filter(selectedCollection));
        updateBalancePill();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void updateBalancePill() {
        double ton = prefs.fakeTonBalanceNanos / 1_000_000_000.0;
        balancePill.setText(String.format(Locale.ROOT, "💎 %,.2f TON", ton)
                .replace(',', ' '));
    }

    private void rebuildChips() {
        chipStrip.removeAllViews();
        addChip("All", null);
        for (String name : PampGramPortalsCatalog.collections()) {
            addChip(name, name);
        }
    }

    private void addChip(String label, String collectionOrNull) {
        boolean selected = TextUtils.equals(selectedCollection, collectionOrNull);
        TextView chip = new TextView(getParentActivity());
        chip.setText(label);
        chip.setTextSize(13);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(18));
        bg.setColor(selected ? CHIP_SELECTED_BG : CHIP_IDLE_BG);
        chip.setBackground(bg);
        chip.setTextColor(selected ? CHIP_SELECTED_FG : CHIP_IDLE_FG);
        chip.setOnClickListener(v -> {
            selectedCollection = collectionOrNull;
            rebuildChips();
            reload();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        chipStrip.addView(chip, lp);
    }

    // ---- Item card view (built in code to avoid new XML in TMessagesProj) --

    private static final class ItemCard extends FrameLayout {
        private final View bg;
        private final TextView emojiView;
        private final TextView collectionView;
        private final TextView numberView;
        private final TextView priceView;

        ItemCard(Context context) {
            super(context);
            setPadding(dp(6), dp(6), dp(6), dp(6));
            setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));

            bg = new View(context);
            addView(bg, LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            emojiView = new TextView(context);
            emojiView.setTextSize(64);
            emojiView.setGravity(Gravity.CENTER);
            addView(emojiView, LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            // Top band: collection name + unique number
            LinearLayout topBand = new LinearLayout(context);
            topBand.setOrientation(LinearLayout.VERTICAL);
            topBand.setPadding(dp(12), dp(12), dp(12), 0);
            collectionView = new TextView(context);
            collectionView.setTextSize(11);
            collectionView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            collectionView.setTextColor(0xCCFFFFFF);
            collectionView.setAllCaps(true);
            collectionView.setLetterSpacing(0.05f);
            topBand.addView(collectionView);
            numberView = new TextView(context);
            numberView.setTextSize(14);
            numberView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            numberView.setTextColor(0xFFFFFFFF);
            topBand.addView(numberView);
            addView(topBand, LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START));

            // Bottom price chip
            priceView = new TextView(context);
            priceView.setTextSize(13);
            priceView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            priceView.setTextColor(0xFFFFFFFF);
            priceView.setPadding(dp(10), dp(6), dp(10), dp(6));
            GradientDrawable priceBg = new GradientDrawable();
            priceBg.setShape(GradientDrawable.RECTANGLE);
            priceBg.setCornerRadius(dp(14));
            priceBg.setColor(0xCC000000);
            priceBg.setStroke(dp(1), 0x55FFFFFF);
            priceView.setBackground(priceBg);
            FrameLayout.LayoutParams priceLp = LayoutHelper.createFrame(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.END);
            priceLp.rightMargin = dp(14);
            priceLp.bottomMargin = dp(14);
            addView(priceView, priceLp);
        }

        void bind(PampGramPortalsCatalog.Item item) {
            GradientDrawable cardBg = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{item.gradientTop, item.gradientBottom});
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(dp(18));
            cardBg.setStroke(dp(1), 0x22FFFFFF);
            bg.setBackground(cardBg);

            emojiView.setText(item.emoji);
            collectionView.setText(item.collection);
            numberView.setText("#" + item.number);
            double ton = item.priceNanos / 1_000_000_000.0;
            priceView.setText(String.format(Locale.ROOT, "💎 %,.0f TON", ton)
                    .replace(',', ' '));
        }
    }

    private final class GridAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public int getItemCount() {
            // visible items + 1 footer (or empty state if nothing matched)
            return visible.isEmpty() ? 1 : visible.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (visible.isEmpty()) return VIEW_TYPE_EMPTY;
            return position < visible.size() ? VIEW_TYPE_ITEM : VIEW_TYPE_FOOTER;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == VIEW_TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context ctx = parent.getContext();
            View v;
            if (viewType == VIEW_TYPE_ITEM) {
                v = new ItemCard(ctx);
            } else if (viewType == VIEW_TYPE_EMPTY) {
                v = makeInfoCell(ctx, "В этой коллекции пока пусто.");
            } else {
                v = makeInfoCell(ctx,
                        "Portals визуально. Покупка списывает фейковый TON и добавляет подарок в вашу локальную коллекцию.");
            }
            return new RecyclerListView.Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == VIEW_TYPE_ITEM) {
                ((ItemCard) holder.itemView).bind(visible.get(position));
            }
        }
    }

    private static View makeInfoCell(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(0x99FFFFFF);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(24), dp(24), dp(24), dp(24));
        return tv;
    }
}
