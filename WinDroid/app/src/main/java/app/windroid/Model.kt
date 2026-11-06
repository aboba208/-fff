package app.windroid

import android.content.Context
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

data class Mode(val id: String, val name: String, val cores: Int, val fps: Int, val res: Int, val dyn: Int, val locked: Boolean = false)
data class Cfg(val id: String, val name: String, val api: String, val drv: String, val wine: String, val vsync: Boolean, val fsr: Boolean, val locked: Boolean = false)
data class App(val id: String, val name: String, val exe: String, val icon: String, val mode: String, val cfg: String)

fun uid() = java.util.UUID.randomUUID().toString().take(6)

/** Определение SoC: Snapdragon -> Turnip, остальные (Mali/Xclipse/PowerVR) -> системный Vulkan через Zink. */
object Soc {
    fun label(): String = (if (Build.VERSION.SDK_INT >= 31) Build.SOC_MANUFACTURER + " " + Build.SOC_MODEL else Build.HARDWARE)
    fun driver(): String {
        val s = (label() + " " + Build.HARDWARE).lowercase()
        return if ("qualcomm" in s || "qcom" in s || "snapdragon" in s) "Turnip" else "Zink"
    }
}

val BUILTIN_MODES = listOf(
    Mode("eco", "🌿 Эконом", 2, 30, 60, 0, true),
    Mode("normal", "⚖️ Обычное", 4, 45, 80, 1, true),
    Mode("fast", "🚀 Быстро", 8, 60, 100, 3, true))
val DEFAULT_CFG get() = Cfg("def", "По умолчанию", "Vulkan", Soc.driver(), "Wine 9.0", true, false, true)

class Store(c: Context) {
    private val p = c.getSharedPreferences("wd", Context.MODE_PRIVATE)
    val modes = mutableStateListOf<Mode>()
    val cfgs = mutableStateListOf<Cfg>()
    val apps = mutableStateListOf<App>()

    init {
        modes.addAll(BUILTIN_MODES); cfgs.add(DEFAULT_CFG)   // встроенные всегда восстанавливаются и не меняются
        runCatching {
            val j = JSONObject(p.getString("d", "{}")!!)
            j.optJSONArray("m")?.let { for (i in 0 until it.length()) it.getJSONObject(i).let { o ->
                modes.add(Mode(o.getString("id"), o.getString("n"), o.getInt("c"), o.getInt("f"), o.getInt("r"), o.getInt("d"))) } }
            j.optJSONArray("c")?.let { for (i in 0 until it.length()) it.getJSONObject(i).let { o ->
                cfgs.add(Cfg(o.getString("id"), o.getString("n"), o.getString("a"), o.getString("v"), o.getString("w"), o.getBoolean("s"), o.getBoolean("u"))) } }
            j.optJSONArray("a")?.let { for (i in 0 until it.length()) it.getJSONObject(i).let { o ->
                apps.add(App(o.getString("id"), o.getString("n"), o.getString("x"), o.getString("i"), o.getString("m"), o.getString("c"))) } }
        }
    }

    fun save() {
        val j = JSONObject()
        j.put("m", JSONArray().apply { modes.filter { !it.locked }.forEach { put(JSONObject().put("id", it.id).put("n", it.name).put("c", it.cores).put("f", it.fps).put("r", it.res).put("d", it.dyn)) } })
        j.put("c", JSONArray().apply { cfgs.filter { !it.locked }.forEach { put(JSONObject().put("id", it.id).put("n", it.name).put("a", it.api).put("v", it.drv).put("w", it.wine).put("s", it.vsync).put("u", it.fsr)) } })
        j.put("a", JSONArray().apply { apps.forEach { put(JSONObject().put("id", it.id).put("n", it.name).put("x", it.exe).put("i", it.icon).put("m", it.mode).put("c", it.cfg)) } })
        p.edit().putString("d", j.toString()).apply()
    }

    fun put(a: App) { val i = apps.indexOfFirst { it.id == a.id }; if (i >= 0) apps[i] = a else apps.add(a); save() }
    fun put(m: Mode) { if (m.locked) return; val i = modes.indexOfFirst { it.id == m.id }; if (i >= 0) modes[i] = m else modes.add(m); save() }
    fun put(c: Cfg) { if (c.locked) return; val i = cfgs.indexOfFirst { it.id == c.id }; if (i >= 0) cfgs[i] = c else cfgs.add(c); save() }
    fun delApp(id: String) { apps.removeAll { it.id == id }; save() }
    fun delMode(id: String) { modes.removeAll { it.id == id && !it.locked }; for (i in apps.indices) if (apps[i].mode == id) apps[i] = apps[i].copy(mode = "normal"); save() }
    fun delCfg(id: String) { cfgs.removeAll { it.id == id && !it.locked }; for (i in apps.indices) if (apps[i].cfg == id) apps[i] = apps[i].copy(cfg = "def"); save() }
}
