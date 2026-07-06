package com.emir.oructakibi

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.text.Normalizer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppEntry() } }
    }
}

@Composable
private fun AppEntry() {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1800)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(450))
        ) {
            SplashScreen()
        }
        AnimatedVisibility(
            visible = !showSplash,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut()
        ) {
            OrucTakipApp()
        }
    }
}

@Composable
private fun SplashScreen() {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "splashScale"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF102A43), Color(0xFF243B53)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(148.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .scale(scale)
            )
            Spacer(Modifier.height(16.dp))
            Text("ORUÇ REHBERİ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        }
    }
}

/* -------------------- PREFS -------------------- */
private const val PREFS = "ramazan_prefs"
private const val KEY_FAST_DATES = "fasted_dates"   // yyyy-MM-dd
private const val KEY_KAZA_DATES = "kaza_dates"     // yyyy-MM-dd
private const val KEY_CITY = "city"
private const val KEY_REMINDER_ENABLED = "reminder_enabled"
private const val KEY_REMINDER_HOUR = "reminder_hour"
private const val KEY_REMINDER_MINUTE = "reminder_minute"
private const val KEY_LOCATION_ASKED = "location_asked"
private const val KEY_WEEKLY_TARGET = "weekly_target"
private const val KEY_KAZA_WEEKLY_PLAN = "kaza_weekly_plan"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_FONT_SCALE = "font_scale"
private const val KEY_REMINDER_TYPE = "reminder_type"

private fun loadCity(ctx: Context): String =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CITY, "İstanbul")
        ?: "İstanbul"

private fun saveCity(ctx: Context, city: String) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CITY, city).apply()
}

private fun loadReminderEnabled(ctx: Context): Boolean =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_REMINDER_ENABLED, false)

private fun saveReminderEnabled(ctx: Context, enabled: Boolean) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
}

private fun loadReminderHour(ctx: Context): Int =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_REMINDER_HOUR, 20)

private fun loadReminderMinute(ctx: Context): Int =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_REMINDER_MINUTE, 30)

private fun saveReminderTime(ctx: Context, hour: Int, minute: Int) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putInt(KEY_REMINDER_HOUR, hour)
        .putInt(KEY_REMINDER_MINUTE, minute)
        .apply()
}

private fun loadLocationAsked(ctx: Context): Boolean =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_LOCATION_ASKED, false)

private fun saveLocationAsked(ctx: Context, asked: Boolean) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putBoolean(KEY_LOCATION_ASKED, asked)
        .apply()
}

private fun loadWeeklyTarget(ctx: Context): Int =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_WEEKLY_TARGET, 4)

private fun saveWeeklyTarget(ctx: Context, value: Int) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putInt(KEY_WEEKLY_TARGET, value.coerceIn(1, 7)).apply()
}

private fun loadKazaWeeklyPlan(ctx: Context): Int =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_KAZA_WEEKLY_PLAN, 2)

private fun saveKazaWeeklyPlan(ctx: Context, value: Int) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putInt(KEY_KAZA_WEEKLY_PLAN, value.coerceIn(1, 7)).apply()
}

private fun loadThemeMode(ctx: Context): Int =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_THEME_MODE, 0)

private fun saveThemeMode(ctx: Context, value: Int) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putInt(KEY_THEME_MODE, value.coerceIn(0, 2)).apply()
}

private fun loadFontScale(ctx: Context): Float =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_FONT_SCALE, 1f)

private fun saveFontScale(ctx: Context, value: Float) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putFloat(KEY_FONT_SCALE, value.coerceIn(0.9f, 1.3f)).apply()
}

private fun loadReminderType(ctx: Context): Int =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_REMINDER_TYPE, 0)

private fun saveReminderType(ctx: Context, value: Int) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putInt(KEY_REMINDER_TYPE, value.coerceIn(0, 2)).apply()
}

private fun loadDateSet(ctx: Context, key: String): Set<String> {
    val s = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, "") ?: ""
    if (s.isBlank()) return emptySet()
    return s.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
}

private fun saveDateSet(ctx: Context, key: String, set: Set<String>) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(key, set.sorted().joinToString(","))
        .apply()
}

/* -------------------- SERİ (Seçili ay içinde) -------------------- */
private fun longestStreakInMonth(fastedDates: Set<String>, ym: YearMonth): Int {
    var best = 0
    var cur = 0
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    for (day in 1..ym.lengthOfMonth()) {
        val date = ym.atDay(day).format(fmt)
        if (fastedDates.contains(date)) {
            cur++
            best = maxOf(best, cur)
        } else cur = 0
    }
    return best
}

private fun currentStreakInMonth(fastedDates: Set<String>, ym: YearMonth, today: LocalDate): Int {
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val endDay =
        if (today.year == ym.year && today.month == ym.month) today.dayOfMonth else ym.lengthOfMonth()

    var cur = 0
    for (day in endDay downTo 1) {
        val date = ym.atDay(day).format(fmt)
        if (fastedDates.contains(date)) cur++ else break
    }
    return cur
}

private fun longestStreakAllTime(fastedDates: Set<String>): Int {
    if (fastedDates.isEmpty()) return 0
    val parsed = fastedDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
    if (parsed.isEmpty()) return 0

    var best = 1
    var cur = 1
    for (i in 1 until parsed.size) {
        if (parsed[i - 1].plusDays(1) == parsed[i]) {
            cur++
            best = maxOf(best, cur)
        } else {
            cur = 1
        }
    }
    return best
}

/* -------------------- AYLIK / YILLIK ÖZET -------------------- */
data class MonthSummary(
    val ym: YearMonth,
    val fastCount: Int,
    val kazaCount: Int,
    val totalDays: Int
) {
    val emptyCount: Int get() = (totalDays - fastCount - kazaCount).coerceAtLeast(0)
}

private fun summaryForMonth(
    ym: YearMonth,
    fastedDates: Set<String>,
    kazaDates: Set<String>
): MonthSummary {
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val days = ym.lengthOfMonth()
    var fast = 0
    var kaza = 0
    for (d in 1..days) {
        val dateStr = ym.atDay(d).format(fmt)
        if (fastedDates.contains(dateStr)) fast++
        else if (kazaDates.contains(dateStr)) kaza++
    }
    return MonthSummary(ym, fast, kaza, days)
}

private fun summariesForYear(
    year: Int,
    fastedDates: Set<String>,
    kazaDates: Set<String>
): List<MonthSummary> =
    (1..12).map { m -> summaryForMonth(YearMonth.of(year, m), fastedDates, kazaDates) }

/* -------------------- VAKİT ÇEKME (API) -------------------- */
data class Timings(val fajr: String, val maghrib: String)

private fun fetchTimingsByCity(city: String, country: String = "Turkey"): Timings {
    val safeCity = city.trim().replace(" ", "%20")
    val safeCountry = country.trim().replace(" ", "%20")
    val url =
        URL("https://api.aladhan.com/v1/timingsByCity?city=$safeCity&country=$safeCountry&method=13")

    val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 10000
        readTimeout = 10000
        requestMethod = "GET"
    }

    val text = conn.inputStream.bufferedReader().use { it.readText() }
    val json = JSONObject(text)
    val timings = json.getJSONObject("data").getJSONObject("timings")

    fun clean(t: String) = t.trim().take(5)
    return Timings(
        fajr = clean(timings.getString("Fajr")),
        maghrib = clean(timings.getString("Maghrib"))
    )
}

/* -------------------- TAKVİM GRID HESABI -------------------- */
// Pazartesi=0 ... Pazar=6
private fun firstDayOffsetMondayBased(ym: YearMonth): Int {
    val dow = ym.atDay(1).dayOfWeek
    return when (dow) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }
}

private fun monthNameTR(ym: YearMonth): String =
    ym.month.getDisplayName(TextStyle.FULL, Locale("tr", "TR"))

private fun hijriDateText(date: LocalDate): String {
    val h = HijrahDate.from(date)
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr", "TR"))
    return formatter.format(h)
}

private fun verseOfTheDay(date: LocalDate): Pair<String, String> {
    val verses = listOf(
        "Bakara 183" to "Ey iman edenler! Oruç sizden öncekilere farz kılındığı gibi size de farz kılındı.",
        "Bakara 185" to "Ramazan ayı, insanlara yol gösterici olan Kur'an'ın indirildiği aydır.",
        "Zümer 10" to "Sabredenlere mükafatları hesapsızca verilecektir.",
        "İnşirah 5-6" to "Şüphesiz zorlukla beraber bir kolaylık vardır.",
        "Ra'd 28" to "Kalpler ancak Allah'ı anmakla huzur bulur."
    )
    val idx = (date.dayOfYear - 1) % verses.size
    return verses[idx]
}

private fun dailyContentsFallback(): List<Pair<String, String>> = listOf(
    "Ayet • Bakara 183" to "Ey iman edenler! Oruç sizden öncekilere farz kılındığı gibi size de farz kılındı.",
    "Ayet • Bakara 185" to "Ramazan ayı, insanlara yol gösterici olan Kur'an'ın indirildiği aydır.",
    "Hadis" to "Ameller niyetlere göredir. (Buhari, Bed'ü'l-vahy)",
    "Hadis" to "Kolaylaştırın, zorlaştırmayın. (Buhari, İlim)",
    "Motivasyon" to "Az ama sürekli adım, en güçlü ilerlemedir."
)

private fun loadDailyContents(context: Context): List<Pair<String, String>> {
    return runCatching {
        val json = context.assets.open("daily_contents.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(o.optString("title", "İçerik") to o.optString("text", ""))
            }
        }.filter { it.second.isNotBlank() }
    }.getOrElse { dailyContentsFallback() }.ifEmpty { dailyContentsFallback() }
}

private fun badgeLabel(totalFastDays: Int): String = when {
    totalFastDays >= 100 -> "100+ Gün Ustası"
    totalFastDays >= 30 -> "30 Gün İstikrar Rozeti"
    totalFastDays >= 7 -> "7 Gün Başlangıç Rozeti"
    else -> "İlk Rozete Devam"
}

private fun kazaPlanStreakWeeks(today: LocalDate, kazaDates: Set<String>, weeklyPlan: Int): Int {
    if (weeklyPlan <= 0) return 0
    var streak = 0
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    var weekEnd = today
    while (true) {
        val count = (0..6).count { d ->
            kazaDates.contains(weekEnd.minusDays(d.toLong()).format(fmt))
        }
        if (count >= weeklyPlan) {
            streak++
            weekEnd = weekEnd.minusDays(7)
        } else break
    }
    return streak
}

private fun reminderMessage(type: Int): String = when (type) {
    1 -> "İftar sonrası oruç durumunu işaretlemeyi unutma."
    2 -> "Sahur öncesi niyetini tazele, gününü planla."
    else -> "Bugünkü oruç durumunu uygulamada işaretlemeyi unutma."
}

private fun specialDayHint(today: LocalDate): String {
    val hijri = HijrahDate.from(today)
    val hMonth = hijri.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
    val hDay = hijri.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
    return when {
        hMonth == 9 && hDay == 27 -> "Kadir Gecesi yaklaşımı: ibadet planı yap."
        hMonth == 1 && hDay == 10 -> "Bugün Aşure günü."
        today.dayOfWeek == DayOfWeek.MONDAY || today.dayOfWeek == DayOfWeek.THURSDAY -> "Bugün nafile oruç için güzel bir gün."
        else -> "Düzenli takip, istikrarı güçlendirir."
    }
}

private fun normalizeTurkishCity(raw: String): String? {
    val key = raw.trim().lowercase(Locale("tr", "TR"))
    val map = mapOf(
        "adana" to "Adana",
        "adiyaman" to "Adıyaman",
        "afyonkarahisar" to "Afyonkarahisar",
        "agri" to "Ağrı",
        "aksaray" to "Aksaray",
        "amasya" to "Amasya",
        "ankara" to "Ankara",
        "antalya" to "Antalya",
        "ardahan" to "Ardahan",
        "artvin" to "Artvin",
        "aydin" to "Aydın",
        "balikesir" to "Balıkesir",
        "bartin" to "Bartın",
        "batman" to "Batman",
        "bayburt" to "Bayburt",
        "bilecik" to "Bilecik",
        "bingol" to "Bingöl",
        "bitlis" to "Bitlis",
        "bolu" to "Bolu",
        "burdur" to "Burdur",
        "bursa" to "Bursa",
        "canakkale" to "Çanakkale",
        "cankiri" to "Çankırı",
        "corum" to "Çorum",
        "denizli" to "Denizli",
        "diyarbakir" to "Diyarbakır",
        "duzce" to "Düzce",
        "edirne" to "Edirne",
        "elazig" to "Elazığ",
        "erzincan" to "Erzincan",
        "erzurum" to "Erzurum",
        "eskisehir" to "Eskişehir",
        "gaziantep" to "Gaziantep",
        "giresun" to "Giresun",
        "gumushane" to "Gümüşhane",
        "hakkari" to "Hakkari",
        "hatay" to "Hatay",
        "igdir" to "Iğdır",
        "isparta" to "Isparta",
        "istanbul" to "İstanbul",
        "izmir" to "İzmir",
        "kahramanmaras" to "Kahramanmaraş",
        "karabuk" to "Karabük",
        "karaman" to "Karaman",
        "kars" to "Kars",
        "kastamonu" to "Kastamonu",
        "kayseri" to "Kayseri",
        "kilis" to "Kilis",
        "kirikkale" to "Kırıkkale",
        "kirklareli" to "Kırklareli",
        "kirsehir" to "Kırşehir",
        "kocaeli" to "Kocaeli",
        "konya" to "Konya",
        "kutahya" to "Kütahya",
        "malatya" to "Malatya",
        "manisa" to "Manisa",
        "mardin" to "Mardin",
        "mersin" to "Mersin",
        "mugla" to "Muğla",
        "mus" to "Muş",
        "nevsehir" to "Nevşehir",
        "nigde" to "Niğde",
        "ordu" to "Ordu",
        "osmaniye" to "Osmaniye",
        "rize" to "Rize",
        "sakarya" to "Sakarya",
        "samsun" to "Samsun",
        "sanliurfa" to "Şanlıurfa",
        "siirt" to "Siirt",
        "sinop" to "Sinop",
        "sirnak" to "Şırnak",
        "sivas" to "Sivas",
        "tekirdag" to "Tekirdağ",
        "tokat" to "Tokat",
        "trabzon" to "Trabzon",
        "tunceli" to "Tunceli",
        "usak" to "Uşak",
        "van" to "Van",
        "yalova" to "Yalova",
        "yozgat" to "Yozgat",
        "zonguldak" to "Zonguldak"
    )
    return map[key]
}

private val TURKISH_CITIES = listOf(
    "Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Aksaray", "Amasya", "Ankara", "Antalya",
    "Ardahan", "Artvin", "Aydın", "Balıkesir", "Bartın", "Batman", "Bayburt", "Bilecik",
    "Bingöl", "Bitlis", "Bolu", "Burdur", "Bursa", "Çanakkale", "Çankırı", "Çorum", "Denizli",
    "Diyarbakır", "Düzce", "Edirne", "Elazığ", "Erzincan", "Erzurum", "Eskişehir", "Gaziantep",
    "Giresun", "Gümüşhane", "Hakkari", "Hatay", "Iğdır", "Isparta", "İstanbul", "İzmir",
    "Kahramanmaraş", "Karabük", "Karaman", "Kars", "Kastamonu", "Kayseri", "Kilis",
    "Kırıkkale", "Kırklareli", "Kırşehir", "Kocaeli", "Konya", "Kütahya", "Malatya",
    "Manisa", "Mardin", "Mersin", "Muğla", "Muş", "Nevşehir", "Niğde", "Ordu", "Osmaniye",
    "Rize", "Sakarya", "Samsun", "Şanlıurfa", "Siirt", "Sinop", "Şırnak", "Sivas", "Tekirdağ",
    "Tokat", "Trabzon", "Tunceli", "Uşak", "Van", "Yalova", "Yozgat", "Zonguldak"
)

private fun normalizeKey(text: String): String {
    val tr = text.lowercase(Locale("tr", "TR"))
        .replace("province", "")
        .replace("ili", "")
        .replace("ilcesi", "")
        .replace("merkez", "")
        .replace(".", " ")
        .trim()
    val ascii = Normalizer.normalize(tr, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return ascii.replace(Regex("\\s+"), " ").trim()
}

private fun detectTurkishCity(
    countryCode: String?,
    countryName: String?,
    locality: String?,
    subAdminArea: String?,
    adminArea: String?
): String? {
    val isTurkey = countryCode == "TR" || countryName?.contains(
        "Turkey",
        ignoreCase = true
    ) == true || countryName?.contains("Türkiye", ignoreCase = true) == true
    if (!isTurkey) return null

    val candidates = listOfNotNull(locality, subAdminArea, adminArea)
    for (raw in candidates) {
        normalizeTurkishCity(raw)?.let { return it }
    }

    val normalizedCandidates = candidates.map { normalizeKey(it) }
    for (city in TURKISH_CITIES) {
        val cityKey = normalizeKey(city)
        if (normalizedCandidates.any { it.contains(cityKey) || cityKey.contains(it) }) return city
    }
    return null
}

private fun tryUpdateCityFromLocation(
    ctx: Context,
    onCity: (String) -> Unit,
    onError: (String) -> Unit
) {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun handleLocation(lat: Double, lon: Double) {
        runCatching {
            val geocoder = Geocoder(ctx, Locale("tr", "TR"))
            val addr = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            val city = detectTurkishCity(
                countryCode = addr?.countryCode?.uppercase(Locale.ROOT),
                countryName = addr?.countryName,
                locality = addr?.locality,
                subAdminArea = addr?.subAdminArea,
                adminArea = addr?.adminArea
            )
            if (!city.isNullOrBlank()) {
                onCity(city)
            } else {
                onError("Konumdan şehir tespit edilemedi. Lütfen tekrar dene veya listeden seç.")
            }
        }.onFailure {
            onError("Konumdan şehir tespit edilemedi. Lütfen tekrar dene veya listeden seç.")
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider != null) {
            runCatching {
                lm.getCurrentLocation(
                    provider,
                    CancellationSignal(),
                    ctx.mainExecutor
                ) { loc ->
                    if (loc != null) handleLocation(loc.latitude, loc.longitude)
                    else onError("Konum alınamadı. Konum servisinin açık olduğundan emin ol.")
                }
            }.onFailure {
                onError("Konum alınamadı. Konum servisinin açık olduğundan emin ol.")
            }
            return
        }
    }

    val providers = lm.getProviders(true)
    val loc =
        providers.firstNotNullOfOrNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
    if (loc != null) {
        handleLocation(loc.latitude, loc.longitude)
    } else {
        onError("Konum alınamadı. Konum servisinin açık olduğundan emin ol.")
    }
}

/* -------------------- UI -------------------- */
@Composable
fun OrucTakipApp() {
    val ctx = LocalContext.current
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    val citiesTR = TURKISH_CITIES

    var selectedCity by remember { mutableStateOf(loadCity(ctx)) }
    var todayFajr by remember { mutableStateOf("--:--") }
    var todayMaghrib by remember { mutableStateOf("--:--") }
    var loadingTimes by remember { mutableStateOf(false) }
    var errorTimes by remember { mutableStateOf<String?>(null) }

    var currentYM by remember { mutableStateOf(YearMonth.now()) }
    var summaryYear by remember { mutableStateOf(LocalDate.now().year) }

    val fastViewModel: FastViewModel = viewModel()
    val fastedDates by fastViewModel.fastedDates.collectAsState()
    val kazaDates by fastViewModel.kazaDates.collectAsState()
    var reminderEnabled by remember { mutableStateOf(loadReminderEnabled(ctx)) }
    var reminderHour by remember { mutableStateOf(loadReminderHour(ctx)) }
    var reminderMinute by remember { mutableStateOf(loadReminderMinute(ctx)) }
    var reminderType by remember { mutableStateOf(loadReminderType(ctx)) }
    var weeklyTarget by remember { mutableStateOf(loadWeeklyTarget(ctx)) }
    var kazaWeeklyPlan by remember { mutableStateOf(loadKazaWeeklyPlan(ctx)) }
    var themeMode by remember { mutableStateOf(loadThemeMode(ctx)) }
    var fontScale by remember { mutableStateOf(loadFontScale(ctx)) }
    var showLocationAsk by remember { mutableStateOf(!loadLocationAsked(ctx)) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    var tabIndex by remember { mutableStateOf(0) } // 0=Takvim, 1=Yıllık, 2=Kaza, 3=Ayarlar

    val suAnkiSeri = currentStreakInMonth(fastedDates, currentYM, today)
    val enUzunSeri = longestStreakInMonth(fastedDates, currentYM)
    val genelEnUzunSeri = longestStreakAllTime(fastedDates)

    val haftaGunleri = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    val offset = firstDayOffsetMondayBased(currentYM)
    val daysInMonth = currentYM.lengthOfMonth()
    val grid = List(offset) { 0 } + (1..daysInMonth).toList()

    val monthSummary = remember(currentYM, fastedDates, kazaDates) {
        summaryForMonth(currentYM, fastedDates, kazaDates)
    }

    val yearSummaries = remember(summaryYear, fastedDates, kazaDates) {
        summariesForYear(summaryYear, fastedDates, kazaDates)
    }
    val yearFastTotal = yearSummaries.sumOf { it.fastCount }
    val yearKazaTotal = yearSummaries.sumOf { it.kazaCount }
    val yearTotalDays = yearSummaries.sumOf { it.totalDays }.coerceAtLeast(1)
    val yearRate = (yearFastTotal * 100f) / yearTotalDays

    val bgPainter = runCatching { painterResource(id = R.drawable.ramadan_bg) }.getOrNull()
    val isRamadan = remember(today) {
        HijrahDate.from(today).get(java.time.temporal.ChronoField.MONTH_OF_YEAR) == 9
    }
    val topCardColor = if (isRamadan) Color(0xFF0B6E4F) else Color(0xFF1B5E20)
    val maghribParsed = runCatching { LocalTime.parse(todayMaghrib) }.getOrNull()
    val isAfterIftar = maghribParsed != null && LocalTime.now().isAfter(maghribParsed)
    val systemDark = isSystemInDarkTheme()
    val darkByTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> systemDark || isAfterIftar
    }
    val fallbackBg = if (isRamadan || darkByTheme) {
        Brush.verticalGradient(listOf(Color(0xFF102A43), Color(0xFF243B53)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFDF6EC), Color(0xFFE8F5E9)))
    }
    val nightOverlay = if (darkByTheme || isAfterIftar) Color(0x55000000) else Color(0xAAFFFFFF)
    val contents = remember { loadDailyContents(ctx) }
    val pagerState = rememberPagerState(pageCount = { contents.size })
    val totalFastDays = fastedDates.size
    val badge = badgeLabel(totalFastDays)
    val kazaDebtTotal = kazaDates.size
    val kazaRemaining = kazaDebtTotal
    val kazaLoadRatio = (kazaDebtTotal.coerceAtMost(30) / 30f)
    val estimatedFinishWeeks =
        if (kazaWeeklyPlan > 0) kotlin.math.ceil(kazaRemaining.toDouble() / kazaWeeklyPlan)
            .toInt() else 0
    val estimatedFinishDate = today.plusWeeks(estimatedFinishWeeks.toLong())
    val kazaStreakWeeks = kazaPlanStreakWeeks(today, kazaDates, kazaWeeklyPlan)
    val hiddenBadge = if (kazaStreakWeeks >= 4) "Kararlı Borç Ödeyici" else null
    val longFastBonus = runCatching {
        if (todayFajr != "--:--" && todayMaghrib != "--:--") {
            val f = LocalTime.parse(todayFajr)
            val m = LocalTime.parse(todayMaghrib)
            val dur = java.time.Duration.between(f, m).toHours()
            if (dur >= 15) 20 else 0
        } else 0
    }.getOrDefault(0)
    val score = (totalFastDays * 10) + (kazaDates.size * 5) + longFastBonus + (kazaStreakWeeks * 15)

    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                tryUpdateCityFromLocation(
                    ctx = ctx,
                    onCity = { city -> selectedCity = city },
                    onError = { msg -> errorTimes = msg }
                )
            }
            saveLocationAsked(ctx, true)
            showLocationAsk = false
        }
    )

    fun refreshTimes() {
        scope.launch {
            loadingTimes = true
            errorTimes = null
            try {
                saveCity(ctx, selectedCity)
                val t = withContext(Dispatchers.IO) { fetchTimingsByCity(selectedCity) }
                todayFajr = t.fajr
                todayMaghrib = t.maghrib
            } catch (_: Exception) {
                errorTimes = "Vakit alınamadı. İnternet var mı?"
            } finally {
                loadingTimes = false
                updateOrucWidget(ctx)
            }
        }
    }

    LaunchedEffect(selectedCity) {
        refreshTimes()
    }

    LaunchedEffect(Unit) {
        if (reminderEnabled) {
            scheduleDailyReminder(
                context = ctx,
                hour = reminderHour,
                minute = reminderMinute,
                content = reminderMessage(reminderType)
            )
        }
    }
    LaunchedEffect(isRamadan) {
        if (isRamadan) currentYM = YearMonth.now()
    }

    if (showLocationAsk) {
        AlertDialog(
            onDismissRequest = {
                saveLocationAsked(ctx, true)
                showLocationAsk = false
            },
            title = { Text("Konum Kullanılsın mı?") },
            text = { Text("Şehri otomatik bulup vakitleri buna göre ayarlayabilirim. Sadece Türkiye illeri seçilir.") },
            confirmButton = {
                TextButton(onClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }) { Text("Evet") }
            },
            dismissButton = {
                TextButton(onClick = {
                    saveLocationAsked(ctx, true)
                    showLocationAsk = false
                }) { Text("Hayır") }
            }
        )
    }

    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
    Box(Modifier.fillMaxSize()) {
        if (bgPainter != null) {
            Image(
                painter = bgPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(nightOverlay)
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(fallbackBg)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            // ÜST KART
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = topCardColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Oruç Takibi",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Şu anki seri: $suAnkiSeri  •  En uzun seri: $enUzunSeri",
                        color = Color.White, fontSize = 13.sp * fontScale
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Hicri: ${hijriDateText(today)}",
                        color = Color.White, fontSize = 13.sp * fontScale
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Seçili ay: ${monthNameTR(currentYM)} ${currentYM.year}",
                        color = Color.White,
                        fontSize = 13.sp * fontScale,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Rozet: $badge",
                        color = Color.White, fontSize = 12.sp * fontScale
                    )
                    if (hiddenBadge != null) {
                        Text(
                            "Gizli Rozet: $hiddenBadge",
                            color = Color(0xFFFFF59D), fontSize = 12.sp * fontScale
                        )
                    }
                    Text(
                        "Puan: $score",
                        color = Color.White, fontSize = 12.sp * fontScale
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Sekmeler
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Takvim") })
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text("Yıllık Özet") })
                Tab(
                    selected = tabIndex == 2,
                    onClick = { tabIndex = 2 },
                    text = { Text("Kaza") })
                Tab(
                    selected = tabIndex == 3,
                    onClick = { tabIndex = 3 },
                    text = { Text("Ayarlar") })
            }

            Spacer(Modifier.height(10.dp))

            if (tabIndex == 0) {
                /* ===================== TAKVİM ===================== */

                // ŞEHİR + VAKİTLER
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Şehir ve Vakitler", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))

                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Şehir: $selectedCity")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                citiesTR.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c) },
                                        onClick = {
                                            expanded = false
                                            selectedCity = c
                                            refreshTimes()
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        when {
                            loadingTimes -> Text("Vakitler yükleniyor...", fontSize = 13.sp)
                            errorTimes != null -> Text(
                                errorTimes!!,
                                color = Color.Red,
                                fontSize = 13.sp
                            )

                            else -> {
                                Text("Sahur (İmsak): $todayFajr", fontSize = 13.sp)
                                Text("İftar: $todayMaghrib", fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val weekFastCount = (0..6).count { delta ->
                        fastedDates.contains(today.minusDays(delta.toLong()).format(fmt))
                    }
                    Column(Modifier.padding(12.dp)) {
                        Text("Haftalık Hedef", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Bu hafta: $weekFastCount / $weeklyTarget", fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = {
                                (weekFastCount.toFloat() / weeklyTarget.coerceAtLeast(1)).coerceIn(
                                    0f,
                                    1f
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Günlük İçerik", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            val item = contents[page]
                            Column {
                                Text(
                                    item.first,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(item.second, fontSize = 13.sp * fontScale)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${pagerState.currentPage + 1}/${contents.size}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Akıllı Öneri", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        if (kazaRemaining > 0) {
                            Text(
                                "Kalan kaza borcu: $kazaRemaining gün. Bugün telafi etmek ister misin?",
                                fontSize = 13.sp
                            )
                        } else {
                            Text(specialDayHint(today), fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // AY GEÇİŞİ
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        enabled = !isRamadan,
                        onClick = { currentYM = currentYM.minusMonths(1) }
                    ) { Text("◀") }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isRamadan) "Ramazan Modu • ${monthNameTR(currentYM)} ${currentYM.year}" else "${
                            monthNameTR(
                                currentYM
                            )
                        } ${currentYM.year}",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        enabled = !isRamadan,
                        onClick = { currentYM = currentYM.plusMonths(1) }
                    ) { Text("▶") }
                }

                Spacer(Modifier.height(10.dp))

                // Aylık Özet
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Aylık Özet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Tuttu: ${monthSummary.fastCount}", fontSize = 13.sp)
                        Text("Kaza: ${monthSummary.kazaCount}", fontSize = 13.sp)
                        Text("Boş: ${monthSummary.emptyCount}", fontSize = 13.sp)

                        Spacer(Modifier.height(8.dp))
                        val total = monthSummary.totalDays.coerceAtLeast(1)
                        LinearProgressIndicator(progress = { monthSummary.fastCount.toFloat() / total })
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tuttu oranı: %${((monthSummary.fastCount * 100f) / total).toInt()}",
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Haftanın günleri
                Row(Modifier.fillMaxWidth()) {
                    haftaGunleri.forEach { g ->
                        Text(
                            g,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // TAKVİM
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(grid) { d ->
                        if (d == 0) {
                            Box(Modifier.aspectRatio(1f))
                        } else {
                            val dateStr = currentYM.atDay(d).format(fmt)
                            val isFast = fastedDates.contains(dateStr)
                            val isKaza = kazaDates.contains(dateStr)
                            val isToday =
                                (today.year == currentYM.year && today.month == currentYM.month && today.dayOfMonth == d)

                            val (bg, label) = when {
                                isFast -> Color(0xFFA5D6A7) to "T"
                                isKaza -> Color(0xFFFFF59D) to "K"
                                else -> Color(0xFFFFCDD2) to "X"
                            }

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .border(
                                        if (isToday) 3.dp else 1.dp,
                                        Color.Black,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        fastViewModel.cycleDate(dateStr)
                                        updateOrucWidget(ctx)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$d", fontWeight = FontWeight.Bold)
                                    Text(label)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Açıklama
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Açıklamalar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("T (Tuttu): O gün oruç tutuldu", fontSize = 13.sp)
                        Text("K (Kaza): O gün tutulamadı, sonradan kaza edilecek", fontSize = 13.sp)
                        Text("X (Boş): O gün tutulmadı veya henüz işaretlenmedi", fontSize = 13.sp)
                    }
                }

            } else if (tabIndex == 1) {
                /* ===================== YILLIK ÖZET ===================== */

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Yıl Seç", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(onClick = { summaryYear -= 1 }) { Text("◀") }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$summaryYear",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { summaryYear += 1 }) { Text("▶") }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Yıllık toplam tuttu: $yearFastTotal", fontSize = 13.sp)
                        Text("Yıllık toplam kaza: $yearKazaTotal", fontSize = 13.sp)
                        Text("Yıllık tutma oranı: %${yearRate.toInt()}", fontSize = 13.sp)
                        Text("Genel en uzun seri: $genelEnUzunSeri gün", fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Aylık Bar Chart", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            yearSummaries.forEach { ms ->
                                val ratio = ms.fastCount.toFloat() / ms.totalDays.coerceAtLeast(1)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(ratio.coerceIn(0.05f, 1f))
                                        .background(Color(0xFF66BB6A), RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Heatmap (Son 35 Gün)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        val heatDates =
                            (34 downTo 0).map { today.minusDays(it.toLong()).format(fmt) }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            heatDates.chunked(7).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    row.forEach { ds ->
                                        val c = when {
                                            fastedDates.contains(ds) -> Color(0xFF43A047)
                                            kazaDates.contains(ds) -> Color(0xFFFBC02D)
                                            else -> Color(0xFFE0E0E0)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .background(c, RoundedCornerShape(3.dp))
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("12 Ay Raporu", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))

                        //  FIX: Changed LazyColumn to Column to prevent crash inside a verticalScroll parent.
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            yearSummaries.forEach { ms ->
                                val total = ms.totalDays.coerceAtLeast(1)
                                val p = ms.fastCount.toFloat() / total

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF7F7F7),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Row(Modifier.fillMaxWidth()) {
                                            Text(
                                                monthNameTR(ms.ym),
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                "T:${ms.fastCount}  K:${ms.kazaCount}",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { p },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Tuttu oranı: %${(p * 100).toInt()}  •  Boş: ${ms.emptyCount}",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (tabIndex == 2) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Kaza Yönetimi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Toplam kaza borcu: $kazaDebtTotal gün", fontSize = 13.sp)
                        Text("Kalan borç: $kazaRemaining gün", fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { kazaLoadRatio },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Bu ekran takvimle otomatik senkron çalışır:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            "Takvimde bir günü K yaparsan borç +1 olur.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            "Takvimde K işaretini kaldırırsan borç -1 olur.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Borç eklemek/silmek için Takvim sekmesinden günleri işaretle.",
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Kaza Planlayıcı", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Haftalık kaza hedefi: $kazaWeeklyPlan gün", fontSize = 13.sp)
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(onClick = {
                                kazaWeeklyPlan = (kazaWeeklyPlan - 1).coerceAtLeast(1)
                                saveKazaWeeklyPlan(ctx, kazaWeeklyPlan)
                            }) { Text("-") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                kazaWeeklyPlan = (kazaWeeklyPlan + 1).coerceAtMost(7)
                                saveKazaWeeklyPlan(ctx, kazaWeeklyPlan)
                            }) { Text("+") }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Tahmini bitiş: $estimatedFinishDate", fontSize = 13.sp)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Ayarlar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Günlük hatırlatma",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp
                            )
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { enabled ->
                                    reminderEnabled = enabled
                                    saveReminderEnabled(ctx, enabled)
                                    if (enabled) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        scheduleDailyReminder(
                                            ctx,
                                            reminderHour,
                                            reminderMinute,
                                            reminderMessage(reminderType)
                                        )
                                    } else {
                                        cancelDailyReminder(ctx)
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Hatırlatma saati", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = {
                                reminderHour = if (reminderHour == 0) 23 else reminderHour - 1
                                saveReminderTime(ctx, reminderHour, reminderMinute)
                                if (reminderEnabled) {
                                    scheduleDailyReminder(
                                        ctx,
                                        reminderHour,
                                        reminderMinute,
                                        reminderMessage(reminderType)
                                    )
                                }
                            }) { Text("Saat -") }

                            Spacer(Modifier.width(8.dp))
                            Text(
                                String.format(
                                    Locale.getDefault(),
                                    "%02d:%02d",
                                    reminderHour,
                                    reminderMinute
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(8.dp))

                            OutlinedButton(onClick = {
                                reminderHour = (reminderHour + 1) % 24
                                saveReminderTime(ctx, reminderHour, reminderMinute)
                                if (reminderEnabled) {
                                    scheduleDailyReminder(
                                        ctx,
                                        reminderHour,
                                        reminderMinute,
                                        reminderMessage(reminderType)
                                    )
                                }
                            }) { Text("Saat +") }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = {
                                reminderMinute = if (reminderMinute < 5) 55 else reminderMinute - 5
                                saveReminderTime(ctx, reminderHour, reminderMinute)
                                if (reminderEnabled) {
                                    scheduleDailyReminder(
                                        ctx,
                                        reminderHour,
                                        reminderMinute,
                                        reminderMessage(reminderType)
                                    )
                                }
                            }) { Text("Dakika -") }

                            Spacer(Modifier.width(8.dp))
                            Text(
                                "5 dk adım",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.width(8.dp))

                            OutlinedButton(onClick = {
                                reminderMinute = (reminderMinute + 5) % 60
                                saveReminderTime(ctx, reminderHour, reminderMinute)
                                if (reminderEnabled) {
                                    scheduleDailyReminder(
                                        ctx,
                                        reminderHour,
                                        reminderMinute,
                                        reminderMessage(reminderType)
                                    )
                                }
                            }) { Text("Dakika +") }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Hatırlatma tipi", fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            listOf(
                                "Genel",
                                "İftar Sonrası",
                                "Sahur Öncesi"
                            ).forEachIndexed { idx, label ->
                                OutlinedButton(
                                    onClick = {
                                        reminderType = idx
                                        saveReminderType(ctx, reminderType)
                                        if (reminderEnabled) {
                                            scheduleDailyReminder(
                                                ctx,
                                                reminderHour,
                                                reminderMinute,
                                                reminderMessage(reminderType)
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) { Text(label) }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Haftalık hedef", fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = {
                                weeklyTarget = (weeklyTarget - 1).coerceAtLeast(1)
                                saveWeeklyTarget(ctx, weeklyTarget)
                            }) { Text("-") }
                            Spacer(Modifier.width(8.dp))
                            Text("$weeklyTarget gün", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                weeklyTarget = (weeklyTarget + 1).coerceAtMost(7)
                                saveWeeklyTarget(ctx, weeklyTarget)
                            }) { Text("+") }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Tema", fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            listOf("Otomatik", "Aydınlık", "Koyu").forEachIndexed { idx, label ->
                                OutlinedButton(
                                    onClick = {
                                        themeMode = idx
                                        saveThemeMode(ctx, themeMode)
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) { Text(label) }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Yazı boyutu", fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = {
                                fontScale = (fontScale - 0.05f).coerceAtLeast(0.9f)
                                saveFontScale(ctx, fontScale)
                            }) { Text("A-") }
                            Spacer(Modifier.width(8.dp))
                            Text(String.format(Locale.getDefault(), "%.2f", fontScale))
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                fontScale = (fontScale + 0.05f).coerceAtMost(1.3f)
                                saveFontScale(ctx, fontScale)
                            }) { Text("A+") }
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val shareText =
                                    "Bu ay ${monthSummary.fastCount} gün oruç tuttum. Ramazan Oruç Takibi ile takip ediyorum."
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                ctx.startActivity(Intent.createChooser(intent, "Paylaş"))
                            }
                        ) { Text("Aylık Sonucu Paylaş") }

                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val json = JSONObject()
                                json.put("city", selectedCity)
                                json.put("fasted", fastedDates.joinToString(","))
                                json.put("kaza", kazaDates.joinToString(","))
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, json.toString())
                                }
                                ctx.startActivity(
                                    Intent.createChooser(
                                        intent,
                                        "Yedek Metnini Paylaş"
                                    )
                                )
                            }
                        ) { Text("Yedek Dışa Aktar (JSON)") }

                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showImportDialog = true }
                        ) { Text("Yedek İçe Aktar (JSON)") }

                        Spacer(Modifier.height(10.dp))
                        Text("Play Store notu", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Uygulama internet ve bildirim izni kullanır. Store sayfasına gizlilik politikası linki eklemeyi unutma.",
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val hasPermission =
                                    ContextCompat.checkSelfPermission(
                                        ctx,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED ||
                                            ContextCompat.checkSelfPermission(
                                                ctx,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    tryUpdateCityFromLocation(
                                        ctx = ctx,
                                        onCity = { city -> selectedCity = city },
                                        onError = { msg -> errorTimes = msg }
                                    )
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        ) { Text("Konumdan Şehir Bul") }
                    }
                }
            }
        }
    }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Yedek İçe Aktar") },
            text = {
                Column {
                    Text("Dışa aktardığın JSON metnini buraya yapıştır.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        val j = JSONObject(importText)
                        val city = j.optString("city", selectedCity)
                        selectedCity = city
                        val fastSet = j.optString("fasted", "").split(",").map { it.trim() }
                            .filter { it.isNotBlank() }.toSet()
                        val kazaSet = j.optString("kaza", "").split(",").map { it.trim() }
                            .filter { it.isNotBlank() }.toSet()
                        fastedDates.forEach {
                            if (!fastSet.contains(it) && !kazaSet.contains(it)) fastViewModel.cycleDate(
                                it
                            )
                        }
                        kazaDates.forEach {
                            if (!kazaSet.contains(it) && !fastSet.contains(it)) fastViewModel.cycleDate(
                                it
                            )
                        }
                        fastSet.forEach { if (!fastedDates.contains(it)) fastViewModel.cycleDate(it) }
                        kazaSet.forEach { if (!kazaDates.contains(it)) fastViewModel.cycleDate(it) }
                    }
                    showImportDialog = false
                }) { Text("Uygula") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("İptal") }
            }
        )
    }
}
