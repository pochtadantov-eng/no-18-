package org.telegram.pampgram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

// PampGram settings hub. Mirrors the iOS PampGramHubScreen layout: a header,
// a single column of 9 section tiles with iOS-matching colors and subtitles,
// and a team footer. Each tile's functional subscreen is a separate port and
// lands in its own later stage; for now every tile shows a "coming soon"
// bulletin.
public final class PampGramSettingsActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER_INFO = 0;
    private static final int VIEW_TYPE_SECTION = 1;
    private static final int VIEW_TYPE_FOOTER = 2;

    // iOS palette taken from PampGramHubScreen.swift so the Android build keeps
    // visual parity when a user has both devices open.
    private static final int COLOR_GIFTS       = 0xFF8E44EC; // purple
    private static final int COLOR_CHATS       = 0xFF3B82F6; // blue
    private static final int COLOR_GHOST       = 0xFF34C759; // iOS green
    private static final int COLOR_APPEARANCE  = 0xFFFF9500; // iOS orange
    private static final int COLOR_ADVANCED    = 0xFF8E8E93; // iOS gray
    private static final int COLOR_ADMIN       = 0xFFFF3B30; // iOS red
    private static final int COLOR_AUTOMOD     = 0xFF30B050; // green shield
    private static final int COLOR_CALL_RECORD = 0xFFFF3B30; // iOS red
    private static final int COLOR_PERFORMANCE = 0xFFFFCC00; // iOS yellow

    private final List<Row> rows = new ArrayList<>();

    private static final class Row {
        final int viewType;
        final int iconRes;
        final int iconBg;
        final String title;
        final String subtitle;
        final int sectionId;

        Row(int viewType, int iconRes, int iconBg, String title, String subtitle, int sectionId) {
            this.viewType = viewType;
            this.iconRes = iconRes;
            this.iconBg = iconBg;
            this.title = title;
            this.subtitle = subtitle;
            this.sectionId = sectionId;
        }

        static Row header(String subtitle) {
            return new Row(VIEW_TYPE_HEADER_INFO, 0, 0, null, subtitle, 0);
        }

        static Row section(int iconRes, int iconBg, String title, String subtitle, int id) {
            return new Row(VIEW_TYPE_SECTION, iconRes, iconBg, title, subtitle, id);
        }

        static Row footer(String text) {
            return new Row(VIEW_TYPE_FOOTER, 0, 0, null, text, 0);
        }
    }

    // Section ids: free-form, used only by onItemClick to decide where to go.
    // Each stage will replace its own "coming soon" bulletin with a
    // presentFragment call to the real screen.
    private static final int ID_GIFTS       = 1;
    private static final int ID_CHATS       = 2;
    private static final int ID_GHOST       = 3;
    private static final int ID_APPEARANCE  = 4;
    private static final int ID_ADVANCED    = 5;
    private static final int ID_ADMIN       = 6;
    private static final int ID_AUTOMOD     = 7;
    private static final int ID_CALL_RECORD = 8;
    private static final int ID_PERFORMANCE = 9;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("PampGram");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frame = new FrameLayout(context);
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frame;

        RecyclerListView listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(new ListAdapter());
        DefaultItemAnimator anim = new DefaultItemAnimator();
        anim.setDurations(350);
        anim.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        listView.setItemAnimator(anim);
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= rows.size()) return;
            Row row = rows.get(position);
            if (row.viewType != VIEW_TYPE_SECTION) return;
            handleSectionClick(row);
        });

        rebuildRows();
        return fragmentView;
    }

    private void rebuildRows() {
        rows.clear();
        rows.add(Row.header("Расширяй. Скрывай. Контролируй."));

        // Order and labels mirror the iOS hub screenshot exactly.
        rows.add(Row.section(R.drawable.settings_gift,  COLOR_GIFTS,       "Подарки",            "Управление подарками и визуалами",      ID_GIFTS));
        rows.add(Row.section(R.drawable.msg2_discussion, COLOR_CHATS,       "Чаты",               "Визуальный редактор и история",         ID_CHATS));
        rows.add(Row.section(R.drawable.msg2_secret,     COLOR_GHOST,       "Ghost",              "Скрытые функции и защита",              ID_GHOST));
        rows.add(Row.section(R.drawable.msg_colors,      COLOR_APPEARANCE,  "Внешний вид",        "Бейджик наверху экрана",                ID_APPEARANCE));
        rows.add(Row.section(R.drawable.msg_customize,   COLOR_ADVANCED,    "Дополнительно",      "Другие возможности PampGram",           ID_ADVANCED));
        rows.add(Row.section(R.drawable.msg2_block2,     COLOR_ADMIN,       "Админ-панель",       "Управление подписками",                 ID_ADMIN));
        rows.add(Row.section(R.drawable.outline_shield_check, COLOR_AUTOMOD, "Автомодерация",      "ИИ удаляет 18+ и жестокость",           ID_AUTOMOD));
        rows.add(Row.section(R.drawable.msg_calls,       COLOR_CALL_RECORD, "Запись звонков",     "Свой голос и собеседник",               ID_CALL_RECORD));
        rows.add(Row.section(R.drawable.msg2_battery,    COLOR_PERFORMANCE, "Плавность и заряд",  "Меньше лагов и расхода батареи",        ID_PERFORMANCE));

        rows.add(Row.footer("PampGram Team\nСделано с 💜 для тебя"));
    }

    private void handleSectionClick(Row row) {
        // Every hub tile now opens its own section screen. Behavior hooks that
        // make the toggles actually change Telegram's behavior (ghost reads,
        // anti-delete, call recording, etc.) are separate anchor patches that
        // land stage by stage; the UI and persistence are already here.
        switch (row.sectionId) {
            case ID_GIFTS:
                presentFragment(new PampGramGiftsScreen());
                return;
            case ID_CHATS:
                presentFragment(new PampGramMessagesScreen());
                return;
            case ID_GHOST:
                presentFragment(new PampGramGhostScreen());
                return;
            case ID_APPEARANCE:
                presentFragment(new PampGramAppearanceScreen());
                return;
            case ID_ADVANCED:
                presentFragment(new PampGramAdditionalScreen());
                return;
            case ID_ADMIN:
                presentFragment(new PampGramAdminScreen());
                return;
            case ID_AUTOMOD:
                presentFragment(new PampGramAutoModerationScreen());
                return;
            case ID_CALL_RECORD:
                presentFragment(new PampGramCallRecordScreen());
                return;
            case ID_PERFORMANCE:
                presentFragment(new PampGramPerformanceScreen());
                return;
            default:
                break;
        }
        String name = row.title == null ? "Раздел" : row.title;
        Drawable icon = getContext().getResources().getDrawable(R.drawable.msg_info).mutate();
        BulletinFactory.of(this)
                .createSimpleBulletin(icon, name + " — в разработке")
                .show();
    }

    /**
     * iOS-styled rounded square with a colored background and a white-tinted
     * glyph. Visually matches generatePampGramSectionIcon() from the Swift
     * code. 36dp outer, 24dp glyph centered inside.
     */
    private static Drawable createSectionIcon(Context ctx, int bgColor, int iconRes) {
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
            if (viewType == VIEW_TYPE_HEADER_INFO) {
                v = new HeaderCell(ctx);
            } else if (viewType == VIEW_TYPE_FOOTER) {
                v = new TextInfoPrivacyCell(ctx);
            } else {
                TextCell cell = new TextCell(ctx);
                cell.heightDp = 60; // taller to fit title + subtitle like the iOS layout
                v = cell;
            }
            return new RecyclerListView.Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            if (row.viewType == VIEW_TYPE_HEADER_INFO) {
                ((HeaderCell) holder.itemView).setText(row.subtitle);
            } else if (row.viewType == VIEW_TYPE_FOOTER) {
                ((TextInfoPrivacyCell) holder.itemView).setText(row.subtitle);
            } else {
                TextCell cell = (TextCell) holder.itemView;
                Drawable icon = createSectionIcon(cell.getContext(), row.iconBg, row.iconRes);
                boolean divider = position + 1 < rows.size()
                        && rows.get(position + 1).viewType == VIEW_TYPE_SECTION;
                cell.setTextAndIcon(row.title, icon, divider);
                cell.setSubtitle(row.subtitle);
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
            return holder.getItemViewType() == VIEW_TYPE_SECTION;
        }
    }
}
