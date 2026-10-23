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
import org.telegram.pampgram.core.PampGramStorage;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Local-only transaction history for all PampGram balance changes
// (purchase / sale / transfer / top-up / manual adjustment) in all three
// currencies (Stars, TON, Rubles). Reads straight from PampGramStorage —
// nothing crosses the network.
public final class PampGramLedgerScreen extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_INFO = 2;

    private final List<Row> rows = new ArrayList<>();

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("d MMM yyyy, HH:mm", new Locale("ru"));

    private static final class Row {
        final int viewType;
        final String title;
        final String subtitle;

        Row(int viewType, String title, String subtitle) {
            this.viewType = viewType;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("История операций");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
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

        rebuildRows();
        return fragmentView;
    }

    private void rebuildRows() {
        rows.clear();
        List<PampGramStorage.HistoryEntry> entries =
                PampGramStorage.getInstance().getHistoryEntries(null, 200);
        if (entries.isEmpty()) {
            rows.add(new Row(VIEW_TYPE_INFO, null,
                    "История пуста. Любое изменение балансов или подарков появится здесь."));
            return;
        }

        rows.add(new Row(VIEW_TYPE_HEADER, "Последние операции", null));
        for (PampGramStorage.HistoryEntry e : entries) {
            String amount = formatAmount(e.currency, e.amount);
            String kind = labelFor(e.type);
            String when = DATE_FMT.format(new Date(e.timestamp));
            String subtitle = kind + " · " + when;
            if (e.note != null && !e.note.isEmpty()) {
                subtitle = subtitle + "\n" + e.note;
            }
            rows.add(new Row(VIEW_TYPE_TEXT, amount, subtitle));
        }
        rows.add(new Row(VIEW_TYPE_INFO, null,
                "Все записи локальные и видны только на этом устройстве."));
    }

    private static String labelFor(String type) {
        if (type == null) return "Операция";
        switch (type) {
            case PampGramStorage.HISTORY_PURCHASE:    return "Покупка";
            case PampGramStorage.HISTORY_SALE:        return "Продажа";
            case PampGramStorage.HISTORY_TRANSFER_IN: return "Поступление";
            case PampGramStorage.HISTORY_TRANSFER_OUT:return "Передача";
            case PampGramStorage.HISTORY_TOP_UP:      return "Пополнение";
            case PampGramStorage.HISTORY_ADJUSTMENT:  return "Коррекция";
            default:                                   return type;
        }
    }

    private static String formatAmount(String currency, long amount) {
        String sign = amount >= 0 ? "+" : "−";
        long abs = Math.abs(amount);
        if (PampGramStorage.CURRENCY_STARS.equals(currency)) {
            return sign + " " + PampGramGiftsScreen.formatStars(abs);
        } else if (PampGramStorage.CURRENCY_TON.equals(currency)) {
            return sign + " " + PampGramGiftsScreen.formatTon(abs);
        } else if (PampGramStorage.CURRENCY_RUB.equals(currency)) {
            return sign + " " + PampGramGiftsScreen.formatRubles(abs);
        }
        return sign + " " + abs + " " + currency;
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
                cell.heightDp = 56;
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
                ((TextInfoPrivacyCell) holder.itemView).setText(row.subtitle);
            } else {
                TextCell cell = (TextCell) holder.itemView;
                boolean divider = position + 1 < rows.size()
                        && rows.get(position + 1).viewType == VIEW_TYPE_TEXT;
                cell.setText(row.title, divider);
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
            return holder.getItemViewType() == VIEW_TYPE_TEXT;
        }
    }
}
