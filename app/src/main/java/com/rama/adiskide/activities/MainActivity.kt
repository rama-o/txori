package com.rama.adiskide.activities

import android.app.AlertDialog
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import com.rama.adiskide.CsActivity
import com.rama.adiskide.DatabaseHelper
import com.rama.adiskide.R
import com.rama.adiskide.SessionItem
import com.rama.adiskide.adapters.SessionAdapter
import com.rama.adiskide.widgets.WdButton

class MainActivity : CsActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: SessionAdapter
    private val dbHelper by lazy { DatabaseHelper(this) }
    private lateinit var db: android.database.sqlite.SQLiteDatabase

    // Top panel
    private lateinit var taskNameView: TextView
    private lateinit var timerView: TextView

    // Flat list of all items
    private val items: MutableList<SessionItem> = mutableListOf()

    // Running state
    private var currentItemIndex: Int = -1   // index into items[] of the active Row
    private var activeSessionId: Long = -1
    private var currentTimer: CountDownTimer? = null
    private var remainingMs: Long = 0
    private var isRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_home)

        val root = findViewById<View>(R.id.root)
        applyEdgeToEdgePadding(root)

        listView = findViewById(R.id.task_list)
        taskNameView = findViewById(R.id.current_task_name)
        timerView = findViewById(R.id.current_task_timer)

        db = dbHelper.writableDatabase
        loadItems()

        adapter = SessionAdapter(
            context = this,
            items = items,
            db = db,
            dbHelper = dbHelper,
            onStartGroup = { sessionId, startIndex -> handleStartGroup(sessionId, startIndex) },
            onResetGroup = { sessionId -> handleResetGroup(sessionId) },
            onDataChanged = { /* headers auto-update via notifyDataSetChanged */ }
        )
        listView.adapter = adapter

        //  Top panel control buttons 
        findViewById<View>(R.id.repeat_task).setOnClickListener {
            if (currentItemIndex >= 0) loadTask(currentItemIndex)
        }

        findViewById<View>(R.id.increase_duration).setOnClickListener {
            if (isRunning) {
                currentTimer?.cancel()
                remainingMs += 30_000L
                launchTimer(remainingMs)
            }
        }

        val advance: (View) -> Unit = { advanceToNext() }
        findViewById<View>(R.id.complete_task).setOnClickListener(advance)
        findViewById<View>(R.id.skip_task).setOnClickListener(advance)

        //  Add Group button 
        findViewById<WdButton>(R.id.add_group_button).setOnClickListener {
            showAddGroupDialog()
        }
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
            // Same group: toggle play/pause
            activeSessionId == sessionId && isRunning -> {
                pauseTimer()
                adapter.setGroupPlayingState(sessionId, false)
            }

            activeSessionId == sessionId && !isRunning && currentItemIndex >= 0 -> {
                adapter.setGroupPlayingState(sessionId, true)
                resumeTimer()
            }
            // Different group or fresh start
            else -> {
                stopCurrentTimer()
                activeSessionId = sessionId
                startFromIndex(startIndex)
                adapter.setGroupPlayingState(sessionId, true)
            }
        }
    }

    private fun handleResetGroup(sessionId: Long) {
        if (activeSessionId == sessionId) {
            stopCurrentTimer()
            activeSessionId = -1
            currentItemIndex = -1
            adapter.setActiveItemIndex(-1)
            adapter.setGroupPlayingState(sessionId, false)
            taskNameView.text = "Olá!"
            timerView.text = "00:00"
        }
    }

    //  Sequence control 

    private fun startFromIndex(index: Int) {
        // Find next Row item from index onward that belongs to activeSessionId
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
        listView.smoothScrollToPosition(index)
        launchTimer(remainingMs)
    }

    private fun finishGroup() {
        isRunning = false
        val doneSessionId = activeSessionId
        activeSessionId = -1
        currentItemIndex = -1
        taskNameView.text = "Done!"
        timerView.text = "00:00"
        adapter.setActiveItemIndex(-1)
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

    private fun launchTimer(durationMs: Long) {
        isRunning = true
        updateTimerDisplay(durationMs)

        currentTimer = object : CountDownTimer(durationMs, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                updateTimerDisplay(millisUntilFinished)

                val row = items.getOrNull(currentItemIndex) as? SessionItem.Row
                val total = (row?.task?.duration ?: 1) * 1000L
                val progress = 1f - (millisUntilFinished.toFloat() / total.toFloat())
                adapter.setProgress(currentItemIndex, progress.coerceIn(0f, 1f))
            }

            override fun onFinish() {
                isRunning = false
                adapter.setProgress(currentItemIndex, 1f)
                advanceToNext()
            }
        }.start()
    }

    private fun updateTimerDisplay(ms: Long) {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        timerView.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    //  Add Group dialog 

    private fun showAddGroupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_session_add, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val input = dialogView.findViewById<EditText>(R.id.edit_text)
        input.hint = "Group name"

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
                dialog.dismiss()
            }
        }

        dialogView.findViewById<WdButton>(R.id.reset_button).visibility = View.GONE

        dialogView.findViewById<WdButton>(R.id.no_button).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroy() {
        currentTimer?.cancel()
        dbHelper.close()
        super.onDestroy()
    }
}