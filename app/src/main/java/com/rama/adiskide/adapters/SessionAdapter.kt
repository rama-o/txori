package com.rama.adiskide.adapters

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.rama.adiskide.DatabaseHelper
import com.rama.adiskide.R
import com.rama.adiskide.SessionItem
import com.rama.adiskide.Task
import com.rama.adiskide.widgets.WdButton

class SessionAdapter(
    private val context: Context,
    val items: MutableList<SessionItem>,
    private val db: SQLiteDatabase,
    private val dbHelper: DatabaseHelper,
    private val onStartGroup: (sessionId: Long, startIndex: Int) -> Unit,
    private val onResetGroup: (sessionId: Long) -> Unit,
    private val onDataChanged: () -> Unit
) : BaseAdapter() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TASK = 1
    }

    private var activeItemIndex: Int = -1
    private var activeProgress: Float = 0f

    fun setActiveItemIndex(index: Int) {
        activeItemIndex = index
        activeProgress = 0f
        notifyDataSetChanged()
    }

    fun setProgress(index: Int, progress: Float) {
        if (index != activeItemIndex) return
        activeProgress = progress

        val listView = (context as? android.app.Activity)
            ?.findViewById<ListView>(R.id.task_list) ?: return
        val firstVisible = listView.firstVisiblePosition
        val localPosition = index - firstVisible
        if (localPosition < 0 || localPosition >= listView.childCount) return
        val itemView = listView.getChildAt(localPosition) ?: return
        applyProgress(progress, itemView)
    }

    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()
    override fun getViewTypeCount() = 2

    override fun getItemViewType(position: Int) = when (items[position]) {
        is SessionItem.Header -> TYPE_HEADER
        is SessionItem.Row -> TYPE_TASK
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return when (val item = items[position]) {
            is SessionItem.Header -> getHeaderView(item, position, convertView, parent)
            is SessionItem.Row -> getTaskView(item, position, convertView, parent)
        }
    }

    private fun getHeaderView(
        header: SessionItem.Header,
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_header, parent, false)

        val totalSec = header.tasks.sumOf { it.duration }
        val timeStr = formatGroupTime(totalSec)

        view.findViewById<TextView>(R.id.group_label).text =
            "${header.name} :: $timeStr"

        view.findViewById<View>(R.id.start_group).setOnClickListener {
            onStartGroup(header.sessionId, position + 1)
        }

        view.findViewById<View>(R.id.reset_group).setOnClickListener {
            onResetGroup(header.sessionId)
        }

        view.findViewById<View>(R.id.add_task).setOnClickListener {
            showAddTaskDialog(header, position)
        }

        view.findViewById<View>(R.id.delete_group).setOnClickListener {
            showDeleteGroupDialog(header)
        }

        return view
    }

    private fun getTaskView(
        row: SessionItem.Row,
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_task, parent, false)

        view.findViewById<TextView>(R.id.task_label).text = row.task.label
        view.findViewById<TextView>(R.id.task_duration).text = formatDuration(row.task.duration)

        var occurrence = 0
        var total = 0
        var currentIndex = -1
        var runningIndex = 0

        for (i in items.indices) {
            val item = items[i]

            if (item is SessionItem.Row &&
                item.sessionId == row.sessionId &&
                item.task.label == row.task.label
            ) {
                if (i == position) {
                    currentIndex = runningIndex
                }
                runningIndex++
            }
        }

        total = runningIndex
        occurrence = currentIndex + 1

        val freqView = view.findViewById<TextView>(R.id.task_frequency)

        if (total > 1) {
            freqView.visibility = View.VISIBLE
            freqView.text = "$occurrence / $total"
        } else {
            freqView.visibility = View.GONE
        }

        val p = if (position == activeItemIndex) activeProgress else 0f
        applyProgress(p, view)

        view.setOnLongClickListener {
            showEditTaskDialog(row, position)
            true
        }

        return view
    }

    //  Progress 

    private fun applyProgress(progress: Float, itemView: View) {
        val container = itemView.findViewById<View>(R.id.app_row_container) ?: return
        val progressView = itemView.findViewById<View>(R.id.progress_bg) ?: return

        container.post {
            val totalWidth = container.width
            if (totalWidth > 0) {
                progressView.layoutParams.width = (totalWidth * progress).toInt()
                progressView.requestLayout()
            }
        }
    }

    //  Play/pause icon on the active header 

    fun setGroupPlayingState(sessionId: Long, playing: Boolean) {
        val listView = (context as? android.app.Activity)
            ?.findViewById<ListView>(R.id.task_list) ?: return

        val firstVisible = listView.firstVisiblePosition

        for (i in items.indices) {
            val item = items[i]
            if (item is SessionItem.Header && item.sessionId == sessionId) {
                val local = i - firstVisible
                if (local >= 0 && local < listView.childCount) {
                    val headerView = listView.getChildAt(local) ?: continue
                    headerView.findViewById<ImageView>(R.id.start_group_icon)
                        ?.setImageResource(
                            if (playing) R.drawable.icon_pause else R.drawable.icon_play
                        )
                }
                break
            }
        }

        notifyDataSetChanged()
    }

    //  Dialogs 

    private fun showAddTaskDialog(header: SessionItem.Header, headerPosition: Int) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_task_edit, null)

        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        dialogView.findViewById<TextView>(R.id.modal_title).setText("Add new task")

        dialogView.findViewById<WdButton>(R.id.add_button).setText("Create Task")

        val labelInput = dialogView.findViewById<EditText>(R.id.label)
        val durationInput = dialogView.findViewById<EditText>(R.id.duration)
        durationInput.setText("60")

        dialogView.findViewById<WdButton>(R.id.delete_button).visibility = View.GONE

        dialogView.findViewById<WdButton>(R.id.add_button).setOnClickListener {
            val label = labelInput.text.toString().trim()
            val duration = durationInput.text.toString().toIntOrNull() ?: 60

            if (label.isEmpty()) {
                labelInput.error = "Label cannot be empty"
                return@setOnClickListener
            }

            dbHelper.addTaskToSession(db, header.sessionId, label, duration)

            val newTask = Task(label = label, duration = duration)

            var insertAt = headerPosition + 1
            while (insertAt < items.size && items[insertAt] is SessionItem.Row) insertAt++

            items.add(insertAt, SessionItem.Row(header.sessionId, newTask))

            notifyDataSetChanged()
            onDataChanged()
            dialog.dismiss()
        }

        dialogView.findViewById<WdButton>(R.id.cancel_button)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showDeleteGroupDialog(header: SessionItem.Header) {
        AlertDialog.Builder(context)
            .setTitle("Delete \"${header.name}\"?")
            .setMessage("This will remove the group and all its tasks from the list.")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteSession(db, header.sessionId)

                items.removeAll { item ->
                    (item is SessionItem.Header && item.sessionId == header.sessionId) ||
                            (item is SessionItem.Row && item.sessionId == header.sessionId)
                }

                notifyDataSetChanged()
                onDataChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTaskDialog(row: SessionItem.Row, position: Int) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_task_edit, null)

        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        val labelInput = dialogView.findViewById<EditText>(R.id.label)
        val durationInput = dialogView.findViewById<EditText>(R.id.duration)

        labelInput.setText(row.task.label)
        durationInput.setText(row.task.duration.toString())

        dialogView.findViewById<WdButton>(R.id.delete_button).setOnClickListener {
            dbHelper.removeTaskFromSession(db, row.sessionId, row.task.id)
            items.removeAt(position)
            notifyDataSetChanged()
            onDataChanged()
            dialog.dismiss()
        }

        dialogView.findViewById<WdButton>(R.id.add_button).setOnClickListener {
            val newLabel = labelInput.text.toString().trim()
            val newDuration = durationInput.text.toString().toIntOrNull() ?: row.task.duration

            if (newLabel.isEmpty()) {
                labelInput.error = "Label cannot be empty"
                return@setOnClickListener
            }

            val values = ContentValues().apply {
                put("label", newLabel)
                put("duration", newDuration)
            }

            db.update("tasks", values, "id = ?", arrayOf(row.task.id.toString()))

            row.task.label = newLabel
            row.task.duration = newDuration

            notifyDataSetChanged()
            onDataChanged()
            dialog.dismiss()
        }

        dialogView.findViewById<WdButton>(R.id.cancel_button)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    //  Formatting 

    private fun formatDuration(seconds: Int): String {
        return if (seconds >= 60) {
            val m = seconds / 60
            val s = seconds % 60
            if (s == 0) "${m}m" else "${m}m ${s}s"
        } else "${seconds}s"
    }

    private fun formatGroupTime(totalSeconds: Int): String {
        return if (totalSeconds >= 3600) {
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            val m = totalSeconds / 60
            val s = totalSeconds % 60
            String.format("%02d:%02d", m, s)
        }
    }
}