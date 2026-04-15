package com.rama.txori.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import com.rama.txori.CsActivity
import com.rama.txori.R
import com.rama.txori.widgets.WdButton
import com.rama.txori.widgets.WdNavbar

class StopwatchActivity : CsActivity() {

    private var counterView: TextView? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private var startTime = 0L
    private var pausedElapsed = 0L
    private lateinit var navbar: WdNavbar

    private val ticker = object : Runnable {
        override fun run() {
            if (isRunning) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                counterView?.text = formatTime(elapsed)
                handler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_stopwatch)

        val root = findViewById<android.view.View>(android.R.id.content)
        applyEdgeToEdgePadding(root)
        applyFont(root)

        navbar = findViewById(R.id.navbar)

        counterView = findViewById(R.id.counter)

        counterView?.setOnClickListener {
            if (isRunning) {
                pauseStopwatch()
            } else {
                startStopwatch()
            }
        }

        counterView?.setOnLongClickListener {
            resetStopwatch()
            true
        }

        findViewById<WdButton>(R.id.reset_counter).setOnClickListener {
            resetStopwatch()
        }
    }

    private fun startStopwatch() {
        navbar.visibility = View.GONE
        startTime = SystemClock.elapsedRealtime() - pausedElapsed
        isRunning = true
        handler.post(ticker)
    }

    private fun pauseStopwatch() {
        navbar.visibility = View.VISIBLE
        pausedElapsed = SystemClock.elapsedRealtime() - startTime
        isRunning = false
        handler.removeCallbacks(ticker)
    }

    private fun resetStopwatch() {
        navbar.visibility = View.VISIBLE
        isRunning = false
        pausedElapsed = 0L
        handler.removeCallbacks(ticker)
        counterView?.text = "0"
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val tenths = (ms % 1000) / 100

        return when {
            hours > 0 -> {
                "$hours:${minutes.toString().padStart(2, '0')}:${
                    seconds.toString().padStart(2, '0')
                }.$tenths"
            }

            minutes > 0 -> {
                "$minutes:${seconds.toString().padStart(2, '0')}.$tenths"
            }

            else -> {
                "$seconds.$tenths"
            }
        }
    }
}