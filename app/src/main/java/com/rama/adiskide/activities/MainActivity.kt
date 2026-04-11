package com.rama.adiskide.activities

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ListView
import android.widget.TextView
import com.rama.adiskide.CsActivity
import com.rama.adiskide.DatabaseHelper
import com.rama.adiskide.R
import com.rama.adiskide.Task
import com.rama.adiskide.adapters.TaskAdapter

class MainActivity : CsActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: TaskAdapter
    private val dbHelper by lazy { DatabaseHelper(this) }

    // Top panel views
    private lateinit var taskNameView: TextView
    private lateinit var timerView: TextView

    // Task sequence state
    private var tasks: MutableList<Task> = mutableListOf()
    private var currentIndex: Int = -1
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

        val db = dbHelper.readableDatabase
        val workouts = dbHelper.getWorkouts(db)
        tasks = if (workouts.isNotEmpty()) {
            dbHelper.getAllWorkoutTasks(db)
        } else {
            dbHelper.getAllTasks(db)
        }

        adapter = TaskAdapter(this, tasks, db)
        listView.adapter = adapter

        // start_group: kick off from the first task
        findViewById<View>(R.id.start_group).setOnClickListener {
            startFromIndex(0)
        }

        // Repeat: restart the current task
        findViewById<View>(R.id.repeat_task).setOnClickListener {
            if (currentIndex >= 0) startFromIndex(currentIndex)
        }

        // +30s
        findViewById<View>(R.id.increase_duration).setOnClickListener {
            if (isRunning) {
                currentTimer?.cancel()
                remainingMs += 30_000L
                launchTimer(remainingMs)
            }
        }

        // Complete / Skip → advance
        val advance: (View) -> Unit = { advanceToNext() }
        findViewById<View>(R.id.complete_task).setOnClickListener(advance)
        findViewById<View>(R.id.skip_task).setOnClickListener(advance)
    }

    // Sequence control

    private fun startFromIndex(index: Int) {
        if (tasks.isEmpty()) return
        currentIndex = index.coerceIn(0, tasks.size - 1)
        loadTask(tasks[currentIndex])
    }

    private fun advanceToNext() {
        currentTimer?.cancel()
        val next = currentIndex + 1
        if (next < tasks.size) {
            startFromIndex(next)
        } else {
            isRunning = false
            currentIndex = -1
            taskNameView.text = "Done!"
            timerView.text = "00:00"
            adapter.setActiveIndex(-1)
        }
    }

    private fun loadTask(task: Task) {
        currentTimer?.cancel()
        taskNameView.text = task.label
        remainingMs = task.duration * 1_000L
        adapter.setActiveIndex(currentIndex)
        listView.smoothScrollToPosition(currentIndex)
        launchTimer(remainingMs)
    }

    private fun launchTimer(durationMs: Long) {
        isRunning = true
        updateTimerDisplay(durationMs)

        currentTimer = object : CountDownTimer(durationMs, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                updateTimerDisplay(millisUntilFinished)

                // Drive progress bar on the active list row
                val total = tasks.getOrNull(currentIndex)?.duration?.times(1000L) ?: 1L
                val progress = 1f - (millisUntilFinished.toFloat() / total.toFloat())
                adapter.setProgress(currentIndex, progress.coerceIn(0f, 1f))
            }

            override fun onFinish() {
                isRunning = false
                adapter.setProgress(currentIndex, 1f)
                advanceToNext()
            }
        }.start()
    }

    private fun updateTimerDisplay(ms: Long) {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        timerView.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    override fun onDestroy() {
        currentTimer?.cancel()
        dbHelper.close()
        super.onDestroy()
    }
}
