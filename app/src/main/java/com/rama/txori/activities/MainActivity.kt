package com.rama.txori.activities

import android.app.AlertDialog
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.rama.txori.CsActivity
import com.rama.txori.DatabaseHelper
import com.rama.txori.R
import com.rama.txori.SessionItem
import com.rama.txori.adapters.SessionAdapter
import com.rama.txori.managers.FontManager
import com.rama.txori.widgets.WdButton

class MainActivity : CsActivity() {
    private var beepArmed: Boolean = false
    private lateinit var listView: ListView
    private lateinit var adapter: SessionAdapter
    private val dbHelper by lazy { DatabaseHelper(this) }
    private lateinit var db: android.database.sqlite.SQLiteDatabase

    private val toneGen by lazy {
        android.media.ToneGenerator(
            android.media.AudioManager.STREAM_MUSIC,
            100
        )
    }

    // Top panel
    private lateinit var taskNameView: TextView
    private lateinit var timerView: TextView
    private lateinit var nextTaskView: TextView

    // Flat list of all items
    private val items: MutableList<SessionItem> = mutableListOf()

    // Running state
    private var currentItemIndex: Int = -1
    private var activeSessionId: Long = -1
    private var currentTimer: CountDownTimer? = null
    private var remainingMs: Long = 0
    private var isRunning: Boolean = false
    private var globalRemainingMs: Long = 0
    private var globalTimer: CountDownTimer? = null
    private var lastBeepSecond: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_home)

        val root = findViewById<View>(R.id.root)
        applyEdgeToEdgePadding(root)
        applyFont(root)

        listView = findViewById(R.id.task_list)
        taskNameView = findViewById(R.id.current_task_name)
        timerView = findViewById(R.id.current_task_timer)
        nextTaskView = findViewById(R.id.next_task_name)

        val workButton =
            findViewById<WdButton>(R.id.work_button)
        val editButton =
            findViewById<WdButton>(R.id.edit_button)
        val repeatTaskButton = findViewById<View>(R.id.repeat_task)
        val increaseTimeButton = findViewById<View>(R.id.increase_duration)
        val playTimeButton = findViewById<View>(R.id.start_task)
        val skipTaskButton = findViewById<View>(R.id.skip_task)
        val timeContainer = findViewById<View>(R.id.time_container)
        var globalPlayPauseicon = playTimeButton.findViewById<ImageView>(R.id.play_pause_icon)

        db = dbHelper.writableDatabase
        loadItems()

        adapter = SessionAdapter(
            context = this,
            items = items,
            db = db,
            dbHelper = dbHelper,
            onStartGroup = { sessionId, startIndex -> handleStartGroup(sessionId, startIndex) },
            onResetGroup = { sessionId -> handleResetGroup(sessionId) },
            // Re-apply font to list rows whenever the adapter refreshes the list
            onDataChanged = { FontManager.applyToListView(this, listView) }
        )
        listView.adapter = adapter

        // Re-apply font after the list has laid out its initial rows
        listView.post { FontManager.applyToListView(this, listView) }

        //  Top panel control buttons
        repeatTaskButton.setOnClickListener {
            if (currentItemIndex >= 0) loadTask(currentItemIndex)
        }

        increaseTimeButton.setOnClickListener {
            if (isRunning) {
                currentTimer?.cancel()
                remainingMs += 30_000L
                globalTimer?.cancel()
                globalRemainingMs += 30_000L
                launchGlobalTimer(globalRemainingMs)
                launchTimer(remainingMs)
            }
        }

        playTimeButton.setOnClickListener {
            if (currentItemIndex < 0) return@setOnClickListener

            if (isRunning) {
                // PAUSE
                pauseTimer()
                globalTimer?.cancel()

                setPlayingState(false)

            } else {
                // PLAY / RESUME
                setPlayingState(true)

                if (remainingMs <= 0L) {
                    launchTimer(remainingMs)
                } else {
                    resumeTimer()
                }

                if (globalRemainingMs > 0) {
                    launchGlobalTimer(globalRemainingMs)
                }
            }
        }

        skipTaskButton.setOnClickListener({
            advanceToNext()
        })

        val addGroupButton = findViewById<WdButton>(R.id.add_group_button)
        addGroupButton.setOnClickListener {
            showAddGroupDialog()
        }

        editButton.setOnClickListener {
            addGroupButton.visibility = View.VISIBLE
            workButton.visibility = View.VISIBLE
            editButton.visibility = View.GONE
            timeContainer.visibility = View.GONE

            adapter.stopAllPlaying()
            adapter.setEditMode(true)

            stopCurrentTimer()
            globalTimer?.cancel()
            isRunning = false
        }

        workButton.setOnClickListener {
            addGroupButton.visibility = View.GONE
            workButton.visibility = View.GONE
            editButton.visibility = View.VISIBLE
            timeContainer.visibility = View.VISIBLE
            adapter.setEditMode(false)
        }
    }

    private fun setPlayingState(playing: Boolean) {
        isRunning = playing

        adapter.setGroupPlayingState(activeSessionId, playing)

        val icon = if (playing) {
            R.drawable.icon_pause
        } else {
            R.drawable.icon_play
        }

        findViewById<ImageView>(R.id.play_pause_icon)
            ?.setImageResource(icon)
    }

    //  Data loading

    private fun loadItems() {
        items.clear()
        val sessions = dbHelper.getSessions(db)
        for ((id, name) in sessions) {
            val tasks = dbHelper.getSessionTasks(db, id)
            items.add(SessionItem.Header(id, name, tasks))
            for (task in tasks) {
                items.add(SessionItem.Row(id, task))
            }
        }
    }

    //  Group start / reset

    private fun handleStartGroup(sessionId: Long, startIndex: Int) {
        when {
            activeSessionId == sessionId && isRunning -> {
                pauseTimer()
                globalTimer?.cancel()
                setPlayingState(false)
            }

            activeSessionId == sessionId && !isRunning && currentItemIndex >= 0 -> {
                setPlayingState(true)
                resumeTimer()
                launchGlobalTimer(globalRemainingMs)
            }

            else -> {
                stopCurrentTimer()
                globalTimer?.cancel()
                // Clear icon on the previously active session if different
                if (activeSessionId != -1L && activeSessionId != sessionId) {
                    adapter.setGroupPlayingState(activeSessionId, false)
                }
                activeSessionId = sessionId
                globalRemainingMs = calculateSessionRemainingMs(sessionId, startIndex)
                startFromIndex(startIndex)
                setPlayingState(true)
                launchGlobalTimer(globalRemainingMs)
            }
        }
    }

    private fun calculateSessionRemainingMs(sessionId: Long, fromIndex: Int): Long {
        var total = 0L
        for (i in fromIndex until items.size) {
            val item = items[i]
            if (item is SessionItem.Row && item.sessionId == sessionId) {
                total += item.task.duration * 1_000L
            }
        }
        return total
    }

    private fun handleResetGroup(sessionId: Long) {
        if (activeSessionId == sessionId) {
            stopCurrentTimer()
            globalTimer?.cancel()
            globalRemainingMs = 0
            activeSessionId = -1
            currentItemIndex = -1
            adapter.setActiveItemIndex(-1)
            setPlayingState(false)
            taskNameView.text = "Kaixo!"
            timerView.text = "00:00"
            nextTaskView.text = "---"
        }
    }

    //  Sequence control

    private fun startFromIndex(index: Int) {
        val target = (index until items.size).firstOrNull {
            val item = items[it]
            item is SessionItem.Row && item.sessionId == activeSessionId
        } ?: run {
            finishGroup()
            return
        }
        loadTask(target)
    }

    private fun advanceToNext() {
        currentTimer?.cancel()
        startFromIndex(currentItemIndex + 1)
    }

    private fun loadTask(index: Int) {
        val row = items[index] as? SessionItem.Row ?: return
        currentTimer?.cancel()
        currentItemIndex = index
        taskNameView.text = row.task.label
        remainingMs = row.task.duration * 1_000L
        adapter.setActiveItemIndex(index)
        listView.post {
            listView.setSelectionFromTop(index, 0)
        }
        updateNextTaskDisplay()
        launchTimer(remainingMs)
    }

    private fun updateNextTaskDisplay() {
        val nextIndex = (currentItemIndex + 1 until items.size).firstOrNull {
            val item = items[it]
            item is SessionItem.Row && item.sessionId == activeSessionId
        }
        val nextRow = nextIndex?.let { items[it] as? SessionItem.Row }
        nextTaskView.text = if (nextRow != null) "Next: ${nextRow.task.label}" else "---"
    }

    private fun finishGroup() {
        val doneSessionId = activeSessionId
        activeSessionId = -1
        currentItemIndex = -1
        globalTimer?.cancel()
        globalRemainingMs = 0
        taskNameView.text = "Done!"
        timerView.text = "00:00"
        nextTaskView.text = "---"
        adapter.setActiveItemIndex(-1)
        setPlayingState(false)
        adapter.setGroupPlayingState(doneSessionId, false)
    }

    //  Timer

    private fun pauseTimer() {
        currentTimer?.cancel()
        isRunning = false
    }

    private fun resumeTimer() {
        launchTimer(remainingMs)
    }

    private fun stopCurrentTimer() {
        currentTimer?.cancel()
        isRunning = false
    }

    // Replace launchTimer entirely
    private fun launchTimer(durationMs: Long) {
        beepArmed = false
        lastBeepSecond = -1
        updateTimerDisplay(durationMs)

        currentTimer = object : CountDownTimer(durationMs, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                updateTimerDisplay(millisUntilFinished)

                val row = items.getOrNull(currentItemIndex) as? SessionItem.Row
                val total = (row?.task?.duration ?: 1) * 1000L
                val progress = 1f - (millisUntilFinished.toFloat() / total.toFloat())
                adapter.setProgress(currentItemIndex, progress.coerceIn(0f, 1f))

                val secondsLeft = millisUntilFinished / 1000

                if (secondsLeft in 0..5 && secondsLeft != lastBeepSecond) {
                    lastBeepSecond = secondsLeft
                    toneGen.startTone(
                        android.media.ToneGenerator.TONE_PROP_BEEP,
                        150
                    )
                }
            }

            override fun onFinish() {
                toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
                isRunning = false
                adapter.setProgress(currentItemIndex, 1f)
                advanceToNext()
            }
        }.start()
    }

    private fun launchGlobalTimer(durationMs: Long) {
        globalTimer?.cancel()
        globalTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                globalRemainingMs = millisUntilFinished
                adapter.updateActiveHeaderTimer(activeSessionId, millisUntilFinished)
            }

            override fun onFinish() {
                globalRemainingMs = 0
                adapter.updateActiveHeaderTimer(activeSessionId, 0)
            }
        }.start()
    }

    private fun updateTimerDisplay(ms: Long) {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        timerView.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private fun showAddGroupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_session_edit, null)
        FontManager.applyToView(this, dialogView)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val input = dialogView.findViewById<EditText>(R.id.edit_text)

        dialogView.findViewById<WdButton>(R.id.yes_button).apply {
            setText("Create")
            setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    input.error = "Name cannot be empty"
                    return@setOnClickListener
                }
                val newId = dbHelper.createSession(db, name)
                items.add(SessionItem.Header(newId, name, emptyList()))
                adapter.notifyDataSetChanged()
                FontManager.applyToListView(this@MainActivity, listView)
                dialog.dismiss()
            }
        }

        dialogView.findViewById<WdButton>(R.id.delete_group_button).visibility = View.GONE

        dialogView.findViewById<WdButton>(R.id.no_button).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroy() {
        currentTimer?.cancel()
        globalTimer?.cancel()
        dbHelper.close()
        super.onDestroy()
    }
}
