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
    val notes: String
) {
    fun toInventoryItem(): InventoryItem {
        val expiryStr = defaultMonthsExpiry?.let { months ->
            LocalDate.now().plusMonths(months).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        return InventoryItem(
            id = id,
            name = name,
            category = category,
            unit = unit,
            location = location,
            initialQty = defaultQty,
            stockIn = 0.0,
            stockOut = 0.0,
            currentQty = defaultQty,
            parLevel = parLevel,
            restockNeeded = maxOf(0.0, parLevel - defaultQty),
            earliestExpiry = expiryStr,
            daysUntilExpiry = defaultMonthsExpiry?.let { (it * 30).toInt() },
            expirationStatus = if (expiryStr == null) "UNKNOWN" else "GOOD",
            stockStatus = if (defaultQty < parLevel) "LOW" else "OK",
            servingsPerUnit = servingsPerUnit,
            totalServings = defaultQty * servingsPerUnit,
            notes = notes,
            syncPending = true
        )
    }
}

object PreplannedCatalog {
    val defaultCategories = listOf(
        CategoryPreset("Grains & Pasta", "🌾"),
        CategoryPreset("Proteins & Lentils", "🫘"),
        CategoryPreset("Canned Goods & Produce", "🥫"),
        CategoryPreset("Baby & Kids (<1 to 3 yrs)", "🍼"),
        CategoryPreset("Water & Drinks", "💧"),
        CategoryPreset("Oils, Spices & Cooking", "🫒"),
        CategoryPreset("Essentials & First Aid", "🩹")
    )

    val defaultCategoryNames: List<String> = defaultCategories.map { it.name }

    val standardUnits = listOf(
        "lbs", "cans", "packs", "bottles", "jars", "boxes", "gallons", "bags", "pouches", "kg"
    )

    val standardLocations = listOf(
        "Main Pantry",
        "Basement Shelf",
        "Kitchen Cabinet",
        "Garage Bins",
        "Deep Freezer",
        "Closet / Hallway"
    )

    val groceryQuickPicks = listOf(
        "Milk", "Eggs", "Bread", "Fresh Fruit", "Yogurt", "Chicken (Halal)",
        "Basmati Rice", "Whole Wheat Atta", "Canned Tuna", "Infant Formula",
        "Diapers & Wipes", "Bottled Water", "Olive Oil", "Red Lentils"
    )

    val plannedRations = listOf(
        PlannedRationTemplate("item-basmati-rice", "Basmati Rice", "Grains & Pasta", "lbs", "Main Pantry", 20.0, 180.0, 10.0, 18, "Long-grain, sealed in Mylar bags for family dinners"),
        PlannedRationTemplate("item-wheat-atta", "Whole Wheat Flour (Atta)", "Grains & Pasta", "lbs", "Basement Shelf", 20.0, 120.0, 12.0, 6, "Daily rotis & baking; rotate older bags first"),
        PlannedRationTemplate("item-rolled-oats", "Rolled Oats", "Grains & Pasta", "lbs", "Main Pantry", 10.0, 36.0, 10.0, 14, "Breakfast for kids & soft toddler porridge"),
        PlannedRationTemplate("item-red-lentils", "Red Lentils (Masoor Daal)", "Proteins & Lentils", "lbs", "Main Pantry", 10.0, 45.0, 10.0, 22, "Quick-cooking protein staple for all ages"),
        PlannedRationTemplate("item-chickpeas", "Canned Chickpeas", "Proteins & Lentils", "cans", "Basement Shelf", 12.0, 60.0, 3.5, 28, "Low sodium, ready to heat"),
        PlannedRationTemplate("item-canned-tuna", "Canned Tuna", "Proteins & Lentils", "cans", "Kitchen Cabinet", 12.0, 48.0, 2.0, 24, "Protein staple for sandwiches & lunches"),
        PlannedRationTemplate("item-crushed-tomatoes", "Crushed Tomatoes", "Canned Goods & Produce", "cans", "Basement Shelf", 12.0, 48.0, 4.0, 20, "Base for curries, soups & pasta sauces"),
        PlannedRationTemplate("item-mixed-veg", "Canned Mixed Vegetables", "Canned Goods & Produce", "cans", "Main Pantry", 12.0, 48.0, 3.5, 18, "Peas, carrots & sweet corn"),
        PlannedRationTemplate("item-infant-formula", "Infant Formula (Stage 1–2)", "Baby & Kids (<1 to 3 yrs)", "cans", "Kitchen Cabinet", 4.0, 18.0, 20.0, 12, "Year 1 baby supply; rotate closely"),
        PlannedRationTemplate("item-toddler-pouches", "Toddler Fruit & Grain Pouches", "Baby & Kids (<1 to 3 yrs)", "pouches", "Main Pantry", 12.0, 60.0, 1.0, 9, "Quick toddler snacks"),
        PlannedRationTemplate("item-powdered-milk", "Powdered Whole Milk", "Baby & Kids (<1 to 3 yrs)", "lbs", "Basement Shelf", 5.0, 30.0, 15.0, 16, "For all kids as baby grows"),
        PlannedRationTemplate("item-bottled-water", "Bottled Drinking Water", "Water & Drinks", "gallons", "Garage Bins", 20.0, 90.0, 16.0, 15, "Stored off concrete floor, shaded corner"),
        PlannedRationTemplate("item-water-filters", "Water Filter Cartridges", "Water & Drinks", "packs", "Closet / Hallway", 2.0, 6.0, 1.0, null, "Gravity filter replacements for clean water"),
        PlannedRationTemplate("item-cooking-oil", "Olive & Cooking Oil", "Oils, Spices & Cooking", "bottles", "Main Pantry", 2.0, 12.0, 30.0, 12, "Keep cool & dark; rotate oldest bottle first"),
        PlannedRationTemplate("item-desi-ghee", "Desi Ghee", "Oils, Spices & Cooking", "jars", "Kitchen Cabinet", 2.0, 10.0, 25.0, 19, "Shelf-stable cooking fat & calorie booster"),
        PlannedRationTemplate("item-sea-salt", "Sea Salt", "Oils, Spices & Cooking", "lbs", "Main Pantry", 4.0, 12.0, 50.0, null, "Iodized & pink salt, keeps indefinitely"),
        PlannedRationTemplate("item-diapers-wipes", "Diapers & Baby Wipes", "Essentials & First Aid", "packs", "Closet / Hallway", 4.0, 16.0, 1.0, null, "Sizes 3–5 essentials"),
        PlannedRationTemplate("item-first-aid", "Children's Fever & First Aid Kit", "Essentials & First Aid", "boxes", "Kitchen Cabinet", 1.0, 2.0, 1.0, 12, "Children's fever meds & bandages")
    )

    fun iconForCategory(category: String): String {
        val lower = category.lowercase()
        return when {
            lower.contains("grain") || lower.contains("rice") || lower.contains("pasta") || lower.contains("flour") -> "🌾"
            lower.contains("protein") || lower.contains("meat") || lower.contains("halal") || lower.contains("lentil") -> "🫘"
            lower.contains("can") || lower.contains("produce") -> "🥫"
            lower.contains("baby") || lower.contains("kid") || lower.contains("dairy") || lower.contains("milk") -> "🍼"
            lower.contains("water") || lower.contains("drink") -> "💧"
            lower.contains("oil") || lower.contains("spice") || lower.contains("ghee") -> "🫒"
            lower.contains("essential") || lower.contains("aid") || lower.contains("med") -> "🩹"
            lower.contains("snack") || lower.contains("bar") -> "🍫"
            else -> "📦"
        }
    }
}
