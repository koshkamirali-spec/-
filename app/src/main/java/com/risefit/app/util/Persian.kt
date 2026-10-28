package com.risefit.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

fun String.fa(): String {
    val sb = StringBuilder(length)
    for (c in this) {
        sb.append(if (c in '0'..'9') faDigits[c - '0'] else c)
    }
    return sb.toString()
}

fun Int.fa(): String = toString().fa()
fun Long.fa(): String = toString().fa()

/** Converts Persian/Arabic digits typed by the user into ASCII digits for parsing. */
fun String.toAsciiDigits(): String {
    val sb = StringBuilder(length)
    for (c in this) {
        sb.append(
            when (c) {
                in '۰'..'۹' -> '0' + (c - '۰')
                in '٠'..'٩' -> '0' + (c - '٠')
                else -> c
            }
        )
    }
    return sb.toString()
}

fun formatTime(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}".fa()

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

fun dayIndexOf(date: LocalDate): Int = date.dayOfWeek.value - 1

fun maskHas(mask: Int, dayIndex: Int): Boolean = ((mask shr dayIndex) and 1) == 1

fun maskToggle(mask: Int, dayIndex: Int): Int = mask xor (1 shl dayIndex)

fun maskCount(mask: Int): Int = Integer.bitCount(mask and 0b1111111)

/** Short Persian description of the days in a mask, e.g. "هر روز" or "ش، ی، چ". */
fun describeDays(mask: Int): String {
    val m = mask and 0b1111111
    if (m == 0b1111111) return "هر روز"
    if (m == 0) return "یک‌بار"
    val short = listOf("د", "س", "چ", "پ", "ج", "ش", "ی")
    return Jalali.weekOrder.filter { maskHas(m, it) }.joinToString("، ") { short[it] }
}

/** "۳ ساعت و ۵ دقیقه" style text for a duration in minutes. */
fun describeDuration(totalMinutes: Long): String {
    val m = if (totalMinutes < 0) 0 else totalMinutes
    val h = m / 60
    val mm = m % 60
    return when {
        h == 0L && mm == 0L -> "کمتر از یک دقیقه"
        h == 0L -> "${mm.fa()} دقیقه"
        mm == 0L -> "${h.fa()} ساعت"
        else -> "${h.fa()} ساعت و ${mm.fa()} دقیقه"
    }
}

/** Next local date-time at which a weekly time (given by mask) occurs, strictly after [from]. */
fun nextOccurrence(hour: Int, minute: Int, daysMask: Int, from: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
    val mask = daysMask and 0b1111111
    if (mask == 0) return null
    for (offset in 0..7) {
        val date = from.toLocalDate().plusDays(offset.toLong())
        if (!maskHas(mask, dayIndexOf(date))) continue
        val candidate = LocalDateTime.of(date, LocalTime.of(hour, minute))
        if (candidate.isAfter(from)) return candidate
    }
    return null
}
