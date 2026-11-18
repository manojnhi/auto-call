package com.gramcall.ai.repository

import androidx.room.withTransaction
import com.gramcall.ai.database.*
import org.json.JSONArray
import org.json.JSONObject

/** Poore local database ka JSON backup / restore. PIN aur token isme nahi jaate. */
object BackupManager {
    const val FORMAT = "GramCall-AI-backup"
    const val SCHEMA = 3

    private fun JSONObject.putN(k: String, v: Any?): JSONObject = put(k, v ?: JSONObject.NULL)
    private fun JSONObject.longN(k: String): Long? = if (isNull(k)) null else getLong(k)
    private fun <T> JSONArray.toObjs(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }
    private fun <T> List<T>.toJson(f: (T) -> JSONObject) = JSONArray().also { a -> forEach { a.put(f(it)) } }

    suspend fun export(db: AppDatabase): String {
        val tables = JSONObject()
        tables.put("villages", db.villageDao().getAll().toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("district", it.district).put("state", it.state)
                .put("pinCode", it.pinCode).put("notes", it.notes).put("createdAt", it.createdAt)
        })
        tables.put("cities", db.cityDao().getAll().toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("district", it.district).put("state", it.state)
                .put("notes", it.notes).put("createdAt", it.createdAt)
        })
        tables.put("areas", db.areaDao().getAll().toJson {
            JSONObject().put("id", it.id).put("cityId", it.cityId).put("name", it.name)
        })
        tables.put("customers", db.customerDao().getAll().toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("mobile", it.mobile).put("altMobile", it.altMobile)
                .putN("villageId", it.villageId).putN("cityId", it.cityId).put("address", it.address)
                .put("category", it.category).put("language", it.language).put("callConsent", it.callConsent)
                .putN("consentDate", it.consentDate).put("preferredTime", it.preferredTime)
                .put("notes", it.notes).put("createdAt", it.createdAt)
        })
        tables.put("campaigns", db.campaignDao().getAll().toJson {
            JSONObject().put("id", it.id).put("title", it.title).putN("villageId", it.villageId)
                .putN("cityId", it.cityId).put("status", it.status).put("createdAt", it.createdAt)
                .putN("finishedAt", it.finishedAt)
        })
        tables.put("call_history", db.callHistoryDao().getAll().toJson {
            JSONObject().put("id", it.id).put("campaignId", it.campaignId).putN("customerId", it.customerId)
                .put("customerName", it.customerName).put("mobile", it.mobile).put("status", it.status)
                .put("position", it.position).putN("startedAt", it.startedAt).putN("endedAt", it.endedAt)
                .put("durationSec", it.durationSec).put("note", it.note)
        })
        tables.put("products", db.productDao().getAll().toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("category", it.category).put("unit", it.unit)
                .put("price", it.price).put("stock", it.stock).put("minStock", it.minStock).put("sku", it.sku)
                .put("description", it.description).put("aliases", it.aliases).put("active", it.active)
                .put("createdAt", it.createdAt)
        })
        tables.put("orders", db.orderDao().getAll().toJson {
            JSONObject().put("id", it.id).putN("customerId", it.customerId).put("customerName", it.customerName)
                .put("customerMobile", it.customerMobile).putN("villageId", it.villageId).putN("cityId", it.cityId)
                .put("villageName", it.villageName).put("cityName", it.cityName).put("orderAt", it.orderAt)
                .put("subtotal", it.subtotal).put("discount", it.discount).put("total", it.total)
                .put("deliveryAddress", it.deliveryAddress).putN("campaignId", it.campaignId)
                .put("status", it.status).put("source", it.source).put("note", it.note)
        })
        tables.put("order_items", db.orderDao().getAllItems().toJson {
            JSONObject().put("id", it.id).put("orderId", it.orderId).putN("productId", it.productId)
                .put("productName", it.productName).put("unit", it.unit).put("quantity", it.quantity)
                .put("unitPrice", it.unitPrice).put("lineTotal", it.lineTotal)
        })
        tables.put("settings", db.settingDao().getAll().toJson {
            JSONObject().put("id", it.id).put("value", it.value)
        })
        tables.put("consent_log", db.consentLogDao().getAll().toJson {
            JSONObject().put("id", it.id).putN("customerId", it.customerId).put("customerName", it.customerName)
                .put("granted", it.granted).put("at", it.at).put("source", it.source)
        })
        return JSONObject().put("format", FORMAT).put("schema", SCHEMA)
            .put("exportedAt", System.currentTimeMillis()).put("tables", tables).toString()
    }

    /** Poora data badal deta hai. Galat/naya format ho to kuch nahi badalta (error deta hai). */
    suspend fun restore(db: AppDatabase, json: String) {
        val root = JSONObject(json)
        require(root.optString("format") == FORMAT) { "यह GramCall AI का बैकअप नहीं है" }
        require(root.optInt("schema", 0) <= SCHEMA) { "यह बैकअप नए ऐप वर्ज़न का है — पहले ऐप अपडेट करें" }
        val t = root.getJSONObject("tables")
        fun arr(n: String) = t.optJSONArray(n) ?: JSONArray()

        val villages = arr("villages").toObjs {
            VillageEntity(it.getLong("id"), it.getString("name"), it.getString("district"), it.getString("state"),
                it.getString("pinCode"), it.getString("notes"), it.getLong("createdAt"))
        }
        val cities = arr("cities").toObjs {
            CityEntity(it.getLong("id"), it.getString("name"), it.getString("district"), it.getString("state"),
                it.getString("notes"), it.getLong("createdAt"))
        }
        val areas = arr("areas").toObjs { AreaEntity(it.getLong("id"), it.getLong("cityId"), it.getString("name")) }
        val customers = arr("customers").toObjs {
            CustomerEntity(it.getLong("id"), it.getString("name"), it.getString("mobile"), it.getString("altMobile"),
                it.longN("villageId"), it.longN("cityId"), it.getString("address"), it.getString("category"),
                it.getString("language"), it.getBoolean("callConsent"), it.longN("consentDate"),
                it.getString("preferredTime"), it.getString("notes"), it.getLong("createdAt"))
        }
        val campaigns = arr("campaigns").toObjs {
            CampaignEntity(it.getLong("id"), it.getString("title"), it.longN("villageId"), it.longN("cityId"),
                it.getString("status"), it.getLong("createdAt"), it.longN("finishedAt"))
        }
        val calls = arr("call_history").toObjs {
            CallHistoryEntity(it.getLong("id"), it.getLong("campaignId"), it.longN("customerId"),
                it.getString("customerName"), it.getString("mobile"), it.getString("status"), it.getInt("position"),
                it.longN("startedAt"), it.longN("endedAt"), it.getInt("durationSec"), it.getString("note"))
        }
        val products = arr("products").toObjs {
            ProductEntity(it.getLong("id"), it.getString("name"), it.getString("category"), it.getString("unit"),
                it.getDouble("price"), it.getDouble("stock"), it.getDouble("minStock"), it.getString("sku"),
                it.getString("description"), it.getString("aliases"), it.getBoolean("active"), it.getLong("createdAt"))
        }
        val orders = arr("orders").toObjs {
            OrderEntity(it.getLong("id"), it.longN("customerId"), it.getString("customerName"),
                it.getString("customerMobile"), it.longN("villageId"), it.longN("cityId"),
                it.getString("villageName"), it.getString("cityName"), it.getLong("orderAt"),
                it.getDouble("subtotal"), it.getDouble("discount"), it.getDouble("total"),
                it.getString("deliveryAddress"), it.longN("campaignId"), it.getString("status"),
                it.getString("source"), it.getString("note"))
        }
        val orderItems = arr("order_items").toObjs {
            OrderItemEntity(it.getLong("id"), it.getLong("orderId"), it.longN("productId"),
                it.getString("productName"), it.getString("unit"), it.getDouble("quantity"),
                it.getDouble("unitPrice"), it.getDouble("lineTotal"))
        }
        val settings = arr("settings").toObjs { SettingEntity(it.getString("id"), it.getString("value")) }
        val consent = arr("consent_log").toObjs {
            ConsentLogEntity(it.getLong("id"), it.longN("customerId"), it.getString("customerName"),
                it.getBoolean("granted"), it.getLong("at"), it.getString("source"))
        }

        db.withTransaction {
            // child -> parent order mein khali karo
            db.orderDao().deleteAllItems(); db.orderDao().deleteAll()
            db.callHistoryDao().deleteAll(); db.campaignDao().deleteAll()
            db.consentLogDao().deleteAll()
            db.customerDao().deleteAll(); db.areaDao().deleteAll()
            db.cityDao().deleteAll(); db.villageDao().deleteAll()
            db.productDao().deleteAll(); db.settingDao().deleteAll()
            // parent -> child order mein bharo
            db.villageDao().insertAll(villages); db.cityDao().insertAll(cities); db.areaDao().insertAll(areas)
            db.customerDao().insertAll(customers); db.campaignDao().insertAll(campaigns)
            db.callHistoryDao().restoreAll(calls); db.productDao().insertAll(products)
            db.orderDao().insertAll(orders); db.orderDao().insertAllItems(orderItems)
            db.consentLogDao().insertAll(consent); db.settingDao().putAll(settings)
        }
    }
}
