package com.emergencysupply.rations.data

import com.emergencysupply.rations.data.local.entity.InventoryItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CategoryPreset(
    val name: String,
    val icon: String
)

data class PlannedRationTemplate(
    val id: String,
    val name: String,
    val category: String,
    val unit: String,
    val location: String,
    val defaultQty: Double,
    val parLevel: Double,
    val servingsPerUnit: Double,
    val defaultMonthsExpiry: Long?,
    val notes: String,
    val icon: String
) {
    fun toInventoryItem(targetLocation: String = location, targetQty: Double = defaultQty): InventoryItem {
        val expiryStr = defaultMonthsExpiry?.let { months ->
            LocalDate.now().plusMonths(months).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        return InventoryItem(
            id = id,
            name = name,
            category = category,
            unit = unit,
            location = targetLocation.ifBlank { location },
            initialQty = targetQty,
            stockIn = 0.0,
            stockOut = 0.0,
            currentQty = targetQty,
            parLevel = parLevel,
            restockNeeded = maxOf(0.0, parLevel - targetQty),
            earliestExpiry = expiryStr,
            daysUntilExpiry = defaultMonthsExpiry?.let { (it * 30).toInt() },
            expirationStatus = if (expiryStr == null) "UNKNOWN" else "GOOD",
            stockStatus = if (targetQty < parLevel) "LOW" else "OK",
            servingsPerUnit = servingsPerUnit,
            totalServings = targetQty * servingsPerUnit,
            notes = notes,
            syncPending = true
        )
    }
}

object PreplannedCatalog {
    val defaultCategories = listOf(
        CategoryPreset("Grains & Pasta", "🌾"),
        CategoryPreset("Proteins & Lentils", "🫘"),
        CategoryPreset("Halal Meats & Freezer", "🍗"),
        CategoryPreset("Canned Goods & Produce", "🥫"),
        CategoryPreset("Baby & Kids (<1 to 3 yrs)", "🍼"),
        CategoryPreset("Dairy & Breakfast", "🥛"),
        CategoryPreset("Water & Drinks", "💧"),
        CategoryPreset("Oils, Spices & Cooking", "🫒"),
        CategoryPreset("Snacks & Bakery", "🍪"),
        CategoryPreset("Essentials & First Aid", "🩹")
    )

    val defaultCategoryNames: List<String> = defaultCategories.map { it.name }

    val standardUnits = listOf(
        "lbs", "cans", "packs", "bottles", "jars", "boxes", "gallons", "bags", "pouches", "kg", "cartons"
    )

    val standardLocations = listOf(
        "Main Pantry",
        "Basement Shelf",
        "Kitchen Cabinet",
        "Deep Freezer",
        "Garage Bins",
        "Closet / Hallway"
    )

    val groceryQuickPicks = listOf(
        "Whole Milk", "Eggs", "Bread", "Bananas", "Yogurt", "Halal Chicken Breast",
        "Basmati Rice", "Whole Wheat Atta", "Canned Tuna", "Infant Formula",
        "Diapers & Wipes", "Bottled Water", "Olive Oil", "Red Lentils", "Onions", "Potatoes"
    )

    val groceryPurposeLists: List<String> = listOf(
        "All Lists",
        "Weekly Groceries",
        "Costco",
        "Halal Market",
        "Grocery Store",
        "Target",
        "Emergency Resupply",
        "Travel / Kit"
    )

    val plannedRations = listOf(
        // Grains & Pasta
        PlannedRationTemplate("item-basmati-rice", "Basmati Rice", "Grains & Pasta", "lbs", "Main Pantry", 20.0, 180.0, 10.0, 18, "Long-grain sealed Mylar bags", "🍚"),
        PlannedRationTemplate("item-wheat-atta", "Whole Wheat Flour (Atta)", "Grains & Pasta", "lbs", "Basement Shelf", 20.0, 120.0, 12.0, 6, "Daily rotis & baking", "🌾"),
        PlannedRationTemplate("item-rolled-oats", "Rolled Oats", "Grains & Pasta", "lbs", "Main Pantry", 10.0, 36.0, 10.0, 14, "Breakfast porridge", "🥣"),
        PlannedRationTemplate("item-pasta", "Dry Pasta & Macaroni", "Grains & Pasta", "boxes", "Main Pantry", 8.0, 24.0, 8.0, 24, "Kid-friendly quick meals", "🍝"),
        PlannedRationTemplate("item-all-purpose-flour", "All-Purpose Flour", "Grains & Pasta", "lbs", "Basement Shelf", 10.0, 30.0, 14.0, 12, "Baking & naan", "🌾"),
        PlannedRationTemplate("item-dry-cereal", "Kids Cereal Boxes", "Grains & Pasta", "boxes", "Kitchen Cabinet", 4.0, 10.0, 10.0, 9, "Daily breakfast", "🥣"),

        // Proteins & Lentils
        PlannedRationTemplate("item-red-lentils", "Red Lentils (Masoor Daal)", "Proteins & Lentils", "lbs", "Main Pantry", 10.0, 45.0, 10.0, 22, "Quick protein staple", "🫘"),
        PlannedRationTemplate("item-chana-daal", "Split Chickpeas (Chana Daal)", "Proteins & Lentils", "lbs", "Basement Shelf", 10.0, 30.0, 10.0, 22, "Curries & soups", "🫘"),
        PlannedRationTemplate("item-chickpeas", "Canned Chickpeas", "Proteins & Lentils", "cans", "Basement Shelf", 12.0, 60.0, 3.5, 28, "Ready to heat", "🥫"),
        PlannedRationTemplate("item-kidney-beans", "Canned Red Kidney Beans", "Proteins & Lentils", "cans", "Basement Shelf", 12.0, 36.0, 3.5, 28, "Rajma & chili", "🫘"),
        PlannedRationTemplate("item-canned-tuna", "Canned Tuna", "Proteins & Lentils", "cans", "Kitchen Cabinet", 12.0, 48.0, 2.0, 24, "Sandwiches & lunches", "🐟"),
        PlannedRationTemplate("item-peanut-butter", "Peanut Butter", "Proteins & Lentils", "jars", "Main Pantry", 4.0, 12.0, 28.0, 14, "High calorie protein", "🥜"),

        // Halal Meats & Freezer
        PlannedRationTemplate("item-halal-chicken", "Halal Chicken Breast", "Halal Meats & Freezer", "lbs", "Deep Freezer", 10.0, 30.0, 4.0, 9, "Portioned vacuum packs", "🍗"),
        PlannedRationTemplate("item-halal-ground-beef", "Halal Ground Beef (Keema)", "Halal Meats & Freezer", "lbs", "Deep Freezer", 10.0, 25.0, 4.0, 9, "1-lb freezer packs", "🥩"),
        PlannedRationTemplate("item-halal-lamb", "Halal Lamb / Goat Cubes", "Halal Meats & Freezer", "lbs", "Deep Freezer", 6.0, 15.0, 4.0, 9, "Curry & biryani cuts", "🍖"),
        PlannedRationTemplate("item-frozen-paratha", "Frozen Parathas / Naan", "Halal Meats & Freezer", "packs", "Deep Freezer", 4.0, 10.0, 6.0, 8, "Quick warm flatbreads", "🫓"),
        PlannedRationTemplate("item-frozen-peas-corn", "Frozen Mixed Veggies & Peas", "Halal Meats & Freezer", "bags", "Deep Freezer", 6.0, 14.0, 6.0, 10, "Long freezer life", "🥦"),
        PlannedRationTemplate("item-frozen-berries", "Frozen Mixed Berries", "Halal Meats & Freezer", "bags", "Deep Freezer", 3.0, 8.0, 6.0, 10, "Smoothies & oatmeal", "🫐"),

        // Canned Goods & Produce
        PlannedRationTemplate("item-crushed-tomatoes", "Crushed Tomatoes", "Canned Goods & Produce", "cans", "Basement Shelf", 12.0, 48.0, 4.0, 20, "Curries & pasta sauce", "🍅"),
        PlannedRationTemplate("item-mixed-veg", "Canned Mixed Vegetables", "Canned Goods & Produce", "cans", "Main Pantry", 12.0, 48.0, 3.5, 18, "Peas, carrots & corn", "🥕"),
        PlannedRationTemplate("item-canned-soup", "Chicken / Lentil Canned Soup", "Canned Goods & Produce", "cans", "Main Pantry", 8.0, 24.0, 2.0, 18, "Instant warm meal", "🍲"),
        PlannedRationTemplate("item-canned-fruit", "Canned Peaches & Pears", "Canned Goods & Produce", "cans", "Main Pantry", 8.0, 24.0, 3.0, 20, "Vitamin C for kids", "🍑"),
        PlannedRationTemplate("item-onions", "Yellow / Red Onions", "Canned Goods & Produce", "lbs", "Main Pantry", 10.0, 15.0, 8.0, 1, "Cool dark basket", "🧅"),
        PlannedRationTemplate("item-potatoes", "Russet / Yukon Potatoes", "Canned Goods & Produce", "lbs", "Basement Shelf", 10.0, 20.0, 6.0, 1, "Store away from onions", "🥔"),

        // Baby & Kids
        PlannedRationTemplate("item-infant-formula", "Infant Formula (Stage 1–2)", "Baby & Kids (<1 to 3 yrs)", "cans", "Kitchen Cabinet", 4.0, 18.0, 20.0, 12, "Year 1 supply; rotate", "🍼"),
        PlannedRationTemplate("item-toddler-pouches", "Toddler Fruit & Grain Pouches", "Baby & Kids (<1 to 3 yrs)", "pouches", "Main Pantry", 12.0, 60.0, 1.0, 9, "Quick toddler snacks", "👶"),
        PlannedRationTemplate("item-powdered-milk", "Powdered Whole Milk (Nido)", "Baby & Kids (<1 to 3 yrs)", "lbs", "Basement Shelf", 5.0, 30.0, 15.0, 16, "Shelf-stable kids milk", "🥛"),
        PlannedRationTemplate("item-kids-vitamins", "Kids Multivitamin Gummies", "Baby & Kids (<1 to 3 yrs)", "bottles", "Kitchen Cabinet", 2.0, 4.0, 60.0, 12, "Daily immunity", "💊"),

        // Dairy & Breakfast
        PlannedRationTemplate("item-uht-milk", "Shelf-Stable Milk Cartons", "Dairy & Breakfast", "cartons", "Basement Shelf", 6.0, 18.0, 4.0, 6, "Unopened room-temp", "🥛"),
        PlannedRationTemplate("item-eggs", "Fresh Eggs (Dozen)", "Dairy & Breakfast", "cartons", "Kitchen Cabinet", 2.0, 4.0, 12.0, 1, "Weekly breakfast", "🥚"),
        PlannedRationTemplate("item-butter", "Salted Butter Packs", "Dairy & Breakfast", "boxes", "Deep Freezer", 4.0, 10.0, 16.0, 8, "Freezes cleanly", "🧈"),
        PlannedRationTemplate("item-cheddar-cheese", "Cheddar / Mozzarella Shredded", "Dairy & Breakfast", "bags", "Deep Freezer", 3.0, 8.0, 12.0, 6, "Meals & snacks", "🧀"),
        PlannedRationTemplate("item-honey", "Raw Natural Honey", "Dairy & Breakfast", "jars", "Main Pantry", 2.0, 6.0, 40.0, null, "Never expires", "🍯"),

        // Water & Drinks
        PlannedRationTemplate("item-bottled-water", "Bottled Drinking Water", "Water & Drinks", "gallons", "Garage Bins", 20.0, 90.0, 16.0, 15, "Rotate in cool corner", "💧"),
        PlannedRationTemplate("item-water-filters", "Water Filter Cartridges", "Water & Drinks", "packs", "Closet / Hallway", 2.0, 6.0, 1.0, null, "Gravity clean water", "🚰"),
        PlannedRationTemplate("item-chai-tea", "Black Tea / Chai Leaves", "Water & Drinks", "boxes", "Kitchen Cabinet", 3.0, 8.0, 50.0, 24, "Daily family tea", "☕"),
        PlannedRationTemplate("item-electrolytes", "Oral Hydration / Electrolytes", "Water & Drinks", "boxes", "Kitchen Cabinet", 2.0, 6.0, 12.0, 24, "Fever & dehydration", "🧃"),

        // Oils, Spices & Cooking
        PlannedRationTemplate("item-cooking-oil", "Olive & Cooking Oil", "Oils, Spices & Cooking", "bottles", "Main Pantry", 2.0, 12.0, 30.0, 12, "Keep cool & dark", "🫒"),
        PlannedRationTemplate("item-desi-ghee", "Desi Ghee", "Oils, Spices & Cooking", "jars", "Kitchen Cabinet", 2.0, 10.0, 25.0, 19, "Shelf-stable fat", "🧈"),
        PlannedRationTemplate("item-sea-salt", "Sea Salt & Pink Salt", "Oils, Spices & Cooking", "lbs", "Main Pantry", 4.0, 12.0, 50.0, null, "Keeps indefinitely", "🧂"),
        PlannedRationTemplate("item-sugar", "Pure Cane Sugar", "Oils, Spices & Cooking", "lbs", "Basement Shelf", 10.0, 30.0, 50.0, null, "Sealed dry bucket", "🍬"),
        PlannedRationTemplate("item-spice-kit", "Turmeric, Cumin & Masala Spices", "Oils, Spices & Cooking", "packs", "Kitchen Cabinet", 4.0, 10.0, 30.0, 18, "Core spice refill", "🌶️"),

        // Snacks & Bakery
        PlannedRationTemplate("item-bread", "Whole Wheat Bread Loaves", "Snacks & Bakery", "bags", "Kitchen Cabinet", 2.0, 4.0, 10.0, 1, "Freeze extras", "🍞"),
        PlannedRationTemplate("item-granola-bars", "Granola & Energy Bars", "Snacks & Bakery", "boxes", "Main Pantry", 4.0, 12.0, 8.0, 12, "Bug-out & kids snacks", "🍫"),
        PlannedRationTemplate("item-nuts-dates", "Almonds, Walnuts & Dates", "Snacks & Bakery", "bags", "Main Pantry", 4.0, 12.0, 16.0, 10, "Calorie-dense nutrition", "🌴"),
        PlannedRationTemplate("item-crackers", "Saltine Crackers & Biscuits", "Snacks & Bakery", "boxes", "Main Pantry", 4.0, 10.0, 12.0, 12, "Tummy-gentle snacks", "🍪"),

        // Essentials & First Aid
        PlannedRationTemplate("item-diapers-wipes", "Diapers & Baby Wipes", "Essentials & First Aid", "packs", "Closet / Hallway", 4.0, 16.0, 1.0, null, "Sizes 3–5 supplies", "🧷"),
        PlannedRationTemplate("item-first-aid", "Children's Fever & First Aid Kit", "Essentials & First Aid", "boxes", "Kitchen Cabinet", 1.0, 2.0, 1.0, 12, "Tylenol, Motrin, bandages", "🩹"),
        PlannedRationTemplate("item-toilet-paper", "Toilet Paper & Paper Towels", "Essentials & First Aid", "packs", "Garage Bins", 3.0, 8.0, 1.0, null, "Bulk Costco packs", "🧻"),
        PlannedRationTemplate("item-dish-soap", "Dish Soap & Hand Sanitizer", "Essentials & First Aid", "bottles", "Closet / Hallway", 3.0, 8.0, 1.0, 24, "Sanitation reserve", "🧼"),
        PlannedRationTemplate("item-trash-bags", "Heavy Duty Trash Bags", "Essentials & First Aid", "boxes", "Garage Bins", 2.0, 5.0, 1.0, null, "Sanitation & storage", "🗑️"),
        PlannedRationTemplate("item-batteries", "AA / AAA Batteries & Flashlights", "Essentials & First Aid", "packs", "Closet / Hallway", 2.0, 6.0, 1.0, 60, "Power outage backup", "🔋")
    )

    fun aisleForItem(name: String, category: String = ""): String {
        val n = name.lowercase()
        return when {
            listOf("chicken", "beef", "lamb", "goat", "keema", "meat", "tuna", "fish", "salmon").any { n.contains(it) } ->
                "🥩 Halal Meat & Seafood"
            listOf("rice", "atta", "flour", "wheat", "oat", "cereal", "pasta", "macaroni", "bread", "paratha", "naan").any { n.contains(it) } ->
                "🌾 Bulk Grains & Pasta"
            listOf("milk", "egg", "butter", "ghee", "cheese", "yogurt").any { n.contains(it) } ->
                "🥛 Dairy & Eggs"
            listOf("onion", "potato", "vegetable", "veg", "pea", "berry", "berries", "frozen", "banana", "apple", "fruit").any { n.contains(it) } ->
                "🥬 Fresh & Frozen"
            listOf("tomato", "chickpea", "bean", "lentil", "daal", "soup", "peanut", "almond", "nut", "date", "honey", "cracker", "bar").any { n.contains(it) } ->
                "🥫 Canned & Dry Goods"
            listOf("formula", "pouch", "toddler", "diaper", "wipe", "vitamin").any { n.contains(it) } ->
                "👶 Baby & Kids"
            listOf("water", "tea", "chai", "coffee", "juice", "electrolyte").any { n.contains(it) } ->
                "💧 Water & Drinks"
            listOf("oil", "salt", "sugar", "spice", "masala").any { n.contains(it) } ->
                "🫒 Oils & Spices"
            listOf("aid", "bandage", "tylenol", "paper", "towel", "soap", "sanitizer", "trash", "batter", "flash").any { n.contains(it) } ->
                "🧴 Household & First Aid"
            category.isNotBlank() -> category
            else -> "🛒 General Aisle"
        }
    }

    fun iconForItem(name: String, category: String): String {
        val n = name.lowercase()
        return when {
            // 1. Fruit accuracy: specific fruits before generic berries/fruit
            n.contains("strawberry") || n.contains("strawberries") -> "🍓"
            n.contains("blueberry") || n.contains("blueberries") -> "🫐"
            n.contains("raspberry") || n.contains("raspberries") -> "🍓"
            n.contains("berry") || n.contains("berries") -> "🫐"
            n.contains("banana") -> "🍌"
            n.contains("peach") -> "🍑"
            n.contains("mango") -> "🥭"
            n.contains("orange") || n.contains("mandarin") || n.contains("tangerine") -> "🍊"
            n.contains("lemon") || n.contains("lime") -> "🍋"
            n.contains("grape") || n.contains("raisin") -> "🍇"
            n.contains("cherry") || n.contains("cherries") -> "🍒"
            n.contains("watermelon") || n.contains("melon") -> "🍉"
            n.contains("pineapple") -> "🍍"
            n.contains("avocado") -> "🥑"
            n.contains("pear") -> "🍐"
            n.contains("apple") -> "🍎"
            n.contains("fruit") -> "🍏"

            // 2. Veggies accuracy: sweet potato/yam before potato, specific veggies before generic
            n.contains("sweet potato") || n.contains("yam") -> "🍠"
            n.contains("potato") -> "🥔"
            n.contains("garlic") -> "🧄"
            n.contains("onion") -> "🧅"
            n.contains("corn") -> "🌽"
            n.contains("pepper") || n.contains("jalapeno") || n.contains("chili") -> "🫑"
            n.contains("cucumber") || n.contains("pickle") -> "🥒"
            n.contains("mushroom") -> "🍄"
            n.contains("spinach") || n.contains("lettuce") || n.contains("kale") || n.contains("greens") || n.contains("salad") -> "🥬"
            n.contains("broccoli") -> "🥦"
            n.contains("carrot") -> "🥕"
            n.contains("chickpea") -> "🫘"
            (n.contains("pea") && !n.contains("peanut") && !n.contains("pear")) -> "🫛"
            n.contains("olive") -> "🫒"
            n.contains("tomato") -> "🍅"
            n.contains("veg") -> "🥦"

            // Household essentials checked before short substrings (e.g. toilet before oil)
            n.contains("tissue") || n.contains("paper") || n.contains("towel") || n.contains("toilet") -> "🧻"
            n.contains("tooth") || n.contains("brush") -> "🪥"
            n.contains("soap") || n.contains("detergent") || n.contains("shampoo") || n.contains("clean") || n.contains("sanitizer") -> "🧼"
            n.contains("diaper") || n.contains("wipe") -> "🧷"
            n.contains("formula") -> "🍼"
            n.contains("pouch") || n.contains("toddler") -> "👶"
            n.contains("vitamin") || n.contains("med") || n.contains("tylenol") -> "💊"
            n.contains("aid") || n.contains("bandage") -> "🩹"
            n.contains("trash") -> "🗑️"
            n.contains("batter") || n.contains("flash") -> "🔋"

            // Specific Pantry/Drinks before generic dairy/bakery nuts
            n.contains("coconut") -> "🥥"
            n.contains("water") && n.contains("filter") -> "🚰"
            n.contains("filter") -> "🚰"

            // 3. Proteins, Dairy & Bakery
            n.contains("yogurt") -> "🥣"
            n.contains("ice cream") -> "🍦"
            n.contains("cream cheese") || n.contains("cheese") -> "🧀"
            n.contains("milk") -> "🥛"
            n.contains("shrimp") || n.contains("prawn") -> "🍤"
            n.contains("peanut butter") -> "🥜"
            (n.contains("peanut") || n.contains("almond") || (n.contains("nut") && !n.contains("coconut"))) -> "🥜"
            n.contains("croissant") || n.contains("pastry") -> "🥐"
            n.contains("bagel") -> "🥯"
            n.contains("pancake") || n.contains("waffle") -> "🥞"
            n.contains("naan") || n.contains("pita") || n.contains("flatbread") || n.contains("tortilla") || n.contains("paratha") -> "🫓"
            n.contains("bread") || n.contains("loaf") || n.contains("toast") -> "🍞"
            n.contains("butter") || n.contains("ghee") -> "🧈"
            (n.contains("egg") && !n.contains("veg")) -> "🥚"
            n.contains("chicken") -> "🍗"
            n.contains("beef") || n.contains("steak") || n.contains("keema") || n.contains("burger") -> "🥩"
            n.contains("lamb") || n.contains("goat") || n.contains("mutton") -> "🍖"
            n.contains("tuna") || n.contains("fish") || n.contains("salmon") || n.contains("sardine") -> "🐟"
            n.contains("lentil") || n.contains("daal") || n.contains("bean") || n.contains("chickpea") -> "🫘"
            n.contains("rice") -> "🍚"
            n.contains("pasta") || n.contains("spaghetti") || n.contains("macaroni") || n.contains("noodle") -> "🍝"
            n.contains("atta") || n.contains("flour") || n.contains("wheat") -> "🌾"
            n.contains("oat") || n.contains("cereal") -> "🥣"
            n.contains("cracker") || n.contains("biscuit") || n.contains("cookie") -> "🍪"
            n.contains("bar") || n.contains("chocolate") -> "🍫"
            n.contains("date") -> "🌴"

            // 4. Drinks, Pantry & Cooking
            n.contains("water") -> "💧"
            n.contains("coffee") || n.contains("espresso") -> "☕"
            n.contains("tea") || n.contains("chai") -> "🍵"
            n.contains("soda") || n.contains("pop") || n.contains("cola") -> "🥤"
            n.contains("juice") || n.contains("electrolyte") -> "🧃"
            n.contains("jam") || n.contains("jelly") -> "🍯"
            n.contains("honey") -> "🍯"
            n.contains("ketchup") || n.contains("sauce") || n.contains("salsa") -> "🍅"
            n.contains("vinegar") || (n.contains("oil") && !n.contains("toilet")) -> "🫒"
            n.contains("salt") -> "🧂"
            n.contains("sugar") -> "🍬"
            n.contains("spice") || n.contains("masala") -> "🌶️"
            n.contains("soup") -> "🍲"

            else -> iconForCategory(category)
        }
    }

    fun iconForCategory(category: String): String {
        val lower = category.lowercase()
        return when {
            lower.contains("grain") || lower.contains("rice") || lower.contains("pasta") || lower.contains("flour") -> "🌾"
            lower.contains("halal") || lower.contains("meat") || lower.contains("freezer") -> "🍗"
            lower.contains("protein") || lower.contains("lentil") -> "🫘"
            lower.contains("can") || lower.contains("produce") -> "🥫"
            lower.contains("baby") || lower.contains("kid") -> "🍼"
            lower.contains("dairy") || lower.contains("milk") || lower.contains("breakfast") -> "🥛"
            lower.contains("water") || lower.contains("drink") -> "💧"
            lower.contains("oil") || lower.contains("spice") || lower.contains("ghee") -> "🫒"
            lower.contains("snack") || lower.contains("bakery") || lower.contains("bar") -> "🍪"
            lower.contains("essential") || lower.contains("aid") || lower.contains("med") -> "🩹"
            else -> "📦"
        }
    }
}
