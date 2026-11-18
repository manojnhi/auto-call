package com.gramcall.ai

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gramcall.ai.database.AppDatabase
import com.gramcall.ai.database.CustomerEntity
import com.gramcall.ai.database.VillageEntity
import com.gramcall.ai.repository.GramRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: GramRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = GramRepository(db)
    }

    @After fun tearDown() = db.close()

    @Test fun villageCreation() = runBlocking {
        assertTrue(repo.saveVillage(VillageEntity(name = "इटारौरा", district = "फतेहपुर")).ok)
        assertEquals(1, repo.villages("").first().size)
    }

    @Test fun duplicateVillageRejected() = runBlocking {
        repo.saveVillage(VillageEntity(name = "इटारौरा", district = "फतेहपुर"))
        assertFalse(repo.saveVillage(VillageEntity(name = "इटारौरा", district = "फतेहपुर")).ok)
    }

    @Test fun customerCreationAndDuplicatePhone() = runBlocking {
        repo.saveVillage(VillageEntity(name = "इटारौरा"))
        val vid = repo.villages("").first().first().village.id
        assertTrue(repo.saveCustomer(CustomerEntity(name = "राम", mobile = "98765 43210", villageId = vid)).ok)
        val dup = repo.saveCustomer(CustomerEntity(name = "श्याम", mobile = "+919876543210", villageId = vid))
        assertFalse(dup.ok)
        assertEquals(1, repo.customers("", null, null).first().size)
    }

    @Test fun invalidPhoneRejected() = runBlocking {
        repo.saveVillage(VillageEntity(name = "इटारौरा"))
        val vid = repo.villages("").first().first().village.id
        assertFalse(repo.saveCustomer(CustomerEntity(name = "राम", mobile = "12345", villageId = vid)).ok)
    }

    @Test fun villageFilter() = runBlocking {
        repo.saveVillage(VillageEntity(name = "A")); repo.saveVillage(VillageEntity(name = "B"))
        val ids = repo.villages("").first().map { it.village.id }
        repo.saveCustomer(CustomerEntity(name = "x", mobile = "9876543210", villageId = ids[0]))
        repo.saveCustomer(CustomerEntity(name = "y", mobile = "9876543211", villageId = ids[1]))
        assertEquals(1, repo.customers("", ids[0], null).first().size)
    }

    @Test fun csvImportSkipsDuplicatesAndInvalid() = runBlocking {
        val csv = "name,mobile,village,consent\nRam,9876543210,Itarora,yes\nRam2,9876543210,Itarora,no\nBad,123,Itarora,no\n"
        val s = repo.importCsv(csv)
        assertEquals(1, s.added); assertEquals(1, s.duplicates); assertEquals(1, s.invalid)
    }
}
