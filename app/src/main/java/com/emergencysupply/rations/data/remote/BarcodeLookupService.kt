package com.emergencysupply.rations.data.remote

import android.util.Log
import com.emergencysupply.rations.data.PreplannedCatalog
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ScannedProductInfo(
    val barcode: String,
    val name: String,
    val category: String,
    val unit: String,
    val defaultQty: Double = 1.0,
    val notes: String = ""
)

class BarcodeLookupService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val gson: Gson = Gson()
) {
    companion object {
        private const val TAG = "BarcodeLookupService"
        private const val BASE_URL = "https://world.openfoodfacts.org/api/v2/product"
        private const val USER_AGENT = "EmergencySupplyRations - Android - Version 1.8.0"
    }

    suspend fun lookupBarcode(barcode: String): ScannedProductInfo? = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return@withContext null

        val url = "$BASE_URL/$cleanBarcode.json?fields=product_name,brands,quantity,categories_tags"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "OpenFoodFacts API error for barcode $cleanBarcode: HTTP ${response.code}")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                val rootObj = try {
                    gson.fromJson(responseBody, JsonObject::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse JSON for barcode $cleanBarcode", e)
                    return@withContext null
                }

                val status = rootObj.get("status")?.asInt ?: 0
                if (status != 1) {
                    Log.i(TAG, "Product not found on OpenFoodFacts for barcode: $cleanBarcode")
                    return@withContext null
                }

                val product = rootObj.getAsJsonObject("product") ?: return@withContext null
                val rawName = product.get("product_name")?.asString?.trim().orEmpty()
                val brands = product.get("brands")?.asString?.trim().orEmpty()
                val quantity = product.get("quantity")?.asString?.trim().orEmpty()
                val categoriesTags = mutableListOf<String>()
                product.getAsJsonArray("categories_tags")?.forEach { elem ->
                    if (elem.isJsonPrimitive) {
                        categoriesTags.add(elem.asString)
                    }
                }

                val productName = buildProductName(rawName, brands, cleanBarcode)
                val category = resolveCategory(productName, categoriesTags)
                val unit = resolveUnit(quantity, productName)
                val notes = buildNotes(brands, quantity)

                ScannedProductInfo(
                    barcode = cleanBarcode,
                    name = productName,
                    category = category,
                    unit = unit,
                    defaultQty = 1.0,
                    notes = notes
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network or parsing exception during barcode lookup for $cleanBarcode", e)
            null
        }
    }

    private fun buildProductName(rawName: String, brand: String, barcode: String): String {
        return when {
            rawName.isNotBlank() && brand.isNotBlank() && !rawName.contains(brand, ignoreCase = true) -> {
                "$brand $rawName"
            }
            rawName.isNotBlank() -> rawName
            brand.isNotBlank() -> "$brand Item ($barcode)"
            else -> "Scanned Product ($barcode)"
        }
    }

    private fun resolveCategory(name: String, tags: List<String>): String {
        val combinedText = (name + " " + tags.joinToString(" ")).lowercase()

        return when {
            combinedText.contains("formula") || combinedText.contains("infant") || combinedText.contains("baby") ||
                combinedText.contains("toddler") || combinedText.contains("diaper") || combinedText.contains("wipes") ||
                combinedText.contains("pouch") -> "Baby & Kids (<1 to 3 yrs)"

            combinedText.contains("halal") || combinedText.contains("chicken") || combinedText.contains("beef") ||
                combinedText.contains("mutton") || combinedText.contains("lamb") || combinedText.contains("goat") ||
                combinedText.contains("keema") || combinedText.contains("meat") || combinedText.contains("freezer") -> "Halal Meats & Freezer"

            combinedText.contains("water") || combinedText.contains("tea") || combinedText.contains("chai") ||
                combinedText.contains("coffee") || combinedText.contains("juice") || combinedText.contains("beverage") ||
                combinedText.contains("drink") || combinedText.contains("soda") || combinedText.contains("electrolyte") -> "Water & Drinks"

            combinedText.contains("milk") || combinedText.contains("dairy") || combinedText.contains("yogurt") ||
                combinedText.contains("cheese") || combinedText.contains("butter") || combinedText.contains("egg") ||
                combinedText.contains("cereal") || combinedText.contains("oat") || combinedText.contains("breakfast") -> "Dairy & Breakfast"

            combinedText.contains("rice") || combinedText.contains("pasta") || combinedText.contains("macaroni") ||
                combinedText.contains("spaghetti") || combinedText.contains("noodle") || combinedText.contains("atta") ||
                combinedText.contains("flour") || combinedText.contains("wheat") || combinedText.contains("grain") -> "Grains & Pasta"

            combinedText.contains("lentil") || combinedText.contains("daal") || combinedText.contains("dal") ||
                combinedText.contains("bean") || combinedText.contains("chickpea") || combinedText.contains("tuna") ||
                combinedText.contains("fish") || combinedText.contains("peanut butter") || combinedText.contains("protein") -> "Proteins & Lentils"

            combinedText.contains("canned") || combinedText.contains("tomato") || combinedText.contains("soup") ||
                combinedText.contains("pea") || combinedText.contains("corn") || combinedText.contains("vegetable") ||
                combinedText.contains("veg") || combinedText.contains("fruit") || combinedText.contains("onion") ||
                combinedText.contains("potato") -> "Canned Goods & Produce"

            combinedText.contains("oil") || combinedText.contains("ghee") || combinedText.contains("spice") ||
                combinedText.contains("masala") || combinedText.contains("salt") || combinedText.contains("sugar") ||
                combinedText.contains("vinegar") || combinedText.contains("sauce") || combinedText.contains("honey") -> "Oils, Spices & Cooking"

            combinedText.contains("snack") || combinedText.contains("biscuit") || combinedText.contains("cookie") ||
                combinedText.contains("cracker") || combinedText.contains("bread") || combinedText.contains("bar") ||
                combinedText.contains("nut") || combinedText.contains("almond") || combinedText.contains("date") ||
                combinedText.contains("bakery") -> "Snacks & Bakery"

            combinedText.contains("aid") || combinedText.contains("bandage") || combinedText.contains("soap") ||
                combinedText.contains("sanitizer") || combinedText.contains("paper") || combinedText.contains("towel") ||
                combinedText.contains("battery") || combinedText.contains("trash") -> "Essentials & First Aid"

            else -> PreplannedCatalog.defaultCategoryNames.firstOrNull() ?: "Canned Goods & Produce"
        }
    }

    private fun resolveUnit(quantity: String, name: String): String {
        val q = "$quantity $name".lowercase()
        return when {
            q.contains("can") || q.contains("tin") -> "cans"
            q.contains("bottle") -> "bottles"
            q.contains("jar") -> "jars"
            q.contains("box") -> "boxes"
            q.contains("bag") -> "bags"
            q.contains("pouch") -> "pouches"
            q.contains("carton") -> "cartons"
            q.contains("gallon") || q.contains("gal") -> "gallons"
            q.contains("kg") || q.contains("kilo") -> "kg"
            q.contains("lb") || q.contains("pound") -> "lbs"
            q.contains("ml") || q.contains("liter") || q.contains("litre") || q.contains(" fl oz") -> "bottles"
            q.contains(" g") || q.contains("gram") || q.contains("oz") -> "packs"
            else -> PreplannedCatalog.standardUnits.firstOrNull { q.contains(it) } ?: "packs"
        }
    }

    private fun buildNotes(brand: String, quantity: String): String {
        val parts = mutableListOf<String>()
        if (brand.isNotBlank()) parts.add("Brand: $brand")
        if (quantity.isNotBlank()) parts.add("Pkg: $quantity")
        return parts.joinToString(" • ")
    }
}
