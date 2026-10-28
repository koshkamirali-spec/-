package com.risefit.app.util

import com.risefit.app.data.GoalStage
import com.risefit.app.data.WorkoutLog

data class ProgressStats(
    val totalSessions: Int,
    val totalPoints: Int,
    /** Consecutive active days; one rest day between sessions never breaks it. */
    val streak: Int,
    val activeDaysLast7: Int,
    val activeDaysThisMonth: Int,
    val level: Int,
    val pointsIntoLevel: Int
)

data class Badge(val title: String, val description: String, val unlocked: Boolean)

object Progress {

    const val POINTS_PER_LEVEL = 100

    fun compute(logs: List<WorkoutLog>, today: Long): ProgressStats {
        val days = logs.map { it.epochDay }.toSet()
        val points = logs.sumOf { it.points }

        var streak = 0
        var gap = 0
        var d = if (today in days) today else today - 1
        var guard = 0
        while (guard < 800) {
            if (d in days) {
                streak++
                gap = 0
            } else {
                gap++
                if (gap >= 2) break
            }
            d--
            guard++
        }

        val last7 = (0..6).count { (today - it) in days }
        val monthStart = java.time.LocalDate.ofEpochDay(today).withDayOfMonth(1).toEpochDay()
        val month = days.count { it in monthStart..today }

        return ProgressStats(
            totalSessions = logs.size,
            totalPoints = points,
            streak = streak,
            activeDaysLast7 = last7,
            activeDaysThisMonth = month,
            level = points / POINTS_PER_LEVEL + 1,
            pointsIntoLevel = points % POINTS_PER_LEVEL
        )
    }

    fun badges(stats: ProgressStats, logs: List<WorkoutLog>, stages: List<GoalStage>): List<Badge> {
        val futsal = logs.count { it.type == "FUTSAL" }
        val gym = logs.count { it.type == "GYM" }
        val stageDone = { s: Int ->
            stages.any { it.stage == s && (it.photoUri != null || it.note.isNotBlank() || it.progress > 0) }
        }
        return listOf(
            Badge("اولین قدم", "اولین تمرینت را ثبت کردی", stats.totalSessions >= 1),
            Badge("۱۰ جلسه", "ده جلسه تمرین ثبت شد", stats.totalSessions >= 10),
            Badge("۵۰ جلسه", "پنجاه جلسه تمرین ثبت شد", stats.totalSessions >= 50),
            Badge("استمرار ۳ روزه", "سه روز فعال پشت سر هم (با احتساب استراحت)", stats.streak >= 3),
            Badge("استمرار ۷ روزه", "هفت روز فعال پشت سر هم (با احتساب استراحت)", stats.streak >= 7),
            Badge("هفته فعال", "حداقل ۴ روز فعال در ۷ روز اخیر", stats.activeDaysLast7 >= 4),
            Badge("فوتسالیست", "۵ جلسه فوتسال", futsal >= 5),
            Badge("همیشه در باشگاه", "۱۰ جلسه باشگاه", gym >= 10),
            Badge("مرحله روز ۳۰", "اطلاعات مرحله ۳۰ روزه ثبت شد", stageDone(30)),
            Badge("مرحله روز ۶۰", "اطلاعات مرحله ۶۰ روزه ثبت شد", stageDone(60)),
            Badge("مرحله روز ۱۰۰", "اطلاعات مرحله ۱۰۰ روزه ثبت شد", stageDone(100))
        )
    }
}
