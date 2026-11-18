package com.gramcall.ai.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VillageEntity::class, CityEntity::class, AreaEntity::class, CustomerEntity::class,
        CampaignEntity::class, CallHistoryEntity::class,
        ProductEntity::class, OrderEntity::class, OrderItemEntity::class,
        SettingEntity::class, ConsentLogEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun villageDao(): VillageDao
    abstract fun cityDao(): CityDao
    abstract fun areaDao(): AreaDao
    abstract fun customerDao(): CustomerDao
    abstract fun campaignDao(): CampaignDao
    abstract fun callHistoryDao(): CallHistoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun settingDao(): SettingDao
    abstract fun consentLogDao(): ConsentLogDao

    companion object {
        /** Phase 3-4: campaigns + call_history tables jode gaye. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `campaigns` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, `villageId` INTEGER, `cityId` INTEGER, `status` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `finishedAt` INTEGER, " +
                        "FOREIGN KEY(`villageId`) REFERENCES `villages`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , " +
                        "FOREIGN KEY(`cityId`) REFERENCES `cities`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_campaigns_villageId` ON `campaigns` (`villageId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_campaigns_cityId` ON `campaigns` (`cityId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `call_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`campaignId` INTEGER NOT NULL, `customerId` INTEGER, `customerName` TEXT NOT NULL, " +
                        "`mobile` TEXT NOT NULL, `status` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                        "`startedAt` INTEGER, `endedAt` INTEGER, `durationSec` INTEGER NOT NULL, `note` TEXT NOT NULL, " +
                        "FOREIGN KEY(`campaignId`) REFERENCES `campaigns`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_history_campaignId` ON `call_history` (`campaignId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_history_customerId` ON `call_history` (`customerId`)")
            }
        }

        /** Phase 5-8: products, orders, order_items, settings, consent_log. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `category` TEXT NOT NULL, `unit` TEXT NOT NULL, `price` REAL NOT NULL, " +
                        "`stock` REAL NOT NULL, `minStock` REAL NOT NULL, `sku` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, `aliases` TEXT NOT NULL, `active` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `orders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`customerId` INTEGER, `customerName` TEXT NOT NULL, `customerMobile` TEXT NOT NULL, " +
                        "`villageId` INTEGER, `cityId` INTEGER, `villageName` TEXT NOT NULL, `cityName` TEXT NOT NULL, " +
                        "`orderAt` INTEGER NOT NULL, `subtotal` REAL NOT NULL, `discount` REAL NOT NULL, " +
                        "`total` REAL NOT NULL, `deliveryAddress` TEXT NOT NULL, `campaignId` INTEGER, " +
                        "`status` TEXT NOT NULL, `source` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                        "FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , " +
                        "FOREIGN KEY(`campaignId`) REFERENCES `campaigns`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                for (col in listOf("customerId", "campaignId", "villageId", "cityId", "orderAt", "status")) {
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_$col` ON `orders` (`$col`)")
                }
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `order_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`orderId` INTEGER NOT NULL, `productId` INTEGER, `productName` TEXT NOT NULL, " +
                        "`unit` TEXT NOT NULL, `quantity` REAL NOT NULL, `unitPrice` REAL NOT NULL, " +
                        "`lineTotal` REAL NOT NULL, " +
                        "FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_items_orderId` ON `order_items` (`orderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_items_productId` ON `order_items` (`productId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `settings` (`id` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `consent_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`customerId` INTEGER, `customerName` TEXT NOT NULL, `granted` INTEGER NOT NULL, " +
                        "`at` INTEGER NOT NULL, `source` TEXT NOT NULL)"
                )
            }
        }

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "gramcall.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
