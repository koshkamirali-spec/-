package com.risefit.app.util

import java.time.DayOfWeek
import java.time.LocalDate

/** Persian (Jalali) calendar conversion, ported from the well-known jalaali algorithm. */
data class JalaliDate(val year: Int, val month: Int, val day: Int)

object Jalali {

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    /** Index = DayOfWeek.value - 1 (Monday = 0). */
    val weekdayNames = listOf(
        "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه", "یکشنبه"
    )

    /** Persian week order Saturday..Friday expressed as day indexes (Monday = 0). */
    val weekOrder = listOf(5, 6, 0, 1, 2, 3, 4)

    private class JalCal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalCal {
        val bl = breaks.size
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        require(jy >= jp && jy < breaks[bl - 1]) { "Invalid Jalali year $jy" }
        for (i in 1 until bl) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = ((n + 1) % 33 - 1) % 4
        if (leap == -1) leap = 4
        return JalCal(leap, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = ((gy + (gm - 8) / 6 + 100100) * 1461) / 4 +
            (153 * ((gm + 9) % 12) + 2) / 5 + gd - 34840408
        d = d - (((gy + 100100 + (gm - 8) / 6) / 100) * 3) / 4 + 752
        return d
    }

    private fun d2g(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j += (((4 * jdn + 183187720) / 146097) * 3) / 4 * 4 - 3908
        val i = ((j % 1461) / 4) * 5 + 308
        val gd = (i % 153) / 5 + 1
        val gm = (i / 153) % 12 + 1
        val gy = j / 1461 - 100100 + (8 - gm) / 6
        return Triple(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1
    }

    private fun d2j(jdn: Int): JalaliDate {
        val gy = d2g(jdn).first
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(gy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                return JalaliDate(jy, 1 + k / 31, k % 31 + 1)
            } else {
                k -= 186
            }
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        return JalaliDate(jy, 7 + k / 30, k % 30 + 1)
    }

    fun fromLocalDate(date: LocalDate): JalaliDate =
        d2j(g2d(date.year, date.monthValue, date.dayOfMonth))

    fun toLocalDate(jy: Int, jm: Int, jd: Int): LocalDate {
        val (gy, gm, gd) = d2g(j2d(jy, jm, jd))
        return LocalDate.of(gy, gm, gd)
    }

    fun daysInMonth(jy: Int, jm: Int): Int {
        val nextY = if (jm == 12) jy + 1 else jy
        val nextM = if (jm == 12) 1 else jm + 1
        return j2d(nextY, nextM, 1) - j2d(jy, jm, 1)
    }

    fun format(date: LocalDate): String {
        val j = fromLocalDate(date)
        return "${j.day.fa()} ${monthNames[j.month - 1]} ${j.year.fa()}"
    }

    fun formatWithWeekday(date: LocalDate): String =
        "${weekdayName(date.dayOfWeek)}، ${format(date)}"

    fun weekdayName(d: DayOfWeek): String = weekdayNames[d.value - 1]

    /** Column offset (0 = Saturday) of a date inside a Persian-week grid. */
    fun weekColumn(date: LocalDate): Int = weekOrder.indexOf(date.dayOfWeek.value - 1)
}
