package com.gramcall.ai

import android.app.Application
import com.gramcall.ai.database.AppDatabase
import com.gramcall.ai.repository.BizRepository
import com.gramcall.ai.repository.GramRepository
import com.gramcall.ai.security.SecureStore

class GramCallApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { GramRepository(database) }
    val biz by lazy { BizRepository(database) }
    val secure by lazy { SecureStore(this) }
}
