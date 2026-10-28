package com.risefit.app.util

import com.risefit.app.data.GoalItem
import com.risefit.app.data.GoalStep
import com.risefit.app.data.TaskItem
import java.time.LocalDate

object PlannerStats {
    /** Share of finished tasks, 0..100. */
    fun percent(tasks: List<TaskItem>): Int =
        if (tasks.isEmpty()) 0 else tasks.count { it.done } * 100 / tasks.size

    /** Minutes since midnight as "HH:MM" in Persian digits. */
    fun time(min: Int): String = formatTime((min / 60).coerceIn(0, 23), (min % 60).coerceIn(0, 59))
}

object HabitStats {

    /**
     * Consecutive days with the habit done, counted back from today. One missed day never breaks it,
     * and today is not counted as missed until the day is over.
     */
    fun streak(done: Set<Long>, today: Long): Int {
        var d = if (today in done) today else today - 1
        var count = 0
        var gap = 0
        var guard = 0
        while (guard < 4000) {
            if (d in done) {
                count++
                gap = 0
            } else {
                gap++
                if (gap >= 2) break
            }
            d--
            guard++
        }
        return count
    }

    fun countIn(done: Set<Long>, from: Long, to: Long): Int {
        var n = 0
        var d = from
        while (d <= to) {
            if (d in done) n++
            d++
        }
        return n
    }

    /** First day (epoch day) of the current Persian month. */
    fun monthStart(today: Long): Long {
        val j = Jalali.fromLocalDate(LocalDate.ofEpochDay(today))
        return Jalali.toLocalDate(j.year, j.month, 1).toEpochDay()
    }

    fun encouragement(streak: Int, doneToday: Boolean): String = when {
        doneToday && streak >= 7 -> "عالیه! ${streak.fa()} روز پشت‌سرهم، ادامه بده."
        doneToday -> "امروز انجام شد. آفرین!"
        streak >= 3 -> "ریتمت خوبه؛ امروز هم یک قدم کوچک بردار."
        else -> "هر وقت آماده بودی شروع کن؛ یک روز جا افتادن اشکالی ندارد."
    }
}

object GoalStats {
    val milestones = listOf(25, 50, 75, 100)

    fun percent(goal: GoalItem, steps: List<GoalStep>): Int =
        if (steps.isNotEmpty()) steps.count { it.done } * 100 / steps.size
        else goal.progress.coerceIn(0, 100)

    fun totalDays(goal: GoalItem): Int = (goal.deadlineEpochDay - goal.startEpochDay).toInt().coerceAtLeast(1)

    fun daysLeft(goal: GoalItem, today: Long): Int = (goal.deadlineEpochDay - today).toInt()

    /** Which day of the goal today is (1-based, capped to the total). */
    fun dayNumber(goal: GoalItem, today: Long): Int =
        ((today - goal.startEpochDay).toInt() + 1).coerceIn(1, totalDays(goal))

    /** Where progress would be if it grew evenly until the deadline. */
    fun expectedPercent(goal: GoalItem, today: Long): Int =
        (dayNumber(goal, today) * 100 / totalDays(goal)).coerceIn(0, 100)

    fun reachedMilestone(percent: Int): Int = milestones.lastOrNull { percent >= it } ?: 0

    fun statusLabel(status: String): String = when (status) {
        "DONE" -> "انجام شد"
        "PAUSED" -> "متوقف"
        else -> "در جریان"
    }

    fun message(goal: GoalItem, percent: Int, today: Long): String = when {
        goal.status == "DONE" || percent >= 100 -> "این هدف را کامل کردی. به خودت افتخار کن!"
        goal.status == "PAUSED" -> "هدف متوقف است؛ هر وقت آماده بودی ادامه‌اش بده."
        daysLeft(goal, today) < 0 -> "مهلت گذشته، ولی هدف هنوز مال توست؛ می‌توانی مهلت را عوض کنی."
        percent + 10 < expectedPercent(goal, today) -> "کمی عقب‌تری، مشکلی نیست؛ قدم بعدی را کوچک‌تر کن و ادامه بده."
        else -> "خوب پیش می‌روی؛ همین روند را نگه دار."
    }
}
