package com.rama.txori.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.rama.txori.CsActivity
import com.rama.txori.DatabaseHelper
import com.rama.txori.R
import com.rama.txori.SessionItem
import com.rama.txori.adapters.SessionAdapter
import com.rama.txori.managers.FontManager
import com.rama.txori.managers.SoundManager
import com.rama.txori.managers.WorkoutManager
import com.rama.txori.widgets.WdButton
import com.rama.txori.widgets.WdNavbar

class MainActivity : CsActivity(), WorkoutManager.Listener {

    private lateinit var listView: ListView
    private lateinit var adapter: SessionAdapter
    private val dbHelper by lazy { DatabaseHelper(this) }
    private lateinit var db: android.database.sqlite.SQLiteDatabase

    private lateinit var taskNameView: TextView
    private lateinit var timerView: TextView
    private lateinit var nextTaskView: TextView
    private lateinit var globalControllers: LinearLayout
    private lateinit var editButton: WdButton
    private lateinit var navbar: WdNavbar
    private lateinit var playPauseIcon: ImageView

    private val items: MutableList<SessionItem> = mutableListOf()
    private lateinit var workout: WorkoutManager

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
        globalControllers = findViewById(R.id.controllers)
        editButton = findViewById(R.id.edit_button)
        navbar = findViewById(R.id.navbar)
        playPauseIcon = findViewById(R.id.play_pause_icon)

        SoundManager.init()
        workout = WorkoutManager(this)

        db = dbHelper.writableDatabase
        loadItems()
        workout.items = items

        adapter = SessionAdapter(
            context = this,
            items = items,
            db = db,
            dbHelper = dbHelper,
            onStartGroup = { sessionId, startIndex -> workout.startGroup(sessionId, startIndex) },
            onResetGroup = { sessionId -> workout.resetGroup(sessionId) },
            onDataChanged = { FontManager.applyToListView(this, listView) }
        )
        listView.adapter = adapter
        listView.post { FontManager.applyToListView(this, listView) }

        // Control buttons
        findViewById<View>(R.id.repeat_task).setOnClickListener {
            workout.repeatCurrentTask()
        }
        findViewById<View>(R.id.increase_duration).setOnClickListener {
            workout.addTime(30_000L)
        }
        findViewById<View>(R.id.start_task).setOnClickListener {
            workout.togglePlayPause()
        }
        findViewById<View>(R.id.skip_task).setOnClickListener {
            workout.skipTask()
        }

        val addGroupButton = findViewById<WdButton>(R.id.add_group_button)
        val workButton = findViewById<WdButton>(R.id.work_button)
        val timeContainer = findViewById<View>(R.id.time_container)

        addGroupButton.setOnClickListener { showAddGroupDialog() }

        editButton.setOnClickListener {
            addGroupButton.visibility = View.VISIBLE
            workButton.visibility = View.VISIBLE
            editButton.visibility = View.GONE
            timeContainer.visibility = View.GONE
            adapter.stopAllPlaying()
            adapter.setEditMode(true)
            workout.stopAndClear()
        }

        workButton.setOnClickListener {
            addGroupButton.visibility = View.GONE
            workButton.visibility = View.GONE
            editButton.visibility = View.VISIBLE
            timeContainer.visibility = View.VISIBLE
            adapter.setEditMode(false)
        }
    }

    //  WorkoutManager.Listener 

    override fun onTaskStarted(index: Int, label: String, remainingMs: Long) {
        taskNameView.text = label
        updateTimerDisplay(remainingMs)
        adapter.setActiveItemIndex(index)
        updateNextTaskDisplay(index, workout.activeSessionId)
        globalControllers.visibility = View.VISIBLE
        listView.post {
            val visiblePos = adapter.rawIndexToVisiblePosition(index)
            if (visiblePos >= 0) listView.setSelectionFromTop(visiblePos, 0)
        }
    }

    override fun onTaskTick(index: Int, remainingMs: Long, progress: Float) {
        updateTimerDisplay(remainingMs)
        adapter.setProgress(index, progress)
    }

    override fun onTaskFinished(index: Int) {
        adapter.setProgress(index, 1f)
    }

    override fun onSessionTick(sessionId: Long, remainingMs: Long) {
        adapter.updateActiveHeaderTimer(sessionId, remainingMs)
    }

    override fun onPlayingStateChanged(sessionId: Long, playing: Boolean) {
        adapter.setGroupPlayingState(sessionId, playing)
        playPauseIcon.setImageResource(
            if (playing) R.drawable.icon_pause else R.drawable.icon_play
        )
        editButton.visibility = if (playing) View.GONE else View.VISIBLE
        navbar.visibility = if (playing) View.GONE else View.VISIBLE
    }

    override fun onGroupFinished(sessionId: Long) {
        taskNameView.text = "Done!"
        timerView.text = "00:00"
        nextTaskView.text = "---"
        adapter.setActiveItemIndex(-1)
        adapter.setGroupPlayingState(sessionId, false)
        playPauseIcon.setImageResource(R.drawable.icon_play)
        globalControllers.visibility = View.GONE
        editButton.visibility = View.VISIBLE
        navbar.visibility = View.VISIBLE
    }

    override fun onGroupReset(sessionId: Long) {
        taskNameView.text = "Kaixo!"
        timerView.text = "00:00"
        nextTaskView.text = "---"
        adapter.setActiveItemIndex(-1)
        adapter.setGroupPlayingState(sessionId, false)
        playPauseIcon.setImageResource(R.drawable.icon_play)
        globalControllers.visibility = View.GONE
    }

    //  Helpers 

    private fun loadItems() {
        items.clear()
        val sessions = dbHelper.getSessions(db)
        for ((id, name) in sessions) {
            val tasks = dbHelper.getSessionTasks(db, id)
            items.add(SessionItem.Header(id, name, tasks))
            for (task in tasks) items.add(SessionItem.Row(id, task))
        }
    }

    private fun updateNextTaskDisplay(currentIndex: Int, sessionId: Long) {
        val nextIndex = (currentIndex + 1 until items.size).firstOrNull {
            val item = items[it]
            item is SessionItem.Row && item.sessionId == sessionId
        }
        val nextRow = nextIndex?.let { items[it] as? SessionItem.Row }
        nextTaskView.text = if (nextRow != null) "Next: ${nextRow.task.label}" else "---"
    }

    private fun updateTimerDisplay(ms: Long) {
        val total = (ms / 1000).coerceAtLeast(0)
        timerView.text = String.format("%02d:%02d", total / 60, total % 60)
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
                    input.error = "Name cannot be empty"; return@setOnClickListener
                }
                val newId = dbHelper.createSession(db, name)
                items.add(SessionItem.Header(newId, name, emptyList()))
                workout.items = items
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
        workout.release()
        SoundManager.release()
        dbHelper.close()
        super.onDestroy()
    }
}