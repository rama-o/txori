package com.rama.txori.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.rama.txori.CsActivity
import com.rama.txori.R

class SettingsActivity : CsActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_settings)

        applyEdgeToEdgePadding(findViewById(android.R.id.content))

        setupBasicButtons()
    }

    // ------------------- Basic buttons -------------------
    private fun setupBasicButtons() {
        findViewById<View>(R.id.reset_button).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        }
    }
}