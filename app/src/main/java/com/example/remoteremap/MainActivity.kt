package com.example.remoteremap

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var mappingsList: ListView
    private var waitingForKey = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Тот же файл SharedPreferences, что читает KeyRemapAccessibilityService
        prefs = getSharedPreferences("mappings", MODE_PRIVATE)
        statusText = findViewById(R.id.statusText)
        mappingsList = findViewById(R.id.mappingsList)

        findViewById<Button>(R.id.addButton).setOnClickListener {
            waitingForKey = true
            statusText.text = "Нажмите кнопку на пульте, которую хотите назначить..."
        }

        findViewById<Button>(R.id.openAccessibilitySettingsButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        refreshList()
    }

    // Работает только пока это Activity на переднем плане — этого достаточно,
    // чтобы поймать код нажатой кнопки для назначения.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (waitingForKey) {
            waitingForKey = false
            statusText.text = "Поймана кнопка: код $keyCode"
            showAppPicker(keyCode)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showAppPicker(keyCode: Int) {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(mainIntent, 0)
            .sortedBy { it.loadLabel(pm).toString() }

        val labels = apps.map { it.loadLabel(pm).toString() }.toTypedArray()
        val packageNames = apps.map { it.activityInfo.packageName }

        AlertDialog.Builder(this)
            .setTitle("Выберите приложение для кнопки $keyCode")
            .setItems(labels) { _, which ->
                val chosenPackage = packageNames[which]
                prefs.edit().putString(keyCode.toString(), chosenPackage).apply()
                Toast.makeText(this, "Сохранено: $keyCode -> $chosenPackage", Toast.LENGTH_SHORT).show()
                refreshList()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun refreshList() {
        // Сортируем по числовому значению keyCode, а не по алфавиту строки
        val sortedEntries = prefs.all.entries.sortedBy { it.key.toIntOrNull() ?: 0 }

        val displayList = sortedEntries.map {
            "Код ${it.key} -> ${appLabelForPackage(it.value as String)}"
        }
        mappingsList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)

        mappingsList.setOnItemLongClickListener { _, _, position, _ ->
            val keyCode = sortedEntries[position].key
            AlertDialog.Builder(this)
                .setTitle("Удалить сопоставление?")
                .setMessage("Код $keyCode")
                .setPositiveButton("Удалить") { _, _ ->
                    prefs.edit().remove(keyCode).apply()
                    refreshList()
                }
                .setNegativeButton("Отмена", null)
                .show()
            true
        }
    }

    private fun appLabelForPackage(packageName: String): String {
        return try {
            val ai: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
