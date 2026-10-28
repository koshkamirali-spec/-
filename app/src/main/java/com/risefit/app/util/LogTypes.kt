package com.risefit.app.util

object LogTypes {
    const val GYM = "GYM"
    const val ABS = "ABS"
    const val HOME = "HOME"
    const val FUTSAL = "FUTSAL"

    val all = listOf(GYM, ABS, HOME, FUTSAL)

    fun label(type: String): String = when (type) {
        GYM -> "باشگاه"
        ABS -> "تمرین شکم"
        HOME -> "تمرین خانه"
        FUTSAL -> "فوتسال"
        else -> "تمرین"
    }

    fun points(type: String): Int = when (type) {
        GYM, FUTSAL -> 15
        else -> 10
    }
}

object Categories {
    val all = listOf("WAKE", "SCHOOL", "GYM", "HOME", "FUTSAL", "MEAL", "SLEEP", "OTHER")

    fun label(c: String): String = when (c) {
        "WAKE" -> "بیدار شدن"
        "SCHOOL" -> "مدرسه"
        "GYM" -> "باشگاه"
        "HOME" -> "تمرین خانه"
        "FUTSAL" -> "فوتسال"
        "MEAL" -> "غذا"
        "SLEEP" -> "خواب"
        else -> "سایر"
    }

    /** Categories that count as a training session (a day without any of them is a rest day). */
    fun isTraining(c: String): Boolean = c == "GYM" || c == "HOME" || c == "FUTSAL"
}
