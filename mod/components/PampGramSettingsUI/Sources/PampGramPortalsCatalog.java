package org.telegram.pampgram.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Static catalog of fake Portals-style unique gifts shown in the visual
// market. Every PampGram build sees exactly the same 30 items in the same
// order — Portals is a browse-and-buy mini app, nothing here is dynamic or
// network-backed. Prices are in TON nano-units (1 TON = 1_000_000_000) to
// match PampGramStorage.fakeTonBalanceNanos.
public final class PampGramPortalsCatalog {

    private PampGramPortalsCatalog() {}

    public static final class Item {
        public final String id;
        public final String collection;   // "Plush Pepe"
        public final int number;          // 4321
        public final String model;        // "Space Pirate"
        public final int modelRarityPct10;    // tenths of a percent (25 = 2.5%)
        public final String backdrop;     // "Dark Pistachio"
        public final int backdropRarityPct10;
        public final String symbol;       // "Nebula"
        public final int symbolRarityPct10;
        public final long priceNanos;     // 125_000_000_000L = 125 TON
        public final String emoji;        // centerpiece glyph
        public final int gradientTop;
        public final int gradientBottom;
        public final String owner;        // fake owner handle

        Item(String id, String collection, int number, String model, int modelRarityPct10,
             String backdrop, int backdropRarityPct10, String symbol, int symbolRarityPct10,
             long priceNanos, String emoji, int gradientTop, int gradientBottom, String owner) {
            this.id = id;
            this.collection = collection;
            this.number = number;
            this.model = model;
            this.modelRarityPct10 = modelRarityPct10;
            this.backdrop = backdrop;
            this.backdropRarityPct10 = backdropRarityPct10;
            this.symbol = symbol;
            this.symbolRarityPct10 = symbolRarityPct10;
            this.priceNanos = priceNanos;
            this.emoji = emoji;
            this.gradientTop = gradientTop;
            this.gradientBottom = gradientBottom;
            this.owner = owner;
        }
    }

    // Collection → (emoji, gradientTop, gradientBottom). Colors picked to
    // match the "signature" look of each collection on portals-market: Plush
    // Pepe stays green, Durov's Cap stays blue, Precious Peach is peach-pink
    // and so on. Backgrounds are intentionally dark — a Portals card is
    // always on a dark surface regardless of the app theme.
    private static final LinkedHashMap<String, int[]> COLLECTION_STYLE = new LinkedHashMap<>();
    static {
        COLLECTION_STYLE.put("Plush Pepe",      new int[]{0xFF1D4B2A, 0xFF0E1F13});
        COLLECTION_STYLE.put("Durov's Cap",     new int[]{0xFF14314F, 0xFF0A1826});
        COLLECTION_STYLE.put("Precious Peach",  new int[]{0xFF5A2535, 0xFF2A121A});
        COLLECTION_STYLE.put("Jolly Chimp",     new int[]{0xFF4A2B10, 0xFF221307});
        COLLECTION_STYLE.put("Low Rider",       new int[]{0xFF3F1F4F, 0xFF1C0E24});
        COLLECTION_STYLE.put("Snoop Dogg",      new int[]{0xFF1F3F2A, 0xFF0D1C13});
        COLLECTION_STYLE.put("Light Sword",     new int[]{0xFF2A2F55, 0xFF101430});
        COLLECTION_STYLE.put("Toy Bear",        new int[]{0xFF4A2A1E, 0xFF21130C});
        COLLECTION_STYLE.put("Astral Shard",    new int[]{0xFF1A2F55, 0xFF0A1326});
        COLLECTION_STYLE.put("Loot Bag",        new int[]{0xFF4F3B15, 0xFF221A09});
    }

    private static final Map<String, String> COLLECTION_EMOJI;
    static {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put("Plush Pepe",     "🐸");  // 🐸
        m.put("Durov's Cap",    "🎩");  // 🎩
        m.put("Precious Peach", "🍑");  // 🍑
        m.put("Jolly Chimp",    "🙊");  // 🙊
        m.put("Low Rider",      "🏎");  // 🏎
        m.put("Snoop Dogg",     "🐕");  // 🐕
        m.put("Light Sword",    "⚔️"); // ⚔️
        m.put("Toy Bear",       "🧸");  // 🧸
        m.put("Astral Shard",   "💎");  // 💎
        m.put("Loot Bag",       "💰");  // 💰
        COLLECTION_EMOJI = Collections.unmodifiableMap(m);
    }

    /** Collection names, in display order. First entry gets its own filter chip. */
    public static List<String> collections() {
        return new ArrayList<>(COLLECTION_STYLE.keySet());
    }

    private static final List<Item> ITEMS;
    static {
        List<Item> items = new ArrayList<>();

        // Plush Pepe — the headline collection, floor around 2500 TON
        items.add(mk("plush-pepe-1143",   "Plush Pepe",     1143, "Jade Prince",   8,  "Dark Pistachio", 12, "Nebula",       3,  2850L, "amber"));
        items.add(mk("plush-pepe-6621",   "Plush Pepe",     6621, "Space Pirate",  11, "Deep Purple",    15, "Galaxy",       7,  2610L, "mango_fin"));
        items.add(mk("plush-pepe-220",    "Plush Pepe",     220,  "King",          2,  "Scarlet",         9, "Crown",        1,  5200L, "whales_only"));

        // Durov's Cap — floor around 900 TON
        items.add(mk("durovs-cap-4420",   "Durov's Cap",    4420, "Alpine",        14, "Winter Sky",     18, "Snowflake",   22, 1180L, "pavel_fan"));
        items.add(mk("durovs-cap-888",    "Durov's Cap",    888,  "Czar",          3,  "Midnight",        6, "Lion",         4,  3600L, "zero_bb"));
        items.add(mk("durovs-cap-10211",  "Durov's Cap",    10211,"Pilot",         17, "Royal Blue",     21, "Thunder",     19,  960L, "tgstars"));

        // Precious Peach — floor around 600 TON
        items.add(mk("precious-peach-512","Precious Peach", 512,  "Sunset",        9,  "Sorbet",         14, "Hearts",       8,  740L, "sweet_tooth"));
        items.add(mk("precious-peach-77", "Precious Peach", 77,   "Blossom",       4,  "Rose Gold",       5, "Twin Hearts",  2,  1890L, "lucky_7s"));
        items.add(mk("precious-peach-3330","Precious Peach",3330, "Candy",         19, "Peach Fuzz",     24, "Sparkle",     15,  620L, "ddd_club"));

        // Jolly Chimp — floor around 420 TON
        items.add(mk("jolly-chimp-99",    "Jolly Chimp",    99,   "Banana King",   6,  "Jungle",         10, "Peel",         3,  1420L, "monke_dao"));
        items.add(mk("jolly-chimp-5513",  "Jolly Chimp",    5513, "Cheeky",        18, "Savanna",        22, "Palm",        16,  440L, "zoo_keeper"));
        items.add(mk("jolly-chimp-2024",  "Jolly Chimp",    2024, "Shy",           13, "Dusk",           17, "Bananas",     11,  520L, "yr_of_monkey"));

        // Low Rider — floor around 300 TON
        items.add(mk("low-rider-808",     "Low Rider",      808,  "Chrome Dreams", 12, "Neon Night",     16, "Flames",      14,  540L, "cholo_club"));
        items.add(mk("low-rider-66",      "Low Rider",      66,   "Candy Flake",   4,  "Vice Pink",       7, "Dice",         5,  1240L, "route66"));
        items.add(mk("low-rider-2200",    "Low Rider",      2200, "Rust Bucket",   21, "Garage",         25, "Spark",       23,  310L, "street_mech"));

        // Snoop Dogg — floor around 250 TON
        items.add(mk("snoop-dogg-187",    "Snoop Dogg",     187,  "West Side",     10, "Smoke",          14, "Fo' Shizzle", 12,  480L, "dpg_classic"));
        items.add(mk("snoop-dogg-4040",   "Snoop Dogg",     4040, "Doggy Style",   16, "Studio",         20, "Mic",         18,  290L, "deathrow"));
        items.add(mk("snoop-dogg-93",     "Snoop Dogg",     93,   "Gin & Juice",   3,  "Sunset Strip",    6, "Palm",         4,  1670L, "og_fan"));

        // Light Sword — floor around 180 TON
        items.add(mk("light-sword-7001",  "Light Sword",    7001, "Jedi Blue",     11, "Starfield",      15, "Rebel",       13,  260L, "may_the_4th"));
        items.add(mk("light-sword-666",   "Light Sword",    666,  "Sith Red",      5,  "Deep Space",      8, "Empire",       6,  940L, "dark_side"));
        items.add(mk("light-sword-12",    "Light Sword",    12,   "Mace",          2,  "Nebula Purple",   4, "Senator",      3,  2800L, "double_digits"));

        // Toy Bear — floor around 140 TON
        items.add(mk("toy-bear-321",      "Toy Bear",       321,  "Honey",         9,  "Forest Mist",    13, "Honeypot",    11,  210L, "cuddle_club"));
        items.add(mk("toy-bear-8800",     "Toy Bear",       8800, "Panda",         17, "Bamboo",         21, "Shoots",      19,  150L, "zoo_crew"));
        items.add(mk("toy-bear-55",       "Toy Bear",       55,   "Grizzly",       4,  "Snow Peak",       7, "Claw",         5,  780L, "two_fives"));

        // Astral Shard — floor around 110 TON
        items.add(mk("astral-shard-4096", "Astral Shard",   4096, "Prism",         15, "Void",           19, "Spark",       17,  170L, "power_of_2"));
        items.add(mk("astral-shard-137",  "Astral Shard",   137,  "Diamond Core",  6,  "Deep Blue",       9, "Comet",        7,  430L, "fine_struct"));

        // Loot Bag — floor around 90 TON
        items.add(mk("loot-bag-999",      "Loot Bag",       999,  "Golden Haul",   8,  "Treasure Room",  12, "Coin",        10,  220L, "dungeon_master"));
        items.add(mk("loot-bag-10",       "Loot Bag",       10,   "Royal Haul",    3,  "Vault",           5, "Crown",        4,  1510L, "top_ten"));
        items.add(mk("loot-bag-5423",     "Loot Bag",       5423, "Starter",       22, "Dirt Road",      26, "Pebble",      24,   95L, "noob_quest"));

        ITEMS = Collections.unmodifiableList(items);
    }

    private static Item mk(String id, String collection, int number, String model, int modelPct10,
                           String backdrop, int backdropPct10, String symbol, int symbolPct10,
                           long priceTon, String owner) {
        int[] style = COLLECTION_STYLE.get(collection);
        String emoji = COLLECTION_EMOJI.get(collection);
        return new Item(id, collection, number, model, modelPct10, backdrop, backdropPct10,
                symbol, symbolPct10, priceTon * 1_000_000_000L, emoji, style[0], style[1], owner);
    }

    /** All items, in catalog order. */
    public static List<Item> all() {
        return ITEMS;
    }

    /** Items whose collection name matches, or everything if {@code collection} is null. */
    public static List<Item> filter(String collection) {
        if (collection == null) return ITEMS;
        List<Item> out = new ArrayList<>();
        for (Item it : ITEMS) {
            if (it.collection.equals(collection)) out.add(it);
        }
        return out;
    }

    public static Item byId(String id) {
        for (Item it : ITEMS) {
            if (it.id.equals(id)) return it;
        }
        return null;
    }
}
