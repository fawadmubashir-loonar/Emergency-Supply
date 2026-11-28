package com.emergencysupply.rations.data

data class BundleItemSpec(
    val name: String,
    val qty: Double,
    val unit: String,
    val category: String,
    val store: String = "Grocery Store"
)

data class SupplyBundle(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val items: List<BundleItemSpec>
)

object BundleCatalog {
    val bundle72HourBugOut = SupplyBundle(
        id = "bundle-72h-bugout",
        title = "72-Hour Bug-Out Kit",
        subtitle = "Essential 3-day survival nutrition & rapid-evacuation readiness kit",
        icon = "🎒",
        items = listOf(
            BundleItemSpec("Bottled Drinking Water", 6.0, "gallons", "Water & Drinks", "Costco"),
            BundleItemSpec("Granola & Energy Bars", 3.0, "boxes", "Snacks & Bakery", "Costco"),
            BundleItemSpec("Canned Tuna", 6.0, "cans", "Proteins & Lentils", "Grocery Store"),
            BundleItemSpec("Saltine Crackers & Biscuits", 2.0, "boxes", "Snacks & Bakery", "Grocery Store"),
            BundleItemSpec("Peanut Butter", 2.0, "jars", "Proteins & Lentils", "Grocery Store"),
            BundleItemSpec("Oral Hydration / Electrolytes", 2.0, "boxes", "Water & Drinks", "Target"),
            BundleItemSpec("Children's Fever & First Aid Kit", 1.0, "boxes", "Essentials & First Aid", "Target"),
            BundleItemSpec("AA / AAA Batteries & Flashlights", 2.0, "packs", "Essentials & First Aid", "Target")
        )
    )

    val bundleWeeklyBiryani = SupplyBundle(
        id = "bundle-weekly-biryani",
        title = "Weekly Biryani & Halal Family Pack",
        subtitle = "Complete halal meats, aged basmati rice, desi ghee & authentic spices",
        icon = "🍗",
        items = listOf(
            BundleItemSpec("Basmati Rice", 10.0, "lbs", "Grains & Pasta", "Halal Market"),
            BundleItemSpec("Halal Chicken Breast", 6.0, "lbs", "Halal Meats & Freezer", "Halal Market"),
            BundleItemSpec("Halal Lamb / Goat Cubes", 4.0, "lbs", "Halal Meats & Freezer", "Halal Market"),
            BundleItemSpec("Desi Ghee", 2.0, "jars", "Oils, Spices & Cooking", "Halal Market"),
            BundleItemSpec("Turmeric, Cumin & Masala Spices", 2.0, "packs", "Oils, Spices & Cooking", "Halal Market"),
            BundleItemSpec("Yellow / Red Onions", 5.0, "lbs", "Canned Goods & Produce", "Grocery Store"),
            BundleItemSpec("Whole Milk", 2.0, "gallons", "Dairy & Breakfast", "Grocery Store"),
            BundleItemSpec("Black Tea / Chai Leaves", 1.0, "boxes", "Water & Drinks", "Halal Market")
        )
    )

    val bundleWinterOutage = SupplyBundle(
        id = "bundle-winter-outage",
        title = "Winter Power Outage Reserve",
        subtitle = "No-cook ready rations, shelf-stable dairy, illumination & warming staples",
        icon = "❄️",
        items = listOf(
            BundleItemSpec("Bottled Drinking Water", 10.0, "gallons", "Water & Drinks", "Costco"),
            BundleItemSpec("Shelf-Stable Milk Cartons", 4.0, "cartons", "Dairy & Breakfast", "Costco"),
            BundleItemSpec("Chicken / Lentil Canned Soup", 8.0, "cans", "Canned Goods & Produce", "Grocery Store"),
            BundleItemSpec("Canned Chickpeas", 6.0, "cans", "Proteins & Lentils", "Grocery Store"),
            BundleItemSpec("Canned Peaches & Pears", 4.0, "cans", "Canned Goods & Produce", "Grocery Store"),
            BundleItemSpec("Almonds, Walnuts & Dates", 3.0, "bags", "Snacks & Bakery", "Costco"),
            BundleItemSpec("AA / AAA Batteries & Flashlights", 3.0, "packs", "Essentials & First Aid", "Target"),
            BundleItemSpec("Heavy Duty Trash Bags", 1.0, "boxes", "Essentials & First Aid", "Costco")
        )
    )

    val bundleBabyToddler = SupplyBundle(
        id = "bundle-baby-toddler",
        title = "Baby & Toddler Care Kit",
        subtitle = "Formula, toddler pouches, pediatric hydration, diapers & multivitamin care",
        icon = "🍼",
        items = listOf(
            BundleItemSpec("Infant Formula (Stage 1–2)", 3.0, "cans", "Baby & Kids (<1 to 3 yrs)", "Target"),
            BundleItemSpec("Toddler Fruit & Grain Pouches", 16.0, "pouches", "Baby & Kids (<1 to 3 yrs)", "Target"),
            BundleItemSpec("Powdered Whole Milk (Nido)", 2.0, "lbs", "Baby & Kids (<1 to 3 yrs)", "Costco"),
            BundleItemSpec("Diapers & Baby Wipes", 3.0, "packs", "Essentials & First Aid", "Target"),
            BundleItemSpec("Kids Multivitamin Gummies", 2.0, "bottles", "Baby & Kids (<1 to 3 yrs)", "Target"),
            BundleItemSpec("Children's Fever & First Aid Kit", 1.0, "boxes", "Essentials & First Aid", "Target"),
            BundleItemSpec("Oral Hydration / Electrolytes", 2.0, "boxes", "Water & Drinks", "Target")
        )
    )

    val bundleQuickBreakfast = SupplyBundle(
        id = "bundle-quick-breakfast",
        title = "Quick Breakfast & Pantry Staples",
        subtitle = "Whole grains, rotis & naan flour, pure honey, morning chai & wholesome oats",
        icon = "🥞",
        items = listOf(
            BundleItemSpec("Rolled Oats", 5.0, "lbs", "Grains & Pasta", "Costco"),
            BundleItemSpec("Whole Wheat Flour (Atta)", 10.0, "lbs", "Grains & Pasta", "Halal Market"),
            BundleItemSpec("Shelf-Stable Milk Cartons", 4.0, "cartons", "Dairy & Breakfast", "Costco"),
            BundleItemSpec("Raw Natural Honey", 2.0, "jars", "Dairy & Breakfast", "Costco"),
            BundleItemSpec("Peanut Butter", 2.0, "jars", "Proteins & Lentils", "Grocery Store"),
            BundleItemSpec("Kids Cereal Boxes", 3.0, "boxes", "Grains & Pasta", "Grocery Store"),
            BundleItemSpec("Black Tea / Chai Leaves", 2.0, "boxes", "Water & Drinks", "Halal Market"),
            BundleItemSpec("Pure Cane Sugar", 5.0, "lbs", "Oils, Spices & Cooking", "Costco")
        )
    )

    val allBundles: List<SupplyBundle> = listOf(
        bundle72HourBugOut,
        bundleWeeklyBiryani,
        bundleWinterOutage,
        bundleBabyToddler,
        bundleQuickBreakfast
    )

    fun getBundleById(id: String): SupplyBundle? {
        return allBundles.find { it.id.equals(id, ignoreCase = true) }
    }
}
