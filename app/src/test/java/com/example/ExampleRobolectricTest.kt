package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ParsedTaskProposal
import com.example.data.model.ActivityCategory
import com.example.data.model.RepeatType
import com.example.data.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Daily Life Manager", appName)
    }

    @Test
    fun `test parsed task proposal entity conversion`() {
        val proposal = ParsedTaskProposal(
            title = "Morning Run",
            description = "Outdoor jogging",
            category = ActivityCategory.WORKOUT,
            startTime = "06:30",
            durationMinutes = 45,
            repeatType = RepeatType.DAILY,
            priority = TaskPriority.HIGH
        )
        val entity = proposal.toEntity()
        assertEquals("Morning Run", entity.title)
        assertEquals("06:30", entity.startTime)
        assertEquals(45, entity.durationMinutes)
        assertEquals("WORKOUT", entity.category)
        assertNotNull(entity.colorHex)
    }
}
