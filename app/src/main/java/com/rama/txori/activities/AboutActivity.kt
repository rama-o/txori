package com.rama.txori.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.rama.txori.CsActivity
import com.rama.txori.R

class AboutActivity : CsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_about)

        val root = findViewById<View>(android.R.id.content)
        applyEdgeToEdgePadding(root)
        applyFont(root)

        val version = packageManager.getPackageInfo(packageName, 0).versionCode
        val nameView = findViewById<TextView>(R.id.name_version)
        nameView.text = getString(R.string.app_name) + ' ' + version
    }
}