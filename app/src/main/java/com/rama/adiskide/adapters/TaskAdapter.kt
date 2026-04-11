package com.rama.adiskide.adapters

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.rama.adiskide.R
import com.rama.adiskide.Task
import com.rama.adiskide.widgets.WdButton

class TaskAdapter(
    context: Context,
    private val tasks: MutableList<Task>,
    private val db: SQLiteDatabase
) : ArrayAdapter<Task>(context, 0, tasks) {

    private var activeIndex: Int = -1
    // progress per position (0f–1f); only meaningful for the active row
    private val progressMap = mutableMapOf<Int, Float>()
    // live view references so we can update without a full notifyDataSetChanged
    private val viewMap = mutableMapOf<Int, View>()

    // Called by MainActivity to highlight the running task
    fun setActiveIndex(index: Int) {
        activeIndex = index
        progressMap.clear()
        notifyDataSetChanged()
    }

    // Called every 100 ms by MainActivity to push the progress bar forward
    fun setProgress(index: Int, progress: Float) {
        progressMap[index] = progress
        viewMap[index]?.let { updateProgress(progress, it) }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_task, parent, false)

        // Cache the view so setProgress() can reach it without a list scan
        viewMap[position] = view

        val task = tasks[position]

        view.findViewById<TextView>(R.id.task_label).text = task.label
        view.findViewById<TextView>(R.id.task_duration).text = formatDuration(task.duration)

        // Restore progress (or reset to 0 for non-active rows)
        val p = if (position == activeIndex) progressMap[position] ?: 0f else 0f
        updateProgress(p, view)

        view.setOnLongClickListener {
            showEditDialog(task, position)
            true
        }

        return view
    }

    // ── Internal helpers ────────────────────────────────────────

    private fun updateProgress(progress: Float, itemView: View) {
        val container = itemView.findViewById<View>(R.id.app_row_container)
        val progressView = itemView.findViewById<View>(R.id.progress_bg)

        container.post {
            val totalWidth = container.width
            if (totalWidth > 0) {
                progressView.layoutParams.width = (totalWidth * progress).toInt()
                progressView.requestLayout()
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        return if (seconds >= 60) {
            val m = seconds / 60
            val s = seconds % 60
            if (s == 0) "${m}m" else "${m}m ${s}s"
        } else {
            "${seconds}s"
        }
    }

    private fun showEditDialog(task: Task, position: Int) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_task_edit, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val labelInput = dialogView.findViewById<EditText>(R.id.label)
        val durationInput = dialogView.findViewById<EditText>(R.id.duration)
        val deleteButton = dialogView.findViewById<WdButton>(R.id.delete_button)
        val saveButton = dialogView.findViewById<WdButton>(R.id.add_button)
        val cancelButton = dialogView.findViewById<WdButton>(R.id.cancel_button)

        labelInput.setText(task.label)
        durationInput.setText(task.duration.toString())

        deleteButton.setOnClickListener { showDeleteDialog(task, position, dialog) }

        saveButton.setOnClickListener {
            val newLabel = labelInput.text.toString().trim()
            val newDuration = durationInput.text.toString().toIntOrNull() ?: task.duration

            if (newLabel.isEmpty()) {
                labelInput.error = "Label cannot be empty"
                return@setOnClickListener
            }

            val values = ContentValues().apply {
                put("label", newLabel)
                put("duration", newDuration)
            }
            db.update("tasks", values, "id = ?", arrayOf(task.id.toString()))

            task.label = newLabel
            task.duration = newDuration
            notifyDataSetChanged()
            dialog.dismiss()
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDeleteDialog(task: Task, position: Int, parentDialog: AlertDialog) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_task_delete, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialogView.findViewById<WdButton>(R.id.delete_button).setOnClickListener {
            db.delete("workout_steps", "task_id = ?", arrayOf(task.id.toString()))
            db.delete("tasks", "id = ?", arrayOf(task.id.toString()))
            tasks.removeAt(position)
            viewMap.remove(position)
            notifyDataSetChanged()
            dialog.dismiss()
            parentDialog.dismiss()
        }

        dialogView.findViewById<WdButton>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
