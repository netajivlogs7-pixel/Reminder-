package com.example

import com.example.data.model.ActivityCategory
import com.example.data.model.RepeatType
import com.example.data.model.TaskPriority
import com.example.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLevelCalculation() {
        val points = 250
        val level = (points / 100) + 1
        assertEquals(3, level)
    }

    @Test
    fun testHabitStreakIncrement() {
        var currentStreak = 2
        var bestStreak = 4
        currentStreak += 1
        bestStreak = maxOf(currentStreak, bestStreak)
        assertEquals(3, currentStreak)
        assertEquals(4, bestStreak)

        currentStreak += 2
        bestStreak = maxOf(currentStreak, bestStreak)
        assertEquals(5, currentStreak)
        assertEquals(5, bestStreak)
    }

    @Test
    fun testWaterGoalPercentage() {
        val totalWater = 1500
        val goal = 2500
        val progress = (totalWater.toFloat() / goal)
        assertEquals(0.6f, progress, 0.001f)
    }

    @Test
    fun testTimeUtilsFormat() {
        assertEquals("08:00 AM", TimeUtils.formatTo12Hour("08:00"))
        assertEquals("06:30 PM", TimeUtils.formatTo12Hour("18:30"))
        assertEquals("12:15 AM", TimeUtils.formatTo12Hour("00:15"))
        assertEquals("12:00 PM", TimeUtils.formatTo12Hour("12:00"))
    }

    @Test
    fun testTimeUtilsAmPmToggle() {
        assertTrue(TimeUtils.isAm("08:15"))
        assertFalse(TimeUtils.isAm("20:15"))

        val pmTime = TimeUtils.setAmPm("08:15", false)
        assertEquals("20:15", pmTime)
        assertFalse(TimeUtils.isAm(pmTime))

        val amTime = TimeUtils.setAmPm("20:15", true)
        assertEquals("08:15", amTime)
        assertTrue(TimeUtils.isAm(amTime))
    }
}
