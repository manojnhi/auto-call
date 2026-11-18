package com.gramcall.ai.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "villages", indices = [Index(value = ["name", "district"], unique = true)])
data class VillageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val district: String = "",
    val state: String = "",
    val pinCode: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cities", indices = [Index(value = ["name", "state"], unique = true)])
data class CityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val district: String = "",
    val state: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "areas",
    foreignKeys = [ForeignKey(
        entity = CityEntity::class, parentColumns = ["id"], childColumns = ["cityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["cityId", "name"], unique = true)]
)
data class AreaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cityId: Long,
    val name: String
)

@Entity(
    tableName = "customers",
    foreignKeys = [
        ForeignKey(
            entity = VillageEntity::class, parentColumns = ["id"], childColumns = ["villageId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CityEntity::class, parentColumns = ["id"], childColumns = ["cityId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("villageId"), Index("cityId"),
        Index(value = ["mobile"], unique = true)
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String,
    val altMobile: String = "",
    val villageId: Long? = null,
    val cityId: Long? = null,
    val address: String = "",
    val category: String = "सामान्य",
    val language: String = "हिंदी",
    val callConsent: Boolean = false,
    val consentDate: Long? = null,
    val preferredTime: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "campaigns",
    foreignKeys = [
        ForeignKey(
            entity = VillageEntity::class, parentColumns = ["id"], childColumns = ["villageId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CityEntity::class, parentColumns = ["id"], childColumns = ["cityId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("villageId"), Index("cityId")]
)
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val villageId: Long? = null,
    val cityId: Long? = null,
    val status: String,
    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
)

@Entity(
    tableName = "call_history",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class, parentColumns = ["id"], childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("campaignId"), Index("customerId")]
)
data class CallHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long,
    val customerId: Long?,
    val customerName: String,
    val mobile: String,
    val status: String,
    val position: Int,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val durationSec: Int = 0,
    val note: String = ""
)
