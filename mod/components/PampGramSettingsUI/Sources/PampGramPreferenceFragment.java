package org.telegram.pampgram.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.pampgram.core.PampGramStorage;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

// Shared skeleton for every PampGram section's settings screen. Each section
// supplies its own list of Row items (headers, info blurbs, toggles that
// persist to PampGramStorage KV, and placeholder "coming soon" rows for
// subscreens that are not yet ported). The adapter and click dispatch live
// here so the actual section files stay 50-150 lines of data, not
// boilerplate.
//
// Section fragments extend this and override onCreateRows().
public abstract class PampGramPreferenceFragment extends BaseFragment {

    static final int VIEW_TYPE_HEADER = 0;
    static final int VIEW_TYPE_TOGGLE = 1;
    static final int VIEW_TYPE_SETTINGS_VALUE = 2; // row with a right-aligned text value, opens a subscreen
    static final int VIEW_TYPE_INFO = 3;
    static final int VIEW_TYPE_ACTION = 4; // plain text row, left-aligned, triggers a Runnable

    public interface RowClickHandler {
        void onClick();
    }

    public static final class Row {
        final int viewType;
        final @Nullable String kvKey;      // for toggles
        final boolean kvDefault;           // for toggles
        final String title;
        final @Nullable String subtitleOrValue;
        final @Nullable RowClickHandler onClick;

        private Row(int viewType, @Nullable String kvKey, boolean kvDefault,
                    String title, @Nullable String subtitleOrValue,
                    @Nullable RowClickHandler onClick) {
            this.viewType = viewType;
            this.kvKey = kvKey;
            this.kvDefault = kvDefault;
            this.title = title;
            this.subtitleOrValue = subtitleOrValue;
            this.onClick = onClick;
        }

        public static Row header(String title) {
            return new Row(VIEW_TYPE_HEADER, null, false, title, null, null);
        }

        public static Row info(String text) {
            return new Row(VIEW_TYPE_INFO, null, false, "", text, null);
        }

        public static Row toggle(String key, boolean defaultValue, String title) {
            return new Row(VIEW_TYPE_TOGGLE, key, defaultValue, title, null, null);
        }

        public static Row value(String title, String value, RowClickHandler onClick) {
            return new Row(VIEW_TYPE_SETTINGS_VALUE, null, false, title, value, onClick);
        }

        public static Row action(String title, RowClickHandler onClick) {
            return new Row(VIEW_TYPE_ACTION, null, false, title, null, onClick);
        }
    }

    protected final List<Row> rows = new ArrayList<>();
    private RecyclerListView listView;
    private ListAdapter adapter;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(screenTitle());
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
        anim.setDurations(300);
        anim.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        listView.setItemAnimator(anim);
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= rows.size()) return;
            Row row = rows.get(position);
            if (row.viewType == VIEW_TYPE_TOGGLE && row.kvKey != null) {
                boolean current = PampGramStorage.getInstance().getKvBool(row.kvKey, row.kvDefault);
                boolean next = !current;
                PampGramStorage.getInstance().setKvBool(row.kvKey, next);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(next);
                }
                onToggleChanged(row.kvKey, next);
            } else if (row.onClick != null) {
                row.onClick.onClick();
            }
        });

        rebuildRows();
        return fragmentView;
    }

    protected final void rebuildRows() {
        rows.clear();
        onCreateRows(rows);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    /** Subclass hook: fill `out` with the rows to show, in order. */
    protected abstract void onCreateRows(List<Row> out);

    /** Subclass hook: the ActionBar title. */
    protected abstract String screenTitle();

    /** Subclass hook: called after a toggle flips. Default no-op. */
    protected void onToggleChanged(@NonNull String key, boolean newValue) {}

    protected final void showComingSoon(String name) {
        Drawable icon = getContext().getResources().getDrawable(R.drawable.msg_info).mutate();
        BulletinFactory.of(this).createSimpleBulletin(icon, name + " — в разработке").show();
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context ctx = parent.getContext();
            View v;
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    v = new HeaderCell(ctx);
                    break;
                case VIEW_TYPE_TOGGLE:
                    v = new TextCheckCell(ctx);
                    break;
                case VIEW_TYPE_SETTINGS_VALUE:
                    v = new TextSettingsCell(ctx);
                    break;
                case VIEW_TYPE_INFO:
                    v = new TextInfoPrivacyCell(ctx);
                    break;
                case VIEW_TYPE_ACTION:
                default:
                    v = new TextCell(ctx);
                    break;
            }
            return new RecyclerListView.Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            boolean nextRowSameSection = position + 1 < rows.size()
                    && rows.get(position + 1).viewType != VIEW_TYPE_HEADER
                    && rows.get(position + 1).viewType != VIEW_TYPE_INFO;
            switch (row.viewType) {
                case VIEW_TYPE_HEADER:
                    ((HeaderCell) holder.itemView).setText(row.title);
                    break;
                case VIEW_TYPE_TOGGLE: {
                    boolean value = PampGramStorage.getInstance().getKvBool(row.kvKey, row.kvDefault);
                    ((TextCheckCell) holder.itemView).setTextAndCheck(row.title, value, nextRowSameSection);
                    break;
                }
                case VIEW_TYPE_SETTINGS_VALUE:
                    ((TextSettingsCell) holder.itemView).setTextAndValue(row.title,
                            row.subtitleOrValue == null ? "" : row.subtitleOrValue, nextRowSameSection);
                    break;
                case VIEW_TYPE_INFO:
                    ((TextInfoPrivacyCell) holder.itemView).setText(row.subtitleOrValue);
                    break;
                case VIEW_TYPE_ACTION:
                    ((TextCell) holder.itemView).setText(row.title, nextRowSameSection);
                    break;
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
            int t = holder.getItemViewType();
            return t == VIEW_TYPE_TOGGLE || t == VIEW_TYPE_SETTINGS_VALUE || t == VIEW_TYPE_ACTION;
        }
    }
}
