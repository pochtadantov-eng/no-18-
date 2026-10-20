package org.telegram.pampgram.settings;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

// PampGram settings hub. Entry row is wired into ProfileActivity by
// mod/apply-settings-entry.py (anchor-checked). Stage 1: only the hub shell
// with placeholder rows — stages 2..6 will replace each "coming soon" row
// with its real subscreen.
public final class PampGramSettingsActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_INFO = 2;

    private final List<Row> rows = new ArrayList<>();

    private static final class Row {
        final int viewType;
        final String text;
        final boolean enabled;
        final int id;

        Row(int viewType, String text, boolean enabled, int id) {
            this.viewType = viewType;
            this.text = text;
            this.enabled = enabled;
            this.id = id;
        }
    }

    // Row ids are opaque — kept as named constants only so onItemClick stays
    // readable when stages 2..6 fill them in with real presentFragment calls.
    private static final int ID_GIFTS = 10;
    private static final int ID_VISUAL_PROFILE = 20;
    private static final int ID_GHOST = 30;
    private static final int ID_APPEARANCE = 40;
    private static final int ID_CASINO_BRIDGE = 50;

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
            if (!row.enabled) return;
            // Stage 1: no subscreens are built yet. Each stage flips the matching
            // row's `enabled` flag to true and presents its own fragment here.
        });

        rebuildRows();
        return fragmentView;
    }

    private void rebuildRows() {
        rows.clear();

        rows.add(new Row(VIEW_TYPE_HEADER, "Подарки", false, 0));
        rows.add(new Row(VIEW_TYPE_TEXT, "Подарки и балансы (этап 2)", false, ID_GIFTS));
        rows.add(new Row(VIEW_TYPE_INFO,
                "Локальные Stars, TON и рубли, коллекция, маркет и история — будут добавлены в этапе 2.",
                false, 0));

        rows.add(new Row(VIEW_TYPE_HEADER, "Профиль", false, 0));
        rows.add(new Row(VIEW_TYPE_TEXT, "Визуальный +888 и рейтинг (этап 3)", false, ID_VISUAL_PROFILE));
        rows.add(new Row(VIEW_TYPE_INFO,
                "Переключатель визуального номера и редактор рейтинга — этап 3.",
                false, 0));

        rows.add(new Row(VIEW_TYPE_HEADER, "Чаты и Ghost", false, 0));
        rows.add(new Row(VIEW_TYPE_TEXT, "Ghost и настройки чатов (этап 4)", false, ID_GHOST));
        rows.add(new Row(VIEW_TYPE_INFO,
                "Прочтения, онлайн, набор, автоофлайн, удалённые сообщения — этап 4.",
                false, 0));

        rows.add(new Row(VIEW_TYPE_HEADER, "Внешний вид", false, 0));
        rows.add(new Row(VIEW_TYPE_TEXT, "Пресеты и иконки (этап 5)", false, ID_APPEARANCE));
        rows.add(new Row(VIEW_TYPE_INFO,
                "Пресеты Standard/Glass/Compact и смена иконки — этап 5.",
                false, 0));

        rows.add(new Row(VIEW_TYPE_HEADER, "Casino bridge", false, 0));
        rows.add(new Row(VIEW_TYPE_TEXT, "Casino bridge (этап 6)", false, ID_CASINO_BRIDGE));
        rows.add(new Row(VIEW_TYPE_INFO,
                "Отрисовка подарков из личного casino-бота — этап 6.",
                false, 0));
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
                v = new TextCell(ctx);
            }
            return new RecyclerListView.Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            boolean divider = position + 1 < rows.size()
                    && rows.get(position + 1).viewType == row.viewType;
            if (row.viewType == VIEW_TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText(row.text);
            } else if (row.viewType == VIEW_TYPE_INFO) {
                ((TextInfoPrivacyCell) holder.itemView).setText(row.text);
            } else {
                TextCell cell = (TextCell) holder.itemView;
                cell.setText(row.text, divider);
                cell.setEnabled(row.enabled, false);
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
            int type = holder.getItemViewType();
            if (type == VIEW_TYPE_HEADER || type == VIEW_TYPE_INFO) return false;
            int pos = holder.getAdapterPosition();
            if (pos < 0 || pos >= rows.size()) return false;
            return rows.get(pos).enabled;
        }
    }
}
