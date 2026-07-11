package com.networth.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromCategory(value: AssetCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): AssetCategory = AssetCategory.valueOf(value)

    @TypeConverter
    fun fromCurrency(value: Currency): String = value.name

    @TypeConverter
    fun toCurrency(value: String): Currency = Currency.valueOf(value)
}

@Database(entities = [AssetEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "net_worth_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
