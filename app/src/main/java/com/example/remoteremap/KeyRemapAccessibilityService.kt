package com.example.remoteremap

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Глобально перехватывает нажатия кнопок пульта (пока сервис включён
 * в Настройки -> Специальные возможности) и запускает приложение,
 * назначенное на этот keyCode в MainActivity.
 *
 * Ограничения:
 *  - кнопку Home перехватить почти никогда нельзя — она зарезервирована системой;
 *  - какие именно keyCode шлёт конкретный пульт Xiaomi TV, нужно проверять
 *    через adb logcat на реальном устройстве — они не всегда совпадают
 *    со стандартными Android KeyEvent.
 */
class KeyRemapAccessibilityService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences("mappings", MODE_PRIVATE)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Реагируем только на нажатие, не на отпускание кнопки
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }

        val packageName = prefs.getString(event.keyCode.toString(), null) ?: return false

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)

        // true = событие "съедено", дальше в систему не пойдёт
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Не используется — этому сервису нужны только события клавиш
    }

    override fun onInterrupt() {
        // Не используется
    }
}
