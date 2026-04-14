package com.rama.txori.activities

import android.os.Bundle
import android.view.View
import com.rama.txori.CsActivity
import com.rama.txori.R

class TimerActivity : CsActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_stopwatch)

        val root = findViewById<View>(android.R.id.content)
        applyEdgeToEdgePadding(root)
        applyFont(root)
    }
}