package com.mailadisin.instareelsblocker

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.mailadisin.instareelsblocker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(ReelsBlockerService.PREFS_NAME, MODE_PRIVATE)

        binding.switchBlocker.isChecked = prefs.getBoolean(ReelsBlockerService.KEY_ENABLED, true)

        binding.switchBlocker.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(ReelsBlockerService.KEY_ENABLED, isChecked).apply()
            updateStatusCard()
        }

        binding.btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatusCard()
    }

    private fun updateStatusCard() {
        val serviceEnabled = isAccessibilityServiceEnabled()
        val blockerEnabled = prefs.getBoolean(ReelsBlockerService.KEY_ENABLED, true)

        when {
            !serviceEnabled -> {
                binding.statusIcon.text = "⚠️"
                binding.statusTitle.text = "Accessibility permission needed"
                binding.statusMessage.text =
                    "Tap \"Open Accessibility Settings\" below, find \"Insta Reels Blocker\", and turn it on."
                binding.btnOpenAccessibility.isEnabled = true
            }
            !blockerEnabled -> {
                binding.statusIcon.text = "⏸️"
                binding.statusTitle.text = "Blocker paused"
                binding.statusMessage.text = "Toggle the switch above to re-enable blocking."
                binding.btnOpenAccessibility.isEnabled = false
            }
            else -> {
                binding.statusIcon.text = "✅"
                binding.statusTitle.text = "Reels are blocked"
                binding.statusMessage.text =
                    "The Reels tab in Instagram is blocked. Tapping it will bounce you straight back."
                binding.btnOpenAccessibility.isEnabled = false
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val serviceName = "$packageName/${ReelsBlockerService::class.java.name}"
        return TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }
            .any { it.equals(serviceName, ignoreCase = true) }
    }
}
