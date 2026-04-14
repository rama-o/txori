package com.rama.txori.activities

import android.os.Bundle
import com.rama.txori.CsActivity
import com.rama.txori.R

class StopwatchActivity : CsActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_stopwatch)

        val root = findViewById<android.view.View>(android.R.id.content)
        applyEdgeToEdgePadding(root)
        applyFont(root)
    }

}