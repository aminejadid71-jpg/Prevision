package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.PayrollRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Prévision Paie", appName)
    }

    @Test
    fun `test initial database seed scenario`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val repository = PayrollRepository(db)

        val id = repository.seedOfficialScenario()
        assertTrue("Seeded quinzaine ID must be greater than 0", id > 0)

        val quinzaine = repository.getQuinzaine(id).first()
        assertEquals("Septembre — 2ème quinzaine", quinzaine?.title)
        assertEquals(37102.0, quinzaine?.currentAmount ?: 0.0, 0.001)

        val groups = repository.getWorkerGroups(id).first()
        assertEquals(2, groups.size)
        assertEquals(8, groups[0].workerCount)
        assertEquals(22, groups[1].workerCount)

        val posts = repository.getFixedPosts(id).first()
        assertEquals(1, posts.size)
        assertEquals(1281.51, posts[0].amount, 0.001)
    }
}
