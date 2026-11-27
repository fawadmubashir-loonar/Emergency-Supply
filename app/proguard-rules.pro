# Keep Room database schemas
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Google API Client and Sheets models
-keep class com.google.api.services.sheets.** { *; }
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**
-dontwarn org.apache.http.**
-dontwarn com.google.common.**

# Keep In-App Updater models
-keep class com.emergencysupply.rations.updater.** { *; }
