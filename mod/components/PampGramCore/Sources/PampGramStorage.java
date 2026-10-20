package org.telegram.pampgram.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.List;

// Local PampGram data store. Everything here is on-device only; nothing in this
// file ever talks to Telegram's own servers or storage. Mirrors the shape of the
// iOS PampGramCore (visual balances, history, visual profile toggles) so later
// stages can port the Swift logic one-to-one.
//
// Backed by a plain SQLiteOpenHelper rather than Room: Telegram-Android already
// uses the stdlib SQLite the same way (see MessagesStorage), so there is no
// gradle change to pull in. Public API is intentionally small — ValueHolder
// classes are nested, DB access goes through singleton methods.
public final class PampGramStorage {

    public static final String DB_NAME = "pampgram.db";
    public static final int DB_VERSION = 1;

    public static final String CURRENCY_STARS = "stars";
    public static final String CURRENCY_TON = "ton";
    public static final String CURRENCY_RUB = "rub";

    public static final String HISTORY_PURCHASE = "purchase";
    public static final String HISTORY_SALE = "sale";
    public static final String HISTORY_TRANSFER_IN = "transfer_in";
    public static final String HISTORY_TRANSFER_OUT = "transfer_out";
    public static final String HISTORY_TOP_UP = "top_up";
    public static final String HISTORY_ADJUSTMENT = "adjustment";

    private static volatile PampGramStorage instance;

    public static PampGramStorage getInstance() {
        PampGramStorage local = instance;
        if (local == null) {
            synchronized (PampGramStorage.class) {
                local = instance;
                if (local == null) {
                    instance = local = new PampGramStorage(ApplicationLoader.applicationContext);
                }
            }
        }
        return local;
    }

    private final Helper helper;

    private PampGramStorage(Context context) {
        this.helper = new Helper(context.getApplicationContext());
    }

    // ----- Visual balances -------------------------------------------------

    public long getVisualBalance(@NonNull String currency) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query("visual_balance",
                new String[]{"amount"}, "currency = ?",
                new String[]{currency}, null, null, null)) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    public void setVisualBalance(@NonNull String currency, long amount) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("currency", currency);
        cv.put("amount", amount);
        db.insertWithOnConflict("visual_balance", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ----- History ---------------------------------------------------------

    public static final class HistoryEntry {
        public final long id;
        public final String type;
        public final String currency;
        public final long amount;
        public final @Nullable String counterparty;
        public final long timestamp;
        public final @Nullable String note;

        public HistoryEntry(long id, String type, String currency, long amount,
                            @Nullable String counterparty, long timestamp,
                            @Nullable String note) {
            this.id = id;
            this.type = type;
            this.currency = currency;
            this.amount = amount;
            this.counterparty = counterparty;
            this.timestamp = timestamp;
            this.note = note;
        }
    }

    public long addHistoryEntry(@NonNull String type, @NonNull String currency, long amount,
                                @Nullable String counterparty, @Nullable String note) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("type", type);
        cv.put("currency", currency);
        cv.put("amount", amount);
        cv.put("counterparty", counterparty);
        cv.put("timestamp", System.currentTimeMillis());
        cv.put("note", note);
        return db.insert("history", null, cv);
    }

    public List<HistoryEntry> getHistoryEntries(@Nullable String currencyOrNull, int limit) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String where = currencyOrNull == null ? null : "currency = ?";
        String[] whereArgs = currencyOrNull == null ? null : new String[]{currencyOrNull};
        List<HistoryEntry> out = new ArrayList<>();
        try (Cursor c = db.query("history",
                new String[]{"id", "type", "currency", "amount", "counterparty", "timestamp", "note"},
                where, whereArgs, null, null, "timestamp DESC",
                limit > 0 ? String.valueOf(limit) : null)) {
            while (c.moveToNext()) {
                out.add(new HistoryEntry(
                        c.getLong(0),
                        c.getString(1),
                        c.getString(2),
                        c.getLong(3),
                        c.isNull(4) ? null : c.getString(4),
                        c.getLong(5),
                        c.isNull(6) ? null : c.getString(6)));
            }
        }
        return out;
    }

    // ----- Visual profile state (plus888, rating) --------------------------

    public static final class VisualProfileState {
        public boolean plus888Enabled;
        public int ratingLevel;   // 1..100
        public int ratingPoints;  // free-form points slider

        public VisualProfileState() {
            this(false, 1, 0);
        }

        public VisualProfileState(boolean plus888Enabled, int ratingLevel, int ratingPoints) {
            this.plus888Enabled = plus888Enabled;
            this.ratingLevel = ratingLevel;
            this.ratingPoints = ratingPoints;
        }
    }

    public VisualProfileState getVisualProfile() {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query("visual_profile",
                new String[]{"plus888", "rating_level", "rating_points"},
                "id = 0", null, null, null, null)) {
            if (c.moveToFirst()) {
                return new VisualProfileState(c.getInt(0) != 0, c.getInt(1), c.getInt(2));
            }
        }
        return new VisualProfileState();
    }

    public void setVisualProfile(@NonNull VisualProfileState state) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", 0);
        cv.put("plus888", state.plus888Enabled ? 1 : 0);
        cv.put("rating_level", Math.max(1, Math.min(100, state.ratingLevel)));
        cv.put("rating_points", state.ratingPoints);
        db.insertWithOnConflict("visual_profile", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ----- Schema ----------------------------------------------------------

    private static final class Helper extends SQLiteOpenHelper {
        Helper(Context ctx) {
            super(ctx, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE visual_balance (" +
                    "currency TEXT PRIMARY KEY, " +
                    "amount INTEGER NOT NULL DEFAULT 0)");
            db.execSQL("CREATE TABLE history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "type TEXT NOT NULL, " +
                    "currency TEXT NOT NULL, " +
                    "amount INTEGER NOT NULL, " +
                    "counterparty TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "note TEXT)");
            db.execSQL("CREATE INDEX idx_history_time ON history(timestamp DESC)");
            db.execSQL("CREATE INDEX idx_history_currency ON history(currency, timestamp DESC)");
            db.execSQL("CREATE TABLE visual_profile (" +
                    "id INTEGER PRIMARY KEY, " +
                    "plus888 INTEGER NOT NULL DEFAULT 0, " +
                    "rating_level INTEGER NOT NULL DEFAULT 1, " +
                    "rating_points INTEGER NOT NULL DEFAULT 0)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Stage 1 is version 1; later stages append migrations here. The
            // mod never DROPs user data on upgrade — rename-and-copy only.
        }
    }
}
