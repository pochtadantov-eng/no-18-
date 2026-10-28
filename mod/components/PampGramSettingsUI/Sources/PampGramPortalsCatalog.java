package org.telegram.pampgram.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Static catalog of fake Portals Market items. Matches the real
// portals-market mini-app's structure: 11 collections, each item carries
// Model/Symbol/Backdrop attributes with rarity percentages, a GRAM price
// and a signature background. All numbers are frozen so every PampGram
// build sees the same market and the UI stays interesting without a
// network layer.
//
// Prices are stored in "nanoGRAM" (1 GRAM = 1_000_000_000 units) so they
// map one-to-one onto PampGramStorage.fakeTonBalanceNanos — the Portals
// screen shows the balance labelled as GRAM but reuses the same backing
// store as the TON balance everywhere else in PampGram.
public final class PampGramPortalsCatalog {

    private PampGramPortalsCatalog() {}

    public static final class Item {
        public final String id;
        public final String collection;    // "Resistance Dog"
        public final int number;           // 3353
        public final String model;         // "Breaker"
        public final int modelPct10;       // tenths of a percent (12 = 1.2%)
        public final String symbol;        // "Crusader"
        public final int symbolPct10;
        public final String backdrop;      // "Moonstone"
        public final int backdropPct10;
        public final long priceNanoGram;   // 469_500_000_000L = 469.5 GRAM
        public final String emoji;         // centerpiece glyph for the icon
        public final int gradientTop;
        public final int gradientBottom;
        public final boolean craftable;    // "Доступно для крафта" ribbon
        public final boolean isNew;        // "NEW" badge in the collection picker

        Item(String id, String collection, int number,
             String model, int modelPct10, String symbol, int symbolPct10,
             String backdrop, int backdropPct10, long priceNanoGram,
             String emoji, int gradientTop, int gradientBottom,
             boolean craftable, boolean isNew) {
            this.id = id;
            this.collection = collection;
            this.number = number;
            this.model = model;
            this.modelPct10 = modelPct10;
            this.symbol = symbol;
            this.symbolPct10 = symbolPct10;
            this.backdrop = backdrop;
            this.backdropPct10 = backdropPct10;
            this.priceNanoGram = priceNanoGram;
            this.emoji = emoji;
            this.gradientTop = gradientTop;
            this.gradientBottom = gradientBottom;
            this.craftable = craftable;
            this.isNew = isNew;
        }

        /** Stars rewarded on a full-price purchase. 100 ✦ per 1 GRAM, same as portals-market. */
        public long rewardStars() {
            return priceNanoGram / 10_000_000L;
        }
    }

    public static final class Collection {
        public final String name;
        public final String emoji;
        public final int tileGradientTop;
        public final int tileGradientBottom;
        public final int supplyLabel;    // e.g. 15800 → "15.8K" badge
        public final long floorNanoGram; // for the picker right column
        public final long volume24hNanoGram;
        public final boolean isNew;

        Collection(String name, String emoji, int tileTop, int tileBottom,
                   int supplyLabel, long floorNanoGram, long volume24hNanoGram, boolean isNew) {
            this.name = name;
            this.emoji = emoji;
            this.tileGradientTop = tileTop;
            this.tileGradientBottom = tileBottom;
            this.supplyLabel = supplyLabel;
            this.floorNanoGram = floorNanoGram;
            this.volume24hNanoGram = volume24hNanoGram;
            this.isNew = isNew;
        }
    }

    // Collection order matches the floor-price sort in the real Portals
    // picker (highest floor first). Gradients are signature per-collection:
    // Resistance Dog stays teal with cross patterns in the real art, Lucky
    // Locket is rose-gold, Heroic Helmet is steel, Swiss Watch is leather
    // brown, Rare Bird is tropical, Sakura is pink mist, Jingle Bells is
    // winter red, Snoop Cigar is purple velvet, Ice Cream is pastel pink,
    // Stellar Rocket is space blue, Pool Float is poolside aqua.
    private static final LinkedHashMap<String, Collection> COLLECTIONS = new LinkedHashMap<>();
    static {
        put(new Collection("Resistance Dog", "🥷", // 🥷
                0xFF1F5B6B, 0xFF0C2A33, 15_800, 469_500_000_000L, 176_500_000_000_000L, true));
        put(new Collection("Heroic Helmet", "🪖", // 🪖
                0xFF2C3E52, 0xFF101822, 8_000, 187_800_000_000L, 2_200_000_000_000L, false));
        put(new Collection("Swiss Watch", "⌚", // ⌚
                0xFF3A2E22, 0xFF17120D, 50_000, 59_500_000_000L, 5_900_000_000_000L, false));
        put(new Collection("Snoop Cigar", "🚬", // 🚬
                0xFF3E2550, 0xFF180F20, 23_000, 39_990_000_000L, 1_400_000_000_000L, false));
        put(new Collection("Rare Bird", "🦜", // 🦜
                0xFF225944, 0xFF0D2219, 15_000, 29_900_000_000L, 2_100_000_000_000L, false));
        put(new Collection("Lucky Locket", "🏵", // 🏵️
                0xFF4A2A3A, 0xFF1C0F17, 66_000, 17_500_000_000L, 6_300_000_000_000L, true));
        put(new Collection("Stellar Rocket", "🚀", // 🚀
                0xFF2E3C66, 0xFF0F1328, 39_000, 15_230_000_000L, 1_800_000_000_000L, false));
        put(new Collection("Sakura Flower", "🌸", // 🌸
                0xFF4F2A3B, 0xFF1D0E17, 100_000, 11_200_000_000L, 1_600_000_000_000L, false));
        put(new Collection("Jingle Bells", "🔔", // 🔔
                0xFF4D1E1E, 0xFF1B0A0A, 200_000, 11_000_000_000L, 959_020_000_000L, false));
        put(new Collection("Pool Float", "🦆", // 🦆
                0xFF3B4C1F, 0xFF161C0C, 201_000, 4_000_000_000L, 420_000_000_000L, false));
        put(new Collection("Ice Cream", "🍦", // 🍦
                0xFF4C2537, 0xFF1C0D15, 275_000, 3_990_000_000L, 320_000_000_000L, false));
    }

    private static void put(Collection c) {
        COLLECTIONS.put(c.name, c);
    }

    public static List<Collection> collections() {
        return new ArrayList<>(COLLECTIONS.values());
    }

    public static Collection collection(String name) {
        return COLLECTIONS.get(name);
    }

    // ----- Catalog items ---------------------------------------------------

    private static final List<Item> ITEMS;
    static {
        List<Item> items = new ArrayList<>();

        // --- Resistance Dog (floor ~469.5 GRAM) ----------------------------
        items.add(mk("rd-3353",  "Resistance Dog", 3353,  "Breaker", 12,  "Crusader", 5,   "Moonstone", 14,       469_500_000_000L, true,  true));
        items.add(mk("rd-12521", "Resistance Dog", 12521, "Rebel",   18,  "Revolver", 9,   "Dark Pistachio", 11,  469_990_000_000L, true,  false));
        items.add(mk("rd-9965",  "Resistance Dog", 9965,  "Nomad",   22,  "Hood",     13,  "Ash Grey", 20,        474_900_000_000L, true,  false));
        items.add(mk("rd-18085", "Resistance Dog", 18085, "Spartan", 17,  "Shield",   8,   "Blood Red", 15,       474_900_000_000L, true,  false));
        items.add(mk("rd-9886",  "Resistance Dog", 9886,  "Vagabond",24,  "Fist",     16,  "Deep Teal", 19,       474_900_000_000L, true,  false));
        items.add(mk("rd-220",   "Resistance Dog", 220,   "King",    2,   "Crown",    1,   "Royal Blue", 3,       1980_000_000_000L, true,  false));

        // --- Heroic Helmet (floor ~187.8) ----------------------------------
        items.add(mk("hh-4420",  "Heroic Helmet", 4420, "Centurion", 14, "Laurel", 10, "Battlefield Dawn", 18, 187_800_000_000L, false, false));
        items.add(mk("hh-77",    "Heroic Helmet", 77,   "Praetorian", 4, "Lion",   3,  "Thunder", 5,          640_000_000_000L, false, false));
        items.add(mk("hh-10211", "Heroic Helmet", 10211,"Berserker", 19, "Sword", 15,  "Snow Peak", 22,        198_400_000_000L, false, false));

        // --- Swiss Watch (floor ~59.5) -------------------------------------
        items.add(mk("sw-512",   "Swiss Watch", 512,   "Chronograph", 8,  "Gear",    6,  "Steel", 11,     59_500_000_000L, false, false));
        items.add(mk("sw-888",   "Swiss Watch", 888,   "Daytona",     3,  "Diamond", 2,  "Gold Strap", 4, 320_000_000_000L, false, false));
        items.add(mk("sw-20244", "Swiss Watch", 20244, "Classic",    20,  "Sapphire",17, "Leather", 24,   62_300_000_000L, false, false));

        // --- Snoop Cigar (floor ~39.99) ------------------------------------
        items.add(mk("sc-23133", "Snoop Cigar", 23133, "OG",         15, "Smoke",  13, "Lounge", 20,       39_990_000_000L, false, false));
        items.add(mk("sc-187",   "Snoop Cigar", 187,   "West Side",   6, "Flame",   4, "Sunset Strip", 8,  180_000_000_000L, false, false));
        items.add(mk("sc-4040",  "Snoop Cigar", 4040,  "Studio",     18, "Mic",    14, "Stage", 21,        41_200_000_000L, false, false));

        // --- Rare Bird (floor ~29.9) ---------------------------------------
        items.add(mk("rb-321",   "Rare Bird", 321,   "Scarlet Macaw", 9,  "Feather", 7, "Rainforest", 12, 29_900_000_000L, false, false));
        items.add(mk("rb-8800",  "Rare Bird", 8800,  "Peacock",      17,  "Nest",   15, "Twilight", 21,   33_500_000_000L, false, false));
        items.add(mk("rb-55",    "Rare Bird", 55,    "Paradise",      3,  "Berry",   2, "Dawn", 5,       128_700_000_000L, false, false));

        // --- Lucky Locket (floor ~17.5) ------------------------------------
        items.add(mk("ll-99",    "Lucky Locket", 99,   "Rose Gold",     5, "Clover",   4, "Rose Blush", 7,     17_500_000_000L, false, true));
        items.add(mk("ll-5513",  "Lucky Locket", 5513, "Secret Garden",19, "Horseshoe",16,"Mint", 22,          18_900_000_000L, false, true));
        items.add(mk("ll-2024",  "Lucky Locket", 2024, "Antique",      14, "Pearl",   11, "Lavender", 18,      19_400_000_000L, false, true));

        // --- Stellar Rocket (floor ~15.23) ---------------------------------
        items.add(mk("sr-39893", "Stellar Rocket", 39893, "Classic",      22, "Thruster", 19, "Launch Pad", 24, 15_230_000_000L, true,  false));
        items.add(mk("sr-137",   "Stellar Rocket", 137,   "Interstellar",  6, "Antenna",   4, "Deep Space", 8,  48_900_000_000L, true,  false));

        // --- Sakura Flower (floor ~11.2) -----------------------------------
        items.add(mk("sf-4096",  "Sakura Flower", 4096,  "Blossom",   15, "Petal",     12, "Pink Mist", 20,  11_200_000_000L, false, false));
        items.add(mk("sf-808",   "Sakura Flower", 808,   "Full Bloom", 8, "Butterfly",  6, "Spring Sky", 10, 14_800_000_000L, false, false));

        // --- Jingle Bells (floor ~11) --------------------------------------
        items.add(mk("jb-999",   "Jingle Bells", 999,   "Golden",   8,  "Snowflake", 6,  "Snowy Night", 10, 11_000_000_000L, false, false));
        items.add(mk("jb-10",    "Jingle Bells", 10,    "Festive",  3,  "Ribbon",    2,  "Hearth", 4,       45_500_000_000L, false, false));
        items.add(mk("jb-5423",  "Jingle Bells", 5423,  "Silver",  22,  "Holly",    19,  "Pine Forest", 25, 11_300_000_000L, false, false));

        // --- Pool Float (floor ~4) -----------------------------------------
        items.add(mk("pf-201657","Pool Float", 201657, "Duck",     21,  "Bubble", 19,  "Poolside", 24, 4_000_000_000L,  false, false));
        items.add(mk("pf-150845","Pool Float", 150845, "Flamingo", 17,  "Palm",   14,  "Beach Day", 20, 4_000_000_000L,  false, false));

        // --- Ice Cream (floor ~3.99) ---------------------------------------
        items.add(mk("ic-275978","Ice Cream", 275978, "Vanilla",  23,  "Sprinkle", 20, "Pastel Pink", 25, 3_990_000_000L, false, false));
        items.add(mk("ic-30011", "Ice Cream", 30011,  "Mint Chip",15,  "Cherry",   12, "Summer Sky", 19,  5_200_000_000L, false, false));

        ITEMS = Collections.unmodifiableList(items);
    }

    private static Item mk(String id, String collection, int number,
                           String model, int modelPct10, String symbol, int symbolPct10,
                           String backdrop, int backdropPct10, long priceNanoGram,
                           boolean craftable, boolean isNew) {
        Collection c = COLLECTIONS.get(collection);
        return new Item(id, collection, number, model, modelPct10, symbol, symbolPct10,
                backdrop, backdropPct10, priceNanoGram, c.emoji, c.tileGradientTop,
                c.tileGradientBottom, craftable, isNew);
    }

    public static List<Item> all() {
        return ITEMS;
    }

    public static List<Item> filter(String collectionOrNull) {
        if (collectionOrNull == null) return ITEMS;
        List<Item> out = new ArrayList<>();
        for (Item it : ITEMS) {
            if (it.collection.equals(collectionOrNull)) out.add(it);
        }
        return out;
    }

    public static Item byId(String id) {
        for (Item it : ITEMS) {
            if (it.id.equals(id)) return it;
        }
        return null;
    }

    // ----- Formatters shared between screens -------------------------------

    /** 469_500_000_000L → "469.5" (drops trailing zeros but keeps up to 2 decimals). */
    public static String formatGram(long nanoGram) {
        double g = nanoGram / 1_000_000_000.0;
        if (Math.abs(g - Math.round(g)) < 0.005) {
            return String.format(java.util.Locale.ROOT, "%d", Math.round(g));
        }
        if (Math.abs(g * 10 - Math.round(g * 10)) < 0.005) {
            return String.format(java.util.Locale.ROOT, "%.1f", g);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", g);
    }

    /** 15800 → "15.8K", 200000 → "200K", 999 → "999", 1_000_000 → "1M". */
    public static String formatCompactSupply(int supply) {
        if (supply >= 1_000_000) {
            double m = supply / 1_000_000.0;
            if (Math.abs(m - Math.round(m)) < 0.05) {
                return String.format(java.util.Locale.ROOT, "%dM", Math.round(m));
            }
            return String.format(java.util.Locale.ROOT, "%.1fM", m);
        }
        if (supply >= 1000) {
            double k = supply / 1000.0;
            if (Math.abs(k - Math.round(k)) < 0.05) {
                return String.format(java.util.Locale.ROOT, "%dK", Math.round(k));
            }
            return String.format(java.util.Locale.ROOT, "%.1fK", k);
        }
        return String.valueOf(supply);
    }

    /** 46950 → "46 950" (NBSP-grouped, matches the Russian format in the UI). */
    public static String formatStars(long stars) {
        String s = Long.toString(Math.abs(stars));
        StringBuilder out = new StringBuilder();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            if (i > 0 && (n - i) % 3 == 0) out.append(' ');
            out.append(s.charAt(i));
        }
        return (stars < 0 ? "-" : "") + out;
    }
}
