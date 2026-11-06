package app.windroid

/** Превращает профили в параметры запуска. Сюда подключается движок (Box64 + Wine, как в Winlator). */
object Engine {
    fun env(m: Mode, c: Cfg): Map<String, String> = buildMap {
        put("BOX64_DYNAREC_BIGBLOCK", m.dyn.toString())
        put("BOX64_DYNAREC_SAFEFLAGS", when { m.dyn >= 3 -> "0"; m.dyn == 2 -> "1"; else -> "2" })
        put("BOX64_DYNAREC_STRONGMEM", if (m.dyn == 0) "1" else "0")
        if (m.dyn >= 3) { put("BOX64_DYNAREC_FASTNAN", "1"); put("BOX64_DYNAREC_FASTROUND", "1") }
        put("WINDROID_CORES", m.cores.toString())
        put("WINDROID_FPS_LIMIT", m.fps.toString())
        put("WINDROID_RENDER_SCALE", (m.res / 100f).toString())
        put("WINDROID_API", c.api); put("WINDROID_DRIVER", c.drv)
        put("WINDROID_LAYER", if (c.api == "Vulkan") "DXVK" else "WineD3D")
        put("WINDROID_VSYNC", if (c.vsync) "1" else "0"); put("WINDROID_FSR", if (c.fsr) "1" else "0")
    }

    /** TODO: здесь запуск процесса wine/box64 внутри контейнера. Пока возвращает описание плана запуска. */
    fun launch(a: App, m: Mode, c: Cfg): String =
        "SoC: ${Soc.label()}\n${a.name}: ${a.exe}\nРежим: ${m.name}\nКонфигурация: ${c.name} (${c.wine})\n" +
            env(m, c).entries.joinToString("\n") { "  ${it.key}=${it.value}" }
}
