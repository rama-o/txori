package com.rama.txori.widgets

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.rama.txori.R
import com.rama.txori.activities.AboutActivity
import com.rama.txori.activities.MainActivity
import com.rama.txori.activities.StopwatchActivity
import com.rama.txori.activities.TimerActivity

class WdNavbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.wd_navbar, this, true)

        findViewById<FrameLayout>(R.id.home_nav).setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }

        findViewById<FrameLayout>(R.id.stopwatch_nav).setOnClickListener {
            val intent = Intent(context, StopwatchActivity::class.java)
            context.startActivity(intent)
        }

        findViewById<FrameLayout>(R.id.timer_nav).setOnClickListener {
            val intent = Intent(context, TimerActivity::class.java)
            context.startActivity(intent)
        }

        findViewById<FrameLayout>(R.id.about_nav).setOnClickListener {
            val intent = Intent(context, AboutActivity::class.java)
            context.startActivity(intent)
        }
    }
}