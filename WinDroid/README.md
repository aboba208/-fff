# WinDroid
Сборка APK без Android Studio: загрузите папку в GitHub-репозиторий → вкладка Actions → «Build APK» → Run workflow → скачайте артефакт WinDroid-apk.
Архитектуры: arm64-v8a (все современные Snapdragon/Dimensity/Exynos/Tensor/Kirin) и x86_64 (эмуляторы/Chromebook) + универсальный APK.
Движок (Box64 + Wine) подключается в Engine.kt — см. TODO.
