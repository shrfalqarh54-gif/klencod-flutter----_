package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.TaskEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("قوة الآن", appName)
    }

    @Test
    fun `verify local offline database operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)

        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = "مهمة اختبار قوة الآن",
            startMinute = 600,
            endMinute = 720,
            durationMinutes = 120,
            startEpochDay = 20000L
        )

        db.taskDao().insertTask(task)
        val loaded = db.taskDao().getTaskById(task.id)
        assertNotNull(loaded)
        assertEquals("مهمة اختبار قوة الآن", loaded?.title)
    }
}
