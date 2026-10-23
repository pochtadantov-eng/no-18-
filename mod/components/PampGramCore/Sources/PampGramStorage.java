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

// Local PampGram data store. Everything here is on-device only; nothing in
// this file ever talks to Telegram's own servers or storage. Mirrors the shape
// of the iOS PampGramCore so later stages port Swift logic one-to-one.
//
// Backed by a plain SQLiteOpenHelper rather than Room: Telegram-Android
// already uses stdlib SQLite the same way (see MessagesStorage), so there is
// no gradle change to pull in. Public API is intentionally small; model
// classes are nested and DB access goes through singleton methods.
public final class PampGramStorage {

    public static final String DB_NAME = "pampgram.db";
    public static final int DB_VERSION = 3;

    public static final String CURRENCY_STARS = "stars";
    public static final String CURRENCY_TON = "ton";
    public static final String CURRENCY_RUB = "rub";

    public static final String HISTORY_PURCHASE = "purchase";
    public static final String HISTORY_SALE = "sale";
    public static final String HISTORY_TRANSFER_IN = "transfer_in";
    public static final String HISTORY_TRANSFER_OUT = "transfer_out";
    public static final String HISTORY_TOP_UP = "top_up";
    public static final String HISTORY_ADJUSTMENT = "adjustment";

    // Mirrors the iOS defaults (PampGramSettings.defaultFakeStarsBalance etc.):
    // a user who just installed the mod opens Gifts and already sees a non-zero
    // visual Stars balance so the UI isn't empty.
    public static final long DEFAULT_FAKE_STARS_BALANCE = 50_000L;
    public static final long DEFAULT_FAKE_TON_BALANCE_NANOS = 0L;
    public static final long DEFAULT_LOCAL_RUBLES_KOPECKS = 0L;

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

    // ----- Visual balances (deprecated flat table, Stage 1) ----------------

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

    // ----- Preferences (singleton row, id = 0) -----------------------------

    public static final class PampGramPreferences {
        public long fakeStarsBalance;         // whole stars
        public long fakeTonBalanceNanos;      // 1 TON = 1e9 nanos (matches iOS: fakeTonBalanceNanos)
        public long localRublesKopecks;       // 1 RUB = 100 kopecks
        public boolean fakeStarsDisplayEnabled;
        public boolean fakeTonDisplayEnabled;
        public boolean localRublesPurchaseEnabled;

        PampGramPreferences() {
            this.fakeStarsBalance = DEFAULT_FAKE_STARS_BALANCE;
            this.fakeTonBalanceNanos = DEFAULT_FAKE_TON_BALANCE_NANOS;
            this.localRublesKopecks = DEFAULT_LOCAL_RUBLES_KOPECKS;
            this.fakeStarsDisplayEnabled = false;
            this.fakeTonDisplayEnabled = false;
            this.localRublesPurchaseEnabled = true;
        }
    }

    public PampGramPreferences getPreferences() {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query("preferences",
                new String[]{"fake_stars", "fake_ton_nanos", "local_rub_kopecks",
                             "fake_stars_show", "fake_ton_show", "local_rub_purchase_on"},
                "id = 0", null, null, null, null)) {
            PampGramPreferences p = new PampGramPreferences();
            if (c.moveToFirst()) {
                p.fakeStarsBalance = c.getLong(0);
                p.fakeTonBalanceNanos = c.getLong(1);
                p.localRublesKopecks = c.getLong(2);
                p.fakeStarsDisplayEnabled = c.getInt(3) != 0;
                p.fakeTonDisplayEnabled = c.getInt(4) != 0;
                p.localRublesPurchaseEnabled = c.getInt(5) != 0;
            }
            return p;
        }
    }

    public void setPreferences(@NonNull PampGramPreferences p) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", 0);
        cv.put("fake_stars", Math.max(0, p.fakeStarsBalance));
        cv.put("fake_ton_nanos", Math.max(0, p.fakeTonBalanceNanos));
        cv.put("local_rub_kopecks", Math.max(0, p.localRublesKopecks));
        cv.put("fake_stars_show", p.fakeStarsDisplayEnabled ? 1 : 0);
        cv.put("fake_ton_show", p.fakeTonDisplayEnabled ? 1 : 0);
        cv.put("local_rub_purchase_on", p.localRublesPurchaseEnabled ? 1 : 0);
        db.insertWithOnConflict("preferences", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Convenience: edit the preferences via a lambda, atomically. */
    public interface PreferencesMutation {
        void apply(PampGramPreferences p);
    }

    public void updatePreferences(@NonNull PreferencesMutation m) {
        PampGramPreferences p = getPreferences();
        m.apply(p);
        setPreferences(p);
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

    // ----- Phantom gifts (Stage 3 Gifts foundation) ------------------------

    public static final class PhantomGift {
        public final long id;
        public final long peerId;
        public final String title;
        public final @Nullable String stickerFileRef;  // whatever cell renderer needs to show it
        public final String giftKind;                  // "generic" | "unique"
        public final @Nullable Integer uniqueNumber;   // #42 for unique gifts
        public final String priceCurrency;             // CURRENCY_STARS / TON / RUB
        public final long priceAmount;                 // native unit for the currency
        public final long dateSec;                     // seconds since epoch
        public final boolean isReceived;               // true for "Подарок мне" / "от него" (no debit)
        public final boolean pinnedToTop;
        public final boolean savedToProfile;
        public final @Nullable Long soldDateSec;
        public final boolean worn;
        public final @Nullable String marketPriceCurrency;
        public final @Nullable Long marketPriceAmount;
        public final @Nullable Long marketListedAtSec;

        public PhantomGift(long id, long peerId, String title, @Nullable String stickerFileRef,
                           String giftKind, @Nullable Integer uniqueNumber,
                           String priceCurrency, long priceAmount, long dateSec,
                           boolean isReceived, boolean pinnedToTop, boolean savedToProfile,
                           @Nullable Long soldDateSec, boolean worn,
                           @Nullable String marketPriceCurrency, @Nullable Long marketPriceAmount,
                           @Nullable Long marketListedAtSec) {
            this.id = id;
            this.peerId = peerId;
            this.title = title;
            this.stickerFileRef = stickerFileRef;
            this.giftKind = giftKind;
            this.uniqueNumber = uniqueNumber;
            this.priceCurrency = priceCurrency;
            this.priceAmount = priceAmount;
            this.dateSec = dateSec;
            this.isReceived = isReceived;
            this.pinnedToTop = pinnedToTop;
            this.savedToProfile = savedToProfile;
            this.soldDateSec = soldDateSec;
            this.worn = worn;
            this.marketPriceCurrency = marketPriceCurrency;
            this.marketPriceAmount = marketPriceAmount;
            this.marketListedAtSec = marketListedAtSec;
        }
    }

    public long addPhantomGift(long peerId, @NonNull String title, @Nullable String stickerFileRef,
                               @NonNull String giftKind, @Nullable Integer uniqueNumber,
                               @NonNull String priceCurrency, long priceAmount,
                               boolean isReceived) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("peer_id", peerId);
        cv.put("title", title);
        cv.put("sticker_file_ref", stickerFileRef);
        cv.put("gift_kind", giftKind);
        if (uniqueNumber != null) cv.put("unique_number", uniqueNumber);
        cv.put("price_currency", priceCurrency);
        cv.put("price_amount", priceAmount);
        cv.put("date_sec", System.currentTimeMillis() / 1000L);
        cv.put("is_received", isReceived ? 1 : 0);
        cv.put("pinned_to_top", 0);
        cv.put("saved_to_profile", 1);
        cv.put("worn", 0);
        return db.insert("phantom_gift", null, cv);
    }

    public List<PhantomGift> listPhantomGifts() {
        return queryPhantomGifts(null, null, "date_sec DESC", 0);
    }

    public List<PhantomGift> listPhantomGiftsForPeer(long peerId, boolean savedOnly) {
        String where = savedOnly ? "peer_id = ? AND saved_to_profile = 1 AND sold_date_sec IS NULL"
                                  : "peer_id = ?";
        return queryPhantomGifts(where, new String[]{String.valueOf(peerId)},
                "pinned_to_top DESC, date_sec DESC", 0);
    }

    public List<PhantomGift> listMarketPhantomGifts() {
        return queryPhantomGifts("market_price_amount IS NOT NULL AND sold_date_sec IS NULL",
                null, "market_listed_at_sec DESC", 0);
    }

    public void updatePhantomGiftFlags(long id, boolean pinnedToTop, boolean savedToProfile,
                                       boolean worn) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("pinned_to_top", pinnedToTop ? 1 : 0);
        cv.put("saved_to_profile", savedToProfile ? 1 : 0);
        cv.put("worn", worn ? 1 : 0);
        db.update("phantom_gift", cv, "id = ?", new String[]{String.valueOf(id)});
    }

    public void markPhantomGiftSold(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("sold_date_sec", System.currentTimeMillis() / 1000L);
        cv.put("saved_to_profile", 0);
        cv.put("pinned_to_top", 0);
        cv.put("worn", 0);
        cv.putNull("market_price_amount");
        cv.putNull("market_price_currency");
        cv.putNull("market_listed_at_sec");
        db.update("phantom_gift", cv, "id = ?", new String[]{String.valueOf(id)});
    }

    public void removePhantomGift(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete("phantom_gift", "id = ?", new String[]{String.valueOf(id)});
    }

    public int countPhantomGiftsForSelf(long selfPeerId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM phantom_gift WHERE peer_id = ? AND saved_to_profile = 1 AND sold_date_sec IS NULL",
                new String[]{String.valueOf(selfPeerId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private List<PhantomGift> queryPhantomGifts(@Nullable String where, @Nullable String[] whereArgs,
                                                @NonNull String orderBy, int limit) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<PhantomGift> out = new ArrayList<>();
        try (Cursor c = db.query("phantom_gift",
                new String[]{"id", "peer_id", "title", "sticker_file_ref", "gift_kind",
                             "unique_number", "price_currency", "price_amount", "date_sec",
                             "is_received", "pinned_to_top", "saved_to_profile", "sold_date_sec",
                             "worn", "market_price_currency", "market_price_amount",
                             "market_listed_at_sec"},
                where, whereArgs, null, null, orderBy, limit > 0 ? String.valueOf(limit) : null)) {
            while (c.moveToNext()) {
                out.add(new PhantomGift(
                        c.getLong(0),
                        c.getLong(1),
                        c.getString(2),
                        c.isNull(3) ? null : c.getString(3),
                        c.getString(4),
                        c.isNull(5) ? null : c.getInt(5),
                        c.getString(6),
                        c.getLong(7),
                        c.getLong(8),
                        c.getInt(9) != 0,
                        c.getInt(10) != 0,
                        c.getInt(11) != 0,
                        c.isNull(12) ? null : c.getLong(12),
                        c.getInt(13) != 0,
                        c.isNull(14) ? null : c.getString(14),
                        c.isNull(15) ? null : c.getLong(15),
                        c.isNull(16) ? null : c.getLong(16)));
            }
        }
        return out;
    }

    // ----- Key-value preferences (Stage 3b: section screens) ---------------
    //
    // Rather than adding a column per toggle to `preferences`, every section's
    // free-form settings go into `kv_prefs(key, value)`. Each key is a string
    // constant declared in the screen class that owns it (see KEY_* in the
    // section fragments). Values are serialized as strings: booleans as "1"/"0",
    // ints as Long.toString(), floats same way, enums as their chosen string.

    public boolean getKvBool(@NonNull String key, boolean fallback) {
        String s = getKvString(key, null);
        if (s == null) return fallback;
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }

    public long getKvLong(@NonNull String key, long fallback) {
        String s = getKvString(key, null);
        if (s == null) return fallback;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public @Nullable String getKvString(@NonNull String key, @Nullable String fallback) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query("kv_prefs", new String[]{"value"},
                "key = ?", new String[]{key}, null, null, null)) {
            if (c.moveToFirst()) return c.isNull(0) ? fallback : c.getString(0);
        }
        return fallback;
    }

    public void setKvBool(@NonNull String key, boolean value) {
        setKvString(key, value ? "1" : "0");
    }

    public void setKvLong(@NonNull String key, long value) {
        setKvString(key, Long.toString(value));
    }

    public void setKvString(@NonNull String key, @Nullable String value) {
        SQLiteDatabase db = helper.getWritableDatabase();
        if (value == null) {
            db.delete("kv_prefs", "key = ?", new String[]{key});
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", value);
        db.insertWithOnConflict("kv_prefs", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
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
            createV1Tables(db);
            createV2Tables(db);
            createV3Tables(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // The mod never DROPs user data on upgrade — add tables and
            // ALTER-add columns only. Each stage bumps DB_VERSION by 1 and
            // appends its migration block below.
            if (oldVersion < 2) {
                createV2Tables(db);
            }
            if (oldVersion < 3) {
                createV3Tables(db);
            }
        }

        private static void createV1Tables(SQLiteDatabase db) {
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

        private static void createV2Tables(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS preferences (" +
                    "id INTEGER PRIMARY KEY, " +
                    "fake_stars INTEGER NOT NULL DEFAULT " + DEFAULT_FAKE_STARS_BALANCE + ", " +
                    "fake_ton_nanos INTEGER NOT NULL DEFAULT 0, " +
                    "local_rub_kopecks INTEGER NOT NULL DEFAULT 0, " +
                    "fake_stars_show INTEGER NOT NULL DEFAULT 0, " +
                    "fake_ton_show INTEGER NOT NULL DEFAULT 0, " +
                    "local_rub_purchase_on INTEGER NOT NULL DEFAULT 1)");

            db.execSQL("CREATE TABLE IF NOT EXISTS phantom_gift (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "peer_id INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "sticker_file_ref TEXT, " +
                    "gift_kind TEXT NOT NULL, " +      // 'generic' | 'unique'
                    "unique_number INTEGER, " +         // #N for unique gifts
                    "price_currency TEXT NOT NULL, " +
                    "price_amount INTEGER NOT NULL, " +
                    "date_sec INTEGER NOT NULL, " +
                    "is_received INTEGER NOT NULL DEFAULT 0, " +
                    "pinned_to_top INTEGER NOT NULL DEFAULT 0, " +
                    "saved_to_profile INTEGER NOT NULL DEFAULT 1, " +
                    "sold_date_sec INTEGER, " +
                    "worn INTEGER NOT NULL DEFAULT 0, " +
                    "market_price_currency TEXT, " +
                    "market_price_amount INTEGER, " +
                    "market_listed_at_sec INTEGER)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_phantom_gift_peer " +
                    "ON phantom_gift(peer_id, saved_to_profile, sold_date_sec)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_phantom_gift_date " +
                    "ON phantom_gift(date_sec DESC)");
        }

        private static void createV3Tables(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS kv_prefs (" +
                    "key TEXT PRIMARY KEY, " +
                    "value TEXT)");
        }
    }
}
