package com.rama.txori.activities

import android.os.SystemClock
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.rama.txori.CsActivity
import com.rama.txori.R
import com.rama.txori.widgets.WdButton

class TimerActivity : CsActivity() {

    private lateinit var timerButton: TextView
    private lateinit var editView: LinearLayout
    private lateinit var timerInput: EditText
    private lateinit var addTimer: View
    private lateinit var startButton: WdButton
    private lateinit var resetButton: WdButton
    private lateinit var editModeButton: WdButton
    private var isRunning = false

    private var initialMs = 0L
    private var remainingMs = 0L
    private var isEditMode = false
    private var startTime = 0L
    private var pausedAt = 0L
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_timer)

        val root = findViewById<View>(android.R.id.content)
        applyEdgeToEdgePadding(root)
        applyFont(root)

        timerButton = findViewById(R.id.timer_button)
        editView = findViewById(R.id.edit_view)
        timerInput = findViewById(R.id.timer)
        addTimer = findViewById(R.id.add_timer)
        startButton = findViewById(R.id.start_timer)
        resetButton = findViewById(R.id.reset_timer)
        editModeButton = findViewById(R.id.edit_mode)

        timerButton.text = "00:00:00"

        editModeButton.setOnClickListener {
            setEditMode(!isEditMode)
        }

        timerButton.setOnClickListener {
            toggleTimer()
        }

        startButton.setOnClickListener {
            toggleTimer()
        }

        addTimer.setOnClickListener {
            applyInput()
        }

        resetButton.setOnClickListener {
            resetTimer()
        }
    }

    private fun toggleTimer() {
        if (isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }

        startButton.setText(
            if (isRunning) "Pause timer"
            else "Start timer"
        )
    }

    private fun updateEditModeUI() {
        if (isEditMode) {

            editModeButton.setText("Switch to work mode")

            timerButton.visibility = View.GONE
            editView.visibility = View.VISIBLE
            startButton.visibility = View.GONE
            resetButton.visibility = View.GONE

            val digits = timerButton.text.toString()
                .filter { it.isDigit() }
                .takeLast(6)
                .trimStart('0')

            timerInput.setText(digits)
            timerInput.setSelection(timerInput.text.length)
            timerInput.requestFocus()

            showKeyboard()

        } else {

            editModeButton.setText("Switch to edit mode")

            editView.visibility = View.GONE
            timerButton.visibility = View.VISIBLE
            startButton.visibility = View.VISIBLE
            resetButton.visibility = View.VISIBLE

            hideKeyboard()
        }
    }

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        updateEditModeUI()
    }

    private fun applyInput() {
        val digits = timerInput.text.toString()
            .filter { it.isDigit() }
            .takeLast(6)

        val formatted = formatDigits(digits)
        timerButton.text = formatted

        initialMs = digitsToMillis(digits)
        remainingMs = initialMs

        setEditMode(false)
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (!isRunning) return

            val elapsed = SystemClock.elapsedRealtime() - startTime
            val msLeft = initialMs - elapsed

            if (msLeft <= 0) {
                timerButton.text = "00:00:00"
                isRunning = false
                remainingMs = 0L
                return
            }

            remainingMs = msLeft
            timerButton.text = formatMillis(msLeft)

            handler.postDelayed(this, 16) // smooth updates (~60fps)
        }
    }

    private fun startTimer() {
        if (remainingMs <= 0L) return
        isRunning = true
        startTime = SystemClock.elapsedRealtime()
        handler.post(ticker)
    }

    private fun pauseTimer() {
        isRunning = false
        handler.removeCallbacks(ticker)
        pausedAt = remainingMs
    }

    private fun resetTimer() {
        handler.removeCallbacks(ticker)
        isRunning = false
        remainingMs = initialMs
        pausedAt = 0L
        timerButton.text = formatMillis(initialMs)
    }

    private fun digitsToMillis(digits: String): Long {
        val padded = digits.padStart(6, '0')

        val hh = padded.substring(0, 2).toLong()
        val mm = padded.substring(2, 4).toLong()
        val ss = padded.substring(4, 6).toLong()

        return ((hh * 3600) + (mm * 60) + ss) * 1000
    }

    private fun formatDigits(digits: String): String {
        val padded = digits.padStart(6, '0')
        return "${padded.substring(0, 2)}:${padded.substring(2, 4)}:${padded.substring(4, 6)}"
    }

    private fun formatMillis(ms: Long): String {
        val total = ms / 1000

        val hh = total / 3600
        val mm = (total % 3600) / 60
        val ss = total % 60

        return "%02d:%02d:%02d".format(hh, mm, ss)
    }

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(timerInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(timerInput.windowToken, 0)
    }
}