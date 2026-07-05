package com.sunitha.fittrack.data.local

data class IndianFood(
    val name: String,
    val category: String,
    val serving: String,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val fiberG: Float = 0f
)

object IndianFoodData {

    val foods: List<IndianFood> = listOf(

        // ── Grains & Rotis ────────────────────────────────────────────────────
        IndianFood("Roti (Whole Wheat)",     "Grains", "1 piece (30g)",       80,  3f,   15f,  1f,   2f),
        IndianFood("Chapati",                "Grains", "1 piece (30g)",       85,  3f,   16f,  2f,   2f),
        IndianFood("Paratha (Plain)",        "Grains", "1 piece (80g)",      260,  5f,   38f, 10f,   2f),
        IndianFood("Aloo Paratha",           "Grains", "1 piece (120g)",     320,  6f,   50f, 12f,   3f),
        IndianFood("Rice (Cooked)",          "Grains", "1 cup (150g)",       195,  4f,   43f,  0.4f, 0.5f),
        IndianFood("Brown Rice (Cooked)",    "Grains", "1 cup (150g)",       215,  5f,   44f,  2f,   3.5f),
        IndianFood("Poha",                   "Grains", "1 cup (100g)",       245,  4f,   52f,  3f,   1f),
        IndianFood("Upma",                   "Grains", "1 cup (150g)",       280,  7f,   48f,  7f,   2f),
        IndianFood("Khichdi",                "Grains", "1 bowl (200g)",      240,  9f,   42f,  5f,   4f),
        IndianFood("Puri",                   "Grains", "1 piece (30g)",      130,  2f,   15f,  7f,   0.5f),
        IndianFood("Tamarind Rice",          "Grains", "1 cup (150g)",       250,  4f,   46f,  6f,   1f),
        IndianFood("Lemon Rice",             "Grains", "1 cup (150g)",       220,  4f,   42f,  5f,   1f),
        IndianFood("Tomato Rice",            "Grains", "1 cup (150g)",       230,  4f,   43f,  5f,   1f),
        IndianFood("Coconut Rice",           "Grains", "1 cup (150g)",       280,  4f,   42f, 10f,   1f),
        IndianFood("Curd Rice",              "Grains", "1 cup (200g)",       210,  6f,   34f,  5f,   1f),

        // ── Dal & Legumes ─────────────────────────────────────────────────────
        IndianFood("Dal Tadka",              "Dal", "1 bowl (200g)",         230, 13f,   30f,  7f,   8f),
        IndianFood("Dal Makhani",            "Dal", "1 bowl (200g)",         280, 14f,   32f, 11f,  10f),
        IndianFood("Rajma",                  "Dal", "1 bowl (200g)",         220, 15f,   35f,  2f,  13f),
        IndianFood("Chole",                  "Dal", "1 bowl (200g)",         270, 14f,   40f,  6f,  12f),
        IndianFood("Moong Dal",              "Dal", "1 bowl (200g)",         210, 14f,   30f,  3f,   8f),
        IndianFood("Sambar",                 "Dal", "1 bowl (200g)",         100,  5f,   18f,  2f,   4f),
        IndianFood("Chana Dal",              "Dal", "1 bowl (200g)",         220, 15f,   34f,  3f,  10f),
        IndianFood("Sprouts (Mixed)",        "Dal", "1 cup (100g)",           80,  6f,   13f,  0.5f, 1.5f),
        IndianFood("Rasam",                  "Dal", "1 bowl (200ml)",         50,  2f,    8f,  1f,   1f),

        // ── South Indian ─────────────────────────────────────────────────────
        IndianFood("Idli",                   "South Indian", "2 pieces (100g)",  130,  4f,  26f,  1f,  1f),
        IndianFood("Dosa (Plain)",           "South Indian", "1 piece (80g)",    170,  4f,  34f,  3f,  1f),
        IndianFood("Masala Dosa",            "South Indian", "1 piece (180g)",   320,  7f,  56f,  9f,  3f),
        IndianFood("Adai",                   "South Indian", "1 piece (120g)",   210, 10f,  32f,  5f,  4f),
        IndianFood("Pesaretu",               "South Indian", "1 piece (100g)",   160,  8f,  26f,  3f,  3f),
        IndianFood("Appam",                  "South Indian", "1 piece (80g)",    130,  3f,  25f,  2f,  1f),
        IndianFood("Puttu",                  "South Indian", "1 serving (150g)", 200,  4f,  38f,  4f,  3f),
        IndianFood("Medu Vada",              "South Indian", "1 piece (50g)",    155,  5f,  17f,  8f,  1.5f),
        IndianFood("Uttapam",                "South Indian", "1 piece (120g)",   200,  6f,  36f,  4f,  2f),
        IndianFood("Ven Pongal",             "South Indian", "1 bowl (200g)",    240,  8f,  38f,  7f,  2f),
        IndianFood("Kootu",                  "South Indian", "1 bowl (200g)",    180,  7f,  20f,  8f,  5f),
        IndianFood("Poriyal",                "South Indian", "1 bowl (150g)",    120,  3f,  12f,  6f,  3f),
        IndianFood("Avial",                  "South Indian", "1 bowl (200g)",    160,  4f,  15f,  9f,  4f),
        IndianFood("Keerai Masiyal",         "South Indian", "1 bowl (150g)",     80,  5f,   8f,  3f,  3f),
        IndianFood("Murukku",                "South Indian", "3 pieces (30g)",   150,  2f,  18f,  7f,  0.5f),
        IndianFood("Papadum (Roasted)",      "South Indian", "1 piece (10g)",     35,  2f,   5f,  1f,  0.5f),
        IndianFood("Papadum (Fried)",        "South Indian", "1 piece (10g)",     55,  2f,   6f,  3f,  0.5f),

        // ── Paneer & Dairy ────────────────────────────────────────────────────
        IndianFood("Paneer (Raw)",           "Dairy", "100g",                265, 18f,   3f, 21f,  0f),
        IndianFood("Paneer Bhurji",          "Dairy", "1 serving (150g)",    280, 20f,   6f, 20f,  0.5f),
        IndianFood("Palak Paneer",           "Dairy", "1 bowl (200g)",       250, 16f,  12f, 17f,  2f),
        IndianFood("Paneer Butter Masala",   "Dairy", "1 bowl (200g)",       330, 14f,  18f, 24f,  1f),
        IndianFood("Paneer Tikka",           "Dairy", "6 pieces (150g)",     290, 22f,   8f, 20f,  1f),
        IndianFood("Dahi (Full Fat)",        "Dairy", "1 cup (200g)",        120,  8f,  14f,  4f,  0f),
        IndianFood("Dahi (Low Fat)",         "Dairy", "1 cup (200g)",         80,  8f,  12f,  1f,  0f),
        IndianFood("Raita",                  "Dairy", "1 cup (150g)",         90,  5f,  10f,  3f,  0.5f),
        IndianFood("Milk (Full Fat)",        "Dairy", "1 cup (240ml)",       150,  8f,  12f,  8f,  0f),
        IndianFood("Milk (Toned)",           "Dairy", "1 cup (240ml)",       110,  8f,  12f,  3f,  0f),
        IndianFood("Lassi (Sweet)",          "Dairy", "1 glass (250ml)",     200,  6f,  34f,  5f,  0f),
        IndianFood("Buttermilk (Chaas)",     "Dairy", "1 glass (250ml)",      50,  3f,   6f,  1f,  0f),
        IndianFood("Greek Yogurt",           "Dairy", "1 cup (150g)",        100, 10f,   4f,  5f,  0f),
        IndianFood("Cheese Slice",           "Dairy", "1 slice (20g)",        60,  4f,   1f,  5f,  0f),

        // ── Fats & Oils ───────────────────────────────────────────────────────
        IndianFood("Ghee",                   "Fats & Oils", "1 tsp (5g)",     44,  0f,   0f,  5f,  0f),
        IndianFood("Ghee",                   "Fats & Oils", "1 tbsp (14g)",  123,  0f,   0f, 14f,  0f),
        IndianFood("Butter",                 "Fats & Oils", "1 tsp (5g)",     36,  0f,   0f,  4f,  0f),
        IndianFood("Coconut Oil",            "Fats & Oils", "1 tsp (5ml)",    40,  0f,   0f,  4.5f,0f),
        IndianFood("Sunflower Oil",          "Fats & Oils", "1 tsp (5ml)",    40,  0f,   0f,  4.5f,0f),
        IndianFood("Olive Oil",              "Fats & Oils", "1 tsp (5ml)",    40,  0f,   0f,  4.5f,0f),
        IndianFood("Sesame Oil",             "Fats & Oils", "1 tsp (5ml)",    40,  0f,   0f,  4.5f,0f),

        // ── Eggs ─────────────────────────────────────────────────────────────
        IndianFood("Boiled Egg",             "Eggs", "1 large",               70,  6f,   0.5f, 5f,  0f),
        IndianFood("Fried Egg",              "Eggs", "1 large",               90,  6f,   0.5f, 7f,  0f),
        IndianFood("Poached Egg",            "Eggs", "1 large",               72,  6f,   0.5f, 5f,  0f),
        IndianFood("Scrambled Eggs",         "Eggs", "2 eggs (cooked)",      180, 13f,   1f,  14f,  0f),
        IndianFood("Egg Whites",             "Eggs", "3 whites",              51, 11f,   0.5f, 0.2f,0f),
        IndianFood("Egg Bhurji",             "Eggs", "2 eggs + spices",      220, 15f,   4f,  16f,  0.5f),
        IndianFood("Omelette (2 egg)",       "Eggs", "1 serving",            180, 13f,   2f,  13f,  0f),

        // ── Non-Veg ───────────────────────────────────────────────────────────
        IndianFood("Chicken Breast (Grilled)", "Non-Veg", "100g",            165, 31f,   0f,   3.6f,0f),
        IndianFood("Chicken Curry",          "Non-Veg", "1 serving (200g)",  350, 35f,   8f,  20f,  1f),
        IndianFood("Butter Chicken",         "Non-Veg", "1 serving (200g)",  380, 33f,  14f,  23f,  1f),
        IndianFood("Chicken Tikka",          "Non-Veg", "6 pieces (150g)",   250, 35f,   5f,  11f,  1f),
        IndianFood("Mutton Curry",           "Non-Veg", "1 serving (200g)",  380, 30f,   8f,  26f,  1f),
        IndianFood("Fish Curry",             "Non-Veg", "1 serving (200g)",  280, 32f,   6f,  14f,  0.5f),
        IndianFood("Tuna (Canned)",          "Non-Veg", "1 can (185g)",      200, 43f,   0f,   2f,  0f),

        // ── Vegetables ───────────────────────────────────────────────────────
        IndianFood("Aloo Gobi",             "Vegetables", "1 bowl (200g)",   200,  5f,  30f,  8f,  4f),
        IndianFood("Bhindi Masala",         "Vegetables", "1 bowl (200g)",   150,  4f,  18f,  7f,  5f),
        IndianFood("Baingan Bharta",        "Vegetables", "1 bowl (200g)",   130,  4f,  18f,  5f,  4f),
        IndianFood("Saag",                  "Vegetables", "1 bowl (200g)",   160,  7f,  18f,  7f,  4f),
        IndianFood("Mix Veg",               "Vegetables", "1 bowl (200g)",   120,  4f,  20f,  3f,  4f),

        // ── Bread & Cereals ───────────────────────────────────────────────────
        IndianFood("White Bread",           "Bread & Cereals", "1 slice (28g)",  70,  2.5f, 13f, 1f,   0.6f),
        IndianFood("Whole Wheat Bread",     "Bread & Cereals", "1 slice (28g)",  80,  4f,   14f, 1.5f, 1.9f),
        IndianFood("Multigrain Bread",      "Bread & Cereals", "1 slice (28g)",  75,  3.5f, 13f, 1.5f, 1.5f),
        IndianFood("White Toast",           "Bread & Cereals", "1 slice (28g)",  75,  2.5f, 14f, 1f,   0.6f),
        IndianFood("Butter Toast",          "Bread & Cereals", "1 slice + 1 tsp butter", 111, 2.5f, 14f, 5f, 0.6f),
        IndianFood("Cornflakes (Dry)",      "Bread & Cereals", "1 cup (30g)",   110,  2f,   25f, 0.3f, 0.9f),
        IndianFood("Cornflakes with Milk",  "Bread & Cereals", "1 bowl (30g + 200ml milk)", 220, 10f, 37f, 3.3f, 0.9f),
        IndianFood("Wheat Flakes",          "Bread & Cereals", "1 cup (30g)",   105,  3f,   22f, 0.5f, 2f),
        IndianFood("Oats (Plain Dry)",      "Bread & Cereals", "½ cup (50g)",   190,  6.5f, 33f, 3.5f, 4f),
        IndianFood("Oats with Water",       "Bread & Cereals", "1 bowl (50g oats)", 150, 5f, 27f, 3f,  4f),
        IndianFood("Oats with Milk",        "Bread & Cereals", "1 bowl (50g + 200ml milk)", 300, 13f, 39f, 7f, 4f),
        IndianFood("Muesli",                "Bread & Cereals", "½ cup (45g)",   165,  5f,   30f, 4f,   3f),
        IndianFood("Granola",               "Bread & Cereals", "½ cup (50g)",   220,  5f,   35f, 7f,   2.5f),

        // ── Pasta & Noodles ───────────────────────────────────────────────────
        IndianFood("Pasta (Plain Cooked)",  "International", "1 cup (140g)",   220,  8f,  43f,  1.5f, 2.5f),
        IndianFood("Pasta with Tomato Sauce", "International", "1 bowl (250g)", 290, 10f, 52f,  5f,   3.5f),
        IndianFood("Noodles (Cooked)",      "International", "1 cup (140g)",   210,  5f,  40f,  2f,   1.5f),
        IndianFood("Instant Noodles",       "International", "1 packet (85g)", 380,  8f,  52f, 15f,   2f),

        // ── Beverages ─────────────────────────────────────────────────────────
        IndianFood("Black Coffee",          "Beverages", "1 cup (240ml)",       5,  0.3f,  1f,  0f,  0f),
        IndianFood("Coffee with Milk",      "Beverages", "1 cup (240ml)",      60,  3f,    6f,  2.5f,0f),
        IndianFood("Filter Coffee",         "Beverages", "1 cup (150ml)",      90,  3f,   12f,  3f,  0f),
        IndianFood("Masala Chai",           "Beverages", "1 cup (150ml)",      80,  2.5f, 12f,  2f,  0f),
        IndianFood("Black Tea",             "Beverages", "1 cup (240ml)",       5,  0f,    1f,  0f,  0f),
        IndianFood("Green Tea",             "Beverages", "1 cup (240ml)",       3,  0f,    0.5f,0f,  0f),
        IndianFood("Hot Chocolate",         "Beverages", "1 cup (240ml)",     200,  7f,   30f,  6f,  1f),
        IndianFood("Coconut Water",         "Beverages", "1 glass (250ml)",    50,  0.5f, 12f,  0.5f,0f),
        IndianFood("Fresh Lime Juice",      "Beverages", "1 glass (240ml)",    20,  0.5f,  5f,  0f,  0f),
        IndianFood("Orange Juice",          "Beverages", "1 glass (240ml)",   110,  1.5f, 26f,  0.5f,0.5f),
        IndianFood("Mango Juice",           "Beverages", "1 glass (240ml)",   160,  1f,   40f,  0.5f,0.5f),

        // ── Nuts & Seeds ──────────────────────────────────────────────────────
        IndianFood("Cashews",              "Nuts & Seeds", "20 pieces (28g)",  160,  5f,   9f, 13f,  0.9f),
        IndianFood("Walnuts",              "Nuts & Seeds", "14 halves (28g)",  185,  4f,   4f, 18f,  1.9f),
        IndianFood("Pistachios",           "Nuts & Seeds", "49 kernels (28g)", 160,  6f,   8f, 13f,  3f),
        IndianFood("Peanuts (Roasted)",    "Nuts & Seeds", "30g",              175,  8f,   5f, 15f,  2.4f),
        IndianFood("Almonds",              "Nuts & Seeds", "20 pieces (28g)",  162,  6f,   6f, 14f,  3.5f),
        IndianFood("Mixed Nuts",           "Nuts & Seeds", "30g",              175,  5f,   7f, 15f,  2f),
        IndianFood("Peanut Butter",        "Nuts & Seeds", "2 tbsp (32g)",     190,  8f,   6f, 16f,  1.5f),
        IndianFood("Chana (Roasted)",      "Nuts & Seeds", "30g",              110,  7f,  17f,  2f,  4f),
        IndianFood("Flaxseeds",            "Nuts & Seeds", "1 tbsp (10g)",      55,  2f,   3f,  4f,  2.8f),
        IndianFood("Chia Seeds",           "Nuts & Seeds", "1 tbsp (12g)",      58,  2f,   5f,  4f,  4f),

        // ── Fruits ───────────────────────────────────────────────────────────
        IndianFood("Banana",               "Fruits", "1 medium (120g)",       105,  1.3f, 27f,  0.4f, 3.1f),
        IndianFood("Apple",                "Fruits", "1 medium",               95,  0.5f, 25f,  0.3f, 4.4f),
        IndianFood("Orange",               "Fruits", "1 medium",               62,  1.2f, 15f,  0.2f, 3.1f),
        IndianFood("Mango",                "Fruits", "100g",                   65,  0.8f, 17f,  0.3f, 1.6f),
        IndianFood("Watermelon",           "Fruits", "1 cup (150g)",           45,  0.9f, 11f,  0.2f, 0.6f),
        IndianFood("Grapes",               "Fruits", "1 cup (100g)",           70,  0.7f, 18f,  0.2f, 0.9f),
        IndianFood("Papaya",               "Fruits", "1 cup (140g)",           60,  0.7f, 15f,  0.4f, 1.8f),
        IndianFood("Pomegranate",          "Fruits", "½ cup seeds (87g)",      72,  1.5f, 16f,  1f,   3.5f),
        IndianFood("Dates",                "Fruits", "2 pieces (24g)",          66,  0.4f, 18f,  0.1f, 1.6f),

        // ── Snacks & Junk ─────────────────────────────────────────────────────
        IndianFood("Potato Chips",         "Junk Food", "1 small bag (30g)",  155,  2f,   14f, 10f,  1f),
        IndianFood("Kurkure",              "Junk Food", "30g",                148,  2f,   18f,  7f,  0.5f),
        IndianFood("Popcorn (Plain)",      "Junk Food", "1 cup (30g)",        110,  3.5f, 22f,  1.5f, 3.5f),
        IndianFood("Samosa",               "Junk Food", "1 piece (80g)",      265,  5f,   34f, 12f,  2f),
        IndianFood("Vada Pav",             "Junk Food", "1 piece",            290,  7f,   44f, 10f,  2f),
        IndianFood("Pakoda",               "Junk Food", "4 pieces (60g)",     180,  4f,   22f,  9f,  2f),

        // ── Biscuits ─────────────────────────────────────────────────────────
        IndianFood("Marie Biscuits",       "Biscuits", "2 pieces (14g)",       62,  1f,   11f,  1.5f, 0.3f),
        IndianFood("Digestive Biscuits",   "Biscuits", "1 piece (14g)",        68,  1f,   10f,  2.8f, 0.8f),
        IndianFood("Good Day Biscuits",    "Biscuits", "2 pieces (17g)",       80,  1.2f, 11f,  3.5f, 0.3f),
        IndianFood("Parle-G",              "Biscuits", "4 pieces (23g)",      107,  1.7f, 17f,  3.5f, 0.3f),
        IndianFood("Bourbon",              "Biscuits", "2 pieces (24g)",      110,  1.5f, 16f,  4.5f, 0.5f),

        // ── Sweets & Desserts ─────────────────────────────────────────────────
        IndianFood("Dark Chocolate",       "Sweets", "30g (70%+ cocoa)",      170,  2f,   13f, 12f,  3f),
        IndianFood("Milk Chocolate",       "Sweets", "30g",                   160,  2f,   20f,  8f,  0.5f),
        IndianFood("Gulab Jamun",          "Sweets", "1 piece (50g)",         175,  3f,   30f,  5f,  0.5f),
        IndianFood("Rasgulla",             "Sweets", "1 piece (60g)",         110,  3f,   22f,  1f,  0f),
        IndianFood("Kheer",                "Sweets", "1 bowl (200g)",         280,  7f,   45f,  8f,  0.5f),
        IndianFood("Gajar Halwa",          "Sweets", "1 bowl (150g)",         280,  5f,   40f, 12f,  2f),
        IndianFood("Semolina Halwa",       "Sweets", "1 bowl (150g)",         320,  5f,   48f, 12f,  1f),
        IndianFood("Mysore Pak",           "Sweets", "1 piece (40g)",         195,  3f,   24f, 10f,  0.5f),
        IndianFood("Besan Ladoo",          "Sweets", "1 piece (50g)",         220,  5f,   28f, 10f,  2f),
        IndianFood("Barfi",                "Sweets", "1 piece (40g)",         175,  4f,   24f,  8f,  0.5f),
        IndianFood("Coconut Barfi",        "Sweets", "1 piece (30g)",         140,  2f,   20f,  6f,  1.5f),
        IndianFood("Jalebi",               "Sweets", "2 pieces (60g)",        200,  1f,   45f,  5f,  0f),
        IndianFood("Ice Cream",            "Sweets", "1 scoop (80g)",         160,  3f,   20f,  8f,  0f),
        IndianFood("Payasam",              "Sweets", "1 bowl (150g)",         220,  5f,   35f,  7f,  0.5f),

        // ── Protein ───────────────────────────────────────────────────────────
        IndianFood("Whey Protein (Water)", "Protein", "1 scoop (30g)",        120, 24f,    3f,  2f,  0f),
        IndianFood("Whey Protein (Milk)",  "Protein", "1 scoop + 1 cup milk", 270, 32f,   15f,  9f,  0f)
    )

    fun search(query: String): List<IndianFood> {
        if (query.isBlank()) return foods
        val q = query.trim().lowercase()
        return foods.filter {
            it.name.lowercase().contains(q) || it.category.lowercase().contains(q)
        }
    }

    val categories: List<String>
        get() = foods.map { it.category }.distinct().sorted()
}
