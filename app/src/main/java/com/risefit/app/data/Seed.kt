package com.risefit.app.data

/** Default data created on the very first launch. Everything is editable or deletable by the user. */
object Seed {

    // Day indexes: Monday = 0 ... Sunday = 6. Bit i of a mask = day index i.
    private const val MON = 1 shl 0
    private const val TUE = 1 shl 1
    private const val WED = 1 shl 2
    private const val THU = 1 shl 3
    private const val FRI = 1 shl 4
    private const val SAT = 1 shl 5
    private const val SUN = 1 shl 6
    private const val ALL = 0b1111111

    fun schedule(): List<ScheduleItem> = listOf(
        ScheduleItem(title = "بیدار شدن، آب خوردن و تحرک سبک", hour = 6, minute = 30, category = "WAKE", daysMask = ALL),
        ScheduleItem(title = "مدرسه", hour = 7, minute = 30, category = "SCHOOL", daysMask = SAT or SUN or MON or TUE or WED),
        ScheduleItem(title = "ناهار", hour = 14, minute = 0, category = "MEAL", daysMask = ALL),
        ScheduleItem(title = "باشگاه بدنسازی", hour = 16, minute = 0, category = "GYM", daysMask = SAT or MON or WED),
        ScheduleItem(title = "فوتسال", hour = 17, minute = 30, category = "FUTSAL", daysMask = TUE or THU),
        ScheduleItem(title = "تمرین شکم", hour = 19, minute = 0, category = "HOME", daysMask = SUN or TUE or THU),
        ScheduleItem(title = "آرام‌سازی و آماده شدن برای خواب", hour = 22, minute = 0, category = "SLEEP", daysMask = ALL)
    )

    fun homeExercises(): List<HomeExercise> = listOf(
        HomeExercise(
            group = "ABS", name = "پلانک",
            howTo = "روی ساعد و پنجه پا قرار بگیر. بدن را صاف نگه دار، شکم را سفت کن و کمر را نینداز. عادی نفس بکش.",
            durationSec = 30, restSec = 45
        ),
        HomeExercise(
            group = "ABS", name = "کرانچ",
            howTo = "به پشت دراز بکش و زانوها را خم کن. دست‌ها کنار سر؛ شانه‌ها را آرام بالا بیاور و پایین برگرد. گردن را نکش.",
            reps = 15, restSec = 45
        ),
        HomeExercise(
            group = "ABS", name = "بالا آوردن پا",
            howTo = "به پشت دراز بکش، پاها صاف. با کنترل پاها را تا حدود ۹۰ درجه بالا ببر و آرام پایین بیاور. کمر از زمین جدا نشود.",
            reps = 10, restSec = 60
        ),
        HomeExercise(
            group = "ABS", name = "دوچرخه هوایی",
            howTo = "به پشت دراز بکش، دست‌ها کنار سر. آرنج را به زانوی مخالف نزدیک کن و پاها را مثل رکاب زدن عوض کن.",
            reps = 20, restSec = 45
        ),
        HomeExercise(
            group = "ABS", name = "کوهنورد",
            howTo = "در حالت شنا قرار بگیر و زانوها را یکی‌یکی به سمت سینه بیاور. شکم سفت و باسن پایین باشد.",
            durationSec = 30, restSec = 45
        ),
        HomeExercise(
            group = "BODY", name = "شنا",
            howTo = "دست‌ها کمی بازتر از شانه، بدن صاف. سینه را تا نزدیک زمین پایین ببر و بالا بیا. اگر سخت بود زانوها روی زمین باشد.",
            reps = 10, restSec = 60
        ),
        HomeExercise(
            group = "BODY", name = "اسکوات",
            howTo = "پاها به اندازه عرض شانه. باسن را عقب و پایین ببر، زانوها در راستای پنجه و پاشنه روی زمین. بعد بایست.",
            reps = 15, restSec = 60
        ),
        HomeExercise(
            group = "BODY", name = "لانج",
            howTo = "یک قدم بلند جلو بردار و هر دو زانو را حدود ۹۰ درجه خم کن. بعد برگرد و پای دیگر را انجام بده.",
            reps = 10, restSec = 60
        ),
        HomeExercise(
            group = "BODY", name = "پل باسن",
            howTo = "به پشت دراز بکش، زانوها خم. باسن را بالا بیاور تا بدن از شانه تا زانو صاف شود، یک ثانیه نگه دار و پایین بیا.",
            reps = 15, restSec = 45
        ),
        HomeExercise(
            group = "BODY", name = "جامپینگ جک",
            howTo = "با پرش، پاها را باز و دست‌ها را بالای سر ببر و دوباره جمع کن. برای گرم‌کردن عالی است.",
            durationSec = 30, restSec = 30
        )
    )

    fun gymExercises(): List<GymExercise> = listOf(
        GymExercise(
            dayIndex = 5, name = "پرس سینه هالتر", sets = 4, reps = 10, restSec = 90,
            guide = "کتف‌ها را جمع کن، هالتر را تا پایین سینه بیاور و با کنترل بالا ببر. برای وزنه سنگین از همراه کمک بگیر."
        ),
        GymExercise(
            dayIndex = 5, name = "بارفیکس یا لت از جلو", sets = 3, reps = 10, restSec = 90,
            guide = "با کشیدن آرنج‌ها به سمت پایین، کتف‌ها را به هم نزدیک کن. از تاب دادن بدن پرهیز کن."
        ),
        GymExercise(
            dayIndex = 0, name = "اسکوات با هالتر", sets = 4, reps = 8, restSec = 120,
            guide = "کمر صاف، نگاه جلو. تا حدی پایین برو که فرم حفظ شود. با وزنه سبک شروع کن."
        ),
        GymExercise(
            dayIndex = 0, name = "پرس پا", sets = 3, reps = 12, restSec = 90,
            guide = "کف پا کامل روی صفحه، زانوها را قفل نکن و حرکت را آرام انجام بده."
        ),
        GymExercise(
            dayIndex = 2, name = "پرس سرشانه دمبل", sets = 3, reps = 10, restSec = 90,
            guide = "دمبل‌ها را از کنار گوش بالای سر ببر؛ کمر قوس نیندازد."
        ),
        GymExercise(
            dayIndex = 2, name = "جلو بازو دمبل", sets = 3, reps = 12, restSec = 60,
            guide = "آرنج‌ها ثابت کنار بدن، حرکت فقط از ساعد. پایین آوردن را آرام انجام بده."
        )
    )
}
