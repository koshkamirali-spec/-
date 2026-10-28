package com.risefit.app.ai

import com.risefit.app.util.toAsciiDigits

/** What the user asked RiseAI to do. Understood on the phone itself, no internet needed. */
sealed class AiIntent {
    object Today : AiIntent()
    object Habits : AiIntent()
    object Focus : AiIntent()
    object Goals : AiIntent()
    object Motivation : AiIntent()
    object Help : AiIntent()
    object Greeting : AiIntent()
    object Unknown : AiIntent()

    /** A planner task. [startMin] is minutes since midnight, or -1 if the user gave no time. */
    class AddTask(val title: String, val startMin: Int, val tomorrow: Boolean) : AiIntent()

    /** A one-time reminder. [hour] is -1 if the user gave no time. */
    class AddReminder(val title: String, val hour: Int, val minute: Int, val tomorrow: Boolean) : AiIntent()
}

object LocalAssistant {

    private val timeRegex = Regex("""ساعت\s*(\d{1,2})(?:\s*[:٫.]\s*(\d{2}))?""")

    private val stopWords = setOf(
        "اضافه", "کن", "کنید", "ثبت", "بنویس", "لطفا", "لطفاً", "یادم", "بنداز", "باشه", "باشد",
        "یادآوری", "یادآور", "را", "رو", "امروز", "فردا", "صبح", "عصر", "شب", "ظهر", "بعدازظهر", "که", "ساعت"
    )

    private fun clean(text: String): String =
        text.toAsciiDigits()
            .replace('ي', 'ی').replace('ك', 'ک')
            .replace("‌", " ")
            .replace("بعد از ظهر", "بعدازظهر")
            .trim()

    /** Same text without spaces, used only for keyword matching. */
    private fun compact(text: String): String = text.replace(" ", "")

    private fun has(compactText: String, vararg words: String): Boolean =
        words.any { compactText.contains(it.replace(" ", "").replace("‌", "")) }

    private fun parseTime(text: String): Pair<Int, Int>? {
        val m = timeRegex.find(text) ?: return null
        var hour = m.groupValues[1].toIntOrNull() ?: return null
        val minute = m.groupValues[2].toIntOrNull() ?: 0
        val after = text.substring(m.range.last + 1)
        val pm = after.contains("عصر") || after.contains("بعدازظهر") || after.contains("شب")
        if (pm && hour in 1..11) hour += 12
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    private fun titleOf(text: String): String {
        var t = timeRegex.replace(text, " ")
        t = Regex("""(به|در)\s+برنامه(\s+امروز|\s+فردا)?""").replace(t, " ")
        t = Regex("""کار\s+جدید""").replace(t, " ")
        t = Regex("""یادم\s+باشه""").replace(t, " ")
        return t.split(Regex("""\s+"""))
            .filter { it.isNotBlank() && it.trim('.', '،', '؟', '!') !in stopWords }
            .joinToString(" ")
            .trim()
    }

    fun parse(raw: String): AiIntent {
        val text = clean(raw)
        if (text.isEmpty()) return AiIntent.Unknown
        val c = compact(text)
        val tomorrow = c.contains("فردا")

        // reminder: "یادم بنداز ... ساعت ۱۵"
        if (has(c, "یادم بنداز", "یادآوری کن", "یادآوری", "یادم باشه", "یادم بیار")) {
            val time = parseTime(text)
            return AiIntent.AddReminder(titleOf(text), time?.first ?: -1, time?.second ?: 0, tomorrow)
        }
        // planner task: "درس ریاضی ساعت ۱۶ اضافه کن"
        if (has(c, "اضافه کن", "اضافه کنید", "ثبت کن", "به برنامه", "در برنامه", "کار جدید")) {
            val time = parseTime(text)
            val start = if (time != null) time.first * 60 + time.second else -1
            return AiIntent.AddTask(titleOf(text), start, tomorrow)
        }

        // long, free-form messages are real questions: leave them for the server instead of guessing a keyword
        if (text.split(Regex("""\s+""")).size > 4) return AiIntent.Unknown

        return when {
            has(c, "برنامه امروز", "امروز چی", "امروز چه", "کارهای امروز", "کارای امروز", "برنامه ام", "برنامم") -> AiIntent.Today
            has(c, "عادت") -> AiIntent.Habits
            has(c, "تمرکز", "فوکوس", "پومودورو") -> AiIntent.Focus
            has(c, "هدف") -> AiIntent.Goals
            has(c, "انگیزه", "خسته", "حال ندارم", "دلم نمیخواد", "تنبل") -> AiIntent.Motivation
            has(c, "کمک", "راهنما", "چه کارهایی", "چی کار میکنی", "چیکار میکنی") -> AiIntent.Help
            has(c, "سلام", "درود") -> AiIntent.Greeting
            else -> AiIntent.Unknown
        }
    }

    const val HELP_TEXT = "من روی گوشی خودت این کارها را بدون اینترنت انجام می‌دهم:\n" +
        "• «برنامه امروز» — کارهای امروز و درصد پیشرفت\n" +
        "• «درس ریاضی ساعت ۱۶ اضافه کن» — کار جدید در پلنر\n" +
        "• «یادم بنداز آب بخورم ساعت ۱۵» — یادآور یک‌باره\n" +
        "• «عادت‌ها»، «تمرکز»، «هدف‌ها» — وضعیت هر بخش\n" +
        "• «انگیزه» — یک جمله انگیزشی\n" +
        "سؤال‌های آزاد را هم می‌توانی بپرسی، اگر آدرس سرور RiseAI را در تنظیمات (آیکن چرخ‌دنده) وارد کرده باشی."
}
