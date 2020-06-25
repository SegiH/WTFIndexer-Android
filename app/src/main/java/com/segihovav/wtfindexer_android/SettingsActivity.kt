package com.segihovav.wtfindexer_android

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : AppCompatActivity() {
    private lateinit var wtfURL: TextInputLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var darkModeCheckbox: Switch
    private val darkMode = R.style.Theme_AppCompat_DayNight
    private val lightMode = R.style.ThemeOverlay_MaterialComponents
    private var darkModeToggled = false
    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        this.setTheme(if (sharedPreferences.getBoolean("DarkThemeOn", false)) darkMode else lightMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        darkModeCheckbox = findViewById(R.id.switchDarkMode)
        wtfURL = findViewById(R.id.URL)

        if (sharedPreferences.getString("WTFIndexerURL", "") != "" && wtfURL.editText != null) {
            wtfURL.editText!!.setText(sharedPreferences.getString("WTFIndexerURL", ""))
        }

        darkModeCheckbox.isChecked = sharedPreferences.getBoolean("DarkThemeOn", false)
    }

    fun darkModeClick(v: View?) {
        darkModeToggled = true
        Toast.makeText(applicationContext, "The app will be restarted when you click on save for this to take effect" + if(darkModeCheckbox.isChecked) ". You must have Dark Mode enabled on Android " else "", Toast.LENGTH_SHORT).show()
    }

    fun goBackClick(v: View?) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    fun saveClick(v: View?) {
        if (wtfURL.editText != null && wtfURL.editText!!.text != null && wtfURL.editText!!.text.toString() == "") {
            Toast.makeText(applicationContext, "Please enter the URL", Toast.LENGTH_LONG).show()
            return
        }
        val editor = sharedPreferences.edit()
        editor.putString("WTFIndexerURL", wtfURL.editText!!.text.toString())
        editor.putBoolean("DarkThemeOn", darkModeCheckbox.isChecked)
        editor.apply()
        if (darkModeToggled) finishAffinity()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
