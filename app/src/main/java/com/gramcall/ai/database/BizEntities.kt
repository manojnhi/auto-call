package com.gramcall.ai.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "products", indices = [Index(value = ["name"], unique = true)])
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val unit: String,
    val price: Double,
    val stock: Double = 0.0,
    val minStock: Double = 5.0,
    val sku: String = "",
    val description: String = "",
    /** Comma-separated naam jo bol-chal mein aate hain, jaise "tel,oil" */
    val aliases: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CampaignEntity::class, parentColumns = ["id"], childColumns = ["campaignId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("customerId"), Index("campaignId"), Index("villageId"),
        Index("cityId"), Index("orderAt"), Index("status")
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long?,
    val customerName: String,
    val customerMobile: String,
    val villageId: Long? = null,
    val cityId: Long? = null,
    val villageName: String = "",
    val cityName: String = "",
    val orderAt: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val deliveryAddress: String = "",
    val campaignId: Long? = null,
    val status: String,
    /** MANUAL ya ASSISTED (order sahayak se) */
    val source: String = "MANUAL",
    val note: String = ""
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("orderId"), Index("productId")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long?,
    val productName: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
    val lineTotal: Double
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val id: String,
    val value: String
)

/** Sahmati ka record (audit). Customer delete hone par bhi bacha rehta hai, isliye koi foreign key nahi. */
@Entity(tableName = "consent_log")
data class ConsentLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long?,
    val customerName: String,
    val granted: Boolean,
    val at: Long = System.currentTimeMillis(),
    val source: String
)
