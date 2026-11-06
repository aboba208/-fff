package app.windroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val st = Store(this)
        setContent { MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) { Root(st) } }
    }
}

@Composable fun Dlg(title: String, close: () -> Unit, ok: (() -> Unit)? = null, okText: String = "Сохранить",
                    neutral: Pair<String, () -> Unit>? = null, body: @Composable ColumnScope.() -> Unit) =
    AlertDialog(onDismissRequest = close, title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp), content = body) },
        confirmButton = { if (ok != null) TextButton({ ok(); close() }) { Text(okText) } },
        dismissButton = { Row {
            neutral?.let { TextButton(it.second) { Text(it.first, color = MaterialTheme.colorScheme.error) } }
            TextButton(close) { Text("Закрыть") } } })

@Composable fun Confirm(title: String, msg: String, no: () -> Unit, yes: () -> Unit) =
    AlertDialog(onDismissRequest = no, title = { Text(title) }, text = { Text(msg) },
        confirmButton = { TextButton(yes) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(no) { Text("Отмена") } })

@Composable fun Sl(label: String, v: Int, r: IntRange, step: Int = 1, enabled: Boolean = true, on: (Int) -> Unit) {
    Text("$label: $v", fontSize = 13.sp)
    Slider(v.toFloat(), { on(((it / step).toInt() * step).coerceIn(r.first, r.last)) }, valueRange = r.first.toFloat()..r.last.toFloat(), enabled = enabled)
}

@Composable fun Chips(opts: List<Pair<String, String>>, cur: String, enabled: Boolean = true, on: (String) -> Unit) =
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opts.forEach { (id, n) -> FilterChip(id == cur, { on(id) }, { Text(n) }, enabled = enabled) }
    }

@Composable fun ModeDlg(st: Store, m: Mode, nw: Boolean, close: () -> Unit) {
    var n by remember { mutableStateOf(m.name) }; var c by remember { mutableIntStateOf(m.cores) }
    var f by remember { mutableIntStateOf(m.fps) }; var r by remember { mutableIntStateOf(m.res) }
    var d by remember { mutableIntStateOf(m.dyn) }; var del by remember { mutableStateOf(false) }
    val L = m.locked
    if (del) { Confirm("Удалить режим?", "«${m.name}» будет удалён, программы перейдут на «Обычное».", { del = false }, { st.delMode(m.id); close() }); return }
    Dlg(if (nw) "Новый режим" else m.name, close,
        ok = { if (L) st.put(m.copy(id = uid(), name = m.name + " (копия)", locked = false)) else if (n.isNotBlank()) st.put(m.copy(name = n.trim(), cores = c, fps = f, res = r, dyn = d)) },
        okText = if (L) "Копировать" else "Сохранить",
        neutral = if (L || nw) null else "Удалить" to { del = true }) {
        if (L) Text("🔒 Встроенный режим нельзя менять — только копировать.", fontSize = 13.sp)
        OutlinedTextField(n, { n = it.take(30) }, label = { Text("Название") }, enabled = !L, singleLine = true)
        Sl("Ядра CPU", c, 1..8, 1, !L) { c = it }
        Sl("Лимит кадров (FPS)", f, 15..144, 5, !L) { f = it }
        Sl("Разрешение, %", r, 30..100, 5, !L) { r = it }
        Sl("Уровень трансляции x86→ARM (0 безопасно … 3 максимум)", d, 0..3, 1, !L) { d = it }
    }
}

@Composable fun CfgDlg(st: Store, c: Cfg, nw: Boolean, close: () -> Unit) {
    var n by remember { mutableStateOf(c.name) }; var api by remember { mutableStateOf(c.api) }
    var drv by remember { mutableStateOf(c.drv) }; var w by remember { mutableStateOf(c.wine) }
    var vs by remember { mutableStateOf(c.vsync) }; var fsr by remember { mutableStateOf(c.fsr) }
    var del by remember { mutableStateOf(false) }
    val L = c.locked
    if (del) { Confirm("Удалить конфигурацию?", "«${c.name}» будет удалена, программы перейдут на «По умолчанию».", { del = false }, { st.delCfg(c.id); close() }); return }
    Dlg(if (nw) "Новая конфигурация" else c.name, close,
        ok = { if (L) st.put(c.copy(id = uid(), name = c.name + " (копия)", locked = false)) else if (n.isNotBlank()) st.put(c.copy(name = n.trim(), api = api, drv = drv, wine = w, vsync = vs, fsr = fsr)) },
        okText = if (L) "Копировать" else "Сохранить",
        neutral = if (L || nw) null else "Удалить" to { del = true }) {
        if (L) Text("🔒 Конфигурацию по умолчанию нельзя менять — только копировать.", fontSize = 13.sp)
        OutlinedTextField(n, { n = it.take(30) }, label = { Text("Название") }, enabled = !L, singleLine = true)
        Text("Графический API", fontSize = 13.sp); Chips(listOf("Vulkan" to "Vulkan", "OpenGL" to "OpenGL"), api, !L) { api = it }
        Text("Драйвер", fontSize = 13.sp); Chips(listOf("Turnip" to "Turnip", "Zink" to "Zink", "Native" to "Системный"), drv, !L) { drv = it }
        Text("Wine", fontSize = 13.sp); Chips(listOf("Wine 8.0" to "8.0", "Wine 9.0" to "9.0", "Wine 10.0" to "10.0"), w, !L) { w = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(vs, { vs = it }, enabled = !L); Text("  VSync") }
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(fsr, { fsr = it }, enabled = !L); Text("  FSR") }
        Text("Слой: " + if (api == "Vulkan") "DXVK / VKD3D" else "WineD3D", fontSize = 12.sp)
    }
}

@Composable fun AppDlg(st: Store, a: App, nw: Boolean, close: () -> Unit) {
    var n by remember { mutableStateOf(a.name) }; var x by remember { mutableStateOf(a.exe) }
    var m by remember { mutableStateOf(a.mode) }; var c by remember { mutableStateOf(a.cfg) }
    Dlg(if (nw) "Новая программа" else "Настройка", close, ok = { if (n.isNotBlank()) st.put(a.copy(name = n.trim(), exe = x, mode = m, cfg = c)) }) {
        if (nw) OutlinedTextField(n, { n = it.take(30) }, label = { Text("Название") }, singleLine = true)
        OutlinedTextField(x, { x = it }, label = { Text("Файл .exe") }, singleLine = true)
        Text("Режим работы", fontSize = 13.sp); Chips(st.modes.map { it.id to it.name }, m) { m = it }
        Text("Конфигурация", fontSize = 13.sp); Chips(st.cfgs.map { it.id to it.name }, c) { c = it }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun Root(st: Store) {
    var tab by remember { mutableIntStateOf(0) }
    var sel by remember { mutableStateOf(st.apps.firstOrNull()?.id) }
    var log by remember { mutableStateOf("Готов к запуску. SoC: ${Soc.label()}") }
    var dlg by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    val close = { dlg = null }

    fun appMenu(a: App) { dlg = {
        Dlg("${a.icon} ${a.name}", close) {
            TextButton({ dlg = { AppDlg(st, a, false, close) } }) { Text("⚙️ Настроить") }
            TextButton({ dlg = { var t by remember { mutableStateOf(a.name) }
                Dlg("Переименовать", close, { if (t.isNotBlank()) st.put(a.copy(name = t.trim())) }, "Готово") { OutlinedTextField(t, { t = it.take(30) }, singleLine = true) } } }) { Text("✏️ Переименовать") }
            TextButton({ dlg = { Dlg("Иконка", close) { Row(Modifier.horizontalScroll(rememberScrollState())) {
                "🎮 📝 🎨 🌐 🎵 🎬 📊 💼 🛠 🧮 📷 🕹 🧩 📁 ⚽ 🚗".split(" ").forEach { e -> TextButton({ st.put(a.copy(icon = e)); close() }) { Text(e, fontSize = 26.sp) } } } } } }) { Text("🖼 Изменить иконку") }
            TextButton({ dlg = { Confirm("Удалить программу?", "«${a.name}» будет удалена из списка. Действие нельзя отменить.", close) {
                st.delApp(a.id); if (sel == a.id) sel = st.apps.firstOrNull()?.id; close() } } }) { Text("🗑 Удалить", color = MaterialTheme.colorScheme.error) }
        } } }

    Scaffold(bottomBar = { NavigationBar {
        listOf("🗂 Программы", "⚡ Режимы", "⚙️ Конфиги").forEachIndexed { i, t -> NavigationBarItem(tab == i, { tab = i }, { Text(t.take(2)) }, label = { Text(t.drop(3), maxLines = 1) }) } } }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("🪟 WinDroid", fontSize = 22.sp)
            Text(listOf("Зажмите программу — откроется меню", "Профили скорости, ядер, кадров и разрешения", "Как запускать: API, драйвер, Wine")[tab], fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            when (tab) {
                0 -> {
                    LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(st.apps.toList(), key = { it.id }) { a ->
                            Card(Modifier.combinedClickable(onClick = { sel = a.id }, onLongClick = { appMenu(a) })
                                .then(if (sel == a.id) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier)) {
                                Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(a.icon, fontSize = 30.sp); Text(a.name, fontSize = 13.sp, maxLines = 1, textAlign = TextAlign.Center)
                                    Text((st.modes.find { it.id == a.mode }?.name ?: "") + "\n" + (st.cfgs.find { it.id == a.cfg }?.name ?: ""), fontSize = 10.sp, textAlign = TextAlign.Center)
                                } } }
                        item { Card(Modifier.clickable { dlg = { AppDlg(st, App(uid(), "Новая программа", "C:\\", "📁", "normal", "def"), true, close) } }) {
                            Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text("➕", fontSize = 30.sp); Text("Добавить", fontSize = 13.sp) } } }
                    }
                    Button({ st.apps.find { it.id == sel }?.let { a -> log = Engine.launch(a, st.modes.find { it.id == a.mode } ?: BUILTIN_MODES[1], st.cfgs.find { it.id == a.cfg } ?: DEFAULT_CFG) } },
                        Modifier.fillMaxWidth(), enabled = sel != null) { Text("▶ Запустить") }
                    Text(log, fontSize = 11.sp, modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState()))
                }
                1 -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    st.modes.forEach { m -> Card(Modifier.fillMaxWidth().clickable { dlg = { ModeDlg(st, m, false, close) } }) { Column(Modifier.padding(12.dp)) {
                        Text((if (m.locked) "🔒 " else "") + m.name); Text("${m.cores} яд. · ${m.fps} FPS · ${m.res}% · трансляция ${m.dyn}", fontSize = 12.sp) } } }
                    Button({ dlg = { ModeDlg(st, Mode(uid(), "Мой режим", 4, 60, 100, 1), true, close) } }, Modifier.fillMaxWidth()) { Text("＋ Создать режим") }
                }
                else -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    st.cfgs.forEach { c -> Card(Modifier.fillMaxWidth().clickable { dlg = { CfgDlg(st, c, false, close) } }) { Column(Modifier.padding(12.dp)) {
                        Text((if (c.locked) "🔒 " else "") + c.name); Text("${c.api} · ${c.drv} · ${c.wine}", fontSize = 12.sp) } } }
                    Button({ dlg = { CfgDlg(st, Cfg(uid(), "Моя конфигурация", "Vulkan", Soc.driver(), "Wine 9.0", true, false), true, close) } }, Modifier.fillMaxWidth()) { Text("＋ Создать конфигурацию") }
                }
            }
        }
    }
    dlg?.invoke()
}
